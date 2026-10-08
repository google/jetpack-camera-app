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
package com.google.jetpackcamera.data.camera

import android.content.Intent
import android.provider.MediaStore
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.DebugSettings
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.LensFacing
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CameraLaunchConfigTest {

    @Test
    fun defaultCameraLaunchConfig_hasDefaultValues() {
        val config = CameraLaunchConfig()
        assertThat(config.externalCaptureMode).isEqualTo(ExternalCaptureMode.Standard)
        assertThat(config.debugSettings).isEqualTo(DebugSettings())
    }

    @Test
    fun cameraLaunchConfigProvider_initiallyHasDefaultConfig() {
        val provider = CameraLaunchConfigProvider()
        assertThat(provider.config.value).isEqualTo(CameraLaunchConfig())
    }

    @Test
    fun cameraLaunchConfigProvider_setConfig_updatesConfig() {
        val provider = CameraLaunchConfigProvider()
        val newConfig = CameraLaunchConfig(
            externalCaptureMode = ExternalCaptureMode.ImageCapture,
            debugSettings = DebugSettings(isDebugModeEnabled = true)
        )
        provider.setConfig(newConfig)
        assertThat(provider.config.value).isEqualTo(newConfig)
    }

    @Test
    fun toExternalCaptureMode_mapsIntentsCorrectly() {
        assertThat(Intent(MediaStore.ACTION_IMAGE_CAPTURE).toExternalCaptureMode())
            .isEqualTo(ExternalCaptureMode.ImageCapture)
        assertThat(Intent(MediaStore.ACTION_VIDEO_CAPTURE).toExternalCaptureMode())
            .isEqualTo(ExternalCaptureMode.VideoCapture)
        assertThat(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).toExternalCaptureMode())
            .isEqualTo(ExternalCaptureMode.MultipleImageCapture)
        assertThat(Intent("UNKNOWN_ACTION").toExternalCaptureMode())
            .isEqualTo(ExternalCaptureMode.Standard)
        assertThat(Intent().toExternalCaptureMode())
            .isEqualTo(ExternalCaptureMode.Standard)
    }

    @Test
    fun toDebugSettings_mapsIntentsCorrectly() {
        val intent = Intent().apply {
            putExtra(KEY_DEBUG_MODE, true)
            putExtra(KEY_DEBUG_SINGLE_LENS_MODE, "front")
        }
        val debugSettings = intent.toDebugSettings()
        assertThat(debugSettings.isDebugModeEnabled).isTrue()
        assertThat(debugSettings.singleLensMode).isEqualTo(LensFacing.FRONT)

        val backIntent = Intent().apply {
            putExtra(KEY_DEBUG_SINGLE_LENS_MODE, "back")
        }
        assertThat(backIntent.toDebugSettings().singleLensMode).isEqualTo(LensFacing.BACK)

        val invalidIntent = Intent().apply {
            putExtra(KEY_DEBUG_SINGLE_LENS_MODE, "invalid")
        }
        assertThat(invalidIntent.toDebugSettings().singleLensMode).isNull()
    }

    @Test
    fun toDebugSettings_mapsVideoBitrateCorrectly() {
        // Positive int extra when debug mode is enabled
        val intBitrateIntent = Intent().apply {
            putExtra(KEY_DEBUG_MODE, true)
            putExtra(KEY_DEBUG_VIDEO_BITRATE, 10_000_000)
        }
        assertThat(intBitrateIntent.toDebugSettings().targetVideoBitrate).isEqualTo(10_000_000)

        // Positive long extra when debug mode is enabled
        val longBitrateIntent = Intent().apply {
            putExtra(KEY_DEBUG_MODE, true)
            putExtra(KEY_DEBUG_VIDEO_BITRATE, 15_000_000L)
        }
        assertThat(longBitrateIntent.toDebugSettings().targetVideoBitrate).isEqualTo(15_000_000)

        // Positive string extra when debug mode is enabled
        val stringBitrateIntent = Intent().apply {
            putExtra(KEY_DEBUG_MODE, true)
            putExtra(KEY_DEBUG_VIDEO_BITRATE, "8000000")
        }
        assertThat(stringBitrateIntent.toDebugSettings().targetVideoBitrate).isEqualTo(8_000_000)

        // Ignored when debug mode is disabled
        val debugDisabledIntent = Intent().apply {
            putExtra(KEY_DEBUG_MODE, false)
            putExtra(KEY_DEBUG_VIDEO_BITRATE, 10_000_000)
        }
        assertThat(debugDisabledIntent.toDebugSettings().targetVideoBitrate).isNull()

        // Zero or negative values fall back to null
        val zeroBitrateIntent = Intent().apply {
            putExtra(KEY_DEBUG_MODE, true)
            putExtra(KEY_DEBUG_VIDEO_BITRATE, 0)
        }
        assertThat(zeroBitrateIntent.toDebugSettings().targetVideoBitrate).isNull()

        val negativeBitrateIntent = Intent().apply {
            putExtra(KEY_DEBUG_MODE, true)
            putExtra(KEY_DEBUG_VIDEO_BITRATE, -500)
        }
        assertThat(negativeBitrateIntent.toDebugSettings().targetVideoBitrate).isNull()

        // Malformed string extra falls back to null
        val invalidStringBitrateIntent = Intent().apply {
            putExtra(KEY_DEBUG_MODE, true)
            putExtra(KEY_DEBUG_VIDEO_BITRATE, "invalid")
        }
        assertThat(invalidStringBitrateIntent.toDebugSettings().targetVideoBitrate).isNull()
    }

    @Test
    fun cameraLaunchConfigProvider_setIntent_updatesConfigFromIntent() {
        val provider = CameraLaunchConfigProvider()
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(KEY_DEBUG_MODE, true)
            putExtra(KEY_DEBUG_SINGLE_LENS_MODE, "back")
            putExtra(KEY_DEBUG_VIDEO_BITRATE, 12_000_000)
        }
        provider.setIntent(intent)

        assertThat(provider.config.value).isEqualTo(
            CameraLaunchConfig(
                externalCaptureMode = ExternalCaptureMode.ImageCapture,
                debugSettings = DebugSettings(
                    isDebugModeEnabled = true,
                    singleLensMode = LensFacing.BACK,
                    targetVideoBitrate = 12_000_000
                )
            )
        )
    }

    @Test
    fun cameraLaunchConfigProvider_setIntent_nullIntent_doesNotUpdate() {
        val provider = CameraLaunchConfigProvider()
        val originalConfig = provider.config.value
        provider.setIntent(null)
        assertThat(provider.config.value).isSameInstanceAs(originalConfig)
    }
}
