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
package com.google.jetpackcamera.core.camera

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import androidx.camera.core.CameraState as CXCameraState
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CameraError
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class CameraErrorMappingTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun toCameraError_mapsStandardErrorCodes() {
        assertThat(
            CXCameraState.StateError.create(CXCameraState.ERROR_CAMERA_IN_USE)
                .toCameraError(context)
        ).isEqualTo(CameraError.CameraInUse)

        assertThat(
            CXCameraState.StateError.create(CXCameraState.ERROR_MAX_CAMERAS_IN_USE)
                .toCameraError(context)
        ).isEqualTo(CameraError.MaxCamerasInUse)

        assertThat(
            CXCameraState.StateError.create(CXCameraState.ERROR_OTHER_RECOVERABLE_ERROR)
                .toCameraError(context)
        ).isEqualTo(CameraError.OtherRecoverableError)

        assertThat(
            CXCameraState.StateError.create(CXCameraState.ERROR_STREAM_CONFIG)
                .toCameraError(context)
        ).isEqualTo(CameraError.StreamConfigError)

        assertThat(
            CXCameraState.StateError.create(CXCameraState.ERROR_CAMERA_FATAL_ERROR)
                .toCameraError(context)
        ).isEqualTo(CameraError.FatalCameraError)

        assertThat(
            CXCameraState.StateError.create(CXCameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED)
                .toCameraError(context)
        ).isEqualTo(CameraError.DoNotDisturbEnabled)

        assertThat(
            CXCameraState.StateError.create(CXCameraState.ERROR_CAMERA_REMOVED)
                .toCameraError(context)
        ).isEqualTo(CameraError.CameraRemoved)
    }

    @Test
    fun toCameraError_cameraDisabled_whenPolicyDisabled_returnsCameraDisabledByPolicy() {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, "TestAdmin")
        shadowOf(dpm).setActiveAdmin(admin)
        dpm.setCameraDisabled(admin, true)

        val error = CXCameraState.StateError.create(CXCameraState.ERROR_CAMERA_DISABLED)
            .toCameraError(context)

        assertThat(error).isEqualTo(CameraError.CameraDisabledByPolicy)
    }

    @Test
    fun toCameraError_cameraDisabled_whenPolicyNotDisabled_returnsCameraSensorPrivacyDisabled() {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, "TestAdmin")
        shadowOf(dpm).setActiveAdmin(admin)
        dpm.setCameraDisabled(admin, false)

        val error = CXCameraState.StateError.create(CXCameraState.ERROR_CAMERA_DISABLED)
            .toCameraError(context)

        assertThat(error).isEqualTo(CameraError.CameraSensorPrivacyDisabled)
    }
}
