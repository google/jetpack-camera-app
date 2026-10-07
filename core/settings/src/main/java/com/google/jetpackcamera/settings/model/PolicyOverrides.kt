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
package com.google.jetpackcamera.settings.model

import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat

/**
 * Records the [CameraAppSettings] values that [CameraFeaturePolicy.enforceRestrictions] replaced,
 * so that they can be restored when the policy no longer applies (for example, when the user
 * leaves a capture sub-mode).
 *
 * The restored values are [CameraAppSettings.aspectRatio], [CameraAppSettings.flashMode],
 * [CameraAppSettings.imageFormat], and [CameraAppSettings.dynamicRange].
 * [CameraAppSettings.captureMode] is not restored. A capture mode change ends a sub-mode rather
 * than being a value that the sub-mode overrides.
 *
 * Instances are compared by value.
 */
@ConsistentCopyVisibility
data class PolicyOverrides private constructor(
    private val aspectRatio: OverriddenValue<AspectRatio>,
    private val flashMode: OverriddenValue<FlashMode>,
    private val imageFormat: OverriddenValue<ImageOutputFormat>,
    private val dynamicRange: OverriddenValue<DynamicRange>
) {
    /**
     * @param beforeEnforcement The settings before the policy was enforced.
     * @param afterEnforcement The settings returned by [CameraFeaturePolicy.enforceRestrictions].
     */
    constructor(beforeEnforcement: CameraAppSettings, afterEnforcement: CameraAppSettings) : this(
        aspectRatio = OverriddenValue(beforeEnforcement.aspectRatio, afterEnforcement.aspectRatio),
        flashMode = OverriddenValue(beforeEnforcement.flashMode, afterEnforcement.flashMode),
        imageFormat = OverriddenValue(beforeEnforcement.imageFormat, afterEnforcement.imageFormat),
        dynamicRange = OverriddenValue(
            beforeEnforcement.dynamicRange,
            afterEnforcement.dynamicRange
        )
    )

    /**
     * Returns [settings] with each overridden value set back to its value before enforcement.
     *
     * A value is only restored if it still equals the value that the policy set. Values that have
     * changed since enforcement (for example, by the user) are kept.
     */
    fun restore(settings: CameraAppSettings): CameraAppSettings = settings.copy(
        aspectRatio = aspectRatio.restore(settings.aspectRatio),
        flashMode = flashMode.restore(settings.flashMode),
        imageFormat = imageFormat.restore(settings.imageFormat),
        dynamicRange = dynamicRange.restore(settings.dynamicRange)
    )

    /** A setting's value before enforcement, and the value that the policy set. */
    private data class OverriddenValue<T>(val before: T, val enforced: T) {
        fun restore(current: T): T = if (current == enforced) before else current
    }
}
