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

package com.google.jetpackcamera.ui.uistateadapter.viewfinder

import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraConstraints
import com.google.jetpackcamera.settings.model.ConcurrentCameraMode
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.SystemConstraints
import com.google.jetpackcamera.settings.model.forCurrentLens
import com.google.jetpackcamera.ui.uistate.viewfinder.HdrUiState

object HdrUiStateAdapter {
    fun getUiState(
        cameraAppSettings: CameraAppSettings,
        systemConstraints: SystemConstraints,
        previewMode: PreviewMode
    ): HdrUiState {
        val cameraConstraints: CameraConstraints? = systemConstraints.forCurrentLens(
            cameraAppSettings
        )
        return when (previewMode) {
            PreviewMode.EXTERNAL_IMAGE_CAPTURE,
            PreviewMode.EXTERNAL_MULTIPLE_IMAGE_CAPTURE -> if (
                cameraConstraints
                    ?.supportedImageFormatsMap?.get(cameraAppSettings.streamConfig)
                    ?.contains(ImageOutputFormat.JPEG_ULTRA_HDR) ?: false
            ) {
                HdrUiState.Available(cameraAppSettings.imageFormat, cameraAppSettings.dynamicRange)
            } else {
                HdrUiState.Unavailable
            }

            PreviewMode.EXTERNAL_VIDEO_CAPTURE -> if (
                cameraConstraints?.supportedDynamicRanges?.contains(DynamicRange.HLG10) == true &&
                cameraAppSettings.concurrentCameraMode != ConcurrentCameraMode.DUAL
            ) {
                HdrUiState.Available(
                    cameraAppSettings.imageFormat,
                    cameraAppSettings.dynamicRange
                )
            } else {
                HdrUiState.Unavailable
            }

            PreviewMode.STANDARD -> if ((
                        cameraConstraints?.supportedDynamicRanges?.contains(DynamicRange.HLG10) ==
                                true ||
                                cameraConstraints?.supportedImageFormatsMap?.get(
                                    cameraAppSettings.streamConfig
                                )
                                    ?.contains(ImageOutputFormat.JPEG_ULTRA_HDR) ?: false
                        ) &&
                cameraAppSettings.concurrentCameraMode != ConcurrentCameraMode.DUAL
            ) {
                HdrUiState.Available(cameraAppSettings.imageFormat, cameraAppSettings.dynamicRange)
            } else {
                HdrUiState.Unavailable
            }
        }
    }
}