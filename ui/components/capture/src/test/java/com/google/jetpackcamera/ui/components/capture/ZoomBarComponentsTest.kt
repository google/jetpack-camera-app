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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState
import java.util.Locale
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Unit and Compose UI tests for [ZoomBar] formatting, stop selection, RTL layout/BiDi rendering,
 * and upright label rotation.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ZoomBarComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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

    @Test
    fun endCapFraction_scalesWithDistanceToEndStop() {
        assertThat(endCapFraction(distance = 0f)).isEqualTo(1f)
        assertThat(endCapFraction(distance = 0.25f)).isEqualTo(0.75f)
        assertThat(endCapFraction(distance = 1f)).isEqualTo(0f)
        assertThat(endCapFraction(distance = 3f)).isEqualTo(0f)
    }

    @Test
    fun endCapFraction_pastEndStop_staysFullyRounded() {
        assertThat(endCapFraction(distance = -0.2f)).isEqualTo(1f)
    }

    @Test
    fun zoomBar_horizontalLtr_ordersStopsLeftToRightAndAlignsIndicator() {
        val stops = listOf(0.5f, 1.0f, 2.0f, 5.0f)
        val stopTags = listOf(
            0.5f to ZOOM_BUTTON_MIN_TAG,
            1.0f to ZOOM_BUTTON_1_TAG,
            2.0f to ZOOM_BUTTON_2_TAG,
            5.0f to ZOOM_BUTTON_5_TAG
        )
        var currentRatio by mutableFloatStateOf(0.5f)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Ltr,
                LocalDisableAnimations provides true
            ) {
                ZoomBar(
                    zoomControlUiState = ZoomControlUiState.Enabled(
                        zoomLevels = stops,
                        primaryLensFacing = LensFacing.BACK,
                        primaryZoomRatio = currentRatio
                    ),
                    onChangeZoom = {},
                    orientation = Orientation.Horizontal
                )
            }
        }

        val minCenterX = stopBounds(ZOOM_BUTTON_MIN_TAG).centerX
        val oneCenterX = stopBounds(ZOOM_BUTTON_1_TAG).centerX
        val twoCenterX = stopBounds(ZOOM_BUTTON_2_TAG).centerX
        val fiveCenterX = stopBounds(ZOOM_BUTTON_5_TAG).centerX

        assertThat(minCenterX).isLessThan(oneCenterX)
        assertThat(oneCenterX).isLessThan(twoCenterX)
        assertThat(twoCenterX).isLessThan(fiveCenterX)

        for ((ratio, tag) in stopTags) {
            currentRatio = ratio
            composeTestRule.waitForIdle()

            val activeStopBounds = stopBounds(tag)
            val indicatorBounds = stopBounds(ZOOM_BAR_INDICATOR_TAG)

            assertWithMessage("Indicator centerX for stop $ratio in LTR")
                .that(indicatorBounds.centerX.value)
                .isWithin(0.5f)
                .of(activeStopBounds.centerX.value)
            assertWithMessage("Indicator centerY for stop $ratio in LTR")
                .that(indicatorBounds.centerY.value)
                .isWithin(0.5f)
                .of(activeStopBounds.centerY.value)
        }
    }

    @Test
    fun zoomBar_horizontalRtl_mirrorsStopsRightToLeftAndAlignsIndicator() {
        val stops = listOf(0.5f, 1.0f, 2.0f, 5.0f)
        val stopTags = listOf(
            0.5f to ZOOM_BUTTON_MIN_TAG,
            1.0f to ZOOM_BUTTON_1_TAG,
            2.0f to ZOOM_BUTTON_2_TAG,
            5.0f to ZOOM_BUTTON_5_TAG
        )
        var currentRatio by mutableFloatStateOf(0.5f)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDisableAnimations provides true
            ) {
                ZoomBar(
                    zoomControlUiState = ZoomControlUiState.Enabled(
                        zoomLevels = stops,
                        primaryLensFacing = LensFacing.BACK,
                        primaryZoomRatio = currentRatio
                    ),
                    onChangeZoom = {},
                    orientation = Orientation.Horizontal
                )
            }
        }

        val minCenterX = stopBounds(ZOOM_BUTTON_MIN_TAG).centerX
        val oneCenterX = stopBounds(ZOOM_BUTTON_1_TAG).centerX
        val twoCenterX = stopBounds(ZOOM_BUTTON_2_TAG).centerX
        val fiveCenterX = stopBounds(ZOOM_BUTTON_5_TAG).centerX

        // In Horizontal RTL, 0.5x sits on the right and 5.0x sits on the left.
        assertThat(minCenterX).isGreaterThan(oneCenterX)
        assertThat(oneCenterX).isGreaterThan(twoCenterX)
        assertThat(twoCenterX).isGreaterThan(fiveCenterX)

        for ((ratio, tag) in stopTags) {
            currentRatio = ratio
            composeTestRule.waitForIdle()

            val activeStopBounds = stopBounds(tag)
            val indicatorBounds = stopBounds(ZOOM_BAR_INDICATOR_TAG)

            assertWithMessage("Indicator centerX for stop $ratio in Horizontal RTL")
                .that(indicatorBounds.centerX.value)
                .isWithin(0.5f)
                .of(activeStopBounds.centerX.value)
            assertWithMessage("Indicator centerY for stop $ratio in Horizontal RTL")
                .that(indicatorBounds.centerY.value)
                .isWithin(0.5f)
                .of(activeStopBounds.centerY.value)
        }
    }

    @Test
    fun zoomBar_verticalRtl_preservesTopToBottomStopOrderAndAlignsIndicator() {
        val stops = listOf(0.5f, 1.0f, 2.0f, 5.0f)
        val stopTags = listOf(
            0.5f to ZOOM_BUTTON_MIN_TAG,
            1.0f to ZOOM_BUTTON_1_TAG,
            2.0f to ZOOM_BUTTON_2_TAG,
            5.0f to ZOOM_BUTTON_5_TAG
        )
        var currentRatio by mutableFloatStateOf(0.5f)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDisableAnimations provides true
            ) {
                ZoomBar(
                    zoomControlUiState = ZoomControlUiState.Enabled(
                        zoomLevels = stops,
                        primaryLensFacing = LensFacing.BACK,
                        primaryZoomRatio = currentRatio
                    ),
                    onChangeZoom = {},
                    orientation = Orientation.Vertical
                )
            }
        }

        val minCenterY = stopBounds(ZOOM_BUTTON_MIN_TAG).centerY
        val oneCenterY = stopBounds(ZOOM_BUTTON_1_TAG).centerY
        val twoCenterY = stopBounds(ZOOM_BUTTON_2_TAG).centerY
        val fiveCenterY = stopBounds(ZOOM_BUTTON_5_TAG).centerY

        // Vertical order remains 0.5x at top -> 5.0x at bottom in RTL.
        assertThat(minCenterY).isLessThan(oneCenterY)
        assertThat(oneCenterY).isLessThan(twoCenterY)
        assertThat(twoCenterY).isLessThan(fiveCenterY)

        for ((ratio, tag) in stopTags) {
            currentRatio = ratio
            composeTestRule.waitForIdle()

            val activeStopBounds = stopBounds(tag)
            val indicatorBounds = stopBounds(ZOOM_BAR_INDICATOR_TAG)

            assertThat(indicatorBounds.width).isEqualTo(ZoomBarDefaults.SlotSize)
            assertThat(indicatorBounds.height).isEqualTo(ZoomBarDefaults.IndicatorHeight)
            assertWithMessage("Indicator centerX for stop $ratio in Vertical RTL")
                .that(indicatorBounds.centerX.value)
                .isWithin(0.5f)
                .of(activeStopBounds.centerX.value)
            assertWithMessage("Indicator centerY for stop $ratio in Vertical RTL")
                .that(indicatorBounds.centerY.value)
                .isWithin(0.5f)
                .of(activeStopBounds.centerY.value)
        }
    }

    @Test
    fun zoomBar_rtlBidiGlyphOrdering_keepsDecimalAndMultiplierLeftToRight() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            var currentRatio by mutableFloatStateOf(1.0f)

            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalDisableAnimations provides true
                ) {
                    ZoomBar(
                        zoomControlUiState = ZoomControlUiState.Enabled(
                            zoomLevels = listOf(0.5f, 1.0f, 2.0f, 5.0f),
                            primaryLensFacing = LensFacing.BACK,
                            primaryZoomRatio = currentRatio
                        ),
                        onChangeZoom = {},
                        orientation = Orientation.Horizontal
                    )
                }
            }

            // Unselected 0.5x stop renders ".5" with '.' left of '5' under RTL layout direction.
            val unselectedHalfLayout = fetchTextLayoutResult(ZOOM_BUTTON_MIN_TAG)
            assertThat(unselectedHalfLayout.layoutInput.text.text).isEqualTo(".5")
            assertThat(unselectedHalfLayout.getParagraphDirection(0))
                .isEqualTo(ResolvedTextDirection.Ltr)
            val dotBoxUnselected = unselectedHalfLayout.getBoundingBox(0)
            val fiveBoxUnselected = unselectedHalfLayout.getBoundingBox(1)
            assertWithMessage("'.' should sit left of '5' in unselected '.5' under RTL")
                .that(dotBoxUnselected.left)
                .isLessThan(fiveBoxUnselected.left)

            // Selected 1.0x stop renders "1×" with '1' left of '×' under RTL layout direction.
            val selectedOneLayout = fetchTextLayoutResult(ZOOM_BUTTON_1_TAG)
            assertThat(selectedOneLayout.layoutInput.text.text).isEqualTo("1\u00D7")
            assertThat(selectedOneLayout.getParagraphDirection(0))
                .isEqualTo(ResolvedTextDirection.Ltr)
            val oneBox = selectedOneLayout.getBoundingBox(0)
            val timesBoxOnOne = selectedOneLayout.getBoundingBox(1)
            assertWithMessage("'1' should sit left of '×' in selected '1×' under RTL")
                .that(oneBox.left)
                .isLessThan(timesBoxOnOne.left)

            // Selected 0.5x stop renders ".5×" with '.' < '5' < '×' under RTL layout direction.
            currentRatio = 0.5f
            composeTestRule.waitForIdle()

            val selectedHalfLayout = fetchTextLayoutResult(ZOOM_BUTTON_MIN_TAG)
            assertThat(selectedHalfLayout.layoutInput.text.text).isEqualTo(".5\u00D7")
            assertThat(selectedHalfLayout.getParagraphDirection(0))
                .isEqualTo(ResolvedTextDirection.Ltr)
            val dotBoxSelected = selectedHalfLayout.getBoundingBox(0)
            val fiveBoxSelected = selectedHalfLayout.getBoundingBox(1)
            val timesBoxOnHalf = selectedHalfLayout.getBoundingBox(2)
            assertWithMessage("'.' should sit left of '5' in selected '.5×' under RTL")
                .that(dotBoxSelected.left)
                .isLessThan(fiveBoxSelected.left)
            assertWithMessage("'5' should sit left of '×' in selected '.5×' under RTL")
                .that(fiveBoxSelected.left)
                .isLessThan(timesBoxOnHalf.left)
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun zoomBar_labelRotationDegrees_keepsStopBoundsAndOrderUnchanged() {
        val tags = listOf(
            ZOOM_BUTTON_MIN_TAG,
            ZOOM_BUTTON_1_TAG,
            ZOOM_BUTTON_2_TAG,
            ZOOM_BUTTON_5_TAG
        )
        var orientation by mutableStateOf(Orientation.Horizontal)
        var rotationDegrees by mutableFloatStateOf(0f)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                ZoomBar(
                    zoomControlUiState = ZoomControlUiState.Enabled(
                        zoomLevels = listOf(0.5f, 1.0f, 2.0f, 5.0f),
                        primaryLensFacing = LensFacing.BACK,
                        primaryZoomRatio = 1.0f
                    ),
                    onChangeZoom = {},
                    orientation = orientation,
                    labelRotationDegrees = rotationDegrees
                )
            }
        }

        for (testedOrientation in listOf(Orientation.Horizontal, Orientation.Vertical)) {
            orientation = testedOrientation
            rotationDegrees = 0f
            composeTestRule.waitForIdle()

            val baselineStopBounds = tags.associateWith { stopBounds(it) }
            val baselineIndicatorBounds = stopBounds(ZOOM_BAR_INDICATOR_TAG)

            for (angle in listOf(30f, 45f, 90f, 180f, 270f)) {
                rotationDegrees = angle
                composeTestRule.waitForIdle()

                for (tag in tags) {
                    assertWithMessage(
                        "Bounds for $tag in $testedOrientation at labelRotationDegrees=$angle"
                    )
                        .that(stopBounds(tag))
                        .isEqualTo(baselineStopBounds.getValue(tag))
                }
                assertWithMessage(
                    "Indicator bounds in $testedOrientation at labelRotationDegrees=$angle"
                )
                    .that(stopBounds(ZOOM_BAR_INDICATOR_TAG))
                    .isEqualTo(baselineIndicatorBounds)
            }
        }
    }

    @Test
    fun zoomBar_pinchZoomRatio_updatesActiveStopLabelByTenths() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            var currentRatio by mutableFloatStateOf(1.0f)

            composeTestRule.setContent {
                CompositionLocalProvider(LocalDisableAnimations provides true) {
                    ZoomBar(
                        zoomControlUiState = ZoomControlUiState.Enabled(
                            zoomLevels = listOf(0.5f, 1.0f, 2.0f, 5.0f),
                            primaryLensFacing = LensFacing.BACK,
                            primaryZoomRatio = currentRatio
                        ),
                        onChangeZoom = {},
                        orientation = Orientation.Horizontal
                    )
                }
            }

            assertThat(fetchTextLayoutResult(ZOOM_BUTTON_1_TAG).layoutInput.text.text)
                .isEqualTo("1\u00D7")

            // Sub-tenth pinch jitter stays at 1.0 tenth ("1×").
            currentRatio = 1.04f
            composeTestRule.waitForIdle()
            assertThat(fetchTextLayoutResult(ZOOM_BUTTON_1_TAG).layoutInput.text.text)
                .isEqualTo("1\u00D7")

            // Crossing tenth boundary updates rendered label to "1.4×".
            currentRatio = 1.42f
            composeTestRule.waitForIdle()
            assertThat(fetchTextLayoutResult(ZOOM_BUTTON_1_TAG).layoutInput.text.text)
                .isEqualTo("1.4\u00D7")
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun formatZoomLabel_arabicEgyptAndPersianLocales_usesConsistentNumeralScript() {
        val arabicEgypt = Locale.forLanguageTag("ar-EG")
        val persianIran = Locale.forLanguageTag("fa-IR")

        val arUnselectedHalf = formatZoomLabel(
            targetZoom = 0.5f,
            currentRatio = 1.0f,
            isSelected = false,
            locale = arabicEgypt
        )
        val arSelectedPinched = formatZoomLabel(
            targetZoom = 1.0f,
            currentRatio = 1.4f,
            isSelected = true,
            locale = arabicEgypt
        )
        val arAccessibleHalf = formatAccessibleZoomLabel(0.50783783f, arabicEgypt)

        assertConsistentNumeralScript(arUnselectedHalf)
        assertConsistentNumeralScript(arSelectedPinched)
        assertConsistentNumeralScript(arAccessibleHalf)
        assertThat(arUnselectedHalf).isEqualTo("\u066B\u0665")
        assertThat(arSelectedPinched).isEqualTo("\u0661\u066B\u0664\u00D7")
        assertThat(arAccessibleHalf).isEqualTo("\u0660\u066B\u0665")

        val faUnselectedHalf = formatZoomLabel(
            targetZoom = 0.5f,
            currentRatio = 1.0f,
            isSelected = false,
            locale = persianIran
        )
        val faSelectedPinched = formatZoomLabel(
            targetZoom = 1.0f,
            currentRatio = 1.4f,
            isSelected = true,
            locale = persianIran
        )
        val faAccessibleHalf = formatAccessibleZoomLabel(0.50783783f, persianIran)

        assertConsistentNumeralScript(faUnselectedHalf)
        assertConsistentNumeralScript(faSelectedPinched)
        assertConsistentNumeralScript(faAccessibleHalf)
        assertThat(faUnselectedHalf).isEqualTo("\u066B\u06F5")
        assertThat(faSelectedPinched).isEqualTo("\u06F1\u066B\u06F4\u00D7")
        assertThat(faAccessibleHalf).isEqualTo("\u06F0\u066B\u06F5")
    }

    @Test
    fun zoomBar_semantics_exposesTraversalGroupRadioRoleContentDescriptionAndPassiveIndicator() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            composeTestRule.setContent {
                CompositionLocalProvider(LocalDisableAnimations provides true) {
                    ZoomBar(
                        zoomControlUiState = ZoomControlUiState.Enabled(
                            zoomLevels = listOf(0.5f, 1.0f, 2.0f, 5.0f),
                            primaryLensFacing = LensFacing.BACK,
                            primaryZoomRatio = 1.0f
                        ),
                        onChangeZoom = {},
                        orientation = Orientation.Horizontal
                    )
                }
            }

            composeTestRule.onNodeWithTag(ZOOM_BUTTON_ROW_TAG)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))

            composeTestRule.onNodeWithTag(ZOOM_BUTTON_MIN_TAG)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
                .assert(isFocusable())
                .assertContentDescriptionEquals("0.5 times zoom")
                .assertIsNotSelected()
                .assert(
                    SemanticsMatcher("onClickLabel equals Zoom to 0.5 times") { node ->
                        node.config.getOrNull(SemanticsActions.OnClick)?.label ==
                            "Zoom to 0.5 times"
                    }
                )

            composeTestRule.onNodeWithTag(ZOOM_BUTTON_1_TAG)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
                .assert(isFocusable())
                .assertContentDescriptionEquals("1 times zoom")
                .assertIsSelected()

            composeTestRule.onNodeWithTag(ZOOM_BAR_INDICATOR_TAG)
                .assertHasNoClickAction()

            // Verify GetTextLayoutResult remains accessible on the merged stop node.
            assertThat(fetchTextLayoutResult(ZOOM_BUTTON_MIN_TAG).layoutInput.text.text)
                .isEqualTo(".5")
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun zoomBar_pinchedOffStop_clearsSelectedSetsStateDescriptionAndAllowsClickReset() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            var lastRequestedZoom: Float? = null

            composeTestRule.setContent {
                CompositionLocalProvider(LocalDisableAnimations provides true) {
                    ZoomBar(
                        zoomControlUiState = ZoomControlUiState.Enabled(
                            zoomLevels = listOf(0.5f, 1.0f, 2.0f, 5.0f),
                            primaryLensFacing = LensFacing.BACK,
                            primaryZoomRatio = 1.4f
                        ),
                        onChangeZoom = { lastRequestedZoom = it },
                        orientation = Orientation.Horizontal
                    )
                }
            }

            composeTestRule.onNodeWithTag(ZOOM_BUTTON_1_TAG)
                .assertIsNotSelected()
                .assertContentDescriptionEquals("1 times zoom")
                .assertHasClickAction()
                .assert(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.StateDescription,
                        "Current zoom 1.4 times"
                    )
                )
                .performSemanticsAction(SemanticsActions.OnClick)

            assertThat(lastRequestedZoom).isEqualTo(1.0f)
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun zoomBar_activeStop_exposesFineZoomCustomActionsAndRespectsBounds() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            var currentRatio by mutableFloatStateOf(1.0f)
            var lastRequestedZoom: Float? = null

            composeTestRule.setContent {
                CompositionLocalProvider(LocalDisableAnimations provides true) {
                    ZoomBar(
                        zoomControlUiState = ZoomControlUiState.Enabled(
                            zoomLevels = listOf(0.5f, 1.0f, 2.0f, 5.0f),
                            primaryLensFacing = LensFacing.BACK,
                            primaryZoomRatio = currentRatio
                        ),
                        onChangeZoom = {
                            lastRequestedZoom = it
                            currentRatio = it
                        },
                        orientation = Orientation.Horizontal
                    )
                }
            }

            // Unselected stops do not expose fine-zoom custom actions.
            assertThat(
                composeTestRule.onNodeWithTag(ZOOM_BUTTON_MIN_TAG)
                    .fetchSemanticsNode()
                    .config
                    .getOrNull(SemanticsActions.CustomActions)
            ).isNull()

            // Active 1.0x stop exposes both "Zoom in" and "Zoom out".
            val actionsAtOne = composeTestRule.onNodeWithTag(ZOOM_BUTTON_1_TAG)
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
            assertThat(actionsAtOne.map { it.label })
                .containsExactly("Zoom in", "Zoom out")

            assertThat(actionsAtOne.first { it.label == "Zoom in" }.action()).isTrue()
            composeTestRule.waitForIdle()
            assertThat(lastRequestedZoom).isWithin(0.001f).of(1.1f)

            val actionsAtOnePointOne = composeTestRule.onNodeWithTag(ZOOM_BUTTON_1_TAG)
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
            assertThat(actionsAtOnePointOne.first { it.label == "Zoom out" }.action()).isTrue()
            composeTestRule.waitForIdle()
            assertThat(lastRequestedZoom).isWithin(0.001f).of(1.0f)

            // At minimum stop (0.5x), only "Zoom in" is exposed.
            currentRatio = 0.5f
            composeTestRule.waitForIdle()
            val actionsAtMin = composeTestRule.onNodeWithTag(ZOOM_BUTTON_MIN_TAG)
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
            assertThat(actionsAtMin.map { it.label }).containsExactly("Zoom in")

            // At maximum stop (5.0x), only "Zoom out" is exposed.
            currentRatio = 5.0f
            composeTestRule.waitForIdle()
            val actionsAtMax = composeTestRule.onNodeWithTag(ZOOM_BUTTON_5_TAG)
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
            assertThat(actionsAtMax.map { it.label }).containsExactly("Zoom out")
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    @Config(qualifiers = "420dpi")
    fun zoomBar_at420Dpi_measuresEveryStopAtLeast48Dp() {
        val stops = listOf(0.5f, 1.0f, 2.0f, 5.0f)
        val stopTags = listOf(
            0.5f to ZOOM_BUTTON_MIN_TAG,
            1.0f to ZOOM_BUTTON_1_TAG,
            2.0f to ZOOM_BUTTON_2_TAG,
            5.0f to ZOOM_BUTTON_5_TAG
        )
        var orientation by mutableStateOf(Orientation.Horizontal)
        var currentRatio by mutableFloatStateOf(1.0f)
        var measuredDensity = 0f

        composeTestRule.setContent {
            measuredDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                ZoomBar(
                    zoomControlUiState = ZoomControlUiState.Enabled(
                        zoomLevels = stops,
                        primaryLensFacing = LensFacing.BACK,
                        primaryZoomRatio = currentRatio
                    ),
                    onChangeZoom = {},
                    orientation = orientation
                )
            }
        }

        assertThat(measuredDensity).isWithin(0.01f).of(2.625f)

        for (testedOrientation in listOf(Orientation.Horizontal, Orientation.Vertical)) {
            orientation = testedOrientation
            composeTestRule.waitForIdle()

            for ((ratio, tag) in stopTags) {
                currentRatio = ratio
                composeTestRule.waitForIdle()

                composeTestRule.onNodeWithTag(tag)
                    .assertWidthIsAtLeast(48.dp)
                    .assertHeightIsAtLeast(48.dp)

                val activeStopBounds = stopBounds(tag)
                val indicatorBounds = stopBounds(ZOOM_BAR_INDICATOR_TAG)
                assertWithMessage("Indicator centerX for $ratio in $testedOrientation at 420dpi")
                    .that(indicatorBounds.centerX.value)
                    .isWithin(0.5f)
                    .of(activeStopBounds.centerX.value)
                assertWithMessage("Indicator centerY for $ratio in $testedOrientation at 420dpi")
                    .that(indicatorBounds.centerY.value)
                    .isWithin(0.5f)
                    .of(activeStopBounds.centerY.value)
            }
        }
    }

    @Test
    fun zoomBar_fontScale2x_clampsLabelFontScaleAndKeepsLabelsWithinStopBounds() {
        val stops = listOf(0.5f, 1.0f, 2.0f, 5.0f)
        val tags = listOf(
            ZOOM_BUTTON_MIN_TAG,
            ZOOM_BUTTON_1_TAG,
            ZOOM_BUTTON_2_TAG,
            ZOOM_BUTTON_5_TAG
        )
        var currentRatio by mutableFloatStateOf(1.0f)
        var rotationDegrees by mutableFloatStateOf(0f)
        var baseDensityValue = 1f

        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            baseDensityValue = baseDensity.density
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 2.0f),
                LocalDisableAnimations provides true
            ) {
                ZoomBar(
                    zoomControlUiState = ZoomControlUiState.Enabled(
                        zoomLevels = stops,
                        primaryLensFacing = LensFacing.BACK,
                        primaryZoomRatio = currentRatio
                    ),
                    onChangeZoom = {},
                    orientation = Orientation.Horizontal,
                    labelRotationDegrees = rotationDegrees
                )
            }
        }

        for ((ratio, angle) in listOf(1.0f to 0f, 1.4f to 0f, 1.4f to 90f)) {
            currentRatio = ratio
            rotationDegrees = angle
            composeTestRule.waitForIdle()

            for (tag in tags) {
                val itemBounds = stopBounds(tag)
                assertThat(itemBounds.width).isEqualTo(ZoomBarDefaults.SlotSize)
                assertThat(itemBounds.height).isEqualTo(ZoomBarDefaults.SlotSize)

                val textLayout = fetchTextLayoutResult(tag)
                assertThat(textLayout.layoutInput.density.fontScale)
                    .isEqualTo(ZoomBarDefaults.MAX_LABEL_FONT_SCALE)
                assertThat(textLayout.lineCount).isEqualTo(1)
                assertThat(textLayout.didOverflowWidth).isFalse()
                assertThat(textLayout.didOverflowHeight).isFalse()

                val labelNode = composeTestRule
                    .onNodeWithTag(tag, useUnmergedTree = true)
                    .fetchSemanticsNode()
                    .children
                    .single()
                val labelBoundsPx = labelNode.boundsInRoot
                val itemBoundsPx = composeTestRule
                    .onNodeWithTag(tag, useUnmergedTree = true)
                    .fetchSemanticsNode()
                    .boundsInRoot

                assertWithMessage(
                    "Label width <= slot width for $tag at ratio=$ratio, rotation=$angle"
                )
                    .that(labelBoundsPx.width)
                    .isAtMost(itemBoundsPx.width + 0.5f * baseDensityValue)
                assertWithMessage(
                    "Label height <= slot height for $tag at ratio=$ratio, rotation=$angle"
                )
                    .that(labelBoundsPx.height)
                    .isAtMost(itemBoundsPx.height + 0.5f * baseDensityValue)
                assertWithMessage("Label left within $tag at ratio=$ratio, rotation=$angle")
                    .that(labelBoundsPx.left)
                    .isAtLeast(itemBoundsPx.left - 0.5f * baseDensityValue)
                assertWithMessage("Label right within $tag at ratio=$ratio, rotation=$angle")
                    .that(labelBoundsPx.right)
                    .isAtMost(itemBoundsPx.right + 0.5f * baseDensityValue)
                assertWithMessage("Label top within $tag at ratio=$ratio, rotation=$angle")
                    .that(labelBoundsPx.top)
                    .isAtLeast(itemBoundsPx.top - 0.5f * baseDensityValue)
                assertWithMessage("Label bottom within $tag at ratio=$ratio, rotation=$angle")
                    .that(labelBoundsPx.bottom)
                    .isAtMost(itemBoundsPx.bottom + 0.5f * baseDensityValue)
            }
        }
    }

    @Test
    fun zoomBar_labelRotationDegrees_interpolatesAutoScaleSmoothlyAcrossAngles() {
        val tenStopTag = "ZoomButton_10.0Tag"
        var rotationDegrees by mutableFloatStateOf(29f)
        var baseDensityValue = 1f

        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            baseDensityValue = baseDensity.density
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 2.0f),
                LocalDisableAnimations provides true
            ) {
                ZoomBar(
                    zoomControlUiState = ZoomControlUiState.Enabled(
                        zoomLevels = listOf(0.5f, 1.0f, 2.0f, 5.0f, 10.0f),
                        primaryLensFacing = LensFacing.BACK,
                        primaryZoomRatio = 10.5f
                    ),
                    onChangeZoom = {},
                    orientation = Orientation.Horizontal,
                    labelRotationDegrees = rotationDegrees
                )
            }
        }

        val boundsAt29 = composeTestRule
            .onNodeWithTag(tenStopTag, useUnmergedTree = true)
            .fetchSemanticsNode()
            .children
            .single()
            .boundsInRoot

        rotationDegrees = 31f
        composeTestRule.waitForIdle()

        val boundsAt31 = composeTestRule
            .onNodeWithTag(tenStopTag, useUnmergedTree = true)
            .fetchSemanticsNode()
            .children
            .single()
            .boundsInRoot

        // Across 29° -> 31° there should be no abrupt 10dp step jump in rendered label bounds.
        assertThat(abs(boundsAt31.width - boundsAt29.width)).isLessThan(2f * baseDensityValue)
        assertThat(abs(boundsAt31.height - boundsAt29.height)).isLessThan(2f * baseDensityValue)
    }

    private fun assertConsistentNumeralScript(formatted: String) {
        val digitBlocks = formatted
            .filter { Character.isDigit(it) }
            .map { Character.UnicodeBlock.of(it) }
            .distinct()
        assertWithMessage("Digits in '$formatted' should share one UnicodeBlock")
            .that(digitBlocks.size)
            .isAtMost(1)
        if (formatted.contains('\u066B')) {
            val hasAsciiDigit = formatted.any { it in '0'..'9' }
            assertWithMessage("Formatted label '$formatted' must not mix ASCII digits and U+066B")
                .that(hasAsciiDigit)
                .isFalse()
        }
    }

    private fun stopBounds(tag: String): DpRect =
        composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun fetchTextLayoutResult(tag: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        val action = composeTestRule
            .onNodeWithTag(tag)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
        assertThat(action).isNotNull()
        assertThat(action!!.invoke(results)).isTrue()
        assertThat(results).hasSize(1)
        return results.single()
    }

    private val DpRect.centerX: Dp
        get() = (left + right) / 2

    private val DpRect.centerY: Dp
        get() = (top + bottom) / 2
}
