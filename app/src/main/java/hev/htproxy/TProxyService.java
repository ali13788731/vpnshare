package hev.htproxy;

/** پل JNI به hev-socks5-tunnel. نام پکیج و کلاس باید با -DPKGNAME / -DCLSNAME در Application.mk یکی باشد. */
public final class TProxyService {
    static { System.loadLibrary("hev-socks5-tunnel"); }

    private TProxyService() {}

    public static native void TProxyStartService(String configPath, int fd);
    public static native void TProxyStopService();
    public static native long[] TProxyGetStats();
}
