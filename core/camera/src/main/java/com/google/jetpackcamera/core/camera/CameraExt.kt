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
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.media.CamcorderProfile
import android.media.EncoderProfiles
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.util.Range
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
import com.google.jetpackcamera.core.camera.lowlight.LowLightBoostAvailabilityChecker
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
import com.google.jetpackcamera.settings.model.BitrateConstraints

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

val CameraInfo.sensorRect: Rect
    @OptIn(ExperimentalCamera2Interop::class)
    get() = Camera2CameraInfo.from(this)
        .getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        .let { sensorRect ->
            if ("robolectric" == Build.FINGERPRINT && sensorRect == null) {
                return Rect(0, 0, 4000, 3000)
            }
            return requireNotNull(sensorRect) { "Sensor rect not available." }
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

/**
 * Checks if the camera advertises the 10-bit dynamic range output capability
 * ([CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT]).
 *
 * This is the capability CameraX requires before it will bind any 10-bit (HDR) stream
 * combination. Always `false` below API 33, where 10-bit dynamic range profiles do not exist.
 */
internal val CameraCharacteristics.isTenBitDynamicRangeSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            ?.contains(
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT
            ) ?: false

internal fun CameraCharacteristics.filterSupportedVideoDynamicRanges(
    lensFacing: LensFacing,
    reportedDynamicRanges: Set<DynamicRange>
): Set<DynamicRange> {
    val supportedDynamicRanges =
        if (isTenBitDynamicRangeSupported) {
            reportedDynamicRanges
        } else {
            setOf(DynamicRange.SDR)
        }
    if (supportedDynamicRanges != reportedDynamicRanges) {
        Log.w(
            TAG,
            "$lensFacing camera reports $reportedDynamicRanges but does " +
                "not advertise the DYNAMIC_RANGE_TEN_BIT capability. " +
                "Restricting dynamic ranges to $supportedDynamicRanges."
        )
    }
    return supportedDynamicRanges
}

@OptIn(ExperimentalCamera2Interop::class)
suspend fun CameraInfo.getLowLightBoostAvailability(
    context: Context,
    availabilityChecker: LowLightBoostAvailabilityChecker?
): LowLightBoostAvailability {
    val camera2Info = Camera2CameraInfo.from(this)
    var llbAeModeSupport = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        llbAeModeSupport = camera2Info
            .getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
            ?.contains(
                CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY
            ) ?: false
    }

    val llbImplementationAvailable =
        availabilityChecker?.isImplementationAvailable(this, context) ?: false

    return if (llbAeModeSupport) {
        if (llbImplementationAvailable) {
            LowLightBoostAvailability.AE_MODE_AND_CAMERA_EFFECT
        } else {
            LowLightBoostAvailability.AE_MODE_ONLY
        }
    } else if (llbImplementationAvailable) {
        LowLightBoostAvailability.CAMERA_EFFECT_ONLY
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

@OptIn(ExperimentalCamera2Interop::class)
internal fun CameraInfo.getBitrateConstraints(
    supportedVideoQualitiesMap: Map<DynamicRange, List<VideoQuality>>
): Pair<Map<DynamicRange, Map<VideoQuality, BitrateConstraints>>, BitrateConstraints?> {
    val cameraId = runCatching { Camera2CameraInfo.from(this).cameraId }.getOrNull()
        ?: return emptyMap<DynamicRange, Map<VideoQuality, BitrateConstraints>>() to null
    val cameraIdInt = cameraId.toIntOrNull()
    val encoderInfos = runCatching {
        MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.filter { it.isEncoder }
    }.getOrDefault(emptyList())
    val videoRangeCache = mutableMapOf<String, Range<Int>?>()
    val audioRangeCache = mutableMapOf<String, Range<Int>?>()

    fun videoRangeForMime(mime: String): Range<Int>? = videoRangeCache.getOrPut(mime) {
        runCatching {
            encoderInfos.firstOrNull { info ->
                info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
            }?.getCapabilitiesForType(mime)?.videoCapabilities?.bitrateRange
        }.getOrNull()
    }

    fun audioRangeForMime(mime: String): Range<Int>? = audioRangeCache.getOrPut(mime) {
        runCatching {
            encoderInfos.firstOrNull { info ->
                info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
            }?.getCapabilitiesForType(mime)?.audioCapabilities?.bitrateRange
        }.getOrNull()
    }

    var audioBitrateConstraints: BitrateConstraints? = null
    val videoBitrateConstraintsMap =
        buildMap<DynamicRange, Map<VideoQuality, BitrateConstraints>> {
            for ((dynamicRange, qualities) in supportedVideoQualitiesMap) {
                val qualityMap = buildMap<VideoQuality, BitrateConstraints> {
                    for (quality in qualities) {
                        val camcorderQuality = quality.toCamcorderQuality() ?: continue
                        if (cameraIdInt != null &&
                            !runCatching {
                                CamcorderProfile.hasProfile(cameraIdInt, camcorderQuality)
                            }.getOrDefault(false)
                        ) {
                            continue
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val profiles = runCatching {
                                CamcorderProfile.getAll(cameraId, camcorderQuality)
                            }.getOrNull()
                            val videoProfile = profiles?.videoProfiles?.firstOrNull { profile ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    when (dynamicRange) {
                                        DynamicRange.SDR ->
                                            profile.hdrFormat ==
                                                EncoderProfiles.VideoProfile.HDR_NONE &&
                                                profile.bitDepth == 8
                                        DynamicRange.HLG10 ->
                                            profile.hdrFormat ==
                                                EncoderProfiles.VideoProfile.HDR_HLG &&
                                                profile.bitDepth == 10
                                    }
                                } else {
                                    dynamicRange == DynamicRange.SDR
                                }
                            } ?: profiles?.videoProfiles?.firstOrNull()
                            if (videoProfile != null) {
                                put(
                                    quality,
                                    BitrateConstraints(
                                        defaultBitrate = videoProfile.bitrate.takeIf { it > 0 },
                                        supportedRange = videoRangeForMime(videoProfile.mediaType)
                                    )
                                )
                            }
                            if (audioBitrateConstraints == null) {
                                profiles?.audioProfiles?.firstOrNull()?.let { audioProfile ->
                                    audioBitrateConstraints = BitrateConstraints(
                                        defaultBitrate = audioProfile.bitrate.takeIf { it > 0 },
                                        supportedRange = audioRangeForMime(audioProfile.mediaType)
                                    )
                                }
                            }
                        } else if (cameraIdInt != null && dynamicRange == DynamicRange.SDR) {
                            @Suppress("DEPRECATION")
                            val profile = runCatching {
                                CamcorderProfile.get(cameraIdInt, camcorderQuality)
                            }.getOrNull()
                            if (profile != null) {
                                val videoMime = profile.videoCodec.toVideoMimeType()
                                put(
                                    quality,
                                    BitrateConstraints(
                                        defaultBitrate = profile.videoBitRate.takeIf { it > 0 },
                                        supportedRange = videoMime?.let(::videoRangeForMime)
                                    )
                                )
                                if (audioBitrateConstraints == null) {
                                    val audioMime = profile.audioCodec.toAudioMimeType()
                                    audioBitrateConstraints = BitrateConstraints(
                                        defaultBitrate = profile.audioBitRate.takeIf { it > 0 },
                                        supportedRange = audioMime?.let(::audioRangeForMime)
                                    )
                                }
                            }
                        }
                    }
                }
                if (qualityMap.isNotEmpty()) {
                    put(dynamicRange, qualityMap)
                }
            }
        }
    if (audioBitrateConstraints == null) {
        audioRangeForMime(MediaFormat.MIMETYPE_AUDIO_AAC)?.let { range ->
            audioBitrateConstraints = BitrateConstraints(
                defaultBitrate = null,
                supportedRange = range
            )
        }
    }
    return videoBitrateConstraintsMap to audioBitrateConstraints
}

private fun VideoQuality.toCamcorderQuality(): Int? = when (this) {
    SD -> CamcorderProfile.QUALITY_480P
    HD -> CamcorderProfile.QUALITY_720P
    FHD -> CamcorderProfile.QUALITY_1080P
    UHD -> CamcorderProfile.QUALITY_2160P
    UNSPECIFIED -> null
}

private fun Int.toVideoMimeType(): String? = when (this) {
    MediaRecorder.VideoEncoder.H263 -> MediaFormat.MIMETYPE_VIDEO_H263
    MediaRecorder.VideoEncoder.H264 -> MediaFormat.MIMETYPE_VIDEO_AVC
    MediaRecorder.VideoEncoder.MPEG_4_SP -> MediaFormat.MIMETYPE_VIDEO_MPEG4
    MediaRecorder.VideoEncoder.VP8 -> MediaFormat.MIMETYPE_VIDEO_VP8
    MediaRecorder.VideoEncoder.HEVC -> MediaFormat.MIMETYPE_VIDEO_HEVC
    else -> null
}

private fun Int.toAudioMimeType(): String? = when (this) {
    MediaRecorder.AudioEncoder.AMR_NB -> MediaFormat.MIMETYPE_AUDIO_AMR_NB
    MediaRecorder.AudioEncoder.AMR_WB -> MediaFormat.MIMETYPE_AUDIO_AMR_WB
    MediaRecorder.AudioEncoder.AAC,
    MediaRecorder.AudioEncoder.HE_AAC,
    MediaRecorder.AudioEncoder.AAC_ELD -> MediaFormat.MIMETYPE_AUDIO_AAC
    MediaRecorder.AudioEncoder.VORBIS -> MediaFormat.MIMETYPE_AUDIO_VORBIS
    MediaRecorder.AudioEncoder.OPUS -> MediaFormat.MIMETYPE_AUDIO_OPUS
    else -> null
}

fun UseCaseGroup.getVideoCapture() = getUseCaseOrNull<VideoCapture<Recorder>>()
fun UseCaseGroup.getImageCapture() = getUseCaseOrNull<ImageCapture>()

private inline fun <reified T : UseCase> UseCaseGroup.getUseCaseOrNull(): T? {
    return useCases.filterIsInstance<T>().singleOrNull()
}
