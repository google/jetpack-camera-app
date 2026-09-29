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

/**
 * Records the [CameraAppSettings] values that [CameraFeaturePolicy.enforceRestrictions] replaced,
 * so that they can be restored when the policy no longer applies (for example, when the user
 * leaves a capture sub-mode).
 *
 * [CameraAppSettings.captureMode] is not restored. A capture mode change ends a sub-mode rather
 * than being a value that the sub-mode overrides.
 *
 * @param beforeEnforcement The settings before the policy was enforced.
 * @param afterEnforcement The settings returned by [CameraFeaturePolicy.enforceRestrictions].
 */
class PolicyOverrides(
    private val beforeEnforcement: CameraAppSettings,
    private val afterEnforcement: CameraAppSettings
) {
    /**
     * Returns [settings] with each overridden value set back to its value before enforcement.
     *
     * A value is only restored if it still equals the value that the policy set. Values that have
     * changed since enforcement (for example, by the user) are kept.
     */
    fun restore(settings: CameraAppSettings): CameraAppSettings = settings.copy(
        aspectRatio = restoreValue(
            current = settings.aspectRatio,
            before = beforeEnforcement.aspectRatio,
            enforced = afterEnforcement.aspectRatio
        ),
        flashMode = restoreValue(
            current = settings.flashMode,
            before = beforeEnforcement.flashMode,
            enforced = afterEnforcement.flashMode
        ),
        imageFormat = restoreValue(
            current = settings.imageFormat,
            before = beforeEnforcement.imageFormat,
            enforced = afterEnforcement.imageFormat
        ),
        dynamicRange = restoreValue(
            current = settings.dynamicRange,
            before = beforeEnforcement.dynamicRange,
            enforced = afterEnforcement.dynamicRange
        )
    )
}

private fun <T> restoreValue(current: T, before: T, enforced: T): T =
    if (current == enforced) before else current
