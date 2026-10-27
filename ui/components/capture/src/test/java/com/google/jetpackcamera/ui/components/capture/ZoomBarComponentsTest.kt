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
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ZoomBarComponentsTest {

    @Test
    fun formatZoomLabel_unselectedStops_formatsWithoutMultiplierOrLeadingZero() {
        assertThat(
            formatZoomLabel(
                targetZoom = 0.5f,
                currentRatio = 1.0f,
                isSelected = false,
                locale = Locale.US
            )
        ).isEqualTo(".5")
        assertThat(
            formatZoomLabel(
                targetZoom = 1.0f,
                currentRatio = 1.0f,
                isSelected = false,
                locale = Locale.US
            )
        ).isEqualTo("1")
        assertThat(
            formatZoomLabel(
                targetZoom = 2.0f,
                currentRatio = 1.0f,
                isSelected = false,
                locale = Locale.US
            )
        ).isEqualTo("2")
        assertThat(
            formatZoomLabel(
                targetZoom = 5.0f,
                currentRatio = 1.0f,
                isSelected = false,
                locale = Locale.US
            )
        ).isEqualTo("5")
        assertThat(
            formatZoomLabel(
                targetZoom = 10.0f,
                currentRatio = 1.0f,
                isSelected = false,
                locale = Locale.US
            )
        ).isEqualTo("10")
    }

    @Test
    fun formatZoomLabel_selectedStops_formatsWithMultiplier() {
        assertThat(
            formatZoomLabel(
                targetZoom = 0.5f,
                currentRatio = 0.5f,
                isSelected = true,
                locale = Locale.US
            )
        ).isEqualTo(".5\u00D7")
        assertThat(
            formatZoomLabel(
                targetZoom = 1.0f,
                currentRatio = 1.0f,
                isSelected = true,
                locale = Locale.US
            )
        ).isEqualTo("1\u00D7")
        assertThat(
            formatZoomLabel(
                targetZoom = 2.0f,
                currentRatio = 2.0f,
                isSelected = true,
                locale = Locale.US
            )
        ).isEqualTo("2\u00D7")
        assertThat(
            formatZoomLabel(
                targetZoom = 5.0f,
                currentRatio = 5.0f,
                isSelected = true,
                locale = Locale.US
            )
        ).isEqualTo("5\u00D7")
    }

    @Test
    fun formatZoomLabel_inBetweenPinchRatio_formatsCurrentRatio() {
        assertThat(
            formatZoomLabel(
                targetZoom = 1.0f,
                currentRatio = 1.4f,
                isSelected = true,
                locale = Locale.US
            )
        ).isEqualTo("1.4\u00D7")
        assertThat(
            formatZoomLabel(
                targetZoom = 0.5f,
                currentRatio = 0.7f,
                isSelected = true,
                locale = Locale.US
            )
        ).isEqualTo(".7\u00D7")
        assertThat(
            formatZoomLabel(
                targetZoom = 0.5f,
                currentRatio = 0.99f,
                isSelected = true,
                locale = Locale.US
            )
        ).isEqualTo("1\u00D7")
    }

    @Test
    fun formatZoomLabel_halfUpBoundaryCases_roundsCorrectly() {
        assertThat(
            formatZoomLabel(
                targetZoom = 1.0f,
                currentRatio = 0.95f,
                isSelected = true,
                locale = Locale.US
            )
        ).isEqualTo("1\u00D7")
        assertThat(
            formatZoomLabel(
                targetZoom = 5.0f,
                currentRatio = 4.95f,
                isSelected = true,
                locale = Locale.US
            )
        ).isEqualTo("5\u00D7")
    }

    @Test
    fun formatZoomLabel_localeWithCommaSeparator_usesLocalizedDecimalSeparator() {
        assertThat(
            formatZoomLabel(
                targetZoom = 0.5f,
                currentRatio = 0.5f,
                isSelected = true,
                locale = Locale.GERMANY
            )
        ).isEqualTo(",5\u00D7")
        assertThat(
            formatZoomLabel(
                targetZoom = 1.0f,
                currentRatio = 1.4f,
                isSelected = true,
                locale = Locale.GERMANY
            )
        ).isEqualTo("1,4\u00D7")
        assertThat(
            formatZoomLabel(
                targetZoom = 0.5f,
                currentRatio = 1.0f,
                isSelected = false,
                locale = Locale.GERMANY
            )
        ).isEqualTo(",5")
    }

    @Test
    fun selectedZoomStopIndex_roundsToTenthsAndResolvesTieBreak() {
        val stops = listOf(0.5f, 1.0f, 2.0f, 5.0f)

        // 0.95 rounds half-up to 1.0 tenth -> selects index 1 (1.0x stop)
        assertThat(selectedZoomStopIndex(stops, 0.95f)).isEqualTo(1)

        // 4.94 rounds to 4.9 tenths -> stays on index 2 (2.0x stop)
        assertThat(selectedZoomStopIndex(stops, 4.94f)).isEqualTo(2)

        // 5.0f selects index 3 (5.0x stop)
        assertThat(selectedZoomStopIndex(stops, 5.0f)).isEqualTo(3)

        // Adjacent stops rounding to same tenth: picks the closer stop
        val adjacentStops = listOf(0.5f, 0.96f, 1.0f, 2.0f)
        assertThat(selectedZoomStopIndex(adjacentStops, 0.97f)).isEqualTo(1)
        assertThat(selectedZoomStopIndex(adjacentStops, 0.99f)).isEqualTo(2)
    }

    @Test
    fun formatAccessibleZoomLabel_roundsAndKeepsLeadingZero() {
        // Pixel minimum zoom ~0.507 rounds to 0.5 with leading zero for speech
        assertThat(formatAccessibleZoomLabel(0.50783783f, Locale.US)).isEqualTo("0.5")
        assertThat(formatAccessibleZoomLabel(1.0f, Locale.US)).isEqualTo("1")
        assertThat(formatAccessibleZoomLabel(2.0f, Locale.US)).isEqualTo("2")
        assertThat(formatAccessibleZoomLabel(5.0f, Locale.US)).isEqualTo("5")
        // Respects locale decimal separator
        assertThat(formatAccessibleZoomLabel(0.50783783f, Locale.GERMANY)).isEqualTo("0,5")
    }
}
