/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange as CXDynamicRange
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.video.Quality
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import com.google.android.gms.cameralowlight.LowLightBoost
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LowLightBoostAvailability
import com.google.jetpackcamera.model.TestPattern
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.model.VideoQuality.FHD
import com.google.jetpackcamera.model.VideoQuality.HD
import com.google.jetpackcamera.model.VideoQuality.SD
import com.google.jetpackcamera.model.VideoQuality.UHD
import com.google.jetpackcamera.model.VideoQuality.UNSPECIFIED
import kotlinx.coroutines.tasks.await

private const val TAG = "CameraExt"

val CameraInfo.appLensFacing: LensFacing
    get() = when (this.lensFacing) {
        CameraSelector.LENS_FACING_FRONT -> LensFacing.FRONT
        CameraSelector.LENS_FACING_BACK -> LensFacing.BACK
        else -> throw IllegalArgumentException(
            "Unknown CameraSelector.LensFacing -> LensFacing mapping. " +
                "[CameraSelector.LensFacing: ${this.lensFacing}]"
        )
    }

fun CXDynamicRange.toSupportedAppDynamicRange(): DynamicRange? {
    return when (this) {
        CXDynamicRange.SDR -> DynamicRange.SDR
        CXDynamicRange.HLG_10_BIT -> DynamicRange.HLG10
        // All other dynamic ranges unsupported. Return null.
        else -> null
    }
}

fun DynamicRange.toCXDynamicRange(): CXDynamicRange {
    return when (this) {
        DynamicRange.SDR -> CXDynamicRange.SDR
        DynamicRange.HLG10 -> CXDynamicRange.HLG_10_BIT
    }
}

fun LensFacing.toCameraSelector(): CameraSelector = when (this) {
    LensFacing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
    LensFacing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
}

val CameraInfo.sensorLandscapeRatio: Float
    @OptIn(ExperimentalCamera2Interop::class)
    get() = Camera2CameraInfo.from(this)
        .getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        ?.let { sensorRect ->
            if (sensorRect.width() > sensorRect.height()) {
                sensorRect.width().toFloat() / sensorRect.height()
            } else {
                sensorRect.height().toFloat() / sensorRect.width()
            }
        } ?: Float.NaN

fun Int.toAppImageFormat(): ImageOutputFormat? {
    return when (this) {
        ImageCapture.OUTPUT_FORMAT_JPEG -> ImageOutputFormat.JPEG
        ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR -> ImageOutputFormat.JPEG_ULTRA_HDR
        // All other output formats unsupported. Return null.
        else -> null
    }
}

fun VideoQuality.toQuality(): Quality? {
    return when (this) {
        SD -> Quality.SD
        HD -> Quality.HD
        FHD -> Quality.FHD
        UHD -> Quality.UHD
        UNSPECIFIED -> null
    }
}

fun Quality.toVideoQuality(): VideoQuality {
    return when (this) {
        Quality.SD -> SD
        Quality.HD -> HD
        Quality.FHD -> FHD
        Quality.UHD -> UHD
        else -> UNSPECIFIED
    }
}

/**
 * Checks if preview stabilization is supported by the device.
 *
 */
val CameraInfo.isPreviewStabilizationSupported: Boolean
    get() = Preview.getPreviewCapabilities(this).isStabilizationSupported

/**
 * Checks if video stabilization is supported by the device.
 *
 */
val CameraInfo.isVideoStabilizationSupported: Boolean
    get() = Recorder.getVideoCapabilities(this).isStabilizationSupported

/** Checks if optical image stabilization (OIS) is supported by the device. */
val CameraInfo.isOpticalStabilizationSupported: Boolean
    @OptIn(ExperimentalCamera2Interop::class)
    get() = Camera2CameraInfo.from(this)
        .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
        ?.contains(
            CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
        ) ?: false

