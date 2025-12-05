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
import com.google.jetpackcamera.core.camera.FeatureGroupData.ExplicitlyGroupable
import com.google.jetpackcamera.core.camera.FeatureGroupData.InexplicitlyGroupable
import com.google.jetpackcamera.core.camera.FeatureGroupData.Ungroupable
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

sealed interface FeatureGroupData<out T> {
    /** Corresponds to a specific [GroupableFeature] object. */
    data class ExplicitlyGroupable(val feature: GroupableFeature) : FeatureGroupData<Nothing>

    /**
     * Does not correspond to a specific [GroupableFeature] object, but inexplicitly groupable as
     * it is equivalent to the base value CameraX feature groups API will use.
     */
    object InexplicitlyGroupable : FeatureGroupData<Nothing>

    /** Feature value that is not usable with CameraX feature groups API. */
    data class Ungroupable<T>(val featureValue: T) : FeatureGroupData<T>
}

/**
 * Returns whether a collection of [FeatureGroupData] represents that CameraX feature group
 * query API should be used.
 *
 * This is based on the fact that feature group query API should be used whenever there's at least
 * two [GroupableFeature].
 */
fun Collection<FeatureGroupData<*>>.requiresFeatureGroupQuery(): Boolean {
    return filter { it is ExplicitlyGroupable }.size >= 2
}

/**
 * Returns whether a collection of [FeatureGroupData] is invalid.
 *
 * The collection should not be used for camera session i.e. invalid whenever it has a
 * [Ungroupable] element and [requiresFeatureGroupQuery] is true for the collection.
 */
internal fun Collection<FeatureGroupData<*>>.isInvalid(): Boolean {
    return requiresFeatureGroupQuery() && any { it is Ungroupable }
}

fun DynamicRange.toFeatureGroupData(): FeatureGroupData<DynamicRange> {
    return when (this) {
        DynamicRange.SDR -> InexplicitlyGroupable
        DynamicRange.HLG10 -> ExplicitlyGroupable(GroupableFeature.HDR_HLG10)
    }
}

fun VideoQuality.toFeatureGroupData(): FeatureGroupData<VideoQuality> {
    return when (this) {
        SD -> ExplicitlyGroupable(GroupableFeatures.SD_RECORDING)
        HD -> ExplicitlyGroupable(GroupableFeatures.HD_RECORDING)
        FHD -> ExplicitlyGroupable(GroupableFeatures.FHD_RECORDING)
        UHD -> ExplicitlyGroupable(GroupableFeatures.UHD_RECORDING)
        else -> InexplicitlyGroupable
    }
}

fun ImageOutputFormat.toFeatureGroupData(): FeatureGroupData<ImageOutputFormat> {
    return when (this) {
        JPEG -> InexplicitlyGroupable
        JPEG_ULTRA_HDR -> ExplicitlyGroupable(GroupableFeature.IMAGE_ULTRA_HDR)
    }
}

fun StabilizationMode.toFeatureGroupData(): FeatureGroupData<StabilizationMode> {
    return when (this) {
        OFF -> InexplicitlyGroupable
        ON -> ExplicitlyGroupable(GroupableFeature.PREVIEW_STABILIZATION)
        HIGH_QUALITY -> ExplicitlyGroupable(GroupableFeatures.VIDEO_STABILIZATION)
        AUTO, // AUTO should be resolved to concrete value before this API is called
        OPTICAL -> Ungroupable(this)
    }
}

fun Int.toFpsGroupableFeatureData(): FeatureGroupData<Int> {
    return when (this) {
        60 -> ExplicitlyGroupable(GroupableFeature.FPS_60)
        30, 0 -> InexplicitlyGroupable
        else -> Ungroupable(this)
    }
}

/**
 * Creates a set of [FeatureGroupData] from a [PerpetualSessionSettings.SingleCamera].
 */
internal fun PerpetualSessionSettings.SingleCamera.toFeatureGroupDataSet():
    Set<FeatureGroupData<*>> {
    return setOf(
        dynamicRange.toFeatureGroupData(),
        targetFrameRate.toFpsGroupableFeatureData(),
        imageFormat.toFeatureGroupData(),
        stabilizationMode.toFeatureGroupData(),
        videoQuality.toFeatureGroupData()
    )
}

/**
 * Returns `this` value if the provided [sessionSettings] is not compatible with CameraX feature
 * groups.
 */
internal fun <T> T.takeIfFeatureGroupInvalid(
    sessionSettings: PerpetualSessionSettings.SingleCamera
): T? {
    return if (sessionSettings.toFeatureGroupDataSet().isInvalid()) this else null
}
