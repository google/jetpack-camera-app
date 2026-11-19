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
import kotlinx.coroutines.async
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
    private val gate = CaptureSessionGate<String>(
        onInFlightCapturesChanged = { delta -> inFlightCaptures += delta },
        idleTimeoutMillis = IDLE_TIMEOUT_MILLIS,
        acquireTimeoutMillis = ACQUIRE_TIMEOUT_MILLIS
    )

    @Test
    fun withCapture_whenOpen_runsOnBoundTargetAndCountsInFlight() = testScope.runTest {
        gate.open(OLD_SESSION)
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
        gate.open(OLD_SESSION)
        val captureFinished = CompletableDeferred<Unit>()
        val capture = async {
            gate.withCapture { target ->
                captureFinished.await()
                target
            }
        }
        runCurrent()

        var restartAllowed = false
        launch {
            gate.closeAndAwaitIdle()
            restartAllowed = true
        }
        runCurrent()
        // The session must not stop while the capture is in flight.
        assertThat(restartAllowed).isFalse()

        captureFinished.complete(Unit)
        runCurrent()

        assertThat(capture.await()).isEqualTo(OLD_SESSION)
        assertThat(restartAllowed).isTrue()
    }

    @Test
    fun withCapture_requestedDuringRestart_runsOnNewSession() = testScope.runTest {
        gate.open(OLD_SESSION)
        gate.closeAndAwaitIdle()

        // A capture requested after the restart began must not go to the old session.
        val capture = async { gate.withCapture { target -> target } }
        runCurrent()
        assertThat(capture.isCompleted).isFalse()
        assertThat(inFlightCaptures).isEqualTo(0)

        gate.open(NEW_SESSION)

        assertThat(capture.await()).isEqualTo(NEW_SESSION)
        assertThat(inFlightCaptures).isEqualTo(0)
    }

    @Test
    fun withCapture_whenSessionHasNoCaptureTarget_throwsWithoutWaiting() = testScope.runTest {
        gate.open(null)

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
        gate.open(OLD_SESSION)
        val neverFinishes = CompletableDeferred<Unit>()
        val capture = launch { gate.withCapture { neverFinishes.await() } }
        runCurrent()

        var restartAllowed = false
        launch {
            gate.closeAndAwaitIdle()
            restartAllowed = true
        }

        advanceTimeBy(IDLE_TIMEOUT_MILLIS - 1)
        runCurrent()
        assertThat(restartAllowed).isFalse()

        advanceTimeBy(2)
        runCurrent()
        assertThat(restartAllowed).isTrue()
        capture.cancel()
    }

    @Test
    fun withCapture_whenBlockThrows_releasesCapture() = testScope.runTest {
        gate.open(OLD_SESSION)

        val result = runCatching {
            gate.withCapture { throw IllegalArgumentException("capture failed") }
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(inFlightCaptures).isEqualTo(0)
        assertThat(gate.inFlightCaptures.value).isEqualTo(0)
    }

    @Test
    fun currentTarget_reflectsGateState() = testScope.runTest {
        assertThat(gate.currentTarget).isNull()

        gate.open(OLD_SESSION)
        assertThat(gate.currentTarget).isEqualTo(OLD_SESSION)

        gate.closeAndAwaitIdle()
        assertThat(gate.currentTarget).isNull()

        gate.open(null)
        assertThat(gate.currentTarget).isNull()
    }

    private companion object {
        const val OLD_SESSION = "old session"
        const val NEW_SESSION = "new session"
        const val IDLE_TIMEOUT_MILLIS = 5_000L
        const val ACQUIRE_TIMEOUT_MILLIS = 3_000L
    }
}
