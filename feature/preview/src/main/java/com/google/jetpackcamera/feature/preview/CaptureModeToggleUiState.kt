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

import com.google.jetpackcamera.feature.preview.ui.HDR_IMAGE_UNSUPPORTED_TAG
import com.google.jetpackcamera.feature.preview.ui.HDR_VIDEO_UNSUPPORTED_TAG
import com.google.jetpackcamera.feature.preview.ui.VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED_TAG

data class CaptureModeToggleUiState(
    val isShown: Boolean = false,
    val enabled: Boolean = false,
    val disabledReason: CaptureModeUnsupportedReason? = null,
    val currentMode: CaptureToggleMode = CaptureToggleMode.CAPTURE_TOGGLE_VIDEO
) {
    enum class CaptureModeUnsupportedReason(val testTag: String, val reasonTextResId: Int) {
        VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED(
            VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED_TAG,
            R.string.toast_video_capture_external_unsupported
        ),
        HDR_VIDEO_UNSUPPORTED(HDR_VIDEO_UNSUPPORTED_TAG, R.string.toast_hdr_video_unsupported),
        HDR_IMAGE_UNSUPPORTED(HDR_IMAGE_UNSUPPORTED_TAG, R.string.toast_hdr_photo_unsupported)
    }

    enum class CaptureToggleMode {
        CAPTURE_TOGGLE_IMAGE,
        CAPTURE_TOGGLE_VIDEO
    }
}