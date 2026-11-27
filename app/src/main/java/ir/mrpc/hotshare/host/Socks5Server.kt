package ir.mrpc.hotshare.host

import java.io.*
import java.net.*
import java.security.MessageDigest
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * سرور SOCKS5 با احراز هویت username/password (RFC 1929) — رمز = کد ۸ رقمی.
 * پشتیبانی از CONNECT (TCP) و UDP ASSOCIATE. پس از ۵ رمز اشتباه، آی‌پی یک دقیقه بلاک می‌شود.
 */
/** A phone that is (or was a moment ago) using this host. */
class ClientInfo(val ip: String, val name: String, val since: Long, val active: Int)

class Socks5Server(private val password: String, private val port: Int) {
    val up = AtomicLong()
    val down = AtomicLong()

    private class Rec(@Volatile var name: String, val since: Long) {
        @Volatile var last = 0L
        val active = AtomicInteger()
    }
    private val recs = ConcurrentHashMap<String, Rec>()
    private val fails = ConcurrentHashMap<String, Pair<Int, Long>>()
    private val open: MutableSet<Closeable> = Collections.newSetFromMap(ConcurrentHashMap())
    private val pool = Executors.newCachedThreadPool()
    @Volatile private var server: ServerSocket? = null

    /**
     * Receivers poll the host every ~2 s while connected, so "seen in the last 9 s" is a steadier signal
     * than "has an open socket right now" (which flickers between web requests).
     */
    fun clients(): List<ClientInfo> {
        val t = System.currentTimeMillis()
        recs.entries.removeIf { it.value.active.get() == 0 && t - it.value.last > 30_000 }
        return recs.filter { it.value.active.get() > 0 || t - it.value.last < 9_000 }
            .map { ClientInfo(it.key, it.value.name, it.value.since, it.value.active.get()) }
            .sortedBy { it.since }
    }

    fun clientCount() = clients().size

    fun start() {
        val s = ServerSocket()
        s.reuseAddress = true
        s.bind(InetSocketAddress(port))
        server = s
        pool.execute {
            while (!s.isClosed) {
                val c = try { s.accept() } catch (_: Exception) { break }
                open.add(c)
                pool.execute {
                    try { handle(c) } catch (_: Exception) {} finally { open.remove(c); runCatching { c.close() } }
                }
            }
        }
    }

    fun stop() {
        runCatching { server?.close() }
        open.toList().forEach { runCatching { it.close() } }
        pool.shutdownNow()
    }

    // ---------- helpers ----------
    private fun now() = System.currentTimeMillis()
    private fun locked(ip: String) = (fails[ip]?.second ?: 0L) > now()
    private fun fail(ip: String) {
        val n = (fails[ip]?.first ?: 0) + 1
        fails[ip] = if (n >= 5) 0 to now() + 60_000 else n to 0L
    }

    private fun reply(o: OutputStream, code: Int, addr: ByteArray = ByteArray(4), port: Int = 0) {
        o.write(byteArrayOf(5, code.toByte(), 0, 1) + addr + byteArrayOf((port shr 8).toByte(), port.toByte()))
        o.flush()
    }

    private fun readAddr(i: DataInputStream): String = when (i.readUnsignedByte()) {
        1 -> InetAddress.getByAddress(ByteArray(4).also { i.readFully(it) }).hostAddress!!
        3 -> String(ByteArray(i.readUnsignedByte()).also { i.readFully(it) }, Charsets.UTF_8)
        4 -> InetAddress.getByAddress(ByteArray(16).also { i.readFully(it) }).hostAddress!!
        else -> throw IOException("bad atyp")
    }

    private fun copy(i: InputStream, o: OutputStream, counter: AtomicLong) {
        val buf = ByteArray(16 * 1024)
        while (true) {
            val n = i.read(buf)
            if (n < 0) break
            o.write(buf, 0, n); o.flush()
            counter.addAndGet(n.toLong())
        }
    }

