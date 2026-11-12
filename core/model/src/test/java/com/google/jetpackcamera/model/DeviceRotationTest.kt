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
package com.google.jetpackcamera.model

import android.view.Surface
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeviceRotationTest {

    @Test
    fun fromUiSurfaceRotation_mapsAllSurfaceRotationsAndIsInverseOfToUiSurfaceRotation() {
        assertThat(DeviceRotation.fromUiSurfaceRotation(Surface.ROTATION_0))
            .isEqualTo(DeviceRotation.Natural)
        assertThat(DeviceRotation.fromUiSurfaceRotation(Surface.ROTATION_90))
            .isEqualTo(DeviceRotation.Rotated270)
        assertThat(DeviceRotation.fromUiSurfaceRotation(Surface.ROTATION_180))
            .isEqualTo(DeviceRotation.Rotated180)
        assertThat(DeviceRotation.fromUiSurfaceRotation(Surface.ROTATION_270))
            .isEqualTo(DeviceRotation.Rotated90)
        assertThat(DeviceRotation.fromUiSurfaceRotation(-1))
            .isEqualTo(DeviceRotation.Natural)

        for (rotation in DeviceRotation.entries) {
            assertThat(DeviceRotation.fromUiSurfaceRotation(rotation.toUiSurfaceRotation()))
                .isEqualTo(rotation)
        }
    }

    @Test
    fun toGravityRotationDegrees_computesInWindowGravityCompensationAngle() {
        // Phone upright (Dev 0°, Win 0°) -> Icons 0°
        assertThat(
            DeviceRotation.Natural.toGravityRotationDegrees(
                windowRotation = DeviceRotation.Natural
            )
        ).isEqualTo(0)

        // Phone turned 90° CCW with locked portrait window (Dev 270°, Win 0°) -> Icons 90°
        assertThat(
            DeviceRotation.Rotated270.toGravityRotationDegrees(
                windowRotation = DeviceRotation.Natural
            )
        ).isEqualTo(90)

        // Phone turned 90° CW with locked portrait window (Dev 90°, Win 0°) -> Icons 270°
        assertThat(
            DeviceRotation.Rotated90.toGravityRotationDegrees(
                windowRotation = DeviceRotation.Natural
            )
        ).isEqualTo(270)

        // Foldable/Tablet turned 90° CCW with OS auto-rotate ON (Dev 270°, Win 270°) -> Icons 0°
        assertThat(
            DeviceRotation.Rotated270.toGravityRotationDegrees(
                windowRotation = DeviceRotation.Rotated270
            )
        ).isEqualTo(0)

        // Foldable/Tablet turned 90° CCW with OS auto-rotate OFF (Dev 270°, Win 0°) -> Icons 90°
        assertThat(
            DeviceRotation.Rotated270.toGravityRotationDegrees(
                windowRotation = DeviceRotation.Natural
            )
        ).isEqualTo(90)

        // Upside-down delta (180°) defaults to 0° and honors stateful fallbackOn180Degrees
        assertThat(
            DeviceRotation.Rotated180.toGravityRotationDegrees(
                windowRotation = DeviceRotation.Natural
            )
        ).isEqualTo(0)
        assertThat(
            DeviceRotation.Rotated180.toGravityRotationDegrees(
                windowRotation = DeviceRotation.Natural,
                fallbackOn180Degrees = 270
            )
        ).isEqualTo(270)
        assertThat(
            DeviceRotation.Rotated90.toGravityRotationDegrees(
                windowRotation = DeviceRotation.Rotated270,
                fallbackOn180Degrees = 90
            )
        ).isEqualTo(90)
    }

    @Test
    fun toWindowCounterRotationDegrees_andIsLandscape_matchAllQuadrants() {
        assertThat(DeviceRotation.Natural.toWindowCounterRotationDegrees()).isEqualTo(0f)
        assertThat(DeviceRotation.Rotated90.toWindowCounterRotationDegrees()).isEqualTo(90f)
        assertThat(DeviceRotation.Rotated180.toWindowCounterRotationDegrees()).isEqualTo(180f)
        assertThat(DeviceRotation.Rotated270.toWindowCounterRotationDegrees()).isEqualTo(-90f)

        assertThat(DeviceRotation.Natural.isLandscape).isFalse()
        assertThat(DeviceRotation.Rotated180.isLandscape).isFalse()
        assertThat(DeviceRotation.Rotated90.isLandscape).isTrue()
        assertThat(DeviceRotation.Rotated270.isLandscape).isTrue()
    }
}
