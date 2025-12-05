/*
 * Copyright (C) 2025 The Android Open Source Project
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

import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.video.GroupableFeatures
import com.google.jetpackcamera.core.camera.FeatureGroupability.ExplicitlyGroupable
import com.google.jetpackcamera.core.camera.FeatureGroupability.ImplicitlyGroupable
import com.google.jetpackcamera.core.camera.FeatureGroupability.Ungroupable
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.ImageOutputFormat.JPEG
import com.google.jetpackcamera.model.ImageOutputFormat.JPEG_ULTRA_HDR
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.StabilizationMode.AUTO
import com.google.jetpackcamera.model.StabilizationMode.HIGH_QUALITY
import com.google.jetpackcamera.model.StabilizationMode.OFF
import com.google.jetpackcamera.model.StabilizationMode.ON
import com.google.jetpackcamera.model.StabilizationMode.OPTICAL
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.model.VideoQuality.FHD
import com.google.jetpackcamera.model.VideoQuality.HD
import com.google.jetpackcamera.model.VideoQuality.SD
import com.google.jetpackcamera.model.VideoQuality.UHD
import com.google.jetpackcamera.model.VideoQuality.UNSPECIFIED

/**
 * Categorizes how a camera feature can be used with CameraX's feature group query API.
 *
 * This allows internal JCA models like [DynamicRange] and [VideoQuality] to be mapped to CameraX
 * [GroupableFeature]s for compatibility checking.
 */
sealed interface FeatureGroupability<out T> {
    /** Corresponds to a specific [GroupableFeature] object. */
    data class ExplicitlyGroupable(val feature: GroupableFeature) : FeatureGroupability<Nothing>

    /**
     * Does not correspond to a specific [GroupableFeature] object, but implicitly groupable as
     * it is equivalent to the base value CameraX feature groups API will use.
     */
    object ImplicitlyGroupable : FeatureGroupability<Nothing>

    /** Feature value that is not usable with CameraX feature groups API. */
    data class Ungroupable<T>(val featureValue: T) : FeatureGroupability<T>
}

/**
 * Returns whether a collection of [FeatureGroupability] represents that CameraX feature group
 * query API should be used.
 *
 * This is based on the fact that feature group query API should be used whenever there's at least
 * two [GroupableFeature].
 */
fun Collection<FeatureGroupability<*>>.requiresFeatureGroupQuery(): Boolean {
    return filter { it is ExplicitlyGroupable }.size >= 2
}

/**
 * Returns whether a collection of [FeatureGroupability] is invalid.
 *
 * The collection should not be used for camera session i.e. invalid whenever it has a
 * [Ungroupable] element and [requiresFeatureGroupQuery] is true for the collection.
 */
internal fun Collection<FeatureGroupability<*>>.isInvalid(): Boolean {
    return requiresFeatureGroupQuery() && any { it is Ungroupable }
}

/**
 * Converts this [DynamicRange] to a [FeatureGroupability].
 *
 * This allows the dynamic range to be used in CameraX feature group compatibility checks.
 */
fun DynamicRange.toFeatureGroupability(): FeatureGroupability<DynamicRange> {
    return when (this) {
        DynamicRange.SDR -> ImplicitlyGroupable
        DynamicRange.HLG10 -> ExplicitlyGroupable(GroupableFeature.HDR_HLG10)
    }
}

/**
 * Converts this [VideoQuality] to a [FeatureGroupability].
 *
 * This allows the video quality to be used in CameraX feature group compatibility checks.
 */
fun VideoQuality.toFeatureGroupability(): FeatureGroupability<VideoQuality> {
    return when (this) {
        SD -> ExplicitlyGroupable(GroupableFeatures.SD_RECORDING)
        HD -> ExplicitlyGroupable(GroupableFeatures.HD_RECORDING)
        FHD -> ExplicitlyGroupable(GroupableFeatures.FHD_RECORDING)
        UHD -> ExplicitlyGroupable(GroupableFeatures.UHD_RECORDING)
        UNSPECIFIED -> ImplicitlyGroupable
    }
}

/**
 * Converts this [ImageOutputFormat] to a [FeatureGroupability].
 *
 * This allows the image output format to be used in CameraX feature group compatibility checks.
 */
fun ImageOutputFormat.toFeatureGroupability(): FeatureGroupability<ImageOutputFormat> {
    return when (this) {
        JPEG -> ImplicitlyGroupable
        JPEG_ULTRA_HDR -> ExplicitlyGroupable(GroupableFeature.IMAGE_ULTRA_HDR)
    }
}

/**
 * Converts this [StabilizationMode] to a [FeatureGroupability].
 *
 * This allows the stabilization mode to be used in CameraX feature group compatibility checks.
 *
 * @throws IllegalStateException When the value of this `StabilizationMode` is [AUTO].
 */
fun StabilizationMode.toFeatureGroupability(): FeatureGroupability<StabilizationMode> {
    return when (this) {
        OFF -> ImplicitlyGroupable
        ON -> ExplicitlyGroupable(GroupableFeature.PREVIEW_STABILIZATION)
        HIGH_QUALITY -> ExplicitlyGroupable(GroupableFeatures.VIDEO_STABILIZATION)
        AUTO ->
            throw IllegalStateException(
                "AUTO should be resolved to concrete value before this API is called!"
            )
        OPTICAL -> Ungroupable(this)
    }
}

/**
 * Converts this integer FPS value to a [FeatureGroupability].
 *
 * This allows the frame rate to be used in CameraX feature group compatibility checks.
 */
fun Int.toFpsFeatureGroupability(): FeatureGroupability<Int> {
    return when (this) {
        60 -> ExplicitlyGroupable(GroupableFeature.FPS_60)
        30, 0 -> ImplicitlyGroupable
        else -> Ungroupable(this)
    }
}

/**
 * Creates a set of [FeatureGroupability] from a [PerpetualSessionSettings.SingleCamera].
 */
internal fun PerpetualSessionSettings.SingleCamera.toFeatureGroupabilities():
    Set<FeatureGroupability<*>> {
    return setOf(
        dynamicRange.toFeatureGroupability(),
        targetFrameRate.toFpsFeatureGroupability(),
        imageFormat.toFeatureGroupability(),
        stabilizationMode.toFeatureGroupability(),
        videoQuality.toFeatureGroupability()
    )
}

/**
 * Returns `this` value if the provided [sessionSettings] is not compatible with CameraX feature
 * groups.
 */
internal fun <T> T.takeIfFeatureGroupInvalid(
    sessionSettings: PerpetualSessionSettings.SingleCamera
): T? {
    return if (sessionSettings.toFeatureGroupabilities().isInvalid()) this else null
}