    // ---------- protocol ----------
    private fun handle(c: Socket) {
        c.tcpNoDelay = true
        val ip = (c.remoteSocketAddress as InetSocketAddress).address.hostAddress ?: return
        if (locked(ip)) return
        c.soTimeout = 15_000
        val i = DataInputStream(BufferedInputStream(c.getInputStream()))
        val o = c.getOutputStream()

        if (i.readUnsignedByte() != 5) return
        val methods = ByteArray(i.readUnsignedByte()).also { i.readFully(it) }
        if (!methods.contains(2.toByte())) { o.write(byteArrayOf(5, 0xFF.toByte())); return }
        o.write(byteArrayOf(5, 2)); o.flush()

        if (i.readUnsignedByte() != 1) return
        val user = String(ByteArray(i.readUnsignedByte()).also { i.readFully(it) }, Charsets.UTF_8)   // نام دستگاه گیرنده
        val pw = ByteArray(i.readUnsignedByte()).also { i.readFully(it) }
        if (!MessageDigest.isEqual(pw, password.toByteArray())) {
            o.write(byteArrayOf(1, 1)); o.flush(); fail(ip); return
        }
        fails.remove(ip)
        o.write(byteArrayOf(1, 0)); o.flush()

        val name = ir.mrpc.hotshare.core.Pairing.nameFromUser(user)
        val t0 = System.currentTimeMillis()
        val rec = recs.getOrPut(ip) { Rec(name ?: ip, t0) }
        if (name != null) rec.name = name
        rec.last = t0
        rec.active.incrementAndGet()
        try {
            i.readUnsignedByte()                       // VER
            val cmd = i.readUnsignedByte()
            i.readUnsignedByte()                       // RSV
            val host = readAddr(i)
            val dport = i.readUnsignedShort()
            c.soTimeout = 0
            when (cmd) {
                1 -> tcp(c, i, o, host, dport)
                3 -> udp(c, ip, i, o)
                else -> reply(o, 7)
            }
        } finally {
            rec.active.decrementAndGet()
            rec.last = System.currentTimeMillis()
        }
    }

    private fun tcp(c: Socket, i: InputStream, o: OutputStream, host: String, port: Int) {
        val r = Socket()
        open.add(r)
        try {
            try {
                r.connect(InetSocketAddress(host, port), 10_000)
            } catch (_: Exception) {
                reply(o, 4); return
            }
            r.tcpNoDelay = true
            reply(o, 0)
            pool.execute {
                try { copy(r.getInputStream(), o, down) } catch (_: Exception) {}
                runCatching { r.close() }; runCatching { c.close() }
            }
            try { copy(i, r.getOutputStream(), up) } catch (_: Exception) {}
        } finally {
            open.remove(r); runCatching { r.close() }; runCatching { c.close() }
        }
    }

    private fun udp(c: Socket, clientIp: String, i: InputStream, o: OutputStream) {
        val relay = DatagramSocket(0)
        open.add(relay)
        val local = (c.localAddress as? Inet4Address)?.address ?: ByteArray(4)
        reply(o, 0, local, relay.localPort)

        pool.execute {
            var client: InetSocketAddress? = null
            val buf = ByteArray(65535)
            try {
                while (!relay.isClosed) {
                    val p = DatagramPacket(buf, buf.size)
                    relay.receive(p)
                    if (p.address.hostAddress == clientIp) {
                        val a = InetSocketAddress(p.address, p.port)
                        if (client == null) client = a else if (client != a) continue
                        val d = p.data; var pos = p.offset
                        if (p.length < 10 || d[pos + 2] != 0.toByte()) continue   // FRAG ≠ 0 پشتیبانی نمی‌شود
                        val atyp = d[pos + 3].toInt(); pos += 4
                        val host = when (atyp) {
                            1 -> { val h = InetAddress.getByAddress(d.copyOfRange(pos, pos + 4)); pos += 4; h }
                            3 -> { val l = d[pos].toInt() and 0xff; val n = String(d, pos + 1, l); pos += 1 + l; InetAddress.getByName(n) }
                            4 -> { val h = InetAddress.getByAddress(d.copyOfRange(pos, pos + 16)); pos += 16; h }
                            else -> continue
                        }
                        val dport = ((d[pos].toInt() and 0xff) shl 8) or (d[pos + 1].toInt() and 0xff); pos += 2
                        val end = p.offset + p.length
                        relay.send(DatagramPacket(d.copyOfRange(pos, end), end - pos, host, dport))
                        up.addAndGet((end - pos).toLong())
                    } else {
                        val to = client ?: continue
                        val src = p.address.address
                        val head = byteArrayOf(0, 0, 0, if (src.size == 4) 1 else 4) + src +
                                byteArrayOf((p.port shr 8).toByte(), p.port.toByte())
                        val out = head + p.data.copyOfRange(p.offset, p.offset + p.length)
                        relay.send(DatagramPacket(out, out.size, to))
                        down.addAndGet(p.length.toLong())
                    }
                }
            } catch (_: Exception) {}
        }
        // تا زمانی که اتصال TCP کنترلی باز است، ارتباط UDP هم زنده است
        try { while (i.read() != -1) { /* wait */ } } catch (_: Exception) {}
        open.remove(relay); relay.close()
    }
}
