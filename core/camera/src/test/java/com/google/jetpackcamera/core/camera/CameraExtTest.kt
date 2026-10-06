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

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.LensFacing
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowCameraCharacteristics

@RunWith(RobolectricTestRunner::class)
class CameraExtTest {

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun isTenBitDynamicRangeSupported_api33WithCapability_returnsTrue() {
        val characteristics =
            createCharacteristics(
                intArrayOf(
                    CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE,
                    CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT
                )
            )

        assertThat(characteristics.isTenBitDynamicRangeSupported).isTrue()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun isTenBitDynamicRangeSupported_api33WithoutCapability_returnsFalse() {
        val characteristics =
            createCharacteristics(
                intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)
            )

        assertThat(characteristics.isTenBitDynamicRangeSupported).isFalse()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun isTenBitDynamicRangeSupported_api33NullCapabilities_returnsFalse() {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()

        assertThat(characteristics.isTenBitDynamicRangeSupported).isFalse()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S_V2])
    fun isTenBitDynamicRangeSupported_belowApi33WithCapability_returnsFalse() {
        val characteristics =
            createCharacteristics(
                intArrayOf(
                    CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE,
                    CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT
                )
            )

        assertThat(characteristics.isTenBitDynamicRangeSupported).isFalse()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun filterSupportedVideoDynamicRanges_withTenBitCapability_retainsReportedRanges() {
        val characteristics =
            createCharacteristics(
                intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT)
            )
        val reportedRanges = setOf(DynamicRange.SDR, DynamicRange.HLG10)

        val supportedRanges =
            characteristics.filterSupportedVideoDynamicRanges(
                lensFacing = LensFacing.BACK,
                reportedDynamicRanges = reportedRanges
            )

        assertThat(supportedRanges).containsExactly(DynamicRange.SDR, DynamicRange.HLG10)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun filterSupportedVideoDynamicRanges_withoutTenBitCapability_restrictsHdrToSdr() {
        val characteristics =
            createCharacteristics(
                intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)
            )

        val supportedRanges =
            characteristics.filterSupportedVideoDynamicRanges(
                lensFacing = LensFacing.BACK,
                reportedDynamicRanges = setOf(DynamicRange.SDR, DynamicRange.HLG10)
            )

        assertThat(supportedRanges).containsExactly(DynamicRange.SDR)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun filterSupportedVideoDynamicRanges_withoutTenBitCapabilityAndSdrOnly_returnsSdr() {
        val characteristics =
            createCharacteristics(
                intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)
            )

        val supportedRanges =
            characteristics.filterSupportedVideoDynamicRanges(
                lensFacing = LensFacing.FRONT,
                reportedDynamicRanges = setOf(DynamicRange.SDR)
            )

        assertThat(supportedRanges).containsExactly(DynamicRange.SDR)
    }

    private fun createCharacteristics(capabilities: IntArray): CameraCharacteristics {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        shadowOf(characteristics)
            .set(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES, capabilities)
        return characteristics
    }
}
