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

data class SystemConstraints(
    val availableLenses: List<LensFacing> = emptyList(),
    val concurrentCamerasSupported: Boolean = false,
    val perLensConstraints: Map<LensFacing, CameraConstraints> = emptyMap()
)

data class CameraConstraints(
    val supportedStabilizationModes: Set<StabilizationMode>,
    val supportedFixedFrameRates: Set<Int>,
    val supportedDynamicRanges: Set<DynamicRange>,
    val supportedVideoQualitiesMap: Map<DynamicRange, List<VideoQuality>>,
    val supportedImageFormatsMap: Map<StreamConfig, Set<ImageOutputFormat>>,
    val supportedIlluminants: Set<Illuminant>,
    val supportedFlashModes: Set<FlashMode>,
    val supportedZoomRange: Range<Float>?,
    val unsupportedStabilizationFpsMap: Map<StabilizationMode, Set<Int>>
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
                        unsupportedStabilizationFpsMap = emptyMap()
                    )
                )
            }
        }
    )
