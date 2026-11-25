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

import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.ui.uistate.capture.CaptureChromeVisibility
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureSubModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest

/**
 * How long the capture queue must stay full before the capture screen treats the capture as a
 * long one and hides its controls.
 */
internal const val CAPTURE_IN_PROGRESS_DELAY_MILLIS = 1_000L

/** How much of the capture screen is locked while still captures are in flight. */
internal enum class CaptureLock {
    /** Nothing is locked. */
    NONE,

    /**
     * No further capture can start. Controls that would change the camera session are disabled,
     * so that taps on them are not queued behind the captures in flight.
     */
    CONTROLS_DISABLED,

    /**
     * A long capture is in progress. In addition to [CONTROLS_DISABLED], the controls that are not
     * needed during the capture are hidden.
     */
    IN_PROGRESS
}

/**
 * Returns a [Flow] of the [CaptureLock] for [cameraState].
 *
 * The controls are disabled as soon as the capture queue is full, and hidden if it is still full
 * after [inProgressDelayMillis]. A queue that drains sooner, such as during a quick burst of
 * photos, never hides them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun captureLock(
    cameraState: Flow<CameraState>,
    inProgressDelayMillis: Long = CAPTURE_IN_PROGRESS_DELAY_MILLIS
): Flow<CaptureLock> = cameraState
    .map { it.isCaptureAtCapacity }
    .distinctUntilChanged()
    .transformLatest { isQueueFull ->
        if (isQueueFull) {
            emit(CaptureLock.CONTROLS_DISABLED)
            delay(inProgressDelayMillis)
            emit(CaptureLock.IN_PROGRESS)
        } else {
            emit(CaptureLock.NONE)
        }
    }

/**
 * Returns a copy of this state with the controls that [lock] locks disabled or hidden.
 */
internal fun CaptureUiState.Ready.withCaptureLock(lock: CaptureLock): CaptureUiState.Ready {
    if (lock == CaptureLock.NONE) return this
    return copy(
        flipLensUiState = flipLensUiState.let {
            if (it is FlipLensUiState.Available) it.copy(isEnabled = false) else it
        },
        captureModeToggleUiState = captureModeToggleUiState.let {
            if (it is CaptureModeToggleUiState.Available) it.copy(isEnabled = false) else it
        },
        captureSubModeUiState = captureSubModeUiState.let {
            if (it is CaptureSubModeUiState.Available) it.copy(isEnabled = false) else it
        },
        imageWellUiState = imageWellUiState.let {
            if (it is ImageWellUiState.Content) it.copy(isEnabled = false) else it
        },
        isQuickSettingsButtonEnabled = false,
        chromeVisibility = if (lock == CaptureLock.IN_PROGRESS) {
            CaptureChromeVisibility.Hidden
        } else {
            chromeVisibility
        }
    )
}
