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
package com.google.jetpackcamera.model

/**
 * Domain representation of camera errors surfaced by CameraX or Android system services.
 */
sealed interface CameraError {
    /** Whether the user can stay on the camera screen (wait/retry) vs. exiting the activity. */
    val isRecoverable: Boolean

    // --- CameraX StateError codes (1..8) ---

    /** `ERROR_CAMERA_IN_USE` (2): Higher-priority camera client holds the camera. */
    data object CameraInUse : CameraError {
        override val isRecoverable = true
    }

    /** `ERROR_MAX_CAMERAS_IN_USE` (1): Device open-camera limit reached (e.g., split-screen). */
    data object MaxCamerasInUse : CameraError {
        override val isRecoverable = true
    }

    /** `ERROR_OTHER_RECOVERABLE_ERROR` (3): Recoverable Camera2 error; CameraX will retry. */
    data object OtherRecoverableError : CameraError {
        override val isRecoverable = true
    }

    /** `ERROR_STREAM_CONFIG` (4): UseCase surface combination unsupported by hardware/HAL. */
    data object StreamConfigError : CameraError {
        override val isRecoverable = false
    }

    /**
     * `ERROR_CAMERA_DISABLED` (5) when `DevicePolicyManager.getCameraDisabled(null) == true`:
     * Camera blocked by IT admin policy.
     */
    data object CameraDisabledByPolicy : CameraError {
        override val isRecoverable = false
    }

    /**
     * `ERROR_CAMERA_DISABLED` (5) when `DevicePolicyManager.getCameraDisabled(null) == false`:
     * User declined the Android 12+ system camera privacy sensor prompt or camera sensor is off.
     */
    data object CameraSensorPrivacyDisabled : CameraError {
        override val isRecoverable = false
    }

    /** `ERROR_CAMERA_FATAL_ERROR` (6): Unrecoverable hardware/HAL failure requiring reboot. */
    data object FatalCameraError : CameraError {
        override val isRecoverable = false
    }

    /** `ERROR_DO_NOT_DISTURB_MODE_ENABLED` (7): DND blocks camera on API 28 legacy devices. */
    data object DoNotDisturbEnabled : CameraError {
        override val isRecoverable = false
    }

    /** `ERROR_CAMERA_REMOVED` (8) or zero available cameras on device. */
    data object CameraRemoved : CameraError {
        override val isRecoverable = false
    }

    // --- Environmental & Capture Errors ---

    /** Device thermal status >= `THERMAL_STATUS_CRITICAL` (API 29+). */
    data object ThermalOverheat : CameraError {
        override val isRecoverable = false
    }

    /** Storage full (`StatFs` pre-check, `ERROR_FILE_IO`, or `ERROR_INSUFFICIENT_STORAGE`). */
    data object InsufficientStorage : CameraError {
        override val isRecoverable = true
    }
}
