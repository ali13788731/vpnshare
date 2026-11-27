package hev.htproxy;

/**
 * JNI bridge to hev-socks5-tunnel (>= 2.7).
 * The package/class name must match -DPKGNAME / -DCLSNAME in Application.mk, and the native
 * signatures must match the table registered in hev-jni.c exactly (boolean returns + TProxyIsRunning),
 * otherwise JNI_OnLoad fails and the process can abort.
 */
public final class TProxyService {
    static { System.loadLibrary("hev-socks5-tunnel"); }

    private TProxyService() {}

    public static native boolean TProxyStartService(String configPath, int fd);
    public static native boolean TProxyStopService();
    public static native boolean TProxyIsRunning();
    public static native long[] TProxyGetStats();
}
