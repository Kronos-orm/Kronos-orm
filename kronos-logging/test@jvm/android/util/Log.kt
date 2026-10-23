package android.util

object Log {
    var lastTag: String? = null
    var lastPriority: Int? = null
    var lastMethod: String? = null
    var lastMessage: String? = null
    var lastThrowable: Throwable? = null

    @JvmStatic
    fun reset() {
        lastTag = null
        lastPriority = null
        lastMethod = null
        lastMessage = null
        lastThrowable = null
    }

    @JvmStatic
    fun isLoggable(tag: String, priority: Int): Boolean {
        lastTag = tag
        lastPriority = priority
        return true
    }

    @JvmStatic
    fun v(tag: String, message: String, throwable: Throwable?): Int = record("v", tag, message, throwable)

    @JvmStatic
    fun d(tag: String, message: String, throwable: Throwable?): Int = record("d", tag, message, throwable)

    @JvmStatic
    fun i(tag: String, message: String, throwable: Throwable?): Int = record("i", tag, message, throwable)

    @JvmStatic
    fun w(tag: String, message: String, throwable: Throwable?): Int = record("w", tag, message, throwable)

    @JvmStatic
    fun e(tag: String, message: String, throwable: Throwable?): Int = record("e", tag, message, throwable)

    private fun record(method: String, tag: String, message: String, throwable: Throwable?): Int {
        lastMethod = method
        lastTag = tag
        lastMessage = message
        lastThrowable = throwable
        return 0
    }
}
