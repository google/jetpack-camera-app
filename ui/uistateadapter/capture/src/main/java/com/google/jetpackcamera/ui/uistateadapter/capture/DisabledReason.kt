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
package com.google.jetpackcamera.ui.uistateadapter.capture

import com.google.jetpackcamera.ui.uistate.DisableRationale

/**
 * Represents reasons why a UI component or functionality might be disabled, providing
 * a string resource ID for user-facing explanation.
 */
enum class DisabledReason(
    override val reasonTextResId: Int
) : DisableRationale {
    VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED(
        R.string.toast_video_capture_external_unsupported
    ),
    IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED(
        R.string.toast_image_capture_external_unsupported
    ),
    IMAGE_CAPTURE_UNSUPPORTED_CONCURRENT_CAMERA(
        R.string.toast_image_capture_unsupported_concurrent_camera
    ),
    HDR_VIDEO_UNSUPPORTED_ON_DEVICE(
        R.string.toast_hdr_video_unsupported_on_device
    ),
    HDR_VIDEO_UNSUPPORTED_ON_LENS(
        R.string.toast_hdr_video_unsupported_on_lens
    ),
    HDR_IMAGE_UNSUPPORTED_ON_DEVICE(
        R.string.toast_hdr_photo_unsupported_on_device
    ),
    HDR_IMAGE_UNSUPPORTED_ON_LENS(
        R.string.toast_hdr_photo_unsupported_on_lens
    ),
    HDR_IMAGE_UNSUPPORTED_ON_SINGLE_STREAM(
        R.string.toast_hdr_photo_unsupported_on_lens_single_stream
    ),
    HDR_IMAGE_UNSUPPORTED_ON_MULTI_STREAM(
        R.string.toast_hdr_photo_unsupported_on_lens_multi_stream
    ),
    HDR_SIMULTANEOUS_IMAGE_VIDEO_UNSUPPORTED(
        R.string.toast_hdr_simultaneous_image_video_unsupported
    )
}
