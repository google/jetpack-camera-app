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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CaptureSubModeId
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureSubModeOption
import com.google.jetpackcamera.ui.uistate.capture.CaptureSubModeUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptureModeCarouselTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val defaultOption = CaptureSubModeOption(
        id = CaptureSubModeId.DEFAULT,
        labelResId = R.string.quick_settings_text_capture_mode_image_only
    )
    private val secondId = CaptureSubModeId("submode_two")
    private val secondOption = CaptureSubModeOption(
        id = secondId,
        labelResId = R.string.quick_settings_text_capture_mode_video_only
    )
    private val thirdId = CaptureSubModeId("submode_three")
    private val thirdOption = CaptureSubModeOption(
        id = thirdId,
        labelResId = R.string.quick_settings_text_capture_mode_standard
    )

    @Test
    fun carousel_whenUnavailable_doesNotRender() {
        composeTestRule.setContent {
            MaterialTheme {
                CaptureModeCarousel(
                    uiState = CaptureSubModeUiState.Unavailable,
                    onSelectSubMode = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(CAPTURE_MODE_CAROUSEL).assertDoesNotExist()
    }

    @Test
    fun carousel_whenAvailableWithTwoItems_rendersAndMarksSelected() {
        val uiState = CaptureSubModeUiState.Available(
            selectedSubMode = CaptureSubModeId.DEFAULT,
            availableSubModes = listOf(
                SingleSelectableUiState.SelectableUi(defaultOption),
                SingleSelectableUiState.SelectableUi(secondOption)
            )
        )

        composeTestRule.setContent {
            MaterialTheme {
                CaptureModeCarousel(
                    uiState = uiState,
                    onSelectSubMode = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(CAPTURE_MODE_CAROUSEL).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CaptureSubModeId.DEFAULT.carouselOptionTag)
            .assertIsDisplayed()
            .assertIsSelected()
        composeTestRule.onNodeWithTag(secondId.carouselOptionTag)
            .assertIsDisplayed()
            .assertIsNotSelected()
    }

    @Test
    fun carousel_whenAvailableWithThreeItems_clickingOptionInvokesCallback() {
        var selectedId: CaptureSubModeId? = null
        setStatefulContent(initialSelection = secondId) { selectedId = it }

        composeTestRule.onNodeWithTag(thirdId.carouselOptionTag).performClick()
        composeTestRule.waitForIdle()

        assertThat(selectedId).isEqualTo(thirdId)
        composeTestRule.onNodeWithTag(thirdId.carouselOptionTag).assertIsSelected()
        composeTestRule.onNodeWithTag(secondId.carouselOptionTag).assertIsNotSelected()
    }

    @Test
    fun carousel_whenOptionClicked_commitsSelectionOnlyAfterSettling() {
        var selectedId: CaptureSubModeId? = null
        setStatefulContent(initialSelection = CaptureSubModeId.DEFAULT) { selectedId = it }
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.onNodeWithTag(secondId.carouselOptionTag).performClick()
        composeTestRule.mainClock.advanceTimeByFrame()
        assertThat(selectedId).isNull()

        composeTestRule.mainClock.advanceTimeBy(SETTLE_TIMEOUT_MS)
        assertThat(selectedId).isEqualTo(secondId)
    }

    @Test
    fun carousel_whenSwipingHorizontally_movesThroughEnabledItems() {
        var selectedId: CaptureSubModeId? = null
        setStatefulContent(initialSelection = secondId) { selectedId = it }

        composeTestRule.onNodeWithTag(CAPTURE_MODE_CAROUSEL).performTouchInput {
            swipeLeft()
        }
        composeTestRule.waitForIdle()
        assertThat(selectedId).isEqualTo(thirdId)

        composeTestRule.onNodeWithTag(CAPTURE_MODE_CAROUSEL).performTouchInput {
            swipeRight()
        }
        composeTestRule.waitForIdle()
        assertThat(selectedId).isEqualTo(CaptureSubModeId.DEFAULT)
        composeTestRule.onNodeWithTag(CaptureSubModeId.DEFAULT.carouselOptionTag)
            .assertIsSelected()
    }

    @Test
    fun carousel_whenShortFlick_advancesOneItem() {
        var selectedId: CaptureSubModeId? = null
        setStatefulContent(initialSelection = CaptureSubModeId.DEFAULT) { selectedId = it }

        composeTestRule.onNodeWithTag(CAPTURE_MODE_CAROUSEL).performTouchInput {
            swipeLeft(startX = centerX, endX = centerX - 60f, durationMillis = 40)
        }
        composeTestRule.waitForIdle()

        assertThat(selectedId).isEqualTo(secondId)
    }

    @Test
    fun carousel_whenSelectionChangesExternally_centersNewSelection() {
        var uiState by mutableStateOf(availableState(CaptureSubModeId.DEFAULT))
        var callbackInvoked = false
        composeTestRule.setContent {
            MaterialTheme {
                CaptureModeCarousel(
                    uiState = uiState,
                    onSelectSubMode = { callbackInvoked = true }
                )
            }
        }

        uiState = availableState(thirdId)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(thirdId.carouselOptionTag).assertIsSelected()
        composeTestRule.onNodeWithTag(CaptureSubModeId.DEFAULT.carouselOptionTag)
            .assertIsNotSelected()
        assertThat(callbackInvoked).isFalse()
    }

    @Test
    fun carousel_whenSelectionRejected_snapsBackToSelectedItem() {
        var attemptedId: CaptureSubModeId? = null
        val uiState = availableState(CaptureSubModeId.DEFAULT)
        composeTestRule.setContent {
            MaterialTheme {
                CaptureModeCarousel(
                    uiState = uiState,
                    onSelectSubMode = { attemptedId = it }
                )
            }
        }

        composeTestRule.onNodeWithTag(secondId.carouselOptionTag).performClick()
        composeTestRule.waitForIdle()

        assertThat(attemptedId).isEqualTo(secondId)
        composeTestRule.onNodeWithTag(CaptureSubModeId.DEFAULT.carouselOptionTag)
            .assertIsSelected()
        composeTestRule.onNodeWithTag(secondId.carouselOptionTag)
            .assertIsNotSelected()
    }

    private fun availableState(selected: CaptureSubModeId) = CaptureSubModeUiState.Available(
        selectedSubMode = selected,
        availableSubModes = listOf(
            SingleSelectableUiState.SelectableUi(defaultOption),
            SingleSelectableUiState.SelectableUi(secondOption),
            SingleSelectableUiState.SelectableUi(thirdOption)
        )
    )

    /**
     * Sets a carousel whose ui state follows the selections it reports, mirroring how the
     * production controller feeds the selected sub-mode back into the ui state.
     */
    private fun setStatefulContent(
        initialSelection: CaptureSubModeId,
        onSelect: (CaptureSubModeId) -> Unit
    ) {
        composeTestRule.setContent {
            var uiState by remember { mutableStateOf(availableState(initialSelection)) }
            MaterialTheme {
                CaptureModeCarousel(
                    uiState = uiState,
                    onSelectSubMode = {
                        onSelect(it)
                        uiState = availableState(it)
                    }
                )
            }
        }
    }

    private companion object {
        const val SETTLE_TIMEOUT_MS = 2_000L
    }
}
