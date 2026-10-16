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
import com.google.jetpackcamera.model.CameraEffectId
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.VideoQuality

/**
 * Records the [CameraAppSettings] values that [CameraFeaturePolicy.enforceRestrictions] replaced,
 * so that they can be restored when the policy no longer applies (for example, when the user
 * leaves a capture sub-mode). Changes that the user requests while the policy applies replace
 * the recorded values (see [withRequestedChange]).
 *
 * The restored values are [CameraAppSettings.aspectRatio], [CameraAppSettings.flashMode],
 * [CameraAppSettings.imageFormat], [CameraAppSettings.dynamicRange],
 * [CameraAppSettings.stabilizationMode], [CameraAppSettings.targetFrameRate],
 * [CameraAppSettings.selectedCameraEffect], and [CameraAppSettings.videoQuality].
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
    private val dynamicRange: OverriddenValue<DynamicRange>,
    private val stabilizationMode: OverriddenValue<StabilizationMode>,
    private val targetFrameRate: OverriddenValue<Int>,
    private val selectedCameraEffect: OverriddenValue<CameraEffectId>,
    private val videoQuality: OverriddenValue<VideoQuality>
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
        ),
        stabilizationMode = OverriddenValue(
            beforeEnforcement.stabilizationMode,
            afterEnforcement.stabilizationMode
        ),
        targetFrameRate = OverriddenValue(
            beforeEnforcement.targetFrameRate,
            afterEnforcement.targetFrameRate
        ),
        selectedCameraEffect = OverriddenValue(
            beforeEnforcement.selectedCameraEffect,
            afterEnforcement.selectedCameraEffect
        ),
        videoQuality = OverriddenValue(beforeEnforcement.videoQuality, afterEnforcement.videoQuality)
    ) {
        require(beforeEnforcement.captureMode == afterEnforcement.captureMode) {
            "PolicyOverrides does not support overriding captureMode; sub-mode feature policies " +
                "must not modify captureMode."
        }
    }

    /**
     * Returns [settings] with the recorded values restored. A value that the user requested is
     * always restored; any other value is only restored if it still equals the value that the
     * policy set.
     */
    fun restore(settings: CameraAppSettings): CameraAppSettings = settings.copy(
        aspectRatio = aspectRatio.restore(settings.aspectRatio),
        flashMode = flashMode.restore(settings.flashMode),
        imageFormat = imageFormat.restore(settings.imageFormat),
        dynamicRange = dynamicRange.restore(settings.dynamicRange),
        stabilizationMode = stabilizationMode.restore(settings.stabilizationMode),
        targetFrameRate = targetFrameRate.restore(settings.targetFrameRate),
        selectedCameraEffect = selectedCameraEffect.restore(settings.selectedCameraEffect),
        videoQuality = videoQuality.restore(settings.videoQuality)
    )

    /**
     * Returns a copy in which [change] is applied to the values to restore, so that a setting the
     * user changes while the policy applies is restored to the user's choice, whether or not the
     * policy permits it.
     *
     * @param change Applies the user's change, for example
     *   `{ it.copy(flashMode = FlashMode.OFF) }`. Only changes to the restored settings are kept.
     */
    fun withRequestedChange(change: (CameraAppSettings) -> CameraAppSettings): PolicyOverrides {
        val requested = change(
            DEFAULT_CAMERA_APP_SETTINGS.copy(
                aspectRatio = aspectRatio.before,
                flashMode = flashMode.before,
                imageFormat = imageFormat.before,
                dynamicRange = dynamicRange.before,
                stabilizationMode = stabilizationMode.before,
                targetFrameRate = targetFrameRate.before,
                selectedCameraEffect = selectedCameraEffect.before,
                videoQuality = videoQuality.before
            )
        )
        return copy(
            aspectRatio = aspectRatio.withRequest(requested.aspectRatio),
            flashMode = flashMode.withRequest(requested.flashMode),
            imageFormat = imageFormat.withRequest(requested.imageFormat),
            dynamicRange = dynamicRange.withRequest(requested.dynamicRange),
            stabilizationMode = stabilizationMode.withRequest(requested.stabilizationMode),
            targetFrameRate = targetFrameRate.withRequest(requested.targetFrameRate),
            selectedCameraEffect = selectedCameraEffect.withRequest(
                requested.selectedCameraEffect
            ),
            videoQuality = videoQuality.withRequest(requested.videoQuality)
        )
    }

    /** A value to restore, the value that the policy set, and whether the user requested it. */
    private data class OverriddenValue<T>(
        val before: T,
        val enforced: T,
        val requested: Boolean = false
    ) {
        fun restore(current: T): T = if (requested || current == enforced) before else current

        fun withRequest(value: T): OverriddenValue<T> =
            if (value == before) this else copy(before = value, requested = true)
    }
}
