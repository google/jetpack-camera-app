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

import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.model.CameraError
import com.google.jetpackcamera.ui.uistate.capture.CameraErrorUiState

/**
 * Maps [CameraState.cameraError] and [acknowledgedError] into a [CameraErrorUiState].
 */
fun CameraErrorUiState.Companion.from(
    cameraState: CameraState,
    acknowledgedError: CameraError?
): CameraErrorUiState {
    val error = cameraState.cameraError ?: return CameraErrorUiState.Hidden
    if (error == acknowledgedError) {
        return CameraErrorUiState.Hidden
    }

    val okButton = R.string.picker_camera_error_dialog_ok
    return when (error) {
        CameraError.CameraInUse -> CameraErrorUiState.Showing(
            error = error,
            titleResId = R.string.picker_camera_error_in_use_title,
            bodyResId = R.string.picker_camera_error_in_use_body,
            confirmButtonTextResId = okButton,
            shouldExitAppOnConfirm = false
        )
        CameraError.MaxCamerasInUse -> CameraErrorUiState.Showing(
            error = error,
            titleResId = R.string.picker_camera_error_max_in_use_title,
            bodyResId = R.string.picker_camera_error_max_in_use_body,
            confirmButtonTextResId = okButton,
            shouldExitAppOnConfirm = false
        )
        CameraError.OtherRecoverableError -> CameraErrorUiState.Showing(
            error = error,
            titleResId = R.string.picker_camera_error_recoverable_title,
            bodyResId = R.string.picker_camera_error_recoverable_body,
            confirmButtonTextResId = okButton,
            shouldExitAppOnConfirm = false
        )
        CameraError.StreamConfigError -> CameraErrorUiState.Showing(
            error = error,
            titleResId = R.string.picker_camera_error_stream_config_title,
            bodyResId = R.string.picker_camera_error_stream_config_body,
            confirmButtonTextResId = okButton,
            shouldExitAppOnConfirm = true
        )
        CameraError.CameraDisabledByPolicy -> CameraErrorUiState.Showing(
            error = error,
            titleResId = R.string.picker_camera_error_disabled_title,
            bodyResId = null,
            confirmButtonTextResId = okButton,
            shouldExitAppOnConfirm = true
        )
        CameraError.CameraSensorPrivacyDisabled -> CameraErrorUiState.Showing(
            error = error,
            titleResId = R.string.picker_camera_error_sensor_privacy_title,
            bodyResId = R.string.picker_camera_error_sensor_privacy_body,
            confirmButtonTextResId = okButton,
            shouldExitAppOnConfirm = true
        )
        CameraError.FatalCameraError -> CameraErrorUiState.Showing(
            error = error,
            titleResId = R.string.picker_camera_error_fatal_title,
            bodyResId = R.string.picker_camera_error_fatal_body,
            confirmButtonTextResId = okButton,
            shouldExitAppOnConfirm = true
        )
        CameraError.DoNotDisturbEnabled -> CameraErrorUiState.Showing(
            error = error,
            titleResId = R.string.picker_camera_error_dnd_title,
            bodyResId = R.string.picker_camera_error_dnd_body,
            confirmButtonTextResId = okButton,
            shouldExitAppOnConfirm = true
        )
        CameraError.CameraRemoved -> CameraErrorUiState.Showing(
            error = error,
            titleResId = R.string.picker_camera_error_removed_title,
            bodyResId = R.string.picker_camera_error_removed_body,
            confirmButtonTextResId = okButton,
            shouldExitAppOnConfirm = true
        )
        CameraError.ThermalOverheat -> CameraErrorUiState.Showing(
            error = error,
            titleResId = R.string.picker_camera_error_thermal_title,
            bodyResId = R.string.picker_camera_error_thermal_body,
            confirmButtonTextResId = okButton,
            shouldExitAppOnConfirm = true
        )
        CameraError.InsufficientStorage -> CameraErrorUiState.Showing(
            error = error,
            titleResId = R.string.picker_camera_error_storage_title,
            bodyResId = R.string.picker_camera_error_storage_body,
            confirmButtonTextResId = okButton,
            shouldExitAppOnConfirm = false
        )
    }
}
