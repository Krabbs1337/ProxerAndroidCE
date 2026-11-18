package com.github.anrwatchdog

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

/**
 * In-repo minimal replacement for `com.github.anrwatchdog:anrwatchdog` (jitpack-only).
 * Posts a tick to the main looper; if the tick is not processed within [timeoutMillis],
 * the ANR listener is invoked.
 */
class ANRWatchDog(
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val tick = AtomicLong(0)
    private val running = AtomicBoolean(false)
    private var anrListener: ((Throwable) -> Unit)? = null
    private var watcher: Thread? = null

    fun setANRListener(listener: (Throwable) -> Unit) = apply { anrListener = listener }

    @Synchronized
    fun start() {
        if (!running.compareAndSet(false, true)) return

        watcher =
            thread(isDaemon = true, name = "ANRWatchDog") {
                while (running.get()) {
                    val before = tick.get()
                    handler.post { tick.set(SystemClock.uptimeMillis()) }
                    Thread.sleep(timeoutMillis)

                    if (running.get() && tick.get() == before) {
                        anrListener?.invoke(ANRError("Main thread blocked for more than ${timeoutMillis}ms"))
                    }
                }
            }
    }

    @Synchronized
    fun stop() {
        running.set(false)
    }

    companion object {
        private const val DEFAULT_TIMEOUT_MILLIS = 5000L
    }
}

class ANRError(
    message: String,
) : RuntimeException(message)
