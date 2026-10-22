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
package com.google.jetpackcamera.ui.components.capture

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ZoomBarComponentsTest {

    @Test
    fun formatZoomLabel_unselectedStops_formatsWithoutMultiplierOrLeadingZero() {
        assertThat(formatZoomLabel(targetZoom = 0.5f, currentRatio = 1.0f, isSelected = false))
            .isEqualTo(".5")
        assertThat(formatZoomLabel(targetZoom = 1.0f, currentRatio = 1.0f, isSelected = false))
            .isEqualTo("1")
        assertThat(formatZoomLabel(targetZoom = 2.0f, currentRatio = 1.0f, isSelected = false))
            .isEqualTo("2")
        assertThat(formatZoomLabel(targetZoom = 5.0f, currentRatio = 1.0f, isSelected = false))
            .isEqualTo("5")
        assertThat(formatZoomLabel(targetZoom = 10.0f, currentRatio = 1.0f, isSelected = false))
            .isEqualTo("10")
    }

    @Test
    fun formatZoomLabel_selectedStops_formatsWithMultiplier() {
        assertThat(formatZoomLabel(targetZoom = 0.5f, currentRatio = 0.5f, isSelected = true))
            .isEqualTo(".5\u00D7")
        assertThat(formatZoomLabel(targetZoom = 1.0f, currentRatio = 1.0f, isSelected = true))
            .isEqualTo("1\u00D7")
        assertThat(formatZoomLabel(targetZoom = 2.0f, currentRatio = 2.0f, isSelected = true))
            .isEqualTo("2\u00D7")
        assertThat(formatZoomLabel(targetZoom = 5.0f, currentRatio = 5.0f, isSelected = true))
            .isEqualTo("5\u00D7")
    }

    @Test
    fun formatZoomLabel_inBetweenPinchRatio_formatsCurrentRatio() {
        assertThat(formatZoomLabel(targetZoom = 1.0f, currentRatio = 1.4f, isSelected = true))
            .isEqualTo("1.4\u00D7")
        assertThat(formatZoomLabel(targetZoom = 0.5f, currentRatio = 0.7f, isSelected = true))
            .isEqualTo(".7\u00D7")
        assertThat(formatZoomLabel(targetZoom = 0.5f, currentRatio = 0.99f, isSelected = true))
            .isEqualTo("1\u00D7")
    }
}
