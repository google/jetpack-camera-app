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
package com.google.jetpackcamera.ui.components.capture.quicksettings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FLASH_OPTION_ON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_BOTTOM_SHEET
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_ASPECT_RATIO
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_CAPTURE_MODE
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_FLASH
import com.google.jetpackcamera.ui.components.capture.SETTINGS_BUTTON
import com.google.jetpackcamera.ui.controller.testing.FakeQuickSettingsController
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuickSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeQuickSettingsController = FakeQuickSettingsController()

    private val sampleAvailableUiState = QuickSettingsUiState.Available(
        aspectRatioUiState = AspectRatioUiState.Available(
            selectedAspectRatio = AspectRatio.NINE_SIXTEEN,
            availableAspectRatios = listOf(
                SingleSelectableUiState.SelectableUi(AspectRatio.NINE_SIXTEEN),
                SingleSelectableUiState.SelectableUi(AspectRatio.THREE_FOUR),
                SingleSelectableUiState.SelectableUi(AspectRatio.ONE_ONE)
            )
        ),
        captureModeUiState = CaptureModeUiState.Available(
            selectedCaptureMode = CaptureMode.STANDARD,
            availableCaptureModes = listOf(
                SingleSelectableUiState.SelectableUi(CaptureMode.STANDARD),
                SingleSelectableUiState.SelectableUi(CaptureMode.VIDEO_ONLY),
                SingleSelectableUiState.SelectableUi(CaptureMode.IMAGE_ONLY)
            )
        ),
        flashModeUiState = FlashModeUiState.Available(
            selectedFlashMode = FlashMode.OFF,
            availableFlashModes = listOf(
                SingleSelectableUiState.SelectableUi(FlashMode.OFF),
                SingleSelectableUiState.SelectableUi(FlashMode.ON),
                SingleSelectableUiState.SelectableUi(FlashMode.AUTO)
            ),
            isLowLightBoostActive = false
        ),
        flipLensUiState = FlipLensUiState.Available(
            selectedLensFacing = LensFacing.BACK,
            availableLensFacings = listOf(
                SingleSelectableUiState.SelectableUi(LensFacing.BACK),
                SingleSelectableUiState.SelectableUi(LensFacing.FRONT)
            )
        ),
        hdrUiState = HdrUiState.Unavailable
    )

    @Test
    fun quickSettingsScaffoldContent_unavailableState_doesNotRender() {
        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsScaffoldContent(
                    quickSettingsUiState = QuickSettingsUiState.Unavailable,
                    onNavigateToSettings = {},
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertDoesNotExist()
    }

    @Test
    fun quickSettingsScaffoldContent_availableState_rendersSettings() {
        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsScaffoldContent(
                    quickSettingsUiState = sampleAvailableUiState,
                    onNavigateToSettings = {},
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ROW_QUICK_SETTINGS_FLASH).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ROW_QUICK_SETTINGS_CAPTURE_MODE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ROW_QUICK_SETTINGS_ASPECT_RATIO).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SETTINGS_BUTTON).assertIsDisplayed()
    }

    @Test
    fun quickSettingsScaffoldContent_flashSelection_callsController() {
        var setFlashCalledWith: FlashMode? = null
        fakeQuickSettingsController.setFlashAction = { setFlashCalledWith = it }

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsScaffoldContent(
                    quickSettingsUiState = sampleAvailableUiState,
                    onNavigateToSettings = {},
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_ON).performClick()
        assertThat(setFlashCalledWith).isEqualTo(FlashMode.ON)
    }

    @Test
    fun quickSettingsScaffoldContent_moreSettingsButton_triggersCallback() {
        var onNavigateToSettingsCalled = false

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsScaffoldContent(
                    quickSettingsUiState = sampleAvailableUiState,
                    onNavigateToSettings = { onNavigateToSettingsCalled = true },
                    quickSettingsController = fakeQuickSettingsController,
                    showMoreSettingsButton = true
                )
            }
        }

        composeTestRule.onNodeWithTag(SETTINGS_BUTTON).performClick()
        assertThat(onNavigateToSettingsCalled).isTrue()
    }

    @Test
    fun quickSettingsScaffoldContent_hideMoreSettingsButton_doesNotRenderButton() {
        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsScaffoldContent(
                    quickSettingsUiState = sampleAvailableUiState,
                    onNavigateToSettings = {},
                    quickSettingsController = fakeQuickSettingsController,
                    showMoreSettingsButton = false
                )
            }
        }

        composeTestRule.onNodeWithTag(SETTINGS_BUTTON).assertDoesNotExist()
    }
}
