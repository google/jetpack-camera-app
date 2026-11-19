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

import android.content.Context
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DEFAULT_HDR_DYNAMIC_RANGE
import com.google.jetpackcamera.model.DEFAULT_HDR_IMAGE_OUTPUT
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_CAPTURE_MODE_OPTION_STANDARD
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_CAPTURE_MODE_OPTION_VIDEO_ONLY
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FLASH_OPTION_AUTO
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FLASH_OPTION_OFF
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FLASH_OPTION_ON
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_HDR_OPTION_ON
import com.google.jetpackcamera.ui.components.capture.CaptureScaffoldDefaults
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_BOTTOM_SHEET
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_DRAG_HANDLE
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_3_4_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_SCRIM
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_SIDE_SHEET_TAG
import com.google.jetpackcamera.ui.components.capture.R
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_ASPECT_RATIO
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_CAPTURE_MODE
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_FLASH
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_HDR
import com.google.jetpackcamera.ui.components.capture.SETTINGS_BUTTON
import com.google.jetpackcamera.ui.controller.testing.FakeQuickSettingsController
import com.google.jetpackcamera.ui.controller.testing.sampleQuickSettingsUiState
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1280dp-h900dp")
class QuickSettingsSideSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeQuickSettingsController = FakeQuickSettingsController()

    private val sampleAvailableUiState = sampleQuickSettingsUiState()

    @Test
    fun quickSettingsSideSheet_whenClosed_doesNotRenderAndDoesNotInterceptBack() {
        var dismissCalled = false
        var outerBackHandled = false
        lateinit var backDispatcher: OnBackPressedDispatcher

        composeTestRule.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            MaterialTheme {
                BackHandler(enabled = true) { outerBackHandled = true }
                QuickSettingsSideSheet(
                    isOpen = false,
                    onDismissRequest = { dismissCalled = true },
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM).assertDoesNotExist()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertDoesNotExist()

        composeTestRule.runOnUiThread {
            backDispatcher.onBackPressed()
        }

        assertThat(dismissCalled).isFalse()
        assertThat(outerBackHandled).isTrue()
    }

    @Test
    fun quickSettingsSideSheet_whenUnavailable_doesNotRenderAndDoesNotInterceptBack() {
        var dismissCalled = false
        var outerBackHandled = false
        lateinit var backDispatcher: OnBackPressedDispatcher

        composeTestRule.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            MaterialTheme {
                BackHandler(enabled = true) { outerBackHandled = true }
                QuickSettingsSideSheet(
                    isOpen = true,
                    onDismissRequest = { dismissCalled = true },
                    quickSettingsUiState = QuickSettingsUiState.Unavailable,
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM).assertDoesNotExist()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertDoesNotExist()

        composeTestRule.runOnUiThread {
            backDispatcher.onBackPressed()
        }

        assertThat(dismissCalled).isFalse()
        assertThat(outerBackHandled).isTrue()
    }

    @Test
    fun quickSettingsSideSheet_whenOpen_rendersInHierarchyAndCapsSheetWidth() {
        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = true,
                    onDismissRequest = {},
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM)
            .assertIsDisplayed()
            .assert(!isDialog())
            .assert(!isPopup())

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .assertIsDisplayed()
            .assert(!isDialog())
            .assert(!isPopup())

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET)
            .assertIsDisplayed()
            .assert(!isDialog())
            .assert(!isPopup())

        composeTestRule.onNodeWithTag(ROW_QUICK_SETTINGS_FLASH).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ROW_QUICK_SETTINGS_CAPTURE_MODE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ROW_QUICK_SETTINGS_ASPECT_RATIO).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ROW_QUICK_SETTINGS_HDR).assertIsDisplayed()

        val sheetBounds = composeTestRule
            .onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .getUnclippedBoundsInRoot()
        assertThat(sheetBounds.width).isAtMost(470.dp)
        assertThat(sheetBounds.height).isEqualTo(570.dp)
    }

    @Test
    fun quickSettingsSideSheet_outsideTap_dismissesWithoutPassingThroughToUnderlyingControl() {
        var dismissCount = 0
        var underlyingClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { underlyingClicked = true }
                    )
                    QuickSettingsSideSheet(
                        isOpen = true,
                        onDismissRequest = { dismissCount++ },
                        quickSettingsUiState = sampleAvailableUiState,
                        quickSettingsController = fakeQuickSettingsController
                    )
                }
            }
        }

        composeTestRule.onRoot().performTouchInput { click(Offset(right - 4f, 4f)) }

        assertThat(dismissCount).isEqualTo(1)
        assertThat(underlyingClicked).isFalse()
    }

    @Test
    fun quickSettingsSideSheet_scrim_exposesButtonSemanticsAndDismissesOnClick() {
        var dismissCount = 0
        val dismissLabel = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.quick_settings_btn_close_expanded_settings_description)

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = true,
                    onDismissRequest = { dismissCount++ },
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM)
            .assertIsDisplayed()
            .assertContentDescriptionEquals(dismissLabel)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, 1f))
            .performClick()

        assertThat(dismissCount).isEqualTo(1)
    }

    @Test
    fun quickSettingsSideSheet_sheetBackgroundTap_doesNotDismissOrPassThroughToScrim() {
        var dismissCount = 0
        var underlyingClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { underlyingClicked = true }
                    )
                    QuickSettingsSideSheet(
                        isOpen = true,
                        onDismissRequest = { dismissCount++ },
                        quickSettingsUiState = sampleAvailableUiState,
                        quickSettingsController = fakeQuickSettingsController
                    )
                }
            }
        }

        // Click inside the top and bottom vertical padding of the sheet surface (horizontally
        // centered so the touch clears the sheet's 28.dp corner radius).
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .performTouchInput {
                click(Offset(centerX, 8f))
                click(Offset(centerX, bottom - 8f))
            }

        assertThat(dismissCount).isEqualTo(0)
        assertThat(underlyingClicked).isFalse()
    }

    @Test
    fun quickSettingsSideSheet_settingSelections_invokeControllerCallbacksWithoutDismissing() {
        var selectedFlash: FlashMode? = null
        var selectedCaptureMode: CaptureMode? = null
        var selectedAspectRatio: AspectRatio? = null
        var selectedDynamicRange: DynamicRange? = null
        var selectedImageFormat: ImageOutputFormat? = null
        var dismissCount = 0

        fakeQuickSettingsController.setFlashAction = { selectedFlash = it }
        fakeQuickSettingsController.setCaptureModeAction = { selectedCaptureMode = it }
        fakeQuickSettingsController.setAspectRatioAction = { selectedAspectRatio = it }
        fakeQuickSettingsController.setDynamicRangeAction = { selectedDynamicRange = it }
        fakeQuickSettingsController.setImageFormatAction = { selectedImageFormat = it }

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = true,
                    onDismissRequest = { dismissCount++ },
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_ON).performClick()
        assertThat(selectedFlash).isEqualTo(FlashMode.ON)

        composeTestRule.onNodeWithTag(BTN_QUICK_SETTINGS_CAPTURE_MODE_OPTION_VIDEO_ONLY)
            .performClick()
        assertThat(selectedCaptureMode).isEqualTo(CaptureMode.VIDEO_ONLY)

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_RATIO_3_4_BUTTON).performClick()
        assertThat(selectedAspectRatio).isEqualTo(AspectRatio.THREE_FOUR)

        composeTestRule.onNodeWithTag(BTN_QUICK_SETTINGS_HDR_OPTION_ON).performClick()
        assertThat(selectedDynamicRange).isEqualTo(DEFAULT_HDR_DYNAMIC_RANGE)
        assertThat(selectedImageFormat).isEqualTo(DEFAULT_HDR_IMAGE_OUTPUT)

        assertThat(dismissCount).isEqualTo(0)
    }

    @Test
    fun quickSettingsSideSheet_backPress_invokesOnDismissRequest() {
        var dismissCount = 0
        lateinit var backDispatcher: OnBackPressedDispatcher

        composeTestRule.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = true,
                    onDismissRequest = { dismissCount++ },
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.runOnUiThread {
            backDispatcher.onBackPressed()
        }

        // The predictive back handler completes on its collecting coroutine.
        composeTestRule.runOnIdle { assertThat(dismissCount).isEqualTo(1) }
    }

    @Test
    fun quickSettingsSideSheet_showMoreSettingsButton_defaultsToFalse() {
        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = true,
                    onDismissRequest = {},
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(SETTINGS_BUTTON).assertDoesNotExist()
    }

    @Test
    fun quickSettingsSideSheet_showMoreSettingsButtonTrue_displaysButtonAndInvokesCallback() {
        var navigatedToSettings = false

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = true,
                    onDismissRequest = {},
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController,
                    onNavigateToSettings = { navigatedToSettings = true },
                    showMoreSettingsButton = true
                )
            }
        }

        composeTestRule.onNodeWithTag(SETTINGS_BUTTON)
            .assertIsDisplayed()
            .performClick()

        assertThat(navigatedToSettings).isTrue()
    }

    @Test
    @Config(qualifiers = "w840dp-h360dp")
    fun quickSettingsSideSheet_compactShortViewport_scrollsToBottomMoreSettingsButton() {
        var navigatedToSettings = false

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = true,
                    onDismissRequest = {},
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController,
                    onNavigateToSettings = { navigatedToSettings = true },
                    showMoreSettingsButton = true
                )
            }
        }

        composeTestRule.onNodeWithTag(SETTINGS_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertThat(navigatedToSettings).isTrue()
    }

    @Test
    fun quickSettingsSideSheet_slotOverload_clearsRailPaddingAndCapsMaxSheetWidth() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    QuickSettingsSideSheet(
                        isOpen = true,
                        onDismissRequest = {},
                        contentAlignment = Alignment.BottomStart,
                        sheetPadding = PaddingValues(
                            start = 92.dp,
                            end = 104.dp,
                            top = 24.dp,
                            bottom = 28.dp
                        ),
                        maxSheetWidth = 380.dp,
                        sheetContainerDirection = LayoutDirection.Ltr
                    ) {
                        CaptureScaffoldDefaults.QuickSettings(
                            uiState = sampleAvailableUiState,
                            controller = fakeQuickSettingsController,
                            onNavigateToSettings = {},
                            showMoreSettingsButton = true
                        )
                    }
                }
            }
        }

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val sheetBounds = composeTestRule
            .onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .getUnclippedBoundsInRoot()
        val flashOffBounds = composeTestRule
            .onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_OFF)
            .getUnclippedBoundsInRoot()
        val flashAutoBounds = composeTestRule
            .onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_AUTO)
            .getUnclippedBoundsInRoot()

        assertThat(sheetBounds.left).isEqualTo(92.dp)
        assertThat(sheetBounds.width).isAtMost(380.dp)
        assertThat(sheetBounds.bottom).isEqualTo(rootBounds.bottom - 28.dp)
        // Sheet interior still respects ambient RTL direction even when container is pinned LTR.
        assertThat(flashOffBounds.left).isGreaterThan(flashAutoBounds.left)
    }

    @Test
    fun quickSettingsSideSheet_rtlLayoutDirection_keepsSheetPlacementStaticAndMirrorsRows() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        val flashTitleText = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.quick_settings_title_flash_mode)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MaterialTheme {
                    QuickSettingsSideSheet(
                        isOpen = true,
                        onDismissRequest = {},
                        quickSettingsUiState = sampleAvailableUiState,
                        quickSettingsController = fakeQuickSettingsController
                    )
                }
            }
        }

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val sheetBoundsLtr = composeTestRule
            .onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .getUnclippedBoundsInRoot()
        val flashTitleBoundsLtr = composeTestRule
            .onNodeWithText(flashTitleText)
            .getUnclippedBoundsInRoot()
        val flashOffBoundsLtr = composeTestRule
            .onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_OFF)
            .getUnclippedBoundsInRoot()
        val flashAutoBoundsLtr = composeTestRule
            .onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_AUTO)
            .getUnclippedBoundsInRoot()

        assertThat(sheetBoundsLtr.left).isEqualTo(0.dp)
        assertThat(sheetBoundsLtr.right).isLessThan(rootBounds.width / 2)
        // Vertically centered along the attached edge by default.
        assertThat((sheetBoundsLtr.top + sheetBoundsLtr.bottom) / 2)
            .isEqualTo((rootBounds.top + rootBounds.bottom) / 2)
        assertThat(flashTitleBoundsLtr.left).isLessThan(flashOffBoundsLtr.left)
        assertThat(flashOffBoundsLtr.left).isLessThan(flashAutoBoundsLtr.left)

        composeTestRule.runOnIdle {
            layoutDirection = LayoutDirection.Rtl
        }
        composeTestRule.waitForIdle()

        val sheetBoundsRtl = composeTestRule
            .onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .getUnclippedBoundsInRoot()
        val flashTitleBoundsRtl = composeTestRule
            .onNodeWithText(flashTitleText)
            .getUnclippedBoundsInRoot()
        val flashOffBoundsRtl = composeTestRule
            .onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_OFF)
            .getUnclippedBoundsInRoot()
        val flashAutoBoundsRtl = composeTestRule
            .onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_AUTO)
            .getUnclippedBoundsInRoot()

        assertThat(sheetBoundsRtl.left).isEqualTo(0.dp)
        assertThat(sheetBoundsRtl.right).isLessThan(rootBounds.width / 2)
        assertThat(sheetBoundsRtl.top).isEqualTo(sheetBoundsLtr.top)
        assertThat(flashTitleBoundsRtl.left).isGreaterThan(flashOffBoundsRtl.left)
        assertThat(flashOffBoundsRtl.left).isGreaterThan(flashAutoBoundsRtl.left)
    }

    @Test
    fun quickSettingsSideSheet_customContentAlignment_positionsSheetAccordingly() {
        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = true,
                    onDismissRequest = {},
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController,
                    contentAlignment = Alignment.TopEnd,
                    minSheetHeight = 200.dp
                )
            }
        }

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val sheetBounds = composeTestRule
            .onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .getUnclippedBoundsInRoot()

        assertThat(sheetBounds.left).isGreaterThan(rootBounds.width / 2)
        assertThat(sheetBounds.right).isEqualTo(rootBounds.right)
        assertThat(sheetBounds.top).isEqualTo(16.dp)
        assertThat(sheetBounds.bottom).isLessThan(rootBounds.height / 2)
    }

    @Test
    fun quickSettingsSideSheet_semantics_exposesPaneTitleHeadingAndSheetFirstTraversal() {
        var captureMode by mutableStateOf(CaptureMode.STANDARD)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val standardTitle = context.getString(
            R.string.quick_settings_title_photo_and_video_settings
        )
        val photoTitle = context.getString(R.string.quick_settings_title_photo_settings)
        val videoTitle = context.getString(R.string.quick_settings_title_video_settings)

        composeTestRule.setContent {
            val uiState = sampleAvailableUiState.copy(
                captureModeUiState = CaptureModeUiState.Available(
                    selectedCaptureMode = captureMode,
                    availableCaptureModes = listOf(
                        SingleSelectableUiState.SelectableUi(CaptureMode.STANDARD),
                        SingleSelectableUiState.SelectableUi(CaptureMode.VIDEO_ONLY),
                        SingleSelectableUiState.SelectableUi(CaptureMode.IMAGE_ONLY)
                    )
                )
            )
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = true,
                    onDismissRequest = {},
                    quickSettingsUiState = uiState,
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .onParent()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, standardTitle))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, 0f))

        composeTestRule.onNodeWithText(standardTitle)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

        composeTestRule.runOnIdle { captureMode = CaptureMode.IMAGE_ONLY }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, photoTitle))
        composeTestRule.onNodeWithText(photoTitle)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

        composeTestRule.runOnIdle { captureMode = CaptureMode.VIDEO_ONLY }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, videoTitle))
        composeTestRule.onNodeWithText(videoTitle)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun quickSettingsSideSheet_escapeKey_dismissesOnKeyUp() {
        var dismissCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = true,
                    onDismissRequest = { dismissCount++ },
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .performKeyInput { keyDown(Key.Escape) }
        assertThat(dismissCount).isEqualTo(0)

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .performKeyInput { keyUp(Key.Escape) }
        assertThat(dismissCount).isEqualTo(1)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun quickSettingsSideSheet_emptyRows_requestsFocusCleanlyAndEscapeDismisses() {
        var isOpen by mutableStateOf(true)
        val emptyRowsState = QuickSettingsUiState.Available(
            aspectRatioUiState = AspectRatioUiState.Unavailable,
            captureModeUiState = CaptureModeUiState.Unavailable,
            flashModeUiState = FlashModeUiState.Unavailable,
            flipLensUiState = FlipLensUiState.Unavailable,
            hdrUiState = HdrUiState.Unavailable
        )

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = isOpen,
                    onDismissRequest = { isOpen = false },
                    quickSettingsUiState = emptyRowsState,
                    quickSettingsController = fakeQuickSettingsController,
                    showMoreSettingsButton = false
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .assertIsDisplayed()
            .performKeyInput { pressKey(Key.Escape) }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG).assertDoesNotExist()
        assertThat(isOpen).isFalse()
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp", fontScale = 2.0f)
    fun quickSettingsSideSheet_fontScale2x_scrollsToBottomControlsWithoutOverlap() {
        var activeFontScale = 0f
        var navigatedToSettings = false
        val captureModeSubtitle = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.quick_settings_text_capture_mode_standard)

        composeTestRule.setContent {
            activeFontScale = LocalDensity.current.fontScale
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = true,
                    onDismissRequest = {},
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController,
                    onNavigateToSettings = { navigatedToSettings = true },
                    showMoreSettingsButton = true
                )
            }
        }

        assertThat(activeFontScale).isEqualTo(2.0f)

        val subtitleBounds = composeTestRule
            .onNodeWithText(captureModeSubtitle, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val leadingButtonBounds = composeTestRule
            .onNodeWithTag(BTN_QUICK_SETTINGS_CAPTURE_MODE_OPTION_STANDARD)
            .getUnclippedBoundsInRoot()
        assertThat(subtitleBounds.right).isLessThan(leadingButtonBounds.left)

        composeTestRule.onNodeWithTag(ROW_QUICK_SETTINGS_HDR)
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(SETTINGS_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertThat(navigatedToSettings).isTrue()
    }

    @Test
    fun quickSettingsSideSheet_customModifier_preservesFullViewportAndScrimBounds() {
        var useSlotOverload by mutableStateOf(false)
        var dismissCount = 0
        var underlyingClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { underlyingClicked = true }
                    )
                    if (!useSlotOverload) {
                        QuickSettingsSideSheet(
                            isOpen = true,
                            onDismissRequest = { dismissCount++ },
                            quickSettingsUiState = sampleAvailableUiState,
                            quickSettingsController = fakeQuickSettingsController,
                            modifier = Modifier.testTag(BOX_QUICK_SETTINGS_CUSTOM_HOST_TAG)
                        )
                    } else {
                        QuickSettingsSideSheet(
                            isOpen = true,
                            onDismissRequest = { dismissCount++ },
                            modifier = Modifier.testTag(BOX_QUICK_SETTINGS_CUSTOM_HOST_TAG)
                        ) {
                            CaptureScaffoldDefaults.QuickSettings(
                                uiState = sampleAvailableUiState,
                                controller = fakeQuickSettingsController,
                                onNavigateToSettings = {},
                                showMoreSettingsButton = false
                            )
                        }
                    }
                }
            }
        }

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()

        listOf(false, true).forEachIndexed { index, slotMode ->
            if (index > 0) {
                composeTestRule.runOnIdle { useSlotOverload = slotMode }
                composeTestRule.waitForIdle()
            }

            val hostBounds = composeTestRule
                .onNodeWithTag(BOX_QUICK_SETTINGS_CUSTOM_HOST_TAG)
                .getUnclippedBoundsInRoot()
            val scrimBounds = composeTestRule
                .onNodeWithTag(QUICK_SETTINGS_SCRIM)
                .getUnclippedBoundsInRoot()
            val sheetBounds = composeTestRule
                .onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
                .getUnclippedBoundsInRoot()

            assertThat(hostBounds).isEqualTo(rootBounds)
            assertThat(scrimBounds).isEqualTo(rootBounds)
            assertThat(sheetBounds.width).isAtMost(470.dp)

            // Tap near the top-right corner, outside the sheet.
            composeTestRule.onRoot().performTouchInput { click(Offset(right - 8f, 8f)) }
        }

        assertThat(dismissCount).isEqualTo(2)
        assertThat(underlyingClicked).isFalse()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun quickSettingsSideSheet_focusedChildConsumingEscape_doesNotDismissUntilUnhandled() {
        var childConsumesEscape by mutableStateOf(true)
        var childEscapeCount = 0
        var dismissCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = true,
                    onDismissRequest = { dismissCount++ }
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(BOX_FOCUSABLE_CHILD_TAG)
                            .onKeyEvent { event ->
                                if (childConsumesEscape && event.key == Key.Escape) {
                                    if (event.type == KeyEventType.KeyUp) {
                                        childEscapeCount++
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                            .focusable()
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(BOX_FOCUSABLE_CHILD_TAG)
            .performKeyInput { pressKey(Key.Escape) }

        assertThat(childEscapeCount).isEqualTo(1)
        assertThat(dismissCount).isEqualTo(0)

        composeTestRule.runOnIdle { childConsumesEscape = false }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(BOX_FOCUSABLE_CHILD_TAG)
            .performKeyInput { pressKey(Key.Escape) }

        assertThat(childEscapeCount).isEqualTo(1)
        assertThat(dismissCount).isEqualTo(1)
    }

    private companion object {
        const val BOX_QUICK_SETTINGS_CUSTOM_HOST_TAG = "box_quick_settings_custom_host"
        const val BOX_FOCUSABLE_CHILD_TAG = "box_focusable_child"
    }

    @Test
    fun quickSettingsSideSheet_swipeTowardAttachedEdge_dismissesAndRemovesSheet() {
        var isOpen by mutableStateOf(true)
        var dismissCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = isOpen,
                    onDismissRequest = {
                        dismissCount++
                        isOpen = false
                    },
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertThat(dismissCount).isEqualTo(1)
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM).assertDoesNotExist()
    }

    @Test
    fun quickSettingsSideSheet_shortSlowDrag_settlesBackToExpanded() {
        var dismissCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = true,
                    onDismissRequest = { dismissCount++ },
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .performTouchInput {
                swipe(
                    start = center,
                    end = Offset(centerX - width * 0.1f, centerY),
                    durationMillis = 1_000
                )
            }
        composeTestRule.waitForIdle()

        assertThat(dismissCount).isEqualTo(0)
        assertThat(
            composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
                .getUnclippedBoundsInRoot()
                .left
        ).isEqualTo(0.dp)
    }

    @Test
    fun quickSettingsSideSheet_rightAlignment_attachesToRightEdgeAndSwipesRightToDismiss() {
        var dismissCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = true,
                    onDismissRequest = { dismissCount++ },
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController,
                    contentAlignment = AbsoluteAlignment.CenterRight
                )
            }
        }

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val sheetBounds = composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .getUnclippedBoundsInRoot()
        val handleBounds = composeTestRule.onNodeWithTag(QUICK_SETTINGS_DRAG_HANDLE)
            .getUnclippedBoundsInRoot()
        assertThat(sheetBounds.right).isEqualTo(rootBounds.right)
        // The handle sits on the free (left) edge.
        assertThat(handleBounds.left).isEqualTo(sheetBounds.left)

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        assertThat(dismissCount).isEqualTo(1)
    }

    @Test
    fun quickSettingsSideSheet_dragHandle_isOnFreeEdgeAndDismissesOnClick() {
        var dismissCount = 0
        val dismissLabel = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.quick_settings_btn_close_expanded_settings_description)

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = true,
                    onDismissRequest = { dismissCount++ },
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        val sheetBounds = composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .getUnclippedBoundsInRoot()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DRAG_HANDLE)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(
                SemanticsMatcher("onClickLabel equals $dismissLabel") { node ->
                    node.config.getOrNull(SemanticsActions.OnClick)?.label == dismissLabel
                }
            )
            .assert(
                SemanticsMatcher("handle on the sheet's right (free) edge") { node ->
                    with(node.layoutInfo.density) {
                        node.boundsInRoot.right.toDp() == sheetBounds.right
                    }
                }
            )
            .performClick()

        assertThat(dismissCount).isEqualTo(1)
    }

    @Test
    fun quickSettingsSideSheet_close_animatesOutBeforeLeavingComposition() {
        var isOpen by mutableStateOf(true)

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = isOpen,
                    onDismissRequest = { isOpen = false },
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { isOpen = false }
        // Step frame by frame: the sheet slides out while staying composed.
        var sawSlidingFrame = false
        repeat(30) {
            if (!sawSlidingFrame) {
                composeTestRule.mainClock.advanceTimeByFrame()
                val left = composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
                    .getUnclippedBoundsInRoot()
                    .left
                sawSlidingFrame = left < 0.dp && left > (-470).dp
            }
        }
        assertThat(sawSlidingFrame).isTrue()

        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG).assertDoesNotExist()
    }

    @Test
    fun quickSettingsSideSheet_predictiveBackCancelled_doesNotDismiss_completed_dismisses() {
        var dismissCount = 0
        lateinit var backDispatcher: OnBackPressedDispatcher

        composeTestRule.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = true,
                    onDismissRequest = { dismissCount++ },
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.runOnUiThread {
            backDispatcher.dispatchOnBackStarted(backEvent(progress = 0f))
            backDispatcher.dispatchOnBackProgressed(backEvent(progress = 0.5f))
            backDispatcher.dispatchOnBackCancelled()
        }
        composeTestRule.waitForIdle()
        assertThat(dismissCount).isEqualTo(0)
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG).assertIsDisplayed()

        composeTestRule.runOnUiThread {
            backDispatcher.dispatchOnBackStarted(backEvent(progress = 0f))
            backDispatcher.dispatchOnBackProgressed(backEvent(progress = 0.5f))
            backDispatcher.onBackPressed()
        }
        composeTestRule.runOnIdle { assertThat(dismissCount).isEqualTo(1) }
    }

    @Test
    fun quickSettingsSideSheet_sheetSurface_exposesDismissSemanticsAction() {
        var dismissCount = 0
        val dismissLabel = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.quick_settings_btn_close_expanded_settings_description)

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = true,
                    onDismissRequest = { dismissCount++ },
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .assert(
                SemanticsMatcher("dismiss label equals $dismissLabel") { node ->
                    node.config.getOrNull(SemanticsActions.Dismiss)?.label == dismissLabel
                }
            )
            .performSemanticsAction(SemanticsActions.Dismiss)

        assertThat(dismissCount).isEqualTo(1)
    }

    @Test
    fun quickSettingsSideSheet_ignoredSwipeDismiss_removesScrimSoTapsPassThrough() {
        var underlyingClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { underlyingClicked = true }
                    )
                    // The caller ignores onDismissRequest, so isOpen stays true.
                    QuickSettingsSideSheet(
                        isOpen = true,
                        onDismissRequest = {},
                        quickSettingsUiState = sampleAvailableUiState,
                        quickSettingsController = fakeQuickSettingsController
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM).assertDoesNotExist()
        composeTestRule.onRoot().performTouchInput { click(Offset(right - 4f, centerY)) }
        assertThat(underlyingClicked).isTrue()
    }

    @Test
    fun quickSettingsSideSheet_windowInsets_areAppliedOutsideSheetPadding() {
        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsSideSheet(
                    isOpen = true,
                    onDismissRequest = {},
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController,
                    sheetPadding = PaddingValues(start = 16.dp),
                    windowInsets = WindowInsets(left = 24.dp)
                )
            }
        }

        assertThat(
            composeTestRule.onNodeWithTag(QUICK_SETTINGS_SIDE_SHEET_TAG)
                .getUnclippedBoundsInRoot()
                .left
        ).isEqualTo(40.dp)
    }

    @Test
    fun dampFlingTowardHidden_dampsOnlyInsideZoneAndTowardHidden() {
        // Outside the damping zone the velocity is unchanged.
        assertThat(
            dampFlingTowardHidden(
                velocity = -1000f,
                offset = 0f,
                hiddenOffset = -500f,
                dampingZone = 100f,
                velocityThreshold = 50f
            )
        ).isEqualTo(-1000f)
        // Moving away from hidden (opening) is never damped.
        assertThat(
            dampFlingTowardHidden(
                velocity = 1000f,
                offset = -450f,
                hiddenOffset = -500f,
                dampingZone = 100f,
                velocityThreshold = 50f
            )
        ).isEqualTo(1000f)
        // Halfway through the zone toward hidden: scaled by 0.5^2.
        assertThat(
            dampFlingTowardHidden(
                velocity = -40f,
                offset = -450f,
                hiddenOffset = -500f,
                dampingZone = 100f,
                velocityThreshold = 50f
            )
        ).isEqualTo(-10f)
        // A fast fling keeps at least the threshold so it still settles hidden.
        assertThat(
            dampFlingTowardHidden(
                velocity = -1000f,
                offset = -495f,
                hiddenOffset = -500f,
                dampingZone = 100f,
                velocityThreshold = 50f
            )
        ).isEqualTo(-50f)
        // Right-attached sheets damp positive velocity the same way.
        assertThat(
            dampFlingTowardHidden(
                velocity = 40f,
                offset = 450f,
                hiddenOffset = 500f,
                dampingZone = 100f,
                velocityThreshold = 50f
            )
        ).isEqualTo(10f)
    }

    @Test
    fun overshootPastExpanded_measuresOnlyMovementAwayFromAttachedEdge() {
        assertThat(overshootPastExpanded(12f, SideSheetEdge.Left)).isEqualTo(12f)
        assertThat(overshootPastExpanded(-12f, SideSheetEdge.Left)).isEqualTo(0f)
        assertThat(overshootPastExpanded(-12f, SideSheetEdge.Right)).isEqualTo(12f)
        assertThat(overshootPastExpanded(12f, SideSheetEdge.Right)).isEqualTo(0f)
        assertThat(overshootPastExpanded(Float.NaN, SideSheetEdge.Left)).isEqualTo(0f)
    }

    private fun backEvent(progress: Float) = BackEventCompat(
        touchX = 0f,
        touchY = 0f,
        progress = progress,
        swipeEdge = BackEventCompat.EDGE_LEFT
    )
}
