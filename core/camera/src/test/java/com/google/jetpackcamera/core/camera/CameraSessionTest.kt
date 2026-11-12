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
@file:OptIn(InternalCameraApi::class)

package com.google.jetpackcamera.core.camera

import android.graphics.Rect
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.submode.CaptureSubModeFeatureKey
import com.google.jetpackcamera.core.camera.testing.FakeCameraSystem
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.CaptureSubModeId
import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LowLightBoostPriority
import com.google.jetpackcamera.model.MirrorMode
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.VideoQuality
import java.lang.reflect.Proxy
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CameraSessionTest {

    @Test
    fun getVideoQualityFromResolution_landscapeResolutions_returnsExpectedQuality() {
        assertThat(getVideoQualityFromResolution(Size(640, 480))).isEqualTo(VideoQuality.SD)
        assertThat(getVideoQualityFromResolution(Size(1280, 720))).isEqualTo(VideoQuality.HD)
        assertThat(getVideoQualityFromResolution(Size(1920, 1080))).isEqualTo(VideoQuality.FHD)
        assertThat(getVideoQualityFromResolution(Size(2560, 1440))).isEqualTo(VideoQuality.FHD)
        assertThat(getVideoQualityFromResolution(Size(3840, 2160))).isEqualTo(VideoQuality.UHD)
    }

    @Test
    fun getVideoQualityFromResolution_portraitResolutions_returnsExpectedQuality() {
        assertThat(getVideoQualityFromResolution(Size(480, 640))).isEqualTo(VideoQuality.SD)
        assertThat(getVideoQualityFromResolution(Size(720, 1280))).isEqualTo(VideoQuality.HD)
        assertThat(getVideoQualityFromResolution(Size(1080, 1920))).isEqualTo(VideoQuality.FHD)
        assertThat(getVideoQualityFromResolution(Size(1440, 2560))).isEqualTo(VideoQuality.FHD)
        assertThat(getVideoQualityFromResolution(Size(2160, 3840))).isEqualTo(VideoQuality.UHD)
    }

    @Test
    fun getVideoQualityFromResolution_rangeBoundaries_returnsExpectedQuality() {
        assertThat(getVideoQualityFromResolution(Size(320, 240)))
            .isEqualTo(VideoQuality.UNSPECIFIED)
        assertThat(getVideoQualityFromResolution(Size(320, 241))).isEqualTo(VideoQuality.SD)
        assertThat(getVideoQualityFromResolution(Size(1280, 719))).isEqualTo(VideoQuality.SD)
        assertThat(getVideoQualityFromResolution(Size(1280, 720))).isEqualTo(VideoQuality.HD)
        assertThat(getVideoQualityFromResolution(Size(1920, 1079))).isEqualTo(VideoQuality.HD)
        assertThat(getVideoQualityFromResolution(Size(1920, 1080))).isEqualTo(VideoQuality.FHD)
        assertThat(getVideoQualityFromResolution(Size(3840, 2159))).isEqualTo(VideoQuality.FHD)
        assertThat(getVideoQualityFromResolution(Size(3840, 2160))).isEqualTo(VideoQuality.UHD)
        assertThat(getVideoQualityFromResolution(Size(7680, 4319))).isEqualTo(VideoQuality.UHD)
        assertThat(getVideoQualityFromResolution(Size(7680, 4320)))
            .isEqualTo(VideoQuality.UNSPECIFIED)
    }

    @Test
    fun getVideoQualityFromResolution_nullResolution_returnsUnspecified() {
        assertThat(getVideoQualityFromResolution(null)).isEqualTo(VideoQuality.UNSPECIFIED)
    }

    @Test
    fun cropRectDimensions_rotatedLandscapeCropRect_swapsToPortrait() {
        val landscapeCropRect = Rect(0, 0, 1280, 720)
        assertThat(getWidthFromCropRect(landscapeCropRect, rotationDegrees = 90)).isEqualTo(720)
        assertThat(getHeightFromCropRect(landscapeCropRect, rotationDegrees = 90)).isEqualTo(1280)
        assertThat(getWidthFromCropRect(landscapeCropRect, rotationDegrees = 270)).isEqualTo(720)
        assertThat(getHeightFromCropRect(landscapeCropRect, rotationDegrees = 270)).isEqualTo(1280)
    }

    @Test
    fun cropRectDimensions_unrotatedPortraitCropRect_preservesPortrait() {
        val portraitCropRect = Rect(0, 0, 720, 1280)
        assertThat(getWidthFromCropRect(portraitCropRect, rotationDegrees = 0)).isEqualTo(720)
        assertThat(getHeightFromCropRect(portraitCropRect, rotationDegrees = 0)).isEqualTo(1280)
        assertThat(getWidthFromCropRect(portraitCropRect, rotationDegrees = 180)).isEqualTo(720)
        assertThat(getHeightFromCropRect(portraitCropRect, rotationDegrees = 180)).isEqualTo(1280)
    }

    @Test
    fun cropRectDimensions_unrotatedLandscapeCropRect_preservesLandscape() {
        val landscapeCropRect = Rect(0, 0, 1280, 720)
        assertThat(getWidthFromCropRect(landscapeCropRect, rotationDegrees = 0)).isEqualTo(1280)
        assertThat(getHeightFromCropRect(landscapeCropRect, rotationDegrees = 0)).isEqualTo(720)
        assertThat(getWidthFromCropRect(landscapeCropRect, rotationDegrees = 180)).isEqualTo(1280)
        assertThat(getHeightFromCropRect(landscapeCropRect, rotationDegrees = 180)).isEqualTo(720)
    }

    @Test
    fun cropRectDimensions_nullCropRect_returnsZero() {
        assertThat(getWidthFromCropRect(null, rotationDegrees = 0)).isEqualTo(0)
        assertThat(getHeightFromCropRect(null, rotationDegrees = 0)).isEqualTo(0)
        assertThat(getWidthFromCropRect(null, rotationDegrees = 90)).isEqualTo(0)
        assertThat(getHeightFromCropRect(null, rotationDegrees = 90)).isEqualTo(0)
    }

    @Test
    fun previewAndViewPort_syncWithWindowRotation_independentlyOfDeviceRotation() {
        val phoneSensorInfo = fakeBackCameraInfo(sensorOrientationDegrees = 90)
        val tabletLandscapeSensorInfo = fakeBackCameraInfo(sensorOrientationDegrees = 0)

        val expectedPhoneViewPorts = mapOf(
            DeviceRotation.Natural to (Surface.ROTATION_0 to Rational(3, 4)),
            DeviceRotation.Rotated270 to (Surface.ROTATION_90 to Rational(4, 3)),
            DeviceRotation.Rotated180 to (Surface.ROTATION_180 to Rational(3, 4)),
            DeviceRotation.Rotated90 to (Surface.ROTATION_270 to Rational(4, 3))
        )
        val expectedTabletViewPorts = mapOf(
            DeviceRotation.Natural to (Surface.ROTATION_0 to Rational(4, 3)),
            DeviceRotation.Rotated270 to (Surface.ROTATION_90 to Rational(3, 4)),
            DeviceRotation.Rotated180 to (Surface.ROTATION_180 to Rational(4, 3)),
            DeviceRotation.Rotated90 to (Surface.ROTATION_270 to Rational(3, 4))
        )

        // Covers foldables, landscape-native tablets, and portrait W <= H split-screen panes on
        // Rotated90 / Rotated270 displays across independent physical deviceRotations.
        for (windowRot in DeviceRotation.entries) {
            for (deviceRot in DeviceRotation.entries) {
                val (expectedSurfaceRot, expectedPhoneRatio) =
                    expectedPhoneViewPorts.getValue(windowRot)

                val phonePreview = Preview.Builder().applyWindowRotation(windowRot).build()
                val phoneImage = ImageCapture.Builder()
                    .setTargetRotation(deviceRot.toUiSurfaceRotation())
                    .build()
                val phoneViewPort = createSensorAlignedViewPort(
                    cameraInfo = phoneSensorInfo,
                    aspectRatio = AspectRatio.THREE_FOUR,
                    targetRotation = phonePreview.targetRotation
                )
                val phoneGroup = UseCaseGroup.Builder()
                    .setViewPort(phoneViewPort)
                    .addUseCase(phonePreview)
                    .addUseCase(phoneImage)
                    .build()

                assertThat(phonePreview.targetRotation).isEqualTo(expectedSurfaceRot)
                assertThat(phoneGroup.viewPort!!.rotation).isEqualTo(expectedSurfaceRot)
                assertThat(phoneGroup.viewPort!!.aspectRatio).isEqualTo(expectedPhoneRatio)
                assertThat(phoneImage.targetRotation).isEqualTo(deviceRot.toUiSurfaceRotation())

                val (_, expectedTabletRatio) = expectedTabletViewPorts.getValue(windowRot)
                val tabletPreview = Preview.Builder().applyWindowRotation(windowRot).build()
                val tabletViewPort = createSensorAlignedViewPort(
                    cameraInfo = tabletLandscapeSensorInfo,
                    aspectRatio = AspectRatio.THREE_FOUR,
                    targetRotation = tabletPreview.targetRotation
                )
                assertThat(tabletPreview.targetRotation).isEqualTo(expectedSurfaceRot)
                assertThat(tabletViewPort.rotation).isEqualTo(expectedSurfaceRot)
                assertThat(tabletViewPort.aspectRatio).isEqualTo(expectedTabletRatio)
            }
        }
    }

    @Test
    fun perpetualSessionSettingsAndFakeCameraSystem_trackWindowRotationAcrossSessionTypes() {
        val baseSingle = PerpetualSessionSettings.SingleCamera(
            aspectRatio = AspectRatio.THREE_FOUR,
            captureMode = CaptureMode.STANDARD,
            activeCameraEffect = null,
            targetFrameRate = 30,
            stabilizationMode = StabilizationMode.OFF,
            dynamicRange = DynamicRange.SDR,
            videoQuality = VideoQuality.FHD,
            mirrorMode = MirrorMode.OFF,
            imageFormat = ImageOutputFormat.JPEG,
            lowLightBoostPriority = LowLightBoostPriority.PRIORITIZE_AE_MODE
        )
        assertThat(baseSingle.windowRotation).isEqualTo(DeviceRotation.Natural)
        assertThat(baseSingle.copy(windowRotation = DeviceRotation.Rotated90))
            .isNotEqualTo(baseSingle)

        val testKey = object : CaptureSubModeFeatureKey {
            override val id: CaptureSubModeId = CaptureSubModeId("test_concurrent")
        }
        val baseCustom = PerpetualSessionSettings.CustomSession(
            subModeKey = testKey,
            aspectRatio = AspectRatio.NINE_SIXTEEN,
            captureMode = CaptureMode.STANDARD
        )
        assertThat(baseCustom.windowRotation).isEqualTo(DeviceRotation.Natural)
        assertThat(baseCustom.copy(windowRotation = DeviceRotation.Rotated270))
            .isNotEqualTo(baseCustom)

        val fakeSystem = FakeCameraSystem()
        assertThat(fakeSystem.windowRotation).isEqualTo(DeviceRotation.Natural)
        fakeSystem.setWindowRotation(DeviceRotation.Rotated180)
        assertThat(fakeSystem.windowRotation).isEqualTo(DeviceRotation.Rotated180)
    }

    private fun fakeBackCameraInfo(sensorOrientationDegrees: Int): CameraInfo =
        Proxy.newProxyInstance(
            CameraInfo::class.java.classLoader,
            arrayOf(CameraInfo::class.java)
        ) { proxy, method, args ->
            when (method.name) {
                "equals" -> proxy === args?.firstOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "FakeCameraInfo($sensorOrientationDegrees)"
                "getLensFacing" -> CameraSelector.LENS_FACING_BACK
                "getSensorRotationDegrees" -> {
                    val relativeSurfaceRotation =
                        (args?.firstOrNull() as? Int) ?: Surface.ROTATION_0
                    val surfaceCounterClockwiseDegrees = when (relativeSurfaceRotation) {
                        Surface.ROTATION_0 -> 0
                        Surface.ROTATION_90 -> 90
                        Surface.ROTATION_180 -> 180
                        Surface.ROTATION_270 -> 270
                        else -> 0
                    }
                    (sensorOrientationDegrees - surfaceCounterClockwiseDegrees + 360) % 360
                }
                else -> when (method.returnType) {
                    Boolean::class.javaPrimitiveType -> false
                    Int::class.javaPrimitiveType -> 0
                    Float::class.javaPrimitiveType -> 0f
                    else -> null
                }
            }
        } as CameraInfo
}
