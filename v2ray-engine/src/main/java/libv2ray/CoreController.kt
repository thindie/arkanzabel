package libv2ray

/** In-memory stub; real AAR provides JNI-backed controller. */
class CoreController {
  var isRunning: Boolean = false
    private set

  fun startLoop(config: String, tunFd: Int) {
    isRunning = true
  }

  fun stopLoop() {
    isRunning = false
  }

  fun queryStats(tag: String, link: String): Long = 0L

  fun measureDelay(url: String): Long = -1L
}
