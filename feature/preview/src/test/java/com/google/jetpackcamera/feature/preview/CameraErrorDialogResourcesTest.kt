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
package com.google.jetpackcamera.feature.preview

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CameraError
import org.junit.Test

class CameraErrorDialogResourcesTest {

    @Test
    fun toDialogResources_cameraInUse() {
        assertThat(CameraError.CameraInUse.toDialogResources()).isEqualTo(
            CameraErrorDialogResources(
                titleResId = R.string.camera_error_in_use_title,
                bodyResId = R.string.camera_error_in_use_body
            )
        )
    }

    @Test
    fun toDialogResources_maxCamerasInUse() {
        assertThat(CameraError.MaxCamerasInUse.toDialogResources()).isEqualTo(
            CameraErrorDialogResources(
                titleResId = R.string.camera_error_max_in_use_title,
                bodyResId = R.string.camera_error_max_in_use_body
            )
        )
    }

    @Test
    fun toDialogResources_otherRecoverableError() {
        assertThat(CameraError.OtherRecoverableError.toDialogResources()).isEqualTo(
            CameraErrorDialogResources(
                titleResId = R.string.camera_error_recoverable_title,
                bodyResId = R.string.camera_error_recoverable_body
            )
        )
    }

    @Test
    fun toDialogResources_streamConfigError() {
        assertThat(CameraError.StreamConfigError.toDialogResources()).isEqualTo(
            CameraErrorDialogResources(
                titleResId = R.string.camera_error_stream_config_title,
                bodyResId = R.string.camera_error_stream_config_body
            )
        )
    }

    @Test
    fun toDialogResources_cameraDisabledByPolicy() {
        assertThat(CameraError.CameraDisabledByPolicy.toDialogResources()).isEqualTo(
            CameraErrorDialogResources(
                titleResId = R.string.camera_error_disabled_title,
                bodyResId = R.string.camera_error_disabled_body
            )
        )
    }

    @Test
    fun toDialogResources_cameraSensorPrivacyDisabled_returnsNull() {
        assertThat(CameraError.CameraSensorPrivacyDisabled.toDialogResources()).isNull()
    }

    @Test
    fun toDialogResources_fatalCameraError() {
        assertThat(CameraError.FatalCameraError.toDialogResources()).isEqualTo(
            CameraErrorDialogResources(
                titleResId = R.string.camera_error_fatal_title,
                bodyResId = R.string.camera_error_fatal_body
            )
        )
    }

    @Test
    fun toDialogResources_doNotDisturbEnabled() {
        assertThat(CameraError.DoNotDisturbEnabled.toDialogResources()).isEqualTo(
            CameraErrorDialogResources(
                titleResId = R.string.camera_error_dnd_title,
                bodyResId = R.string.camera_error_dnd_body
            )
        )
    }

    @Test
    fun toDialogResources_cameraRemoved() {
        assertThat(CameraError.CameraRemoved.toDialogResources()).isEqualTo(
            CameraErrorDialogResources(
                titleResId = R.string.camera_error_removed_title,
                bodyResId = R.string.camera_error_removed_body
            )
        )
    }
}
