package gg.essential.universal.render

import org.jetbrains.annotations.ApiStatus.NonExtendable

@NonExtendable
interface UGpuFence : AutoCloseable {
    /**
     * Waits for this fence to be completed.
     * Waits at most [timeoutNs] nanoseconds. Returns `true` when the fence was completed, `false` when the timeout has
     * expired.
     * When [timeoutNs] is zero, returns the state of the fence immediately, without blocking.
     *
     * Note that a fence cannot be blocked on until the frame it was created in has been submitted.
     * As of 26.2, such a call will throw an exception. On older version it depends on the OpenGL implementation.
     */
    fun awaitCompletion(timeoutNs: Long): Boolean
}
