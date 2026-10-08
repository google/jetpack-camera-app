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
package com.google.jetpackcamera.core.camera.submode

import android.content.Context
import androidx.camera.core.CameraInfo
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.CaptureSubModeDescriptor
import com.google.jetpackcamera.model.CaptureSubModeId
import com.google.jetpackcamera.settings.model.CameraFeaturePolicy
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CaptureSubModeProviderTest {

    /** A provider that only implements the members without a default implementation. */
    private val provider = object : CaptureSubModeProvider {
        override val descriptor = CaptureSubModeDescriptor(
            id = CaptureSubModeId("test_sub_mode"),
            parentCaptureMode = CaptureMode.IMAGE_ONLY,
            labelResId = 0
        )

        override suspend fun isSupported(
            context: Context,
            cameraProvider: ProcessCameraProvider,
            cameraInfo: CameraInfo
        ): Boolean = true

        override val sessionBinding: CameraSessionBinding =
            CameraSessionBinding.SingleCamera { _, baseSelector -> baseSelector }
    }

    @Test
    fun featurePolicy_byDefault_isUnrestricted() {
        assertThat(provider.featurePolicy).isEqualTo(CameraFeaturePolicy())
    }

    @Test
    fun isCompatibleWith_byDefault_requiresParentCaptureMode() {
        val inParentMode = DEFAULT_CAMERA_APP_SETTINGS.copy(captureMode = CaptureMode.IMAGE_ONLY)
        val inOtherMode = DEFAULT_CAMERA_APP_SETTINGS.copy(captureMode = CaptureMode.VIDEO_ONLY)
        // The default implementation only looks at the capture mode, so the constraints are empty.
        val constraints = CameraSystemConstraints()

        assertThat(provider.isCompatibleWith(inParentMode, constraints)).isTrue()
        assertThat(provider.isCompatibleWith(inOtherMode, constraints)).isFalse()
    }

    @Test
    fun sessionBinding_supportsSingleCameraAndCustomVariants() {
        val customBinding: CameraSessionBinding = CameraSessionBinding.Custom {}
        assertThat(provider.sessionBinding).isInstanceOf(CameraSessionBinding.SingleCamera::class.java)
        assertThat(customBinding).isInstanceOf(CameraSessionBinding.Custom::class.java)
    }
}
