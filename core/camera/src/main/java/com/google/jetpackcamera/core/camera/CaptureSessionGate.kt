/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.jetpackcamera.core.camera

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.tracing.traceAsync
import kotlin.coroutines.coroutineContext
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "CaptureSessionGate"

/** Async trace section spanning the time a session restart waits for in-flight captures. */
private const val AWAIT_CAPTURES_BEFORE_RESTART_TRACE = "JCA Await Captures Before Restart"

/**
 * Default time to wait for in-flight captures to finish before a camera session restarts anyway.
 */
internal const val DEFAULT_CAPTURE_IDLE_TIMEOUT_MILLIS = 10_000L

/**
 * Default time a capture request waits for a camera session that can accept it.
 */
internal const val DEFAULT_CAPTURE_ACQUIRE_TIMEOUT_MILLIS = 10_000L

/**
 * Coordinates still captures with camera session restarts.
 *
 * Stopping a camera session aborts any capture that is still in flight. To prevent that:
 * - Before a session restarts, the session calls [closeAndAwaitIdle]. This stops new captures from
 *   starting on the session that is about to stop, waits for captures already in flight to
 *   finish, and returns the [Restart] that identifies the session about to start.
 * - Once the new session is bound, it calls [open] with that [Restart] and the capture target it
 *   provides, or `null` if it provides none. The gate stays open until that session ends or the
 *   next restart begins.
 * - [withCapture] waits while no session is bound and then runs the capture on the newly bound
 *   target, so a capture requested during a restart is taken by the new session instead of being
 *   aborted with the old one.
 *
 * @param T The capture target provided by a bound session.
 * @param onInFlightCapturesChanged Called with `+1` when a capture starts and `-1` when it ends.
 * @param onRestartingChanged Called with `true` when a session restart begins, from which point
 * captures wait for the next session, and with `false` once that session is bound.
 * @param idleTimeoutMillis The maximum time [closeAndAwaitIdle] waits for in-flight captures.
 * @param acquireTimeoutMillis The maximum time [withCapture] waits for a session to accept a capture.
 */
