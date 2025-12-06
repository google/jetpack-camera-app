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
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.FeatureGroupData.ExplicitlyGroupable
import com.google.jetpackcamera.core.camera.FeatureGroupData.InexplicitlyGroupable
import com.google.jetpackcamera.core.camera.FeatureGroupData.Ungroupable
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LowLightBoostPriority
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.model.VideoQuality
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FeatureGroupDataTest {
    @Test
    fun requiresFeatureGroupQuery_returnsTrue_whenTwoOrMoreExplicit() {
        val collection = listOf(
            ExplicitlyGroupable(GroupableFeatures.SD_RECORDING),
            ExplicitlyGroupable(GroupableFeature.IMAGE_ULTRA_HDR),
            InexplicitlyGroupable
        )
        assertThat(collection.requiresFeatureGroupQuery()).isTrue()
    }

    @Test
    fun requiresFeatureGroupQuery_returnsFalse_whenLessThanTwoExplicit() {
        val collection1 = listOf(
            ExplicitlyGroupable(GroupableFeatures.SD_RECORDING),
            InexplicitlyGroupable
        )
        val collection2 = listOf(
            InexplicitlyGroupable,
            InexplicitlyGroupable
        )
        assertThat(collection1.requiresFeatureGroupQuery()).isFalse()
        assertThat(collection2.requiresFeatureGroupQuery()).isFalse()
    }

    @Test
    fun isInvalid_returnsTrue_whenRequiresQueryAndHasUngroupable() {
        val collection = listOf(
            ExplicitlyGroupable(GroupableFeatures.SD_RECORDING),
            ExplicitlyGroupable(GroupableFeature.IMAGE_ULTRA_HDR),
            Ungroupable("someValue")
        )
        assertThat(collection.isInvalid()).isTrue()
    }

    @Test
    fun isInvalid_returnsFalse_whenRequiresQueryAndNoUngroupable() {
        val collection = listOf(
            ExplicitlyGroupable(GroupableFeatures.SD_RECORDING),
            ExplicitlyGroupable(GroupableFeature.IMAGE_ULTRA_HDR),
            InexplicitlyGroupable
        )
        assertThat(collection.isInvalid()).isFalse()
    }

    @Test
    fun isInvalid_returnsFalse_whenNotRequiresQueryAndHasUngroupable() {
        val collection = listOf(
            ExplicitlyGroupable(GroupableFeatures.SD_RECORDING),
            Ungroupable("someValue")
        )
        assertThat(collection.isInvalid()).isFalse()
    }

    @Test
    fun dynamicRange_toFeatureGroupData_mapsCorrectly() {
        assertThat(DynamicRange.SDR.toFeatureGroupData()).isEqualTo(InexplicitlyGroupable)
        assertThat(
            DynamicRange.HLG10.toFeatureGroupData()
        ).isEqualTo(ExplicitlyGroupable(GroupableFeature.HDR_HLG10))
    }

    @Test
    fun videoQuality_toFeatureGroupData_mapsCorrectly() {
        assertThat(
            VideoQuality.SD.toFeatureGroupData()
        ).isEqualTo(ExplicitlyGroupable(GroupableFeatures.SD_RECORDING))
        assertThat(
            VideoQuality.HD.toFeatureGroupData()
        ).isEqualTo(ExplicitlyGroupable(GroupableFeatures.HD_RECORDING))
        assertThat(
            VideoQuality.FHD.toFeatureGroupData()
        ).isEqualTo(ExplicitlyGroupable(GroupableFeatures.FHD_RECORDING))
        assertThat(
            VideoQuality.UHD.toFeatureGroupData()
        ).isEqualTo(ExplicitlyGroupable(GroupableFeatures.UHD_RECORDING))
    }

    @Test
    fun imageOutputFormat_toFeatureGroupData_mapsCorrectly() {
        assertThat(ImageOutputFormat.JPEG.toFeatureGroupData()).isEqualTo(InexplicitlyGroupable)
        assertThat(
            ImageOutputFormat.JPEG_ULTRA_HDR.toFeatureGroupData()
        ).isEqualTo(ExplicitlyGroupable(GroupableFeature.IMAGE_ULTRA_HDR))
    }

    @Test
    fun stabilizationMode_toFeatureGroupData_mapsCorrectly() {
        assertThat(StabilizationMode.OFF.toFeatureGroupData()).isEqualTo(InexplicitlyGroupable)
        assertThat(
            StabilizationMode.ON.toFeatureGroupData()
        ).isEqualTo(ExplicitlyGroupable(GroupableFeature.PREVIEW_STABILIZATION))
        assertThat(
            StabilizationMode.HIGH_QUALITY.toFeatureGroupData()
        ).isEqualTo(ExplicitlyGroupable(GroupableFeatures.VIDEO_STABILIZATION))
        assertThat(
            StabilizationMode.AUTO.toFeatureGroupData()
        ).isInstanceOf(Ungroupable::class.java)
        assertThat(
            StabilizationMode.OPTICAL.toFeatureGroupData()
        ).isInstanceOf(Ungroupable::class.java)
    }

    @Test
    fun int_toFpsGroupableFeatureData_mapsCorrectly() {
        assertThat(
            60.toFpsGroupableFeatureData()
        ).isEqualTo(ExplicitlyGroupable(GroupableFeature.FPS_60))
        assertThat(30.toFpsGroupableFeatureData()).isEqualTo(InexplicitlyGroupable)
        assertThat(0.toFpsGroupableFeatureData()).isEqualTo(InexplicitlyGroupable)
        assertThat(15.toFpsGroupableFeatureData()).isInstanceOf(Ungroupable::class.java)
    }

    @Test
    fun singleCamera_toFeatureGroupDataSet_containsCorrectElements() {
        val settings = PerpetualSessionSettings.SingleCamera(
            aspectRatio = AspectRatio.THREE_FOUR,
            captureMode = CaptureMode.STANDARD,
            streamConfig = StreamConfig.MULTI_STREAM,
            targetFrameRate = 60,
            stabilizationMode = StabilizationMode.ON,
            dynamicRange = DynamicRange.HLG10,
            videoQuality = VideoQuality.UHD,
            imageFormat = ImageOutputFormat.JPEG_ULTRA_HDR,
            lowLightBoostPriority = LowLightBoostPriority.PRIORITIZE_AE_MODE
        )

        val dataSet = settings.toFeatureGroupDataSet()

        assertThat(dataSet).contains(ExplicitlyGroupable(GroupableFeature.FPS_60))
        assertThat(dataSet).contains(ExplicitlyGroupable(GroupableFeature.PREVIEW_STABILIZATION))
        assertThat(dataSet).contains(ExplicitlyGroupable(GroupableFeature.HDR_HLG10))
        assertThat(dataSet).contains(ExplicitlyGroupable(GroupableFeatures.UHD_RECORDING))
        assertThat(dataSet).contains(ExplicitlyGroupable(GroupableFeature.IMAGE_ULTRA_HDR))
    }

    @Test
    fun takeIfFeatureGroupInvalid_returnsObject_whenInvalid() {
        // Construct an invalid combination: e.g. using feature groups (>=2 explicit) AND an ungroupable
        // Here: 60fps (Explicit), Stabilization ON (Explicit) AND Stabilization OPTICAL (Ungroupable - wait, can't set two stabilization modes)

        // Let's use a combination that results in 2 explicits and 1 ungroupable.
        // Explicit: 60fps
        // Explicit: HLG10
        // Ungroupable: Stabilization OPTICAL

        val settings = PerpetualSessionSettings.SingleCamera(
            aspectRatio = AspectRatio.THREE_FOUR,
            captureMode = CaptureMode.STANDARD,
            streamConfig = StreamConfig.MULTI_STREAM,
            targetFrameRate = 60, // Explicit
            stabilizationMode = StabilizationMode.OPTICAL, // Ungroupable
            dynamicRange = DynamicRange.HLG10, // Explicit
            videoQuality = VideoQuality.SD, // Explicit
            imageFormat = ImageOutputFormat.JPEG, // Inexplicit
            lowLightBoostPriority = LowLightBoostPriority.PRIORITIZE_AE_MODE
        )

        val obj = "TestObject"
        assertThat(obj.takeIfFeatureGroupInvalid(settings)).isEqualTo(obj)
    }

    @Test
    fun takeIfFeatureGroupInvalid_returnsNull_whenValid() {
        // Valid combination: 2 explicits, no ungroupables
        val settings = PerpetualSessionSettings.SingleCamera(
            aspectRatio = AspectRatio.THREE_FOUR,
            captureMode = CaptureMode.STANDARD,
            streamConfig = StreamConfig.MULTI_STREAM,
            targetFrameRate = 60, // Explicit
            stabilizationMode = StabilizationMode.ON, // Explicit
            dynamicRange = DynamicRange.HLG10, // Explicit
            videoQuality = VideoQuality.SD, // Explicit
            imageFormat = ImageOutputFormat.JPEG, // Inexplicit
            lowLightBoostPriority = LowLightBoostPriority.PRIORITIZE_AE_MODE
        )

        val obj = "TestObject"
        assertThat(obj.takeIfFeatureGroupInvalid(settings)).isNull()
    }

    @Test
    fun singleCamera_toGroupableFeatures_returnsCorrectSet() {
        val settings = PerpetualSessionSettings.SingleCamera(
            aspectRatio = AspectRatio.THREE_FOUR,
            captureMode = CaptureMode.STANDARD,
            streamConfig = StreamConfig.MULTI_STREAM,
            targetFrameRate = 60,
            stabilizationMode = StabilizationMode.ON,
            dynamicRange = DynamicRange.HLG10,
            videoQuality = VideoQuality.UHD,
            imageFormat = ImageOutputFormat.JPEG_ULTRA_HDR,
            lowLightBoostPriority = LowLightBoostPriority.PRIORITIZE_AE_MODE
        )

        val features = settings.toGroupableFeatures()

        assertThat(features!!).contains(GroupableFeature.FPS_60)
        assertThat(features).contains(GroupableFeature.PREVIEW_STABILIZATION)
        assertThat(features).contains(GroupableFeature.HDR_HLG10)
        assertThat(features).contains(GroupableFeatures.UHD_RECORDING)
        assertThat(features).contains(GroupableFeature.IMAGE_ULTRA_HDR)
    }

    @Test
    fun singleCamera_toGroupableFeatures_filtersUltraHdrWhenVideoOnly() {
        val settings = PerpetualSessionSettings.SingleCamera(
            aspectRatio = AspectRatio.THREE_FOUR,
            captureMode = CaptureMode.VIDEO_ONLY,
            streamConfig = StreamConfig.MULTI_STREAM,
            targetFrameRate = 60,
            stabilizationMode = StabilizationMode.ON,
            dynamicRange = DynamicRange.HLG10,
            videoQuality = VideoQuality.UHD,
            imageFormat = ImageOutputFormat.JPEG_ULTRA_HDR,
            lowLightBoostPriority = LowLightBoostPriority.PRIORITIZE_AE_MODE
        )

        val features = settings.toGroupableFeatures()

        assertThat(features!!).contains(GroupableFeature.FPS_60)
        assertThat(features).contains(GroupableFeature.PREVIEW_STABILIZATION)
        assertThat(features).contains(GroupableFeature.HDR_HLG10)
        assertThat(features).contains(GroupableFeatures.UHD_RECORDING)
        assertThat(features).doesNotContain(GroupableFeature.IMAGE_ULTRA_HDR)
    }

    @Test
    fun singleCamera_toGroupableFeatures_filtersVideoQualityWhenImageOnly() {
        val settings = PerpetualSessionSettings.SingleCamera(
            aspectRatio = AspectRatio.THREE_FOUR,
            captureMode = CaptureMode.IMAGE_ONLY,
            streamConfig = StreamConfig.MULTI_STREAM,
            targetFrameRate = 60,
            stabilizationMode = StabilizationMode.ON,
            dynamicRange = DynamicRange.HLG10,
            videoQuality = VideoQuality.UHD,
            imageFormat = ImageOutputFormat.JPEG_ULTRA_HDR,
            lowLightBoostPriority = LowLightBoostPriority.PRIORITIZE_AE_MODE
        )

        val features = settings.toGroupableFeatures()

        assertThat(features!!).contains(GroupableFeature.FPS_60)
        assertThat(features).contains(GroupableFeature.PREVIEW_STABILIZATION)
        assertThat(features).contains(GroupableFeature.HDR_HLG10)
        assertThat(features).doesNotContain(GroupableFeatures.UHD_RECORDING)
        assertThat(features).contains(GroupableFeature.IMAGE_ULTRA_HDR)
    }
}
