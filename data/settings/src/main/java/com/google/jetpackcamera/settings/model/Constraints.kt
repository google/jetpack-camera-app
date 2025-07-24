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
package com.google.jetpackcamera.settings.model

import android.util.Range
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.Illuminant
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.model.VideoQuality

/**
 * Represents the overall constraints and capabilities of the camera system on the device.
 *
 * This data class aggregates information about available lenses, support for concurrent
 * camera usage, and detailed constraints for each individual camera lens. It serves as a
 * central point for querying what features and settings are supported by the device's
 * camera hardware and software stack.
 *
 * @property availableLenses A list of [LensFacing] values indicating which camera lenses
 *                           (e.g., front, back) are available on the device.
 * @property concurrentCamerasSupported A boolean indicating whether the device supports
 *                                      operating multiple cameras concurrently.
 * @property perLensConstraints A map where each key is a [LensFacing] value and the
 *                              corresponding value is a [CameraConstraints] object
 *                              detailing the specific capabilities and limitations of that lens.
 */
data class SystemConstraints(
    val availableLenses: List<LensFacing> = emptyList(),
    val concurrentCamerasSupported: Boolean = false,
    val perLensConstraints: Map<LensFacing, CameraConstraints> = emptyMap()
)

inline fun <reified T> SystemConstraints.forDevice(
    crossinline constraintSelector: (CameraConstraints) -> Iterable<T>
) = perLensConstraints.values.asSequence().flatMap { constraintSelector(it) }.toSet()

/**
 * Defines the specific capabilities, limitations, and supported settings for a single camera lens.
 *
 * This data class encapsulates various constraints related to video and image capture for a
 * particular camera, such as supported stabilization modes, frame rates, dynamic ranges,
 * image formats, and zoom capabilities.
 *
 * @property supportedStabilizationModes A set of [StabilizationMode] values that are supported
 *                                       by this camera lens.
 * @property supportedFixedFrameRates A set of integers representing fixed frame rates (FPS)
 *                                    supported for video recording with this lens.
 *                                    May include values like [FPS_AUTO], [FPS_15], [FPS_30], [FPS_60].
 * @property supportedDynamicRanges A set of [DynamicRange] values (e.g., SDR, HDR10) that
 *                                  this camera lens can capture.
 * @property supportedVideoQualitiesMap A map where keys are [DynamicRange] values and values
 *                                      are lists of [VideoQuality] settings supported for that
 *                                      dynamic range.
 * @property supportedImageFormatsMap A map where keys are [StreamConfig] values (indicating single
 *                                    or multi-stream configurations) and values are sets of
 *                                    [ImageOutputFormat] (e.g., JPEG, DNG) supported for that
 *                                    stream configuration.
 * @property supportedIlluminants A set of [Illuminant] values supported by this camera, typically
 *                                indicating the type of flash unit available (e.g., FLASH_UNIT).
 * @property supportedFlashModes A set of [FlashMode] values (e.g., OFF, ON, AUTO) that can be
 *                               used with this camera lens.
 * @property supportedZoomRange An optional [Range] of floats indicating the minimum and maximum
 *                              zoom ratios supported by this lens. Null if zoom is not supported
 *                              or the range is not available.
 * @property unsupportedStabilizationFpsMap A map where keys are [StabilizationMode] values and
 *                                          values are sets of frame rates (FPS) that are
 *                                          *not* supported when that specific stabilization mode
 *                                          is active. This helps in understanding combinations
 *                                          that are disallowed.
 */
data class CameraConstraints(
    val supportedStabilizationModes: Set<StabilizationMode>,
    val supportedFixedFrameRates: Set<Int>,
    val supportedDynamicRanges: Set<DynamicRange>,
    val supportedVideoQualitiesMap: Map<DynamicRange, List<VideoQuality>>,
    val supportedImageFormatsMap: Map<StreamConfig, Set<ImageOutputFormat>>,
    val supportedIlluminants: Set<Illuminant>,
    val supportedFlashModes: Set<FlashMode>,
    val supportedZoomRange: Range<Float>?,
    val unsupportedStabilizationFpsMap: Map<StabilizationMode, Set<Int>>,
    val supportedTestPatterns: Set<TestPattern>
) {
    val StabilizationMode.unsupportedFpsSet
        get() = unsupportedStabilizationFpsMap[this] ?: emptySet()

    companion object {
        const val FPS_AUTO = 0
        const val FPS_15 = 15
        const val FPS_30 = 30
        const val FPS_60 = 60
    }
}

/**
 * Useful set of constraints for testing
 */
val TYPICAL_SYSTEM_CONSTRAINTS =
    SystemConstraints(
        availableLenses = listOf(LensFacing.FRONT, LensFacing.BACK),
        concurrentCamerasSupported = false,
        perLensConstraints = buildMap {
            for (lensFacing in listOf(LensFacing.FRONT, LensFacing.BACK)) {
                put(
                    lensFacing,
                    CameraConstraints(
                        supportedFixedFrameRates = setOf(15, 30),
                        supportedStabilizationModes = setOf(StabilizationMode.OFF),
                        supportedDynamicRanges = setOf(DynamicRange.SDR),
                        supportedImageFormatsMap = mapOf(
                            Pair(StreamConfig.SINGLE_STREAM, setOf(ImageOutputFormat.JPEG)),
                            Pair(StreamConfig.MULTI_STREAM, setOf(ImageOutputFormat.JPEG))
                        ),
                        supportedVideoQualitiesMap = emptyMap(),
                        supportedIlluminants = setOf(Illuminant.FLASH_UNIT),
                        supportedFlashModes = setOf(FlashMode.OFF, FlashMode.ON, FlashMode.AUTO),
                        supportedZoomRange = Range(.5f, 10f),
                        unsupportedStabilizationFpsMap = emptyMap(),
                        supportedTestPatterns = setOf(TestPattern.Off)
                    )
                )
            }
        }
    )
