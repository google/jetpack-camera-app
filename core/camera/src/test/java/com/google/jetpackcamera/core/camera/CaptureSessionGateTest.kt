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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureSessionGateTest {

    private val testScope = TestScope(StandardTestDispatcher())
    private var inFlightCaptures = 0
    private val restartingChanges = mutableListOf<Boolean>()
    private val gate = CaptureSessionGate<String>(
        onInFlightCapturesChanged = { delta -> inFlightCaptures += delta },
        onRestartingChanged = { isRestarting -> restartingChanges += isRestarting },
        idleTimeoutMillis = IDLE_TIMEOUT_MILLIS,
        acquireTimeoutMillis = ACQUIRE_TIMEOUT_MILLIS
    )

    @Test
    fun withCapture_whenOpen_runsOnBoundTargetAndCountsInFlight() = testScope.runTest {
        bindSession(OLD_SESSION)
        val captureFinished = CompletableDeferred<Unit>()

        val capture = async {
            gate.withCapture { target ->
                captureFinished.await()
                target
            }
        }
        runCurrent()

        assertThat(inFlightCaptures).isEqualTo(1)
        assertThat(gate.inFlightCaptures.value).isEqualTo(1)

        captureFinished.complete(Unit)
        assertThat(capture.await()).isEqualTo(OLD_SESSION)
        assertThat(inFlightCaptures).isEqualTo(0)
        assertThat(gate.inFlightCaptures.value).isEqualTo(0)
    }

    @Test
    fun closeAndAwaitIdle_waitsForInFlightCaptureBeforeReturning() = testScope.runTest {
        bindSession(OLD_SESSION)
        val captureFinished = CompletableDeferred<Unit>()
        val capture = async {
            gate.withCapture { target ->
                captureFinished.await()
                target
            }
        }
        runCurrent()

        val restart = async { gate.closeAndAwaitIdle() }
        runCurrent()
        // The session must not stop while the capture is in flight.
        assertThat(restart.isCompleted).isFalse()

        captureFinished.complete(Unit)
        runCurrent()

        assertThat(capture.await()).isEqualTo(OLD_SESSION)
        assertThat(restart.isCompleted).isTrue()
    }

    @Test
    fun closeAndAwaitIdle_reportsRestartingUntilNextSessionIsBound() = testScope.runTest {
        bindSession(OLD_SESSION)
        restartingChanges.clear()
        val captureFinished = CompletableDeferred<Unit>()
        launch { gate.withCapture { captureFinished.await() } }
        runCurrent()

        val restart = async { gate.closeAndAwaitIdle() }
        runCurrent()

        // Reported as soon as the restart begins, while it still waits for the capture in flight.
        assertThat(restart.isCompleted).isFalse()
        assertThat(restartingChanges).containsExactly(true)

        captureFinished.complete(Unit)
        gate.open(restart.await(), NEW_SESSION)

        assertThat(restartingChanges).containsExactly(true, false).inOrder()
    }

    @Test
    fun open_withoutCaptureTarget_reportsRestartFinished() = testScope.runTest {
        val restart = gate.closeAndAwaitIdle()

        gate.open(restart, null)

        assertThat(restartingChanges).containsExactly(true, false).inOrder()
    }

    @Test
    fun open_forRestartThatNewerRestartReplaced_isIgnored() = testScope.runTest {
        val replacedRestart = gate.closeAndAwaitIdle()
        val latestRestart = gate.closeAndAwaitIdle()

        // The session of the replaced restart finishes binding after the newer restart began.
        gate.open(replacedRestart, OLD_SESSION)

        assertThat(gate.currentTarget).isNull()
        assertThat(restartingChanges).containsExactly(true, true)

        gate.open(latestRestart, NEW_SESSION)

        assertThat(gate.currentTarget).isEqualTo(NEW_SESSION)
    }

    @Test
    fun open_fromCancelledSession_doesNotOpen() = testScope.runTest {
        val restart = gate.closeAndAwaitIdle()

        // Like a session that finishes binding after it was cancelled.
        launch {
            cancel()
            gate.open(restart, OLD_SESSION)
        }
        runCurrent()

        assertThat(gate.currentTarget).isNull()
        assertThat(restartingChanges).containsExactly(true)
    }

    @Test
    fun open_whenSessionEnds_closesGateUntilNextSessionIsBound() = testScope.runTest {
        val session = launch {
            gate.open(gate.closeAndAwaitIdle(), OLD_SESSION)
            awaitCancellation()
        }
        runCurrent()
        assertThat(gate.currentTarget).isEqualTo(OLD_SESSION)

        // The camera stops without a restart.
        session.cancel()
        runCurrent()

        assertThat(gate.currentTarget).isNull()
        // A capture requested while the camera is stopped waits for the next session.
        val capture = async { gate.withCapture { target -> target } }
        runCurrent()
        assertThat(capture.isCompleted).isFalse()

        bindSession(NEW_SESSION)

        assertThat(capture.await()).isEqualTo(NEW_SESSION)
    }

    @Test
    fun open_whenSessionEndsAfterNextSessionIsBound_keepsGateOpen() = testScope.runTest {
        val oldSession = launch {
            gate.open(gate.closeAndAwaitIdle(), OLD_SESSION)
            awaitCancellation()
        }
        runCurrent()
        // The camera restarts, and the next session is bound before the old one finishes ending.
        bindSession(NEW_SESSION)

        oldSession.cancel()
        runCurrent()

        assertThat(gate.currentTarget).isEqualTo(NEW_SESSION)
    }

    @Test
    fun withCapture_requestedDuringRestart_runsOnNewSession() = testScope.runTest {
        bindSession(OLD_SESSION)
        val restart = gate.closeAndAwaitIdle()

        // A capture requested after the restart began must not go to the old session.
        val capture = async { gate.withCapture { target -> target } }
        runCurrent()
        assertThat(capture.isCompleted).isFalse()
        assertThat(inFlightCaptures).isEqualTo(0)

        gate.open(restart, NEW_SESSION)

        assertThat(capture.await()).isEqualTo(NEW_SESSION)
        assertThat(inFlightCaptures).isEqualTo(0)
    }

    @Test
    fun withCapture_whenSessionHasNoCaptureTarget_throwsWithoutWaiting() = testScope.runTest {
        bindSession(null)

        val result = runCatching { gate.withCapture { target -> target } }

        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(currentTime).isEqualTo(0)
        assertThat(inFlightCaptures).isEqualTo(0)
    }

    @Test
    fun withCapture_whenNoSessionBinds_throwsAfterAcquireTimeout() = testScope.runTest {
        val capture = async { runCatching { gate.withCapture { target -> target } } }

        advanceTimeBy(ACQUIRE_TIMEOUT_MILLIS - 1)
        runCurrent()
        assertThat(capture.isCompleted).isFalse()

        advanceTimeBy(2)
        runCurrent()
        assertThat(capture.await().exceptionOrNull())
            .isInstanceOf(IllegalStateException::class.java)
        assertThat(inFlightCaptures).isEqualTo(0)
    }

    @Test
    fun closeAndAwaitIdle_whenCaptureNeverFinishes_returnsAfterIdleTimeout() = testScope.runTest {
        bindSession(OLD_SESSION)
        val neverFinishes = CompletableDeferred<Unit>()
        val capture = launch { gate.withCapture { neverFinishes.await() } }
        runCurrent()

        val restart = async { gate.closeAndAwaitIdle() }

        advanceTimeBy(IDLE_TIMEOUT_MILLIS - 1)
        runCurrent()
        assertThat(restart.isCompleted).isFalse()

        advanceTimeBy(2)
        runCurrent()
        assertThat(restart.isCompleted).isTrue()
        capture.cancel()
    }

    @Test
    fun withCapture_whenBlockThrows_releasesCapture() = testScope.runTest {
        bindSession(OLD_SESSION)

        val result = runCatching {
            gate.withCapture { throw IllegalArgumentException("capture failed") }
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(inFlightCaptures).isEqualTo(0)
        assertThat(gate.inFlightCaptures.value).isEqualTo(0)
    }

    @Test
    fun withCapture_whenCancelledDuringBlock_releasesCapture() = testScope.runTest {
        bindSession(OLD_SESSION)
        val captureJob = launch {
            gate.withCapture {
                awaitCancellation()
            }
        }
        runCurrent()
        assertThat(inFlightCaptures).isEqualTo(1)

        captureJob.cancel()
        runCurrent()

        assertThat(inFlightCaptures).isEqualTo(0)
        assertThat(gate.inFlightCaptures.value).isEqualTo(0)
    }

    @Test
    fun withCapture_whenCancelledDuringAcquire_doesNotLeakInFlightCount() = testScope.runTest {
        var count = 0
        var captureJob: Job? = null
        val raceGate = CaptureSessionGate<String>(
            onInFlightCapturesChanged = { delta ->
                count += delta
                if (delta > 0) {
                    // Simulate cancellation arriving after acquire() increments the count inside
                    // mutex.withLock, before withTimeoutOrNull returns to withCapture().
                    captureJob?.cancel()
                }
            },
            idleTimeoutMillis = IDLE_TIMEOUT_MILLIS,
            acquireTimeoutMillis = ACQUIRE_TIMEOUT_MILLIS
        )
        raceGate.open(raceGate.closeAndAwaitIdle(), OLD_SESSION)

        var blockEntered = false
        captureJob = launch {
            raceGate.withCapture {
                blockEntered = true
            }
        }
        runCurrent()

        assertThat(blockEntered).isFalse()
        assertThat(count).isEqualTo(0)
        assertThat(raceGate.inFlightCaptures.value).isEqualTo(0)
    }

    @Test
    fun currentTarget_reflectsGateState() = testScope.runTest {
        assertThat(gate.currentTarget).isNull()

        bindSession(OLD_SESSION)
        assertThat(gate.currentTarget).isEqualTo(OLD_SESSION)

        val restart = gate.closeAndAwaitIdle()
        assertThat(gate.currentTarget).isNull()

        gate.open(restart, null)
        assertThat(gate.currentTarget).isNull()
    }

    /** Restarts the gate and binds a session that provides [target], until the test ends. */
    private suspend fun bindSession(target: String?) {
        gate.open(gate.closeAndAwaitIdle(), target)
    }

    private companion object {
        const val OLD_SESSION = "old session"
        const val NEW_SESSION = "new session"
        const val IDLE_TIMEOUT_MILLIS = 5_000L
        const val ACQUIRE_TIMEOUT_MILLIS = 3_000L
    }
}
