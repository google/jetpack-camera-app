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

data class SystemConstraints(
    val availableLenses: List<LensFacing>,
    val concurrentCamerasSupported: Boolean,
    val perLensConstraints: Map<LensFacing, CameraConstraints>
)

data class CameraConstraints(
    val supportedStabilizationModes: Set<SupportedStabilizationMode>,
    val supportedFixedFrameRates: Set<Int>,
    val supportedDynamicRanges: Set<DynamicRange>,
    val supportedImageFormatsMap: Map<CaptureMode, Set<ImageOutputFormat>>,
    val hasFlashUnit: Boolean
)

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
                        supportedStabilizationModes = emptySet(),
                        supportedDynamicRanges = setOf(DynamicRange.SDR),
                        supportedImageFormatsMap = mapOf(
                            Pair(CaptureMode.SINGLE_STREAM, setOf(ImageOutputFormat.JPEG)),
                            Pair(CaptureMode.MULTI_STREAM, setOf(ImageOutputFormat.JPEG))
                        ),
                        hasFlashUnit = lensFacing == LensFacing.BACK
                    )
                )
            }
        }
    )
