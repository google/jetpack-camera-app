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
import androidx.tracing.traceAsync
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
 *   starting on the session that is about to stop, and waits for captures already in flight to
 *   finish.
 * - Once the new session is bound, it calls [open] with the capture target it provides, or `null`
 *   if it provides none.
 * - [withCapture] waits while a restart is in progress and then runs the capture on the newly bound
 *   target, so a capture requested during a restart is taken by the new session instead of being
 *   aborted with the old one.
 *
 * @param T The capture target provided by a bound session.
 * @param onInFlightCapturesChanged Called with `+1` when a capture starts and `-1` when it ends.
 * @param idleTimeoutMillis The maximum time [closeAndAwaitIdle] waits for in-flight captures.
 * @param acquireTimeoutMillis The maximum time [withCapture] waits for a session to accept a capture.
 */
internal class CaptureSessionGate<T : Any>(
    private val onInFlightCapturesChanged: (delta: Int) -> Unit = {},
    private val idleTimeoutMillis: Long = DEFAULT_CAPTURE_IDLE_TIMEOUT_MILLIS,
    private val acquireTimeoutMillis: Long = DEFAULT_CAPTURE_ACQUIRE_TIMEOUT_MILLIS
) {
    private sealed interface GateState<out T> {
        /** A session is starting or restarting. Captures wait until it is bound. */
        object Switching : GateState<Nothing>

        /** The bound session does not provide a capture target. */
        object NoCapture : GateState<Nothing>

        /** The bound session accepts captures on [target]. */
        class Ready<T>(val target: T) : GateState<T>
    }

    private val mutex = Mutex()
    private val state = MutableStateFlow<GateState<T>>(GateState.Switching)
    private val _inFlightCaptures = MutableStateFlow(0)
    private val traceCookie = AtomicInteger()

    /** The number of captures currently in flight. */
    val inFlightCaptures: StateFlow<Int> = _inFlightCaptures.asStateFlow()

    /**
     * The capture target of the bound session, or `null` if a session restart is in progress or
     * the bound session provides no capture target.
     */
    val currentTarget: T?
        get() = (state.value as? GateState.Ready)?.target

    /**
     * Marks a newly bound session as able to accept captures on [target], or as providing no
     * capture target if [target] is `null`.
     */
    suspend fun open(target: T?) {
        mutex.withLock {
            state.value = if (target != null) GateState.Ready(target) else GateState.NoCapture
        }
    }

    /**
     * Stops new captures from starting on the current session, then waits up to
     * [idleTimeoutMillis] for captures already in flight to finish.
     *
     * Call this before stopping a session.
     */
    suspend fun closeAndAwaitIdle() {
        traceAsync(AWAIT_CAPTURES_BEFORE_RESTART_TRACE, traceCookie.incrementAndGet()) {
            mutex.withLock { state.value = GateState.Switching }
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

    /**
     * Runs [block] on the capture target of the bound session, counting it as an in-flight capture
     * until it returns.
     *
     * If a session restart is in progress, this waits up to [acquireTimeoutMillis] for the new
     * session to be bound.
     *
     * @throws IllegalStateException if the bound session provides no capture target, or no session
     * is bound within [acquireTimeoutMillis].
     */
    suspend fun <R> withCapture(block: suspend (T) -> R): R {
        val target = acquire()
        try {
            return block(target)
        } finally {
            _inFlightCaptures.update { (it - 1).coerceAtLeast(0) }
            onInFlightCapturesChanged(-1)
        }
    }

    private suspend fun acquire(): T = withTimeoutOrNull(acquireTimeoutMillis) {
        var target: T? = null
        while (target == null) {
            state.first { it !is GateState.Switching }
            target = mutex.withLock {
                when (val current = state.value) {
                    is GateState.Ready -> {
                        _inFlightCaptures.update { it + 1 }
                        onInFlightCapturesChanged(1)
                        current.target
                    }

                    GateState.NoCapture -> throw IllegalStateException(
                        "The camera session does not support image capture"
                    )

                    // The gate closed again before the lock was acquired; wait for the next
                    // session.
                    GateState.Switching -> null
                }
            }
        }
        target
    } ?: throw IllegalStateException(
        "Timed out after ${acquireTimeoutMillis}ms waiting for a camera session to accept a capture"
    )
}
