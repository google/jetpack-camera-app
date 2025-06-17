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
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.DEFAULT_HDR_DYNAMIC_RANGE
import com.google.jetpackcamera.settings.model.DEFAULT_HDR_IMAGE_OUTPUT
import com.google.jetpackcamera.settings.model.SystemConstraints
import com.google.jetpackcamera.ui.uistate.viewfinder.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.ConcurrentCameraUiState

object ConcurrentCameraUiStateAdapter {
    fun getUiState(cameraAppSettings: CameraAppSettings,
                   systemConstraints: SystemConstraints,
                   previewMode: PreviewMode,
                   captureModeUiState: CaptureModeUiState): ConcurrentCameraUiState {
        return ConcurrentCameraUiState.Available(
            selectedConcurrentCameraMode = cameraAppSettings.concurrentCameraMode,
            isEnabled = systemConstraints.concurrentCamerasSupported &&
                    previewMode != PreviewMode.EXTERNAL_IMAGE_CAPTURE && ((
                    captureModeUiState as?
                            CaptureModeUiState.Available
                    )
                ?.selectedCaptureMode !=
                    CaptureMode.IMAGE_ONLY) && (cameraAppSettings.dynamicRange !=
                    DEFAULT_HDR_DYNAMIC_RANGE &&
                    cameraAppSettings.imageFormat !=
                    DEFAULT_HDR_IMAGE_OUTPUT)
        )
    }
}
