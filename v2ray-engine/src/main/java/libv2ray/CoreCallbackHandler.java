package libv2ray;

public interface CoreCallbackHandler {
    long startup();
    long shutdown();
    long onEmitStatus(long code, String message);
}