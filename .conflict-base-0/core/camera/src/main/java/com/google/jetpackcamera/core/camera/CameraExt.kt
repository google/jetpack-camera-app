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

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange as CXDynamicRange
import androidx.camera.core.ExperimentalImageCaptureOutputFormat
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.LensFacing

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
        com.google.jetpackcamera.settings.model.DynamicRange.SDR -> CXDynamicRange.SDR
        com.google.jetpackcamera.settings.model.DynamicRange.HLG10 -> CXDynamicRange.HLG_10_BIT
    }
}

fun LensFacing.toCameraSelector(): CameraSelector = when (this) {
    LensFacing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
    LensFacing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
}

@SuppressLint("RestrictedApi")
fun CameraSelector.toAppLensFacing(): LensFacing = when (this.lensFacing) {
    CameraSelector.LENS_FACING_FRONT -> LensFacing.FRONT
    CameraSelector.LENS_FACING_BACK -> LensFacing.BACK
    else -> throw IllegalArgumentException(
        "Unknown CameraSelector -> LensFacing mapping. [CameraSelector: $this]"
    )
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

@OptIn(ExperimentalImageCaptureOutputFormat::class)
fun Int.toAppImageFormat(): ImageOutputFormat? {
    return when (this) {
        ImageCapture.OUTPUT_FORMAT_JPEG -> ImageOutputFormat.JPEG
        ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR -> ImageOutputFormat.JPEG_ULTRA_HDR
        // All other output formats unsupported. Return null.
        else -> null
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
    @OptIn(ExperimentalImageCaptureOutputFormat::class)
    get() = ImageCapture.getImageCaptureCapabilities(this).supportedOutputFormats
        .mapNotNull(Int::toAppImageFormat)
        .toSet()

fun UseCaseGroup.getVideoCapture() = getUseCaseOrNull<VideoCapture<Recorder>>()
fun UseCaseGroup.getImageCapture() = getUseCaseOrNull<ImageCapture>()

private inline fun <reified T : UseCase> UseCaseGroup.getUseCaseOrNull(): T? {
    return useCases.filterIsInstance<T>().singleOrNull()
}
