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
package com.google.jetpackcamera.core.camera

import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.settings.model.CameraConstraints

/**
 * Resolves [requestedStabilizationMode] to the [StabilizationMode] to apply to a camera session.
 *
 * [StabilizationMode.AUTO] resolves to the first mode in the following order that
 * [cameraConstraints] supports at [targetFrameRate]:
 * - In [CaptureMode.IMAGE_ONLY]: [StabilizationMode.OPTICAL], then [StabilizationMode.OFF].
 *   Preview stabilization ([StabilizationMode.ON]) is intended for video and crops the field of
 *   view, so it is not chosen automatically when no video can be recorded.
 * - In other capture modes: [StabilizationMode.ON], [StabilizationMode.OPTICAL], then
 *   [StabilizationMode.OFF].
 *
 * Any other requested mode is returned unchanged if it is supported at [targetFrameRate], and
 * resolves to [StabilizationMode.OFF] otherwise. This includes [StabilizationMode.ON] in
 * [CaptureMode.IMAGE_ONLY], so preview stabilization can still be selected explicitly.
 */
internal fun resolveStabilizationMode(
    requestedStabilizationMode: StabilizationMode,
    captureMode: CaptureMode,
    targetFrameRate: Int,
    cameraConstraints: CameraConstraints
): StabilizationMode = with(cameraConstraints) {
    fun StabilizationMode.isSupported(): Boolean =
        this in supportedStabilizationModes && targetFrameRate !in unsupportedFpsSet

    if (requestedStabilizationMode == StabilizationMode.AUTO) {
        val autoCandidates = if (captureMode == CaptureMode.IMAGE_ONLY) {
            listOf(StabilizationMode.OPTICAL)
        } else {
            listOf(StabilizationMode.ON, StabilizationMode.OPTICAL)
        }
        autoCandidates.firstOrNull { it.isSupported() } ?: StabilizationMode.OFF
    } else {
        requestedStabilizationMode.takeIf { it.isSupported() } ?: StabilizationMode.OFF
    }
}
