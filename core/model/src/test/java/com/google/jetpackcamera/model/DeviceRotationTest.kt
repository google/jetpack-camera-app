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
package com.google.jetpackcamera.model

import android.view.Surface
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DeviceRotationTest {

    @Test
    fun toUiRotationDegrees_mapsRotated180ToZeroAndLandscapesToOppositeSign() {
        assertThat(DeviceRotation.Natural.toUiRotationDegrees()).isEqualTo(0f)
        assertThat(DeviceRotation.Rotated180.toUiRotationDegrees()).isEqualTo(0f)
        assertThat(DeviceRotation.Rotated90.toUiRotationDegrees()).isEqualTo(-90f)
        assertThat(DeviceRotation.Rotated270.toUiRotationDegrees()).isEqualTo(90f)
    }

    @Test
    fun toWindowCounterRotationDegrees_cancelsOutUiRotationInLandscape() {
        assertThat(DeviceRotation.Natural.toWindowCounterRotationDegrees()).isEqualTo(0f)
        assertThat(DeviceRotation.Rotated90.toWindowCounterRotationDegrees()).isEqualTo(90f)
        assertThat(DeviceRotation.Rotated180.toWindowCounterRotationDegrees()).isEqualTo(180f)
        assertThat(DeviceRotation.Rotated270.toWindowCounterRotationDegrees()).isEqualTo(-90f)

        assertThat(
            DeviceRotation.Rotated90.toWindowCounterRotationDegrees() +
                DeviceRotation.Rotated90.toUiRotationDegrees()
        ).isEqualTo(0f)
        assertThat(
            DeviceRotation.Rotated270.toWindowCounterRotationDegrees() +
                DeviceRotation.Rotated270.toUiRotationDegrees()
        ).isEqualTo(0f)
    }

    @Test
    fun isLandscape_isTrueOnlyForRotated90AndRotated270() {
        assertThat(DeviceRotation.Natural.isLandscape).isFalse()
        assertThat(DeviceRotation.Rotated180.isLandscape).isFalse()
        assertThat(DeviceRotation.Rotated90.isLandscape).isTrue()
        assertThat(DeviceRotation.Rotated270.isLandscape).isTrue()
    }

    @Test
    fun fromSurfaceRotation_isInverseOfToUiSurfaceRotation() {
        for (rotation in DeviceRotation.entries) {
            val surfaceRotation = rotation.toUiSurfaceRotation()
            assertThat(DeviceRotation.fromSurfaceRotation(surfaceRotation)).isEqualTo(rotation)
        }
        assertThat(DeviceRotation.fromSurfaceRotation(Surface.ROTATION_0))
            .isEqualTo(DeviceRotation.Natural)
        assertThat(DeviceRotation.fromSurfaceRotation(Surface.ROTATION_90))
            .isEqualTo(DeviceRotation.Rotated270)
        assertThat(DeviceRotation.fromSurfaceRotation(Surface.ROTATION_180))
            .isEqualTo(DeviceRotation.Rotated180)
        assertThat(DeviceRotation.fromSurfaceRotation(Surface.ROTATION_270))
            .isEqualTo(DeviceRotation.Rotated90)
    }
}
