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
package com.google.jetpackcamera.settings

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.proto.CameraAppSettings as CameraAppSettingsProto
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CameraAppSettingsProtoDefaultsTest {

    @Test
    fun defaultProtoMapsToDefaultModel() {
        assertThat(
            DEFAULT_CAMERA_APP_SETTINGS_PROTO.toModel(DEFAULT_CAMERA_APP_SETTINGS.captureMode)
        ).isEqualTo(DEFAULT_CAMERA_APP_SETTINGS)
    }

    @Test
    fun defaultProtoDiffersFromProtoZeroValueInstance() {
        // Guards against callers substituting getDefaultInstance() for
        // DEFAULT_CAMERA_APP_SETTINGS_PROTO. The proto3 zero-value instance maps every enum to
        // <ENUM_NAME>_UNSPECIFIED and audio_enabled to false, which does not match the documented
        // application defaults.
        assertThat(
            CameraAppSettingsProto.getDefaultInstance().toModel(CaptureMode.STANDARD)
        ).isNotEqualTo(DEFAULT_CAMERA_APP_SETTINGS)
    }
}
