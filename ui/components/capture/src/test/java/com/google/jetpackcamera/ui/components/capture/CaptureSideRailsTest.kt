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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [StartSideRail], [EndSideRail], [CaptureSlots], and [CaptureSlotScope].
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w800dp-h800dp")
class CaptureSideRailsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun boundsOf(tag: String): DpRect =
        composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    @Test
    fun startSideRail_ltrAndRtl_preservesStrictTopToBottomSlotOrder_andChildLayoutDirection() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        var indicatorDirection: LayoutDirection? = null
        var zoomDirection: LayoutDirection? = null
        var quickSettingsDirection: LayoutDirection? = null

        val baseSlots = PlaceholderCaptureSlots.sideRail()
        val instrumentedSlots = baseSlots.copy(
            indicatorColumn = { mod ->
                indicatorDirection = LocalLayoutDirection.current
                baseSlots.indicatorColumn(this, mod)
            },
            zoomControls = { mod ->
                zoomDirection = LocalLayoutDirection.current
                baseSlots.zoomControls(this, mod)
            },
            quickSettingsButton = { mod ->
                quickSettingsDirection = LocalLayoutDirection.current
                baseSlots.quickSettingsButton(this, mod)
            }
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                StartSideRail(
                    slots = instrumentedSlots,
                    modifier = Modifier.height(800.dp)
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(START_SIDE_RAIL_TAG).assertIsDisplayed()
        val ltrRail = boundsOf(START_SIDE_RAIL_TAG)
        val ltrIndicator = boundsOf(VIDEO_QUALITY_TAG)
        val ltrZoom = boundsOf(ZOOM_BUTTON_ROW_TAG)
        val ltrQuickSettings = boundsOf(QUICK_SETTINGS_DROP_DOWN)

        assertThat(ltrIndicator.top).isAtLeast(ltrRail.top)
        assertThat(ltrIndicator.bottom).isAtMost(ltrZoom.top)
        assertThat(ltrZoom.bottom).isAtMost(ltrQuickSettings.top)
        assertThat(ltrQuickSettings.bottom).isAtMost(ltrRail.bottom)
        assertThat(indicatorDirection).isEqualTo(LayoutDirection.Ltr)
        assertThat(zoomDirection).isEqualTo(LayoutDirection.Ltr)
        assertThat(quickSettingsDirection).isEqualTo(LayoutDirection.Ltr)

        layoutDirection = LayoutDirection.Rtl
        composeTestRule.waitForIdle()

        val rtlRail = boundsOf(START_SIDE_RAIL_TAG)
        val rtlIndicator = boundsOf(VIDEO_QUALITY_TAG)
        val rtlZoom = boundsOf(ZOOM_BUTTON_ROW_TAG)
        val rtlQuickSettings = boundsOf(QUICK_SETTINGS_DROP_DOWN)

        assertThat(rtlIndicator.top).isAtLeast(rtlRail.top)
        assertThat(rtlIndicator.bottom).isAtMost(rtlZoom.top)
        assertThat(rtlZoom.bottom).isAtMost(rtlQuickSettings.top)
        assertThat(rtlQuickSettings.bottom).isAtMost(rtlRail.bottom)

        // Vertical coordinates must remain identical under RTL while child slots inherit RTL.
        assertThat(rtlIndicator.top.value).isWithin(0.5f).of(ltrIndicator.top.value)
        assertThat(rtlZoom.top.value).isWithin(0.5f).of(ltrZoom.top.value)
        assertThat(rtlQuickSettings.top.value).isWithin(0.5f).of(ltrQuickSettings.top.value)
        assertThat(indicatorDirection).isEqualTo(LayoutDirection.Rtl)
        assertThat(zoomDirection).isEqualTo(LayoutDirection.Rtl)
        assertThat(quickSettingsDirection).isEqualTo(LayoutDirection.Rtl)
    }

    @Test
    fun endSideRail_ltrAndRtl_preservesStrictTopToBottomShutterAndModeToggleOrder() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        var modeToggleDirection: LayoutDirection? = null

        val baseSlots = PlaceholderCaptureSlots.sideRail(
            includeImageWell = true,
            includePauseSlot = true,
            includeElapsedTime = true
        )
        val instrumentedSlots = baseSlots.copy(
            captureModeToggle = { mod ->
                modeToggleDirection = LocalLayoutDirection.current
                baseSlots.captureModeToggle(this, mod)
            }
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                EndSideRail(
                    slots = instrumentedSlots,
                    modifier = Modifier.height(800.dp),
                    isRecording = true,
                    showElapsedTimeInRail = true
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(END_SIDE_RAIL_TAG).assertIsDisplayed()
        val orderedTags = listOf(
            ELAPSED_TIME_TAG,
            FLIP_CAMERA_BUTTON,
            CAPTURE_BUTTON,
            IMAGE_WELL_TAG,
            AUDIO_INPUT_TOGGLE,
            CAPTURE_MODE_TOGGLE_BUTTON
        )

        fun assertStrictVerticalSequence(contextLabel: String): List<DpRect> {
            val railBounds = boundsOf(END_SIDE_RAIL_TAG)
            val slotBounds = orderedTags.map { tag ->
                boundsOf(tag).also { rect ->
                    assertWithMessage("$contextLabel: $tag should have positive height")
                        .that(rect.height)
                        .isGreaterThan(0.dp)
                }
            }
            assertWithMessage("$contextLabel: top item inside rail")
                .that(slotBounds.first().top)
                .isAtLeast(railBounds.top)
            for (i in 0 until slotBounds.lastIndex) {
                assertWithMessage(
                    "$contextLabel: ${orderedTags[i]} must sit strictly above ${orderedTags[i + 1]}"
                )
                    .that(slotBounds[i].bottom)
                    .isAtMost(slotBounds[i + 1].top)
            }
            assertWithMessage("$contextLabel: bottom item inside rail")
                .that(slotBounds.last().bottom)
                .isAtMost(railBounds.bottom)
            return slotBounds
        }

        val ltrBounds = assertStrictVerticalSequence("LTR")
        assertThat(modeToggleDirection).isEqualTo(LayoutDirection.Ltr)

        layoutDirection = LayoutDirection.Rtl
        composeTestRule.waitForIdle()

        val rtlBounds = assertStrictVerticalSequence("RTL")
        assertThat(modeToggleDirection).isEqualTo(LayoutDirection.Rtl)
        ltrBounds.zip(rtlBounds).forEachIndexed { index, (ltrRect, rtlRect) ->
            assertWithMessage("Vertical top for ${orderedTags[index]} should match in LTR and RTL")
                .that(rtlRect.top.value)
                .isWithin(0.5f)
                .of(ltrRect.top.value)
        }
    }

    @Test
    fun sideRails_compactHeightMode_fitsWithin360dpWithoutClippingOrOverlap() {
        val compactSlots = PlaceholderCaptureSlots.sideRail(
            isCompactHeight = true,
            includeImageWell = true,
            includePauseSlot = true,
            includeElapsedTime = true
        )

        composeTestRule.setContent {
            Row(modifier = Modifier.height(360.dp)) {
                StartSideRail(
                    slots = compactSlots,
                    modifier = Modifier.height(360.dp),
                    isCompactHeight = true,
                    isRecording = true
                )
                EndSideRail(
                    slots = compactSlots,
                    modifier = Modifier.height(360.dp),
                    isCompactHeight = true,
                    isRecording = true,
                    showElapsedTimeInRail = true
                )
            }
        }
        composeTestRule.waitForIdle()

        val startRailTags = listOf(
            VIDEO_QUALITY_TAG,
            ZOOM_BUTTON_ROW_TAG,
            QUICK_SETTINGS_DROP_DOWN
        )
        val endRailTags = listOf(
            ELAPSED_TIME_TAG,
            FLIP_CAMERA_BUTTON,
            CAPTURE_BUTTON,
            IMAGE_WELL_TAG,
            AUDIO_INPUT_TOGGLE,
            CAPTURE_MODE_TOGGLE_BUTTON
        )

        fun verifyRailFitsIn360dp(railLabel: String, tags: List<String>) {
            val unclippedList = tags.map { tag ->
                val clipped = composeTestRule.onNodeWithTag(tag).getBoundsInRoot()
                val unclipped = composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
                assertWithMessage("$railLabel: $tag must have non-zero size")
                    .that(unclipped.height)
                    .isGreaterThan(0.dp)
                assertWithMessage("$railLabel: $tag must have non-zero width")
                    .that(unclipped.width)
                    .isGreaterThan(0.dp)
                assertWithMessage("$railLabel: $tag top must be >= 0dp")
                    .that(unclipped.top.value)
                    .isAtLeast(-0.5f)
                assertWithMessage("$railLabel: $tag bottom must be <= 360dp")
                    .that(unclipped.bottom.value)
                    .isAtMost(360.5f)
                assertWithMessage("$railLabel: $tag should not be clipped vertically")
                    .that(clipped.height.value)
                    .isWithin(0.5f)
                    .of(unclipped.height.value)
                unclipped
            }
            for (i in 0 until unclippedList.lastIndex) {
                assertWithMessage(
                    "$railLabel: ${tags[i]} bottom (${unclippedList[i].bottom}) must not overlap " +
                        "${tags[i + 1]} top (${unclippedList[i + 1].top})"
                )
                    .that(unclippedList[i].bottom.value)
                    .isAtMost(unclippedList[i + 1].top.value + 0.5f)
            }
        }

        verifyRailFitsIn360dp("StartSideRail", startRailTags)
        verifyRailFitsIn360dp("EndSideRail", endRailTags)
    }

    @Test
    fun endSideRail_omittedOptionalSlots_collapsesSpacesCleanly() {
        var includePause by mutableStateOf(true)

        composeTestRule.setContent {
            val slots = PlaceholderCaptureSlots.sideRail(
                isCompactHeight = false,
                includeImageWell = false,
                includePauseSlot = includePause,
                includeElapsedTime = false
            )
            EndSideRail(
                slots = slots,
                modifier = Modifier.height(800.dp),
                isCompactHeight = false,
                showElapsedTimeInRail = false
            )
        }
        composeTestRule.waitForIdle()

        // Omitted middle slot (imageWell) must not exist and must not leave a double 96dp gap
        // between captureButton and pauseToggleButton.
        composeTestRule.onNodeWithTag(IMAGE_WELL_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG).assertDoesNotExist()

        val flipBoundsWithPause = boundsOf(FLIP_CAMERA_BUTTON)
        val captureBoundsWithPause = boundsOf(CAPTURE_BUTTON)
        val pauseBounds = boundsOf(AUDIO_INPUT_TOGGLE)

        val flipToCaptureGap = captureBoundsWithPause.top - flipBoundsWithPause.bottom
        val captureToPauseGap = pauseBounds.top - captureBoundsWithPause.bottom
        assertThat(flipToCaptureGap.value).isWithin(1f).of(48f)
        assertThat(captureToPauseGap.value).isWithin(1f).of(48f)

        // Omitting trailing pause slot as well leaves only Flip + Capture separated by 48dp,
        // centered cleanly in the 800dp rail with zero phantom trailing space.
        includePause = false
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(AUDIO_INPUT_TOGGLE).assertDoesNotExist()
        val railBounds = boundsOf(END_SIDE_RAIL_TAG)
        val flipBounds = boundsOf(FLIP_CAMERA_BUTTON)
        val captureBounds = boundsOf(CAPTURE_BUTTON)
        assertThat((captureBounds.top - flipBounds.bottom).value).isWithin(1f).of(48f)

        val clusterCenterY = (flipBounds.top.value + captureBounds.bottom.value) / 2f
        val railCenterY = (railBounds.top.value + railBounds.bottom.value) / 2f
        assertThat(clusterCenterY).isWithin(1f).of(railCenterY)
    }

    @Test
    fun startSideRail_omittedIndicatorAndQuickSettings_keepsZoomCentered() {
        val defaultSlots = PlaceholderCaptureSlots.default()
        val zoomOnlySlots = CaptureSlots(
            zoomControls = defaultSlots.zoomControls
        )

        composeTestRule.setContent {
            StartSideRail(
                slots = zoomOnlySlots,
                modifier = Modifier.height(800.dp)
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(VIDEO_QUALITY_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN).assertDoesNotExist()

        val railBounds = boundsOf(START_SIDE_RAIL_TAG)
        val zoomBounds = boundsOf(ZOOM_BUTTON_ROW_TAG)
        val railCenterY = (railBounds.top.value + railBounds.bottom.value) / 2f
        val zoomCenterY = (zoomBounds.top.value + zoomBounds.bottom.value) / 2f

        assertThat(zoomCenterY).isWithin(1f).of(railCenterY)
    }

    @Test
    fun endSideRail_customShutterStackOverride_invokesOverrideWithCompactFlag() {
        var isCompactHeight by mutableStateOf(false)
        var observedCompactParam: Boolean? = null
        var observedScopeCompact: Boolean? = null

        val baseSlots = PlaceholderCaptureSlots.sideRail()
        val overrideSlots = baseSlots.copy(
            shutterStack = { mod, compactFlag ->
                observedCompactParam = compactFlag
                observedScopeCompact = isCompact
                PlaceholderControlBox(
                    label = "CustomShutter",
                    width = 64.dp,
                    height = 140.dp,
                    modifier = mod,
                    testTag = IMAGE_WELL_TAG
                )
            }
        )

        composeTestRule.setContent {
            EndSideRail(
                slots = overrideSlots,
                modifier = Modifier.height(800.dp),
                isCompactHeight = isCompactHeight
            )
        }
        composeTestRule.waitForIdle()

        // Default VerticalRailShutterCluster must be bypassed when shutterStack is provided.
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertDoesNotExist()
        composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).assertDoesNotExist()
        composeTestRule.onNodeWithTag(IMAGE_WELL_TAG).assertIsDisplayed()
        assertThat(observedCompactParam).isFalse()
        assertThat(observedScopeCompact).isFalse()

        isCompactHeight = true
        composeTestRule.waitForIdle()

        assertThat(observedCompactParam).isTrue()
        assertThat(observedScopeCompact).isTrue()
    }

    @Test
    fun sideRails_recordingScope_propagatesOrientationCompactIsRecordingAndDismissToSlots() {
        var isCompactHeight by mutableStateOf(false)
        var isRecording by mutableStateOf(false)
        var dismissCallCount = 0

        var startOrientation: Orientation? = null
        var startCompact: Boolean? = null
        var startRecording: Boolean? = null

        var endOrientation: Orientation? = null
        var endCompact: Boolean? = null
        var endRecording: Boolean? = null

        val scopedSlots = CaptureSlots(
            quickSettingsButton = { mod ->
                startOrientation = orientation
                startCompact = isCompact
                startRecording = this.isRecording
                Box(
                    modifier = mod
                        .size(48.dp)
                        .testTag(QUICK_SETTINGS_DROP_DOWN)
                        .clickable { dismissQuickSettings() }
                )
            },
            captureButton = { mod ->
                endOrientation = orientation
                endCompact = isCompact
                endRecording = this.isRecording
                Box(
                    modifier = mod
                        .size(72.dp)
                        .testTag(CAPTURE_BUTTON)
                        .clickable { dismissQuickSettings() }
                )
            }
        )

        composeTestRule.setContent {
            Row(modifier = Modifier.height(800.dp)) {
                StartSideRail(
                    slots = scopedSlots,
                    modifier = Modifier.height(800.dp),
                    isCompactHeight = isCompactHeight,
                    isRecording = isRecording,
                    onDismissQuickSettings = { dismissCallCount++ }
                )
                EndSideRail(
                    slots = scopedSlots,
                    modifier = Modifier.height(800.dp),
                    isCompactHeight = isCompactHeight,
                    isRecording = isRecording,
                    onDismissQuickSettings = { dismissCallCount++ }
                )
            }
        }
        composeTestRule.waitForIdle()

        assertThat(startOrientation).isEqualTo(Orientation.Vertical)
        assertThat(endOrientation).isEqualTo(Orientation.Vertical)
        assertThat(startCompact).isFalse()
        assertThat(endCompact).isFalse()
        assertThat(startRecording).isFalse()
        assertThat(endRecording).isFalse()

        isCompactHeight = true
        isRecording = true
        composeTestRule.waitForIdle()

        assertThat(startCompact).isTrue()
        assertThat(endCompact).isTrue()
        assertThat(startRecording).isTrue()
        assertThat(endRecording).isTrue()

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN).performClick()
        composeTestRule.waitForIdle()
        assertThat(dismissCallCount).isEqualTo(1)

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).performClick()
        composeTestRule.waitForIdle()
        assertThat(dismissCallCount).isEqualTo(2)
    }
}
