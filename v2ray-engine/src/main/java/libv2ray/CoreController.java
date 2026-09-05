package libv2ray;

public class CoreController {
    private final CoreCallbackHandler handler;
    private boolean running = false;

    public CoreController(CoreCallbackHandler handler) {
        this.handler = handler;
    }

    public boolean isRunning() {
        return running;
    }

    public void startLoop(String json, int tunFd) {
        running = true;
        if (handler != null) {
            handler.startup();
        }
    }

    public void stopLoop() {
        running = false;
        if (handler != null) {
            handler.shutdown();
        }
    }

    public long queryStats(String tag, String link) {
        return 0L;
    }

    public long measureDelay(String url) {
        return -1L;
    }
}