package libv2ray;

public class Libv2ray {
    public static void initCoreEnv(String assetPath, String deviceId) {
        // Stub: actual implementation in native AAR
    }

    public static String checkVersionX() {
        return "stub-1.0.0";
    }

    public static long measureOutboundDelay(String config, String testUrl) {
        return -1L;
    }

    public static CoreController newCoreController(CoreCallbackHandler handler) {
        return new CoreController(handler);
    }
}