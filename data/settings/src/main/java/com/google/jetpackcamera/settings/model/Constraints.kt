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
import com.google.jetpackcamera.model.TARGET_FPS_15
import com.google.jetpackcamera.model.TARGET_FPS_30
import com.google.jetpackcamera.model.TestPattern
import com.google.jetpackcamera.model.VideoQuality

/**
 * Represents the overall constraints and capabilities of the camera system on the device.
 *
 * This data class aggregates information about available lenses, support for concurrent
 * camera usage, and detailed constraints for each individual camera lens. It serves as a
 * central point for querying what features and settings are supported by the device's
 * camera hardware and software stack.
 *
 * @property availableLenses A list of [com.google.jetpackcamera.model.LensFacing] values indicating which camera lenses
 *                           (e.g., front, back) are available on the device.
 * @property concurrentCamerasSupported A boolean indicating whether the device supports
 *                                      operating multiple cameras concurrently.
 * @property perLensConstraints A map where each key is a [com.google.jetpackcamera.model.LensFacing] value and the
 *                              corresponding value is a [CameraConstraints] object
 *                              detailing the specific capabilities and limitations of that lens.
 */
data class CameraSystemConstraints(
    val availableLenses: List<LensFacing> = emptyList(),
    val concurrentCamerasSupported: Boolean = false,
    val perLensConstraints: Map<LensFacing, CameraConstraints> = emptyMap()
)

inline fun <reified T> CameraSystemConstraints.forDevice(
    crossinline constraintSelector: (CameraConstraints) -> Iterable<T>
) = perLensConstraints.values.asSequence().flatMap { constraintSelector(it) }.toSet()

/**
 * Defines the capabilities and limitations for a single camera lens.
 *
 * Encapsulates constraints for video and image capture, including stabilization,
 * frame rates, dynamic ranges, formats, flash, zoom, and test patterns.
 *
 * @property supportedStabilizationModes Set of [StabilizationMode] values supported by this lens.
 * @property supportedFixedFrameRates A set of integers representing fixed frame rates that this
 * lens supports for video recordings. Only 15, 30, or 60 FPS can be displayed, and a lens must
 * exactly support ranges like `[15,15]` to be included in the set.
 * @property supportedDynamicRanges Set of [DynamicRange] values (e.g., SDR, HLG10) this lens can capture.
 * @property supportedVideoQualitiesMap Map of [DynamicRange] to a list of supported [VideoQuality] settings.
 * @property supportedImageFormatsMap Map of [StreamConfig] to a set of supported [ImageOutputFormat]s.
 * @property supportedIlluminants Set of supported [Illuminant] types (e.g., flash unit).
 * @property supportedFlashModes Set of [FlashMode] values (e.g., ON, OFF, AUTO) supported by this lens.
 * @property supportedZoomRange Optional [Range] of floats for zoom ratios. Null if zoom is not supported.
 * @property unsupportedStabilizationFpsMap Map of [StabilizationMode] to a set of frame rates (FPS) that are unsupported with that mode.
 * @property supportedTestPatterns Set of [TestPattern] values supported by this lens, used for debugging.
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
}

/**
 * Useful set of constraints for testing
 */
val TYPICAL_SYSTEM_CONSTRAINTS =
    CameraSystemConstraints(
        availableLenses = listOf(LensFacing.FRONT, LensFacing.BACK),
        concurrentCamerasSupported = false,
        perLensConstraints = buildMap {
            for (lensFacing in listOf(LensFacing.FRONT, LensFacing.BACK)) {
                put(
                    lensFacing,
                    CameraConstraints(
                        supportedFixedFrameRates = setOf(TARGET_FPS_15, TARGET_FPS_30),
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
