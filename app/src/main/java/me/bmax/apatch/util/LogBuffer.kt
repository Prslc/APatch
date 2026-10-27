package me.bmax.apatch.util

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Accumulates line-oriented process output and publishes a snapshot at most once per
 * [intervalMs]. [flush] publishes the final contents immediately.
 */
class LogBuffer(
    private val scope: CoroutineScope,
    private val intervalMs: Long = 100L,
    private val maxLength: Int? = null,
    private val onSnapshot: (String) -> Unit,
) {
    private val builder = StringBuilder()
    private var lastEmitAt = 0L
    private var pendingJob: Job? = null

    fun append(line: String) {
        var snapshot: String? = null
        synchronized(this) {
            builder.append(line).append('\n')
            trimLocked()

            val now = SystemClock.elapsedRealtime()
            if (now - lastEmitAt >= intervalMs) {
                lastEmitAt = now
                snapshot = builder.toString()
            } else if (pendingJob?.isActive != true) {
                pendingJob = scope.launch {
                    delay(intervalMs.milliseconds)
                    emit()
                }
            }
        }
        snapshot?.let(onSnapshot)
    }

    /** Clears the buffer and appends [line] in a single immediate update. */
    fun clearAndAppend(line: String) {
        synchronized(this) {
            builder.clear()
            if (line.isNotEmpty()) {
                builder.append(line).append('\n')
            }
        }
        emit()
    }

    fun clear() {
        synchronized(this) {
            builder.clear()
        }
        emit()
    }

    fun snapshot(): String = synchronized(this) { builder.toString() }

    /** Publishes the latest buffer immediately; call when the process finishes. */
    fun flush() = emit()

    private fun emit() {
        val snapshot = synchronized(this) {
            lastEmitAt = SystemClock.elapsedRealtime()
            builder.toString()
        }
        onSnapshot(snapshot)
    }

    private fun trimLocked() {
        val limit = maxLength ?: return
        if (builder.length > limit) {
            builder.delete(0, builder.length - limit)
        }
    }
}
