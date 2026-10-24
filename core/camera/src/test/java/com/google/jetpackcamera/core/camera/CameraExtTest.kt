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
import androidx.camera.core.MirrorMode as CXMirrorMode
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.MirrorMode
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

    @Test
    fun toCXMirrorMode_mapsToCameraXMirrorModes() {
        assertThat(MirrorMode.OFF.toCXMirrorMode()).isEqualTo(CXMirrorMode.MIRROR_MODE_OFF)
        assertThat(MirrorMode.ON.toCXMirrorMode()).isEqualTo(CXMirrorMode.MIRROR_MODE_ON)
        assertThat(MirrorMode.ON_FRONT_ONLY.toCXMirrorMode())
            .isEqualTo(CXMirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
    }

    @Test
    fun toCXPreviewMirrorMode_mirrorsFrontPreviewUnlessAllLensesAreMirrored() {
        assertThat(MirrorMode.OFF.toCXPreviewMirrorMode())
            .isEqualTo(CXMirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
        assertThat(MirrorMode.ON_FRONT_ONLY.toCXPreviewMirrorMode())
            .isEqualTo(CXMirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
        assertThat(MirrorMode.ON.toCXPreviewMirrorMode()).isEqualTo(CXMirrorMode.MIRROR_MODE_ON)
    }

    @Test
    fun toCXConcurrentPreviewMirrorMode_disablesPreviewMirroringWhenCamerasAreMirrored() {
        assertThat(MirrorMode.OFF.toCXConcurrentPreviewMirrorMode())
            .isEqualTo(CXMirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
        assertThat(MirrorMode.ON_FRONT_ONLY.toCXConcurrentPreviewMirrorMode())
            .isEqualTo(CXMirrorMode.MIRROR_MODE_OFF)
        assertThat(MirrorMode.ON.toCXConcurrentPreviewMirrorMode())
            .isEqualTo(CXMirrorMode.MIRROR_MODE_OFF)
    }

    @Test
    fun concurrentCompositionScaleX_negatesScaleForMirroredLenses() {
        assertThat(MirrorMode.ON_FRONT_ONLY.concurrentCompositionScaleX(0.5f, LensFacing.FRONT))
            .isEqualTo(-0.5f)
        assertThat(MirrorMode.ON_FRONT_ONLY.concurrentCompositionScaleX(0.5f, LensFacing.BACK))
            .isEqualTo(0.5f)
        assertThat(MirrorMode.OFF.concurrentCompositionScaleX(1.0f, LensFacing.FRONT))
            .isEqualTo(1.0f)
        assertThat(MirrorMode.ON.concurrentCompositionScaleX(1.0f, LensFacing.BACK))
            .isEqualTo(-1.0f)
    }

    private fun createCharacteristics(capabilities: IntArray): CameraCharacteristics {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        shadowOf(characteristics)
            .set(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES, capabilities)
        return characteristics
    }
}
