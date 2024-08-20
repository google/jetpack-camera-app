/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.jetpackcamera.feature.preview

import com.google.jetpackcamera.feature.preview.ui.HDR_IMAGE_UNSUPPORTED_ON_DEVICE_TAG
import com.google.jetpackcamera.feature.preview.ui.HDR_IMAGE_UNSUPPORTED_ON_LENS_TAG
import com.google.jetpackcamera.feature.preview.ui.HDR_IMAGE_UNSUPPORTED_ON_MULTI_STREAM_TAG
import com.google.jetpackcamera.feature.preview.ui.HDR_IMAGE_UNSUPPORTED_ON_SINGLE_STREAM_TAG
import com.google.jetpackcamera.feature.preview.ui.HDR_VIDEO_UNSUPPORTED_ON_DEVICE_TAG
import com.google.jetpackcamera.feature.preview.ui.HDR_VIDEO_UNSUPPORTED_ON_LENS_TAG
import com.google.jetpackcamera.feature.preview.ui.IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
import com.google.jetpackcamera.feature.preview.ui.IMAGE_CAPTURE_UNSUPPORTED_CONCURRENT_CAMERA_TAG
import com.google.jetpackcamera.feature.preview.ui.VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED_TAG

sealed interface CaptureModeToggleUiState {

    data object Invisible : CaptureModeToggleUiState

    sealed interface Visible : CaptureModeToggleUiState {
        val currentMode: ToggleMode
    }

    data class Enabled(override val currentMode: ToggleMode) : Visible

    data class Disabled(
        override val currentMode: ToggleMode,
        val disabledReason: DisabledReason
    ) : Visible

    enum class DisabledReason(val testTag: String, val reasonTextResId: Int) {
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
        )
    }

    enum class ToggleMode {
        CAPTURE_TOGGLE_IMAGE,
        CAPTURE_TOGGLE_VIDEO
    }
}
