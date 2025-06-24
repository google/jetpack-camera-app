/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.google.jetpackcamera.ui.uistate.viewfinder

import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.ui.uistate.ReasonDisplayable
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState

sealed interface CaptureModeUiState {
    data object Unavailable : CaptureModeUiState

    data class Available(
        val selectedCaptureMode: CaptureMode,
        val availableCaptureModes: List<SingleSelectableUiState<CaptureMode>>
    ) : CaptureModeUiState {
        init {
            val isSelectedModePresentAndSelectable = availableCaptureModes.any { state ->
                state is SingleSelectableUiState.SelectableUi && state.value == selectedCaptureMode
            }

            check(isSelectedModePresentAndSelectable) {
                "Selected capture mode $selectedCaptureMode is not among the available and " +
                    "selectable capture modes. Available modes: ${
                        availableCaptureModes.mapNotNull {
                            if (it is SingleSelectableUiState.SelectableUi) it.value else null
                        }
                    }"
            }
        }
    }

    fun CaptureModeUiState.isCaptureModeSelectable(captureMode: CaptureMode): Boolean {
        return when (this) {
            is Available -> {
                availableCaptureModes.any {
                    it is SingleSelectableUiState.SelectableUi && it.value == captureMode
                }
            }

            Unavailable -> false
        }
    }

    fun CaptureModeUiState.findSelectableStateFor(
        targetCaptureMode: CaptureMode
    ): SingleSelectableUiState<CaptureMode>? {
        if (this is Available) {
            return this.availableCaptureModes.firstOrNull { state ->
                (
                    state is SingleSelectableUiState.SelectableUi &&
                        state.value == targetCaptureMode
                    ) ||
                    (
                        state is SingleSelectableUiState.Disabled &&
                            state.value == targetCaptureMode
                        )
            }
        }
        return null
    }
}

enum class DisabledReason(
    // 'override' is required
    override val testTag: String,
    // 'override' is required
    override val reasonTextResId: Int
) : ReasonDisplayable {
    VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED(
        VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED_TAG,
        R.string.toast_video_capture_external_unsupported
    ),
    IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED(
        IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED_TAG,
        R.string.toast_image_capture_external_unsupported
    ),
    IMAGE_CAPTURE_UNSUPPORTED_CONCURRENT_CAMERA(
        IMAGE_CAPTURE_UNSUPPORTED_CONCURRENT_CAMERA_TAG,
        R.string.toast_image_capture_unsupported_concurrent_camera
    ),
    HDR_VIDEO_UNSUPPORTED_ON_DEVICE(
        HDR_VIDEO_UNSUPPORTED_ON_DEVICE_TAG,
        R.string.toast_hdr_video_unsupported_on_device
    ),
    HDR_VIDEO_UNSUPPORTED_ON_LENS(
        HDR_VIDEO_UNSUPPORTED_ON_LENS_TAG,
        R.string.toast_hdr_video_unsupported_on_lens
    ),
    HDR_IMAGE_UNSUPPORTED_ON_DEVICE(
        HDR_IMAGE_UNSUPPORTED_ON_DEVICE_TAG,
        R.string.toast_hdr_photo_unsupported_on_device
    ),
    HDR_IMAGE_UNSUPPORTED_ON_LENS(
        HDR_IMAGE_UNSUPPORTED_ON_LENS_TAG,
        R.string.toast_hdr_photo_unsupported_on_lens
    ),
    HDR_IMAGE_UNSUPPORTED_ON_SINGLE_STREAM(
        HDR_IMAGE_UNSUPPORTED_ON_SINGLE_STREAM_TAG,
        R.string.toast_hdr_photo_unsupported_on_lens_single_stream
    ),
    HDR_IMAGE_UNSUPPORTED_ON_MULTI_STREAM(
        HDR_IMAGE_UNSUPPORTED_ON_MULTI_STREAM_TAG,
        R.string.toast_hdr_photo_unsupported_on_lens_multi_stream
    ),
    HDR_SIMULTANEOUS_IMAGE_VIDEO_UNSUPPORTED(
        HDR_SIMULTANEOUS_IMAGE_VIDEO_UNSUPPORTED_TAG,
        R.string.toast_hdr_simultaneous_image_video_unsupported
    )
}