@OptIn(ExperimentalCamera2Interop::class)
suspend fun CameraInfo.getLowLightBoostAvailability(context: Context): LowLightBoostAvailability {
    val camera2Info = Camera2CameraInfo.from(this)

    // Check for LLB AE Mode support.
    var llbAEModeSupport = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        llbAEModeSupport = camera2Info
            .getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
            ?.contains(
                CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY
            ) ?: false
    }

    // Check for Google LLB support.
    var gLlbSupport = false
    var gLlbAvailable = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val cameraId = camera2Info.cameraId
        try {
            // TODO: Remove when Google LLB beta07 is available with this fixed.
            if (!isGooglePlayServicesWithVideoTimestampFixAvailable(context)) {
                throw Exception("Google Play Services with video timestamp fix not available.")
            }
            val lowLightBoostClient = LowLightBoost.getClient(context)
            gLlbSupport = lowLightBoostClient.isCameraSupported(cameraId).await()
            gLlbAvailable = lowLightBoostClient.isModuleInstalled().await()
            if (gLlbSupport && !gLlbAvailable) {
                // Install the module for future use, but the install will take too long to use
                // now since the camera needs to be opened right away.
                lowLightBoostClient.installModule(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set up Google Low Light Boost for camera $cameraId", e)
            gLlbSupport = false
            gLlbAvailable = false
        }
    }
    return if (llbAEModeSupport) {
        if (gLlbSupport && gLlbAvailable) {
            LowLightBoostAvailability.AE_MODE_AND_GOOGLE_PLAY_SERVICES
        } else {
            LowLightBoostAvailability.AE_MODE_ONLY
        }
    } else if (gLlbSupport && gLlbAvailable) {
        LowLightBoostAvailability.GOOGLE_PLAY_SERVICES_ONLY
    } else {
        LowLightBoostAvailability.NONE
    }
}

val CameraInfo.availableTestPatterns: Set<TestPattern>
    @OptIn(ExperimentalCamera2Interop::class)
    get() = buildSet {
        add(TestPattern.Off)
        Camera2CameraInfo.from(this@availableTestPatterns)
            .getCameraCharacteristic(CameraCharacteristics.SENSOR_AVAILABLE_TEST_PATTERN_MODES)
            ?.forEach { pattern ->
                when (pattern) {
                    CameraMetadata.SENSOR_TEST_PATTERN_MODE_OFF -> TestPattern.Off
                    CameraMetadata.SENSOR_TEST_PATTERN_MODE_COLOR_BARS -> TestPattern.ColorBars
                    CameraMetadata.SENSOR_TEST_PATTERN_MODE_COLOR_BARS_FADE_TO_GRAY ->
                        TestPattern.ColorBarsFadeToGray
                    CameraMetadata.SENSOR_TEST_PATTERN_MODE_PN9 -> TestPattern.PN9
                    CameraMetadata.SENSOR_TEST_PATTERN_MODE_CUSTOM1 -> TestPattern.Custom1
                    // Use white as a stand-in for any solid color test pattern
                    CameraMetadata.SENSOR_TEST_PATTERN_MODE_SOLID_COLOR ->
                        TestPattern.SolidColor.WHITE
                    else -> {
                        // Ignore unknown test pattern mode
                        null
                    }
                }?.let { add(it) }
            }
    }

fun CameraInfo.filterSupportedFixedFrameRates(desired: Set<Int>): Set<Int> {
    return buildSet {
        this@filterSupportedFixedFrameRates.supportedFrameRateRanges.forEach { e ->
            if (e.upper == e.lower && desired.contains(e.upper)) {
                add(e.upper)
            }
        }
    }
}

val CameraInfo.supportedImageFormats: Set<ImageOutputFormat>
    get() = ImageCapture.getImageCaptureCapabilities(this).supportedOutputFormats
        .mapNotNull(Int::toAppImageFormat)
        .toSet()

fun UseCaseGroup.getVideoCapture() = getUseCaseOrNull<VideoCapture<Recorder>>()
fun UseCaseGroup.getImageCapture() = getUseCaseOrNull<ImageCapture>()

private inline fun <reified T : UseCase> UseCaseGroup.getUseCaseOrNull(): T? {
    return useCases.filterIsInstance<T>().singleOrNull()
}

// TODO: Remove when Google LLB beta07 is available with this fixed.
fun isGooglePlayServicesWithVideoTimestampFixAvailable(context: Context): Boolean {
    val minVersion = 253300000 // (Y25W33)
    return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context, minVersion) ==
        ConnectionResult.SUCCESS
}
