package me.bmax.apatch.util

import kotlinx.coroutines.suspendCancellableCoroutine
import me.bmax.apatch.Natives
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** The kernel rejected [Natives.su] (stale superkey / untrusted caller). */
class SuUnavailableException : IllegalStateException("Natives.su() was rejected")

/**
 * The only place in the app allowed to call [Natives.su].
 *
 * `su()` elevates the **calling thread** to uid 0 for the rest of the process, and
 * KernelPatch authorises supercalls by the caller thread's uid. A shared dispatcher
 * worker (`Dispatchers.IO`, `viewModelScope`, ...) must therefore never be elevated:
 * it stops being the trusted manager uid and every later grant/revoke issued from it
 * is rejected. Each call runs on a fresh thread that dies afterwards, taking the
 * elevation with it.
 *
 * Only put privileged file I/O in [block]. Supercalls issued from this thread are
 * not authorised, because uid 0 is not the manager uid.
 */
object RootIo {
    const val THREAD_NAME = "apatch-root-io"

    /** Blocking variant for callers that already own a background thread. */
    fun <T> withRootBlocking(block: () -> T): T {
        var outcome: Result<T>? = null
        thread(name = THREAD_NAME, isDaemon = true) {
            outcome = runCatching {
                if (!Natives.su(0, null)) throw SuUnavailableException()
                block()
            }
        }.join()
        return checkNotNull(outcome) { "root thread did not run" }.getOrThrow()
    }

    /** Suspending variant; the block still completes if the caller is cancelled. */
    suspend fun <T> withRoot(block: () -> T): T = suspendCancellableCoroutine { cont ->
        thread(name = THREAD_NAME, isDaemon = true) {
            val outcome = runCatching {
                if (!Natives.su(0, null)) throw SuUnavailableException()
                block()
            }
            if (cont.isActive) {
                try {
                    outcome.fold(cont::resume, cont::resumeWithException)
                } catch (_: Throwable) {
                    // Cancelled while the block ran; the side effects are applied.
                }
            }
        }
    }
}
