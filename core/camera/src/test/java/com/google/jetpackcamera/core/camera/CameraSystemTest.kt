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

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.CameraSystem.Companion.applyDiffs
import com.google.jetpackcamera.core.camera.testing.FakeCameraSystem
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.CameraAppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for [CameraSystem] extension functions, specifically [CameraAppSettings.applyDiffs].
 */
@RunWith(JUnit4::class)
class CameraSystemTest {

    private val fakeCameraSystem = FakeCameraSystem()

    /**
     * Verifies that when [CameraAppSettings.dynamicRange] changes, [applyDiffs] propagates
     * the new dynamic range to the [CameraSystem].
     */
    @Test
    fun applyDiffs_dynamicRangeChanged_propagatesToCameraSystem() = runTest {
        val oldSettings = CameraAppSettings(dynamicRange = DynamicRange.SDR)
        val newSettings = CameraAppSettings(dynamicRange = DynamicRange.HLG10)

        oldSettings.applyDiffs(newSettings, fakeCameraSystem)

        assertThat(fakeCameraSystem.getCurrentSettings().first()?.dynamicRange)
            .isEqualTo(DynamicRange.HLG10)
    }

    /**
     * Verifies that when [CameraAppSettings.imageFormat] changes, [applyDiffs] propagates
     * the new image format to the [CameraSystem].
     */
    @Test
    fun applyDiffs_imageFormatChanged_propagatesToCameraSystem() = runTest {
        val oldSettings = CameraAppSettings(imageFormat = ImageOutputFormat.JPEG)
        val newSettings = CameraAppSettings(imageFormat = ImageOutputFormat.JPEG_ULTRA_HDR)

        oldSettings.applyDiffs(newSettings, fakeCameraSystem)

        assertThat(fakeCameraSystem.getCurrentSettings().first()?.imageFormat)
            .isEqualTo(ImageOutputFormat.JPEG_ULTRA_HDR)
    }

    /**
     * Verifies that when settings have not changed, [applyDiffs] does not alter the camera
     * system settings.
     */
    @Test
    fun applyDiffs_unchangedSettings_doesNotAlterCameraSystem() = runTest {
        val initialSettings = CameraAppSettings(
            dynamicRange = DynamicRange.HLG10,
            imageFormat = ImageOutputFormat.JPEG_ULTRA_HDR
        )
        val newSettings = initialSettings.copy()

        // fakeCameraSystem defaults to SDR and JPEG. If unchanged settings are properly
        // skipped by applyDiffs, fakeCameraSystem's settings will remain unaltered.
        initialSettings.applyDiffs(newSettings, fakeCameraSystem)

        assertThat(fakeCameraSystem.getCurrentSettings().first()?.dynamicRange)
            .isEqualTo(DynamicRange.SDR)
        assertThat(fakeCameraSystem.getCurrentSettings().first()?.imageFormat)
            .isEqualTo(ImageOutputFormat.JPEG)
    }
}