internal class CaptureSessionGate<T : Any>(
    private val onInFlightCapturesChanged: (delta: Int) -> Unit = {},
    private val onRestartingChanged: (isRestarting: Boolean) -> Unit = {},
    private val idleTimeoutMillis: Long = DEFAULT_CAPTURE_IDLE_TIMEOUT_MILLIS,
    private val acquireTimeoutMillis: Long = DEFAULT_CAPTURE_ACQUIRE_TIMEOUT_MILLIS
) {
    /**
     * Identifies one session restart. [closeAndAwaitIdle] returns it when the restart begins, and
     * the session that the restart starts passes it to [open].
     */
    class Restart internal constructor(internal val generation: Int)

    private sealed interface GateState<out T> {
        /** The number of restarts that had begun when the gate entered this state. */
        val generation: Int

        /**
         * No session is bound: one is starting or restarting, or the camera is stopped. Captures
         * wait until a session is bound.
         */
        data class Switching(override val generation: Int) : GateState<Nothing>

        /** The bound session does not provide a capture target. */
        class NoCapture(override val generation: Int) : GateState<Nothing>

        /** The bound session accepts captures on [target]. */
        class Ready<T>(override val generation: Int, val target: T) : GateState<T>
    }

    private val mutex = Mutex()
    private val state = MutableStateFlow<GateState<T>>(GateState.Switching(generation = 0))
    private val _inFlightCaptures = MutableStateFlow(0)
    private val traceCookie = atomic(0)

    /** The number of captures currently in flight. */
    @VisibleForTesting
    val inFlightCaptures: StateFlow<Int> = _inFlightCaptures.asStateFlow()

    /**
     * The capture target of the bound session, or `null` if no session is bound or the bound
     * session provides no capture target.
     */
    val currentTarget: T?
        get() = (state.value as? GateState.Ready)?.target

    /**
     * Marks the session started by [restart] as bound: able to accept captures on [target], or
     * providing no capture target if [target] is `null`.
     *
     * Call this from the session's coroutine. The gate stays open until that coroutine completes,
     * which is when the session ends, or until the next restart begins.
     *
     * A call for a [restart] that a newer restart has replaced is ignored, so that a session that
     * is about to stop cannot reopen the gate.
     */
    suspend fun open(restart: Restart, target: T?) {
        val session = coroutineContext.job
        val bound = mutex.withLock {
            // An uncontended lock does not check for cancellation. A cancelled session is being
            // torn down, so it must not accept captures.
            session.ensureActive()
            if (state.value.generation != restart.generation) {
                Log.d(TAG, "Ignoring open() from a session that a newer restart has replaced")
                return
            }
            val bound = if (target != null) {
                GateState.Ready(restart.generation, target)
            } else {
                GateState.NoCapture(restart.generation)
            }
            state.value = bound
            onRestartingChanged(false)
            bound
        }
        // The session can end without a restart, such as when the camera stops. Close the gate
        // then, unless a newer restart or session has changed its state already.
        session.invokeOnCompletion {
            state.compareAndSet(bound, GateState.Switching(bound.generation))
        }
    }

    /**
     * Begins a session restart. Stops new captures from starting on the current session, then
     * waits up to [idleTimeoutMillis] for captures already in flight to finish.
     *
     * Call this before stopping a session.
     *
     * @return The [Restart] to pass to [open] once the next session is bound.
     */
    suspend fun closeAndAwaitIdle(): Restart {
        val restart = mutex.withLock {
            val generation = state.value.generation + 1
            state.value = GateState.Switching(generation)
            onRestartingChanged(true)
            Restart(generation)
        }
        // Most restarts have nothing to wait for; only trace the ones that do.
        if (_inFlightCaptures.value > 0) {
            traceAsync(AWAIT_CAPTURES_BEFORE_RESTART_TRACE, traceCookie.incrementAndGet()) {
                val idle = withTimeoutOrNull(idleTimeoutMillis) {
                    _inFlightCaptures.first { it == 0 }
                }
                if (idle == null) {
                    Log.w(
                        TAG,
                        "${_inFlightCaptures.value} capture(s) still in flight after " +
                            "${idleTimeoutMillis}ms; restarting the camera session anyway"
                    )
                }
            }
        }
        return restart
    }

    /**
     * Runs [block] on the capture target of the bound session, counting it as an in-flight capture
     * until it returns.
     *
     * If no session is bound, this waits up to [acquireTimeoutMillis] for one to be bound.
     *
     * @throws IllegalStateException if the bound session provides no capture target, or no session
     * is bound within [acquireTimeoutMillis].
     */
    suspend fun <R> withCapture(block: suspend (T) -> R): R {
        val target = acquire()
        try {
            return block(target)
        } finally {
            releaseCapture()
        }
    }

    private suspend fun acquire(): T {
        var acquired = false
        try {
            return withTimeoutOrNull(acquireTimeoutMillis) {
                var target: T? = null
                while (target == null) {
                    state.first { it !is GateState.Switching }
                    target = mutex.withLock {
                        coroutineContext.ensureActive()
                        when (val current = state.value) {
                            is GateState.Ready -> {
                                _inFlightCaptures.update { it + 1 }
                                onInFlightCapturesChanged(1)
                                acquired = true
                                current.target
                            }

                            is GateState.NoCapture -> throw IllegalStateException(
                                "The camera session does not support image capture"
                            )

                            // The gate closed again before the lock was acquired; wait for the next
                            // session.
                            is GateState.Switching -> null
                        }
                    }
                }
                target
            } ?: throw IllegalStateException(
                "Timed out after ${acquireTimeoutMillis}ms waiting for a camera session to " +
                    "accept a capture"
            )
        } catch (t: Throwable) {
            if (acquired) {
                releaseCapture()
            }
            throw t
        }
    }

    private fun releaseCapture() {
        _inFlightCaptures.update { (it - 1).coerceAtLeast(0) }
        onInFlightCapturesChanged(-1)
    }
}
