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
package com.google.jetpackcamera.ui.uistateadapter.capture.compound

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.core.camera.StillCaptureProgress
import com.google.jetpackcamera.core.camera.testing.FakeCameraSystem
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.CaptureSubModeId
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.MediaDescriptor
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureChromeVisibility
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureProgressUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureSubModeOption
import com.google.jetpackcamera.ui.uistate.capture.CaptureSubModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class CaptureLockTest {

    private val cameraState = MutableStateFlow(CameraState(isCameraRunning = true))

    private fun queueFull() = cameraState.value.copy(
        inFlightCapturesCount = 1,
        maxInFlightCaptures = 1
    )

    private fun queueEmpty() = cameraState.value.copy(inFlightCapturesCount = 0)

    @Test
    fun captureLock_whenQueueNotFull_isNone() = runTest {
        val locks = mutableListOf<CaptureLock>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            captureLock(cameraState).toList(locks)
        }

        assertThat(locks).containsExactly(CaptureLock.NONE)
    }

    @Test
    fun captureLock_whenQueueFills_disablesControlsAtOnceAndHidesThemAfterDelay() = runTest {
        val locks = mutableListOf<CaptureLock>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            captureLock(cameraState).toList(locks)
        }

        cameraState.value = queueFull()
        runCurrent()
        assertThat(locks.last()).isEqualTo(CaptureLock.CONTROLS_DISABLED)

        advanceTimeBy(CAPTURE_IN_PROGRESS_DELAY_MILLIS - 1)
        runCurrent()
        assertThat(locks.last()).isEqualTo(CaptureLock.CONTROLS_DISABLED)

        advanceTimeBy(1)
        runCurrent()
        assertThat(locks.last()).isEqualTo(CaptureLock.IN_PROGRESS)

        cameraState.value = queueEmpty()
        runCurrent()
        assertThat(locks).containsExactly(
            CaptureLock.NONE,
            CaptureLock.CONTROLS_DISABLED,
            CaptureLock.IN_PROGRESS,
            CaptureLock.NONE
        ).inOrder()
    }

    @Test
    fun captureLock_whenQueueDrainsBeforeDelay_neverHidesControls() = runTest {
        val locks = mutableListOf<CaptureLock>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            captureLock(cameraState).toList(locks)
        }

        cameraState.value = queueFull()
        advanceTimeBy(CAPTURE_IN_PROGRESS_DELAY_MILLIS / 2)
        runCurrent()
        cameraState.value = queueEmpty()
        advanceTimeBy(CAPTURE_IN_PROGRESS_DELAY_MILLIS * 2)
        runCurrent()

        assertThat(locks).containsExactly(
            CaptureLock.NONE,
            CaptureLock.CONTROLS_DISABLED,
            CaptureLock.NONE
        ).inOrder()
    }

    @Test
    fun captureLock_whenUnrelatedStateChanges_doesNotRestartDelay() = runTest {
        val locks = mutableListOf<CaptureLock>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            captureLock(cameraState).toList(locks)
        }

        cameraState.value = queueFull()
        advanceTimeBy(CAPTURE_IN_PROGRESS_DELAY_MILLIS / 2)
        cameraState.value = cameraState.value.copy(isTorchEnabled = true)
        advanceTimeBy(CAPTURE_IN_PROGRESS_DELAY_MILLIS / 2)
        runCurrent()

        assertThat(locks.last()).isEqualTo(CaptureLock.IN_PROGRESS)
    }

    @Test
    fun captureLock_whenLongCaptureStarts_hidesControlsWithoutWaitingForDelay() = runTest {
        val locks = mutableListOf<CaptureLock>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            captureLock(cameraState).toList(locks)
        }

        cameraState.value = queueFull()
        runCurrent()
        cameraState.value = cameraState.value.copy(
            stillCaptureProgress = StillCaptureProgress(
                startedAtUptimeMillis = 0L,
                captureLatencyMillis = CAPTURE_IN_PROGRESS_DELAY_MILLIS
            )
        )
        runCurrent()

        assertThat(locks).containsExactly(
            CaptureLock.NONE,
            CaptureLock.CONTROLS_DISABLED,
            CaptureLock.IN_PROGRESS
        ).inOrder()
    }

    @Test
    fun captureLock_whenEstimatedCaptureIsShorterThanDelay_waitsForDelay() = runTest {
        val locks = mutableListOf<CaptureLock>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            captureLock(cameraState).toList(locks)
        }

        cameraState.value = queueFull().copy(
            stillCaptureProgress = StillCaptureProgress(
                startedAtUptimeMillis = 0L,
                captureLatencyMillis = CAPTURE_IN_PROGRESS_DELAY_MILLIS - 1
            )
        )
        runCurrent()
        assertThat(locks.last()).isEqualTo(CaptureLock.CONTROLS_DISABLED)

        advanceTimeBy(CAPTURE_IN_PROGRESS_DELAY_MILLIS)
        runCurrent()
        assertThat(locks.last()).isEqualTo(CaptureLock.IN_PROGRESS)
    }

    @Test
    fun captureLock_whenProgressClearsWhileQueueStaysFull_staysInProgress() = runTest {
        val locks = mutableListOf<CaptureLock>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            captureLock(cameraState).toList(locks)
        }

        cameraState.value = queueFull().copy(
            stillCaptureProgress = StillCaptureProgress(
                startedAtUptimeMillis = 0L,
                captureLatencyMillis = CAPTURE_IN_PROGRESS_DELAY_MILLIS * 3
            )
        )
        runCurrent()
        cameraState.value = cameraState.value.copy(stillCaptureProgress = null)
        advanceTimeBy(CAPTURE_IN_PROGRESS_DELAY_MILLIS * 2)
        runCurrent()

        assertThat(locks).containsExactly(
            CaptureLock.NONE,
            CaptureLock.CONTROLS_DISABLED,
            CaptureLock.IN_PROGRESS
        ).inOrder()
    }

    @Test
    fun withCaptureLock_none_returnsSameState() {
        val state = unlockedState()

        assertThat(state.withCaptureLock(CaptureLock.NONE)).isSameInstanceAs(state)
    }

    @Test
    fun withCaptureLock_controlsDisabled_disablesControlsAndKeepsThemVisible() {
        val locked = unlockedState().withCaptureLock(CaptureLock.CONTROLS_DISABLED)

        assertThat((locked.flipLensUiState as FlipLensUiState.Available).isEnabled).isFalse()
        assertThat(
            (locked.captureModeToggleUiState as CaptureModeToggleUiState.Available).isEnabled
        ).isFalse()
        assertThat((locked.captureSubModeUiState as CaptureSubModeUiState.Available).isEnabled)
            .isFalse()
        assertThat((locked.imageWellUiState as ImageWellUiState.Content).isEnabled).isFalse()
        assertThat(locked.isQuickSettingsButtonEnabled).isFalse()
        assertThat(locked.chromeVisibility).isEqualTo(CaptureChromeVisibility())
    }

    @Test
    fun withCaptureLock_inProgress_disablesControlsAndHidesChrome() {
        val locked = unlockedState().withCaptureLock(CaptureLock.IN_PROGRESS)

        assertThat((locked.flipLensUiState as FlipLensUiState.Available).isEnabled).isFalse()
        assertThat(locked.isQuickSettingsButtonEnabled).isFalse()
        assertThat(locked.chromeVisibility).isEqualTo(CaptureChromeVisibility.Hidden)
    }

    @Test
    fun withCaptureLock_controlsDisabled_keepsIdleCaptureButton() {
        val state = unlockedState()

        val locked = state.withCaptureLock(
            CaptureLock.CONTROLS_DISABLED,
            StillCaptureProgress(startedAtUptimeMillis = 0L, captureLatencyMillis = 3_000L)
        )

        assertThat(locked.captureButtonUiState).isEqualTo(state.captureButtonUiState)
    }

    @Test
    fun withCaptureLock_inProgress_showsCaptureProgressOnCaptureButton() {
        val locked = unlockedState().withCaptureLock(
            CaptureLock.IN_PROGRESS,
            StillCaptureProgress(startedAtUptimeMillis = 5L, captureLatencyMillis = 3_000L)
        )

        assertThat(locked.captureButtonUiState).isEqualTo(
            CaptureButtonUiState.Enabled.Capturing(
                CaptureProgressUiState.Estimated(
                    startedAtUptimeMillis = 5L,
                    durationMillis = 3_000L
                )
            )
        )
    }

    @Test
    fun withCaptureLock_inProgressWithoutProgress_showsIndeterminateProgress() {
        val locked = unlockedState().withCaptureLock(CaptureLock.IN_PROGRESS)

        assertThat(locked.captureButtonUiState).isEqualTo(
            CaptureButtonUiState.Enabled.Capturing(CaptureProgressUiState.Indeterminate)
        )
    }

    @Test
    fun withCaptureLock_inProgress_leavesUnavailableCaptureButtonUnchanged() {
        val locked = unlockedState()
            .copy(captureButtonUiState = CaptureButtonUiState.Unavailable)
            .withCaptureLock(CaptureLock.IN_PROGRESS)

        assertThat(locked.captureButtonUiState).isEqualTo(CaptureButtonUiState.Unavailable)
    }

    @Test
    fun withCaptureLock_leavesUnavailableControlsUnchanged() {
        val state = CaptureUiState.Ready()

        val locked = state.withCaptureLock(CaptureLock.CONTROLS_DISABLED)

        assertThat(locked.flipLensUiState).isEqualTo(FlipLensUiState.Unavailable)
        assertThat(locked.captureModeToggleUiState)
            .isEqualTo(CaptureModeToggleUiState.Unavailable)
        assertThat(locked.captureSubModeUiState).isEqualTo(CaptureSubModeUiState.Unavailable)
        assertThat(locked.imageWellUiState).isEqualTo(ImageWellUiState.Unavailable)
    }

    @Test
    fun captureUiState_whenCaptureQueueFull_disablesControls() = runTest {
        val cameraSystem = FakeCameraSystem().apply {
            setSystemConstraints(TYPICAL_SYSTEM_CONSTRAINTS)
        }
        val uiStates = captureUiState(
            currentSettings = cameraSystem.getCurrentSettings(),
            systemConstraints = cameraSystem.getSystemConstraints(),
            currentCameraState = cameraSystem.getCurrentCameraState(),
            trackedCaptureUiState = MutableStateFlow(TrackedCaptureUiState()),
            externalCaptureMode = ExternalCaptureMode.Standard
        )
        val unlocked = uiStates.first() as CaptureUiState.Ready
        assertThat(unlocked.isQuickSettingsButtonEnabled).isTrue()

        cameraSystem.setCurrentCameraState(
            CameraState(isCameraRunning = true, inFlightCapturesCount = 1, maxInFlightCaptures = 1)
        )

        val locked = uiStates.first {
            it is CaptureUiState.Ready && !it.isQuickSettingsButtonEnabled
        } as CaptureUiState.Ready
        assertThat(locked.chromeVisibility).isEqualTo(CaptureChromeVisibility())
        (locked.flipLensUiState as? FlipLensUiState.Available)?.let {
            assertThat(it.isEnabled).isFalse()
        }
    }

    @Test
    fun captureUiState_whenLongCaptureStarts_showsCaptureProgressAndHidesChrome() = runTest {
        val cameraSystem = FakeCameraSystem().apply {
            setSystemConstraints(TYPICAL_SYSTEM_CONSTRAINTS)
        }
        val uiStates = captureUiState(
            currentSettings = cameraSystem.getCurrentSettings(),
            systemConstraints = cameraSystem.getSystemConstraints(),
            currentCameraState = cameraSystem.getCurrentCameraState(),
            trackedCaptureUiState = MutableStateFlow(TrackedCaptureUiState()),
            externalCaptureMode = ExternalCaptureMode.Standard
        )
        val progress = StillCaptureProgress(
            startedAtUptimeMillis = 100L,
            captureLatencyMillis = 3_000L,
            processProgressPercent = 25
        )

        cameraSystem.setCurrentCameraState(
            CameraState(
                isCameraRunning = true,
                inFlightCapturesCount = 1,
                maxInFlightCaptures = 1,
                stillCaptureProgress = progress
            )
        )

        val inProgress = uiStates.first {
            it is CaptureUiState.Ready &&
                it.captureButtonUiState is CaptureButtonUiState.Enabled.Capturing
        } as CaptureUiState.Ready
        assertThat(inProgress.captureButtonUiState).isEqualTo(
            CaptureButtonUiState.Enabled.Capturing(CaptureProgressUiState.Determinate(0.25f))
        )
        assertThat(inProgress.chromeVisibility).isEqualTo(CaptureChromeVisibility.Hidden)
    }

    private fun unlockedState() = CaptureUiState.Ready(
        captureButtonUiState = CaptureButtonUiState.Enabled.Idle(
            captureMode = CaptureMode.STANDARD,
            isEnabled = false,
            debounceVisualDisable = false
        ),
        flipLensUiState = FlipLensUiState.Available(
            selectedLensFacing = LensFacing.BACK,
            availableLensFacings = listOf(
                SingleSelectableUiState.SelectableUi(LensFacing.FRONT),
                SingleSelectableUiState.SelectableUi(LensFacing.BACK)
            )
        ),
        captureModeToggleUiState = CaptureModeToggleUiState.Available(
            selectedCaptureMode = CaptureMode.IMAGE_ONLY,
            imageOnlyUiState = SingleSelectableUiState.SelectableUi(CaptureMode.IMAGE_ONLY),
            videoOnlyUiState = SingleSelectableUiState.SelectableUi(CaptureMode.VIDEO_ONLY)
        ),
        captureSubModeUiState = CaptureSubModeUiState.Available(
            selectedSubMode = CaptureSubModeId.DEFAULT,
            availableSubModes = listOf(
                SingleSelectableUiState.SelectableUi(
                    CaptureSubModeOption(id = CaptureSubModeId.DEFAULT, labelResId = 0)
                )
            )
        ),
        imageWellUiState = ImageWellUiState.Content(
            mediaDescriptor = MediaDescriptor.Content.Image(uri = Uri.EMPTY, thumbnail = null)
        )
    )
}
