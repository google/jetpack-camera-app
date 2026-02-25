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
package com.google.jetpackcamera.ui.uistateadapter.capture

import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraConstraints
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.forCurrentLens
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState

/**
 * Creates an [HdrUiState] based on the current camera settings, system constraints, and capture mode.
 *
 * This function determines whether the High Dynamic Range (HDR) feature is available for the user
 * to interact with. The availability depends on a combination of hardware support for specific
 * HDR formats ([DynamicRange.HLG10] for video, [ImageOutputFormat.JPEG_ULTRA_HDR] for images) and
 * various other settings that may conflict with HDR, such as flash mode or concurrent camera mode.
 *
 * The logic is tailored to the [ExternalCaptureMode]:
 * - **ImageCapture / MultipleImageCapture**: Checks for `JPEG_ULTRA_HDR` support.
 * - **VideoCapture**: Checks for `HLG10` dynamic range support.
 * - **Standard**: Checks for support for either `HLG10` or `JPEG_ULTRA_HDR`.
 *
 * In all cases, HDR is disabled if `LOW_LIGHT_BOOST` flash mode is active. For video and standard
 * modes, it is also disabled if concurrent camera mode is active.
 *
 * @param cameraAppSettings The current application and camera settings.
 * @param systemConstraints The capabilities and limitations of the device's camera hardware.
 * @param externalCaptureMode The mode indicating how the camera was launched (e.g., via an
 * external intent), which influences which HDR formats are relevant.
 *
 * @return [HdrUiState.Available] if the feature is supported and not blocked by other settings,
 * otherwise returns [HdrUiState.Unavailable].
 */
fun HdrUiState.Companion.from(
    cameraAppSettings: CameraAppSettings,
    systemConstraints: CameraSystemConstraints,
    externalCaptureMode: ExternalCaptureMode
): HdrUiState {
    val cameraConstraints: CameraConstraints? = systemConstraints.forCurrentLens(
        cameraAppSettings
    )
    return when (externalCaptureMode) {
        ExternalCaptureMode.ImageCapture,
        ExternalCaptureMode.MultipleImageCapture -> if (
            cameraConstraints
                ?.supportedImageFormatsMap?.get(cameraAppSettings.streamConfig)
                ?.contains(ImageOutputFormat.JPEG_ULTRA_HDR) ?: false &&
            cameraAppSettings.flashMode != FlashMode.LOW_LIGHT_BOOST
        ) {
            HdrUiState.Available(cameraAppSettings.imageFormat, cameraAppSettings.dynamicRange)
        } else {
            HdrUiState.Unavailable
        }

        ExternalCaptureMode.VideoCapture -> if (
            cameraConstraints?.supportedDynamicRanges?.contains(DynamicRange.HLG10) == true &&
            cameraAppSettings.concurrentCameraMode != ConcurrentCameraMode.DUAL &&
            cameraAppSettings.flashMode != FlashMode.LOW_LIGHT_BOOST
        ) {
            HdrUiState.Available(
                cameraAppSettings.imageFormat,
                cameraAppSettings.dynamicRange
            )
        } else {
            HdrUiState.Unavailable
        }

        ExternalCaptureMode.Standard -> if ((
                cameraConstraints?.supportedDynamicRanges?.contains(DynamicRange.HLG10) ==
                    true ||
                    cameraConstraints?.supportedImageFormatsMap?.get(
                        cameraAppSettings.streamConfig
                    )
                        ?.contains(ImageOutputFormat.JPEG_ULTRA_HDR) ?: false
                ) &&
            cameraAppSettings.concurrentCameraMode != ConcurrentCameraMode.DUAL &&
            cameraAppSettings.flashMode != FlashMode.LOW_LIGHT_BOOST
        ) {
            HdrUiState.Available(cameraAppSettings.imageFormat, cameraAppSettings.dynamicRange)
        } else {
            HdrUiState.Unavailable
        }
    }
}
