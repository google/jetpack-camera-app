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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.text.TextLayoutResult
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CaptureSubModeId
import com.google.jetpackcamera.ui.uistate.DisableRationale
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureSubModeOption
import com.google.jetpackcamera.ui.uistate.capture.CaptureSubModeUiState
import kotlinx.coroutines.delay
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

    /** A different sub-mode with the same label, and therefore the same width, as [secondOption]. */
    private val otherSecondId = CaptureSubModeId("submode_other_two")
    private val otherSecondOption = CaptureSubModeOption(
        id = otherSecondId,
        labelResId = R.string.quick_settings_text_capture_mode_video_only
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

        // The selection never follows the commit, so the row returns once the wait expires.
        composeTestRule.mainClock.advanceTimeBy(SETTLE_TIMEOUT_MS)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(CaptureSubModeId.DEFAULT.carouselOptionTag)
            .assertIsSelected()
        composeTestRule.onNodeWithTag(secondId.carouselOptionTag)
            .assertIsNotSelected()
    }

    @Test
    fun carousel_whenSelectionFollowsAfterDelay_staysOnCommittedItem() {
        val selections = mutableListOf<CaptureSubModeId>()
        composeTestRule.setContent {
            var uiState by remember { mutableStateOf(availableState(CaptureSubModeId.DEFAULT)) }
            var pendingSelection by remember { mutableStateOf<CaptureSubModeId?>(null) }
            // Delivers each commit back as the selection after a delay, like a camera round trip
            // that takes several frames.
            LaunchedEffect(pendingSelection) {
                val id = pendingSelection ?: return@LaunchedEffect
                delay(CONFIRMATION_DELAY_MS)
                uiState = availableState(id)
            }
            MaterialTheme {
                CaptureModeCarousel(
                    uiState = uiState,
                    onSelectSubMode = {
                        selections += it
                        pendingSelection = it
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag(secondId.carouselOptionTag).performClick()
        composeTestRule.waitForIdle()
        assertThat(selections).containsExactly(secondId)

        // Shortly before the selection follows, the row is still on the committed item.
        composeTestRule.mainClock.advanceTimeBy(CONFIRMATION_DELAY_MS - 50)
        composeTestRule.onNodeWithTag(secondId.carouselOptionTag).assertIsSelected()

        composeTestRule.mainClock.advanceTimeBy(SETTLE_TIMEOUT_MS)
        composeTestRule.waitForIdle()

        assertThat(selections).containsExactly(secondId)
        composeTestRule.onNodeWithTag(secondId.carouselOptionTag).assertIsSelected()
    }

    @Test
    fun carousel_whenEarlierCommitIsConfirmedAfterLaterCommit_staysOnLaterItem() {
        var uiState by mutableStateOf(availableState(CaptureSubModeId.DEFAULT))
        val selections = mutableListOf<CaptureSubModeId>()
        composeTestRule.setContent {
            MaterialTheme {
                CaptureModeCarousel(uiState = uiState, onSelectSubMode = { selections += it })
            }
        }

        composeTestRule.onNodeWithTag(secondId.carouselOptionTag).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(thirdId.carouselOptionTag).performClick()
        composeTestRule.waitForIdle()
        assertThat(selections).containsExactly(secondId, thirdId).inOrder()

        // The selection for the first commit arrives after the user has moved on.
        uiState = availableState(secondId)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(thirdId.carouselOptionTag).assertIsSelected()
        assertThat(selections).containsExactly(secondId, thirdId).inOrder()
    }

    @Test
    fun carousel_whenEarlierCommitIsConfirmedAfterLaterCommit_endsOnLaterSelection() {
        var uiState by mutableStateOf(availableState(CaptureSubModeId.DEFAULT))
        val selections = mutableListOf<CaptureSubModeId>()
        composeTestRule.setContent {
            MaterialTheme {
                CaptureModeCarousel(uiState = uiState, onSelectSubMode = { selections += it })
            }
        }

        composeTestRule.onNodeWithTag(secondId.carouselOptionTag).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(thirdId.carouselOptionTag).performClick()
        composeTestRule.waitForIdle()
        uiState = availableState(secondId)
        composeTestRule.waitForIdle()
        uiState = availableState(thirdId)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(thirdId.carouselOptionTag).assertIsSelected()
        assertThat(selections).containsExactly(secondId, thirdId).inOrder()
    }

    @Test
    fun carousel_whenSelectionChangesExternallyDuringTapAnimation_completesTap() {
        var uiState by mutableStateOf(availableState(CaptureSubModeId.DEFAULT))
        val selections = mutableListOf<CaptureSubModeId>()
        composeTestRule.setContent {
            MaterialTheme {
                CaptureModeCarousel(uiState = uiState, onSelectSubMode = { selections += it })
            }
        }
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.onNodeWithTag(thirdId.carouselOptionTag).performClick()
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeByFrame()
        uiState = availableState(secondId)
        advanceFramesUntil { selections.isNotEmpty() }

        assertThat(selections).containsExactly(thirdId)
        composeTestRule.onNodeWithTag(thirdId.carouselOptionTag).assertIsSelected()
    }

    @Test
    fun carousel_whenSelectionChangesExternallyDuringFling_completesFling() {
        var uiState by mutableStateOf(availableState(CaptureSubModeId.DEFAULT))
        val selections = mutableListOf<CaptureSubModeId>()
        composeTestRule.setContent {
            MaterialTheme {
                CaptureModeCarousel(uiState = uiState, onSelectSubMode = { selections += it })
            }
        }
        composeTestRule.mainClock.autoAdvance = false

        // The row is released short of the next item and flings the rest of the way.
        flickTowardsNextItem()
        // Change the selection after the release, while the fling is still moving the row.
        repeat(FLING_IN_PROGRESS_FRAMES) { composeTestRule.mainClock.advanceTimeByFrame() }
        assertThat(selections).isEmpty()
        uiState = availableState(thirdId)
        advanceFramesUntil { selections.isNotEmpty() }

        assertThat(selections).containsExactly(secondId)
        composeTestRule.onNodeWithTag(secondId.carouselOptionTag).assertIsSelected()
    }

    @Test
    fun carousel_whenReleasedShortOfNextItemWithVelocity_flingsToNextItem() {
        val uiState = availableState(CaptureSubModeId.DEFAULT)
        val selections = mutableListOf<CaptureSubModeId>()
        composeTestRule.setContent {
            MaterialTheme {
                CaptureModeCarousel(uiState = uiState, onSelectSubMode = { selections += it })
            }
        }
        composeTestRule.mainClock.autoAdvance = false
        flickTowardsNextItem()
        advanceFramesUntil { selections.isNotEmpty() }
        assertThat(selections).containsExactly(secondId)
    }

    @Test
    fun carousel_whenDraggedWhileFollowingExternalChange_dragTakesOver() {
        var uiState by mutableStateOf(availableState(CaptureSubModeId.DEFAULT))
        val selections = mutableListOf<CaptureSubModeId>()
        composeTestRule.setContent {
            MaterialTheme {
                CaptureModeCarousel(uiState = uiState, onSelectSubMode = { selections += it })
            }
        }
        composeTestRule.mainClock.autoAdvance = false

        uiState = availableState(thirdId)
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeByFrame()
        // Dragging the row stops it from following the selection.
        composeTestRule.onNodeWithTag(CAPTURE_MODE_CAROUSEL).performTouchInput {
            down(center)
            moveBy(Offset(viewConfiguration.touchSlop + DRAG_DISTANCE_PX, 0f))
        }
        composeTestRule.mainClock.advanceTimeBy(SETTLE_TIMEOUT_MS)
        composeTestRule.onNodeWithTag(thirdId.carouselOptionTag).assertIsNotSelected()

        // Once released, the row settles and still follows later selection changes.
        composeTestRule.onNodeWithTag(CAPTURE_MODE_CAROUSEL).performTouchInput { up() }
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(SETTLE_TIMEOUT_MS)
        uiState = availableState(CaptureSubModeId.DEFAULT)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(CaptureSubModeId.DEFAULT.carouselOptionTag).assertIsSelected()
    }

    @Test
    fun carousel_whenConfirmationTimesOutDuringTapAnimation_completesTap() {
        val selections = mutableListOf<CaptureSubModeId>()
        composeTestRule.setContent {
            MaterialTheme {
                CaptureModeCarousel(
                    uiState = availableState(CaptureSubModeId.DEFAULT),
                    onSelectSubMode = { selections += it }
                )
            }
        }
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.onNodeWithTag(secondId.carouselOptionTag).performClick()
        advanceFramesUntil { selections.isNotEmpty() }
        val firstCommitTimeMs = composeTestRule.mainClock.currentTime
        // Tap the next item shortly before the first commit's confirmation wait expires, so that
        // the wait expires while the tap animation is running.
        composeTestRule.mainClock.advanceTimeBy(CONFIRMATION_TIMEOUT_MS - 3 * FRAME_MS)
        composeTestRule.onNodeWithTag(thirdId.carouselOptionTag).performClick()
        composeTestRule.mainClock.advanceTimeByFrame()
        assertThat(composeTestRule.mainClock.currentTime - firstCommitTimeMs)
            .isLessThan(CONFIRMATION_TIMEOUT_MS)
        advanceFramesUntil { selections.size == 2 }

        assertThat(selections).containsExactly(secondId, thirdId).inOrder()
        composeTestRule.onNodeWithTag(thirdId.carouselOptionTag).assertIsSelected()
    }

    @Test
    fun carousel_whenTouchedWhileSettling_cancelsSettleWithoutCommitting() {
        val selections = mutableListOf<CaptureSubModeId>()
        setStatefulContent(initialSelection = CaptureSubModeId.DEFAULT) { selections += it }
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.onNodeWithTag(thirdId.carouselOptionTag).performClick()
        composeTestRule.mainClock.advanceTimeByFrame()
        // A touch while settling starts a drag immediately, which takes over from the settle.
        composeTestRule.onNodeWithTag(CAPTURE_MODE_CAROUSEL).performTouchInput { down(center) }
        composeTestRule.mainClock.advanceTimeBy(SETTLE_TIMEOUT_MS)

        assertThat(selections).isEmpty()

        composeTestRule.onNodeWithTag(CAPTURE_MODE_CAROUSEL).performTouchInput { up() }
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        assertThat(selections).doesNotContain(thirdId)
    }

    @Test
    fun carousel_whenItemsChangeWhileSettling_cancelsSettleAndCentersSelection() {
        var uiState by mutableStateOf(availableState(CaptureSubModeId.DEFAULT))
        val selections = mutableListOf<CaptureSubModeId>()
        composeTestRule.setContent {
            MaterialTheme {
                CaptureModeCarousel(uiState = uiState, onSelectSubMode = { selections += it })
            }
        }
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.onNodeWithTag(thirdId.carouselOptionTag).performClick()
        composeTestRule.mainClock.advanceTimeByFrame()
        uiState = twoItemState(CaptureSubModeId.DEFAULT)
        composeTestRule.mainClock.advanceTimeBy(SETTLE_TIMEOUT_MS)

        assertThat(selections).isEmpty()
        composeTestRule.onNodeWithTag(thirdId.carouselOptionTag).assertDoesNotExist()
        composeTestRule.onNodeWithTag(CaptureSubModeId.DEFAULT.carouselOptionTag)
            .assertIsSelected()
    }

    @Test
    fun carousel_whenItemsChangeDuringDrag_endsDragAndStaysDraggable() {
        var uiState by mutableStateOf(availableState(CaptureSubModeId.DEFAULT))
        val selections = mutableListOf<CaptureSubModeId>()
        composeTestRule.setContent {
            MaterialTheme {
                CaptureModeCarousel(
                    uiState = uiState,
                    onSelectSubMode = {
                        selections += it
                        uiState = twoItemState(it)
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag(CAPTURE_MODE_CAROUSEL).performTouchInput {
            down(center)
            moveBy(Offset(-(viewConfiguration.touchSlop + DRAG_DISTANCE_PX), 0f))
        }
        uiState = twoItemState(CaptureSubModeId.DEFAULT)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(CAPTURE_MODE_CAROUSEL).performTouchInput {
            moveBy(Offset(-DRAG_DISTANCE_PX, 0f))
            up()
        }
        composeTestRule.waitForIdle()

        // The change to the items ended the drag, so the rest of the gesture selects nothing.
        assertThat(selections).isEmpty()
        composeTestRule.onNodeWithTag(CaptureSubModeId.DEFAULT.carouselOptionTag)
            .assertIsSelected()

        // A new gesture still drags the row.
        composeTestRule.onNodeWithTag(CAPTURE_MODE_CAROUSEL).performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        assertThat(selections).containsExactly(secondId)
    }

    @Test
    fun carousel_whenItemDisabledDuringDrag_continuesDrag() {
        var uiState by mutableStateOf(availableState(CaptureSubModeId.DEFAULT))
        val selections = mutableListOf<CaptureSubModeId>()
        composeTestRule.setContent {
            MaterialTheme {
                CaptureModeCarousel(
                    uiState = uiState,
                    onSelectSubMode = {
                        selections += it
                        uiState = stateWithDisabledThirdItem(it)
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag(CAPTURE_MODE_CAROUSEL).performTouchInput {
            down(center)
            moveBy(Offset(-(viewConfiguration.touchSlop + DRAG_DISTANCE_PX), 0f))
        }
        // The same items, with the third item now disabled.
        uiState = stateWithDisabledThirdItem(CaptureSubModeId.DEFAULT)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(CAPTURE_MODE_CAROUSEL).performTouchInput {
            moveBy(Offset(-DRAG_DISTANCE_PX, 0f))
            up()
        }
        composeTestRule.waitForIdle()

        // The drag continued, so it selects the item it was dragged to.
        assertThat(selections).containsExactly(secondId)
        composeTestRule.onNodeWithTag(secondId.carouselOptionTag).assertIsSelected()
    }

    @Test
    fun carousel_whenItemDisabledWhileSettling_settlesOnTappedItem() {
        var uiState by mutableStateOf(availableState(CaptureSubModeId.DEFAULT))
        val selections = mutableListOf<CaptureSubModeId>()
        composeTestRule.setContent {
            MaterialTheme {
                CaptureModeCarousel(
                    uiState = uiState,
                    onSelectSubMode = {
                        selections += it
                        uiState = stateWithDisabledSecondItem(it)
                    }
                )
            }
        }
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.onNodeWithTag(thirdId.carouselOptionTag).performClick()
        composeTestRule.mainClock.advanceTimeByFrame()
        // The same items, with an item between the start and the tapped item now disabled.
        uiState = stateWithDisabledSecondItem(CaptureSubModeId.DEFAULT)
        composeTestRule.mainClock.advanceTimeBy(SETTLE_TIMEOUT_MS)

        assertThat(selections).containsExactly(thirdId)
        composeTestRule.onNodeWithTag(thirdId.carouselOptionTag).assertIsSelected()
    }

    @Test
    fun carousel_whenItemsChangeToDifferentSubModes_centersSelectionWithoutCommitting() {
        // The second item of each list is at the same position, so a row that kept its offset
        // would come to rest on the new list's second item.
        var uiState by mutableStateOf(twoItemState(secondId))
        val selections = mutableListOf<CaptureSubModeId>()
        composeTestRule.setContent {
            MaterialTheme {
                CaptureModeCarousel(uiState = uiState, onSelectSubMode = { selections += it })
            }
        }
        composeTestRule.waitForIdle()

        uiState = CaptureSubModeUiState.Available(
            selectedSubMode = CaptureSubModeId.DEFAULT,
            availableSubModes = listOf(
                SingleSelectableUiState.SelectableUi(defaultOption),
                SingleSelectableUiState.SelectableUi(otherSecondOption)
            )
        )
        composeTestRule.mainClock.advanceTimeBy(SETTLE_TIMEOUT_MS)
        composeTestRule.waitForIdle()

        assertThat(selections).isEmpty()
        composeTestRule.onNodeWithTag(CaptureSubModeId.DEFAULT.carouselOptionTag)
            .assertIsSelected()
        composeTestRule.onNodeWithTag(otherSecondId.carouselOptionTag).assertIsNotSelected()
    }

    @Test
    fun carousel_whenFlickedTowardsDisabledItem_skipsToNextEnabledItem() {
        val selections = mutableListOf<CaptureSubModeId>()
        composeTestRule.setContent {
            var uiState by remember {
                mutableStateOf(stateWithDisabledSecondItem(CaptureSubModeId.DEFAULT))
            }
            MaterialTheme {
                CaptureModeCarousel(
                    uiState = uiState,
                    onSelectSubMode = {
                        selections += it
                        uiState = stateWithDisabledSecondItem(it)
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag(CAPTURE_MODE_CAROUSEL).performTouchInput {
            swipeLeft(startX = centerX, endX = centerX - 60f, durationMillis = 40)
        }
        composeTestRule.waitForIdle()

        assertThat(selections).containsExactly(thirdId)
        composeTestRule.onNodeWithTag(thirdId.carouselOptionTag).assertIsSelected()
    }

    @Test
    fun carousel_whenAnimationsDisabled_commitsTappedItemWithoutAnimating() {
        var selectedId: CaptureSubModeId? = null
        composeTestRule.setContent {
            var uiState by remember { mutableStateOf(availableState(secondId)) }
            MaterialTheme {
                CompositionLocalProvider(LocalDisableAnimations provides true) {
                    CaptureModeCarousel(
                        uiState = uiState,
                        onSelectSubMode = {
                            selectedId = it
                            uiState = availableState(it)
                        }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.onNodeWithTag(thirdId.carouselOptionTag).performClick()
        // Far shorter than the settle spring, which takes several hundred milliseconds.
        composeTestRule.mainClock.advanceTimeBy(SNAP_COMMIT_WINDOW_MS)

        assertThat(selectedId).isEqualTo(thirdId)
    }

    @Test
    fun carousel_whenOverlapWithViewfinderChanges_togglesLabelShadow() {
        val viewfinderBounds = mutableStateOf(Rect.Zero)
        composeTestRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalOverlapTargetBounds provides viewfinderBounds) {
                    OverlapAwareStyleProvider {
                        CaptureModeCarousel(
                            uiState = availableState(CaptureSubModeId.DEFAULT),
                            onSelectSubMode = {}
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        assertThat(labelShadow(secondId)).isNull()

        viewfinderBounds.value = Rect(0f, 0f, LARGE_BOUNDS_PX, LARGE_BOUNDS_PX)
        composeTestRule.waitForIdle()

        assertThat(labelShadow(secondId)).isNotNull()
        composeTestRule.onNodeWithTag(CaptureSubModeId.DEFAULT.carouselOptionTag)
            .assertIsSelected()

        viewfinderBounds.value = Rect.Zero
        composeTestRule.waitForIdle()

        assertThat(labelShadow(secondId)).isNull()
    }

    @Test
    fun carousel_whenDrawnToCanvas_drawsCenterHighlightPillAcrossPositions() {
        var hostView: View? = null
        var uiState by mutableStateOf(availableState(CaptureSubModeId.DEFAULT))
        composeTestRule.setContent {
            hostView = LocalView.current
            MaterialTheme {
                CaptureModeCarousel(
                    uiState = uiState,
                    onSelectSubMode = { uiState = availableState(it) }
                )
            }
        }
        composeTestRule.waitForIdle()

        val bitmap = Bitmap.createBitmap(400, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw at first item (position <= centers.first())
        composeTestRule.runOnIdle { hostView!!.draw(canvas) }

        // Drag partially between first and second item and draw (interpolated pillWidth)
        composeTestRule.onNodeWithTag(CAPTURE_MODE_CAROUSEL).performTouchInput {
            down(center)
            moveBy(Offset(-(viewConfiguration.touchSlop + DRAG_DISTANCE_PX), 0f))
        }
        composeTestRule.runOnIdle { hostView!!.draw(canvas) }
        composeTestRule.onNodeWithTag(CAPTURE_MODE_CAROUSEL).performTouchInput { up() }
        composeTestRule.waitForIdle()

        // Draw at last item (position >= centers.last())
        uiState = availableState(thirdId)
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { hostView!!.draw(canvas) }

        assertThat(hostView).isNotNull()
    }

    /** Drags the row a short way towards the next item and releases it with a flinging velocity. */
    private fun flickTowardsNextItem() {
        composeTestRule.onNodeWithTag(CAPTURE_MODE_CAROUSEL).performTouchInput {
            down(center)
            repeat(FLICK_MOVES) {
                moveBy(Offset(-FLICK_STEP_PX, 0f), delayMillis = FLICK_STEP_MS)
            }
            up()
        }
    }

    /**
     * Advances the main clock one frame at a time until [condition] holds, for at most
     * [SETTLE_TIMEOUT_MS].
     */
    private fun advanceFramesUntil(condition: () -> Boolean) {
        val deadline = composeTestRule.mainClock.currentTime + SETTLE_TIMEOUT_MS
        while (!condition() && composeTestRule.mainClock.currentTime < deadline) {
            composeTestRule.mainClock.advanceTimeByFrame()
        }
        assertThat(condition()).isTrue()
    }

    /** Returns the shadow of the interactive label for [id]. */
    private fun labelShadow(id: CaptureSubModeId): Shadow? {
        val textNode = composeTestRule
            .onNode(hasParent(hasTestTag(id.carouselOptionTag)), useUnmergedTree = true)
            .fetchSemanticsNode()
        val layoutResults = mutableListOf<TextLayoutResult>()
        textNode.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layoutResults)
        return layoutResults.single().layoutInput.style.shadow
    }

    private fun availableState(selected: CaptureSubModeId) = CaptureSubModeUiState.Available(
        selectedSubMode = selected,
        availableSubModes = listOf(
            SingleSelectableUiState.SelectableUi(defaultOption),
            SingleSelectableUiState.SelectableUi(secondOption),
            SingleSelectableUiState.SelectableUi(thirdOption)
        )
    )

    private fun twoItemState(selected: CaptureSubModeId) = CaptureSubModeUiState.Available(
        selectedSubMode = selected,
        availableSubModes = listOf(
            SingleSelectableUiState.SelectableUi(defaultOption),
            SingleSelectableUiState.SelectableUi(secondOption)
        )
    )

    private fun stateWithDisabledSecondItem(selected: CaptureSubModeId) =
        CaptureSubModeUiState.Available(
            selectedSubMode = selected,
            availableSubModes = listOf(
                SingleSelectableUiState.SelectableUi(defaultOption),
                SingleSelectableUiState.Disabled(
                    secondOption,
                    object : DisableRationale {
                        override val reasonTextResId: Int = 0
                    }
                ),
                SingleSelectableUiState.SelectableUi(thirdOption)
            )
        )

    private fun stateWithDisabledThirdItem(selected: CaptureSubModeId) =
        CaptureSubModeUiState.Available(
            selectedSubMode = selected,
            availableSubModes = listOf(
                SingleSelectableUiState.SelectableUi(defaultOption),
                SingleSelectableUiState.SelectableUi(secondOption),
                SingleSelectableUiState.Disabled(
                    thirdOption,
                    object : DisableRationale {
                        override val reasonTextResId: Int = 0
                    }
                )
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
        const val DRAG_DISTANCE_PX = 40f
        const val CONFIRMATION_DELAY_MS = 300L
        const val LARGE_BOUNDS_PX = 10_000f
        const val SNAP_COMMIT_WINDOW_MS = 50L

        /** Matches the carousel's wait for the selection to follow a commit. */
        const val CONFIRMATION_TIMEOUT_MS = 500L
        const val FRAME_MS = 16L
        const val FLING_IN_PROGRESS_FRAMES = 2
        const val FLICK_MOVES = 3
        const val FLICK_STEP_PX = 10f
        const val FLICK_STEP_MS = 8L
    }
}
