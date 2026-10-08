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
package com.google.jetpackcamera.core.camera.night

import android.content.Context
import android.os.Build
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CameraEffectId
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import java.lang.reflect.Proxy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S_V2])
class NightCaptureSubModeProviderTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val provider = NightCaptureSubModeProvider(context)

    @Test
    fun descriptor_hasImageOnlyParentAndNightId() {
        assertThat(NightCaptureSubModeKey.id).isEqualTo(NIGHT_SUB_MODE_ID)
        assertThat(provider.descriptor.id).isEqualTo(NIGHT_SUB_MODE_ID)
        assertThat(provider.descriptor.parentCaptureMode).isEqualTo(CaptureMode.IMAGE_ONLY)
        assertThat(provider.descriptor.quickSettingsTitleResId)
            .isEqualTo(R.string.quick_settings_title_night_settings)
    }

    @Test
    fun isCompatibleWith_returnsTrueOnlyForImageOnly() {
        val constraints = CameraSystemConstraints()

        assertThat(
            provider.isCompatibleWith(
                settings = CameraAppSettings(captureMode = CaptureMode.IMAGE_ONLY),
                systemConstraints = constraints,
                externalCaptureMode = ExternalCaptureMode.Standard
            )
        ).isTrue()

        assertThat(
            provider.isCompatibleWith(
                settings = CameraAppSettings(captureMode = CaptureMode.STANDARD),
                systemConstraints = constraints,
                externalCaptureMode = ExternalCaptureMode.Standard
            )
        ).isFalse()

        assertThat(
            provider.isCompatibleWith(
                settings = CameraAppSettings(captureMode = CaptureMode.VIDEO_ONLY),
                systemConstraints = constraints,
                externalCaptureMode = ExternalCaptureMode.Standard
            )
        ).isFalse()

        assertThat(
            provider.isCompatibleWith(
                settings = CameraAppSettings(captureMode = CaptureMode.IMAGE_ONLY),
                systemConstraints = constraints,
                externalCaptureMode = ExternalCaptureMode.VideoCapture
            )
        ).isFalse()

        assertThat(
            provider.isCompatibleWith(
                settings = CameraAppSettings(
                    captureMode = CaptureMode.IMAGE_ONLY,
                    selectedCameraEffect = CameraEffectId("custom_effect")
                ),
                systemConstraints = constraints,
                externalCaptureMode = ExternalCaptureMode.Standard
            )
        ).isFalse()
    }

    @Test
    fun featurePolicy_enforcesSdrJpegAndExcludesLowLightBoost() {
        assertThat(provider.featurePolicy.captureMode).isNull()

        val restrictedFromLlb = provider.featurePolicy.enforceRestrictions(
            CameraAppSettings(
                captureMode = CaptureMode.IMAGE_ONLY,
                dynamicRange = DynamicRange.HLG10,
                imageFormat = ImageOutputFormat.JPEG_ULTRA_HDR,
                flashMode = FlashMode.LOW_LIGHT_BOOST
            )
        )
        assertThat(restrictedFromLlb.captureMode).isEqualTo(CaptureMode.IMAGE_ONLY)
        assertThat(restrictedFromLlb.dynamicRange).isEqualTo(DynamicRange.SDR)
        assertThat(restrictedFromLlb.imageFormat).isEqualTo(ImageOutputFormat.JPEG)
        assertThat(restrictedFromLlb.flashMode).isEqualTo(FlashMode.OFF)

        val restrictedWithFlashOn = provider.featurePolicy.enforceRestrictions(
            CameraAppSettings(
                captureMode = CaptureMode.IMAGE_ONLY,
                flashMode = FlashMode.ON
            )
        )
        assertThat(restrictedWithFlashOn.flashMode).isEqualTo(FlashMode.ON)

        val restrictedWithFlashAuto = provider.featurePolicy.enforceRestrictions(
            CameraAppSettings(
                captureMode = CaptureMode.IMAGE_ONLY,
                flashMode = FlashMode.AUTO
            )
        )
        assertThat(restrictedWithFlashAuto.flashMode).isEqualTo(FlashMode.AUTO)
    }

    @Test
    fun isSupportedAndSessionBinding_whenExtensionUnavailable_returnsFalseAndBaseSelector() =
        runTest {
            val cameraProvider = ProcessCameraProvider.awaitInstance(context)
            val cameraInfo = fakeCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA)

            assertThat(provider.isSupported(context, cameraProvider, cameraInfo)).isFalse()
            assertThat(
                provider.sessionBinding.transformCameraSelector(
                    cameraProvider,
                    CameraSelector.DEFAULT_BACK_CAMERA
                )
            ).isSameInstanceAs(CameraSelector.DEFAULT_BACK_CAMERA)
        }

    @Test
    fun isSupportedAndSessionBinding_whenExtensionAvailable_returnsNightEnabledSelector() =
        runTest {
            val nightSelector = CameraSelector.Builder().build()
            val supportedProvider = NightCaptureSubModeProvider(
                applicationContext = context,
                isNightAvailable = { _, selector ->
                    selector === CameraSelector.DEFAULT_BACK_CAMERA
                },
                enableNightSelector = { _, _ -> nightSelector }
            )
            val cameraProvider = ProcessCameraProvider.awaitInstance(context)
            val backCameraInfo = fakeCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA)

            assertThat(supportedProvider.isSupported(context, cameraProvider, backCameraInfo))
                .isTrue()
            assertThat(
                supportedProvider.sessionBinding.transformCameraSelector(
                    cameraProvider,
                    CameraSelector.DEFAULT_BACK_CAMERA
                )
            ).isSameInstanceAs(nightSelector)
            assertThat(
                supportedProvider.sessionBinding.transformCameraSelector(
                    cameraProvider,
                    CameraSelector.DEFAULT_FRONT_CAMERA
                )
            ).isSameInstanceAs(CameraSelector.DEFAULT_FRONT_CAMERA)
        }

    @Test
    fun isSupportedAndSessionBinding_whenQueryThrowsException_fallsBackGracefully() = runTest {
        val throwingProvider = NightCaptureSubModeProvider(
            applicationContext = context,
            isNightAvailable = { _, _ -> throw IllegalStateException("Extension failure") },
            enableNightSelector = { _, selector -> selector }
        )
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        val backCameraInfo = fakeCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA)

        assertThat(throwingProvider.isSupported(context, cameraProvider, backCameraInfo)).isFalse()
        assertThat(
            throwingProvider.sessionBinding.transformCameraSelector(
                cameraProvider,
                CameraSelector.DEFAULT_BACK_CAMERA
            )
        ).isSameInstanceAs(CameraSelector.DEFAULT_BACK_CAMERA)

        val cancellingProvider = NightCaptureSubModeProvider(
            applicationContext = context,
            isNightAvailable = { _, _ ->
                throw kotlinx.coroutines.CancellationException("Cancelled")
            },
            enableNightSelector = { _, selector -> selector }
        )
        val isSupportedCancelled = try {
            !cancellingProvider.isSupported(context, cameraProvider, backCameraInfo)
        } catch (_: kotlinx.coroutines.CancellationException) {
            true
        }
        assertThat(isSupportedCancelled).isTrue()

        val sessionBindingCancelled = try {
            cancellingProvider.sessionBinding.transformCameraSelector(
                cameraProvider,
                CameraSelector.DEFAULT_BACK_CAMERA
            ) === CameraSelector.DEFAULT_BACK_CAMERA
        } catch (_: kotlinx.coroutines.CancellationException) {
            true
        }
        assertThat(sessionBindingCancelled).isTrue()
    }

    private fun fakeCameraInfo(selector: CameraSelector): CameraInfo = Proxy.newProxyInstance(
        CameraInfo::class.java.classLoader,
        arrayOf(CameraInfo::class.java)
    ) { _, _, _ -> selector } as CameraInfo
}
