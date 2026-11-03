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
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
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
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_CAPTURE_MODE_OPTION_STANDARD
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_CAPTURE_MODE_OPTION_VIDEO_ONLY
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FLASH_OPTION_AUTO
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FLASH_OPTION_OFF
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FLASH_OPTION_ON
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_HDR_OPTION_ON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_BOTTOM_SHEET
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_OVERLAY_CARD_TAG
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_3_4_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_SCRIM
import com.google.jetpackcamera.ui.components.capture.R
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_ASPECT_RATIO
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_CAPTURE_MODE
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_FLASH
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_HDR
import com.google.jetpackcamera.ui.components.capture.SETTINGS_BUTTON
import com.google.jetpackcamera.ui.controller.testing.FakeQuickSettingsController
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1280dp-h900dp")
class QuickSettingsOverlayCardTest {

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
        hdrUiState = HdrUiState.Available(
            selectedImageFormat = ImageOutputFormat.JPEG,
            selectedDynamicRange = DynamicRange.SDR,
            isSupported = true
        )
    )

    @Test
    fun quickSettingsOverlayCard_whenClosed_doesNotRenderAndDoesNotInterceptBack() {
        var dismissCalled = false
        var outerBackHandled = false
        lateinit var backDispatcher: OnBackPressedDispatcher

        composeTestRule.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            MaterialTheme {
                BackHandler(enabled = true) { outerBackHandled = true }
                QuickSettingsOverlayCard(
                    isOpen = false,
                    onDismissRequest = { dismissCalled = true },
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM).assertDoesNotExist()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertDoesNotExist()

        composeTestRule.runOnUiThread {
            backDispatcher.onBackPressed()
        }

        assertThat(dismissCalled).isFalse()
        assertThat(outerBackHandled).isTrue()
    }

    @Test
    fun quickSettingsOverlayCard_whenUnavailable_doesNotRenderAndDoesNotInterceptBack() {
        var dismissCalled = false
        var outerBackHandled = false
        lateinit var backDispatcher: OnBackPressedDispatcher

        composeTestRule.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            MaterialTheme {
                BackHandler(enabled = true) { outerBackHandled = true }
                QuickSettingsOverlayCard(
                    isOpen = true,
                    onDismissRequest = { dismissCalled = true },
                    quickSettingsUiState = QuickSettingsUiState.Unavailable,
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM).assertDoesNotExist()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertDoesNotExist()

        composeTestRule.runOnUiThread {
            backDispatcher.onBackPressed()
        }

        assertThat(dismissCalled).isFalse()
        assertThat(outerBackHandled).isTrue()
    }

    @Test
    fun quickSettingsOverlayCard_whenOpen_rendersInHierarchyAndCapsCardWidth() {
        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsOverlayCard(
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

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_TAG)
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

        val cardWidth = composeTestRule
            .onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_TAG)
            .getUnclippedBoundsInRoot()
            .width
        assertThat(cardWidth).isAtMost(412.dp)
    }

    @Test
    fun quickSettingsOverlayCard_scrimTap_dismissesAndInterceptsUnderlyingClicks() {
        var dismissCount = 0
        var underlyingClicked = false
        val expectedDismissDescription = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.quick_settings_btn_close_expanded_settings_description)

        composeTestRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { underlyingClicked = true }
                    )
                    QuickSettingsOverlayCard(
                        isOpen = true,
                        onDismissRequest = { dismissCount++ },
                        quickSettingsUiState = sampleAvailableUiState,
                        quickSettingsController = fakeQuickSettingsController
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM)
            .assertContentDescriptionEquals(expectedDismissDescription)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, 1f))
            .assert(
                SemanticsMatcher("onClickLabel is null to avoid duplicate speech") { node ->
                    node.config.getOrNull(SemanticsActions.OnClick)?.label == null
                }
            )
            .performTouchInput {
                click(Offset(4f, 4f))
            }

        assertThat(dismissCount).isEqualTo(1)
        assertThat(underlyingClicked).isFalse()
    }

    @Test
    fun quickSettingsOverlayCard_cardBackgroundTap_doesNotDismissOrPassThroughToScrim() {
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
                    QuickSettingsOverlayCard(
                        isOpen = true,
                        onDismissRequest = { dismissCount++ },
                        quickSettingsUiState = sampleAvailableUiState,
                        quickSettingsController = fakeQuickSettingsController
                    )
                }
            }
        }

        // Click inside the top and bottom vertical padding of the card surface (horizontally
        // centered so the touch is inside MaterialTheme.shapes.extraLarge's 28.dp corner radius).
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_TAG)
            .performTouchInput {
                click(Offset(centerX, 8f))
                click(Offset(centerX, bottom - 8f))
            }

        assertThat(dismissCount).isEqualTo(0)
        assertThat(underlyingClicked).isFalse()
    }

    @Test
    fun quickSettingsOverlayCard_settingSelections_invokeControllerCallbacksWithoutDismissing() {
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
                QuickSettingsOverlayCard(
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
    fun quickSettingsOverlayCard_backPress_invokesOnDismissRequest() {
        var dismissCount = 0
        lateinit var backDispatcher: OnBackPressedDispatcher

        composeTestRule.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            MaterialTheme {
                QuickSettingsOverlayCard(
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

        assertThat(dismissCount).isEqualTo(1)
    }

    @Test
    fun quickSettingsOverlayCard_showMoreSettingsButton_defaultsToFalse() {
        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsOverlayCard(
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
    fun quickSettingsOverlayCard_showMoreSettingsButtonTrue_displaysButtonAndInvokesCallback() {
        var navigatedToSettings = false

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsOverlayCard(
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
    fun quickSettingsOverlayCard_wideShortViewport_scrollsToBottomMoreSettingsButton() {
        var navigatedToSettings = false

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsOverlayCard(
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
    fun quickSettingsHostState_openDismissAndToggle_updatesIsOpen() {
        val state = QuickSettingsHostState()
        assertThat(state.isOpen).isFalse()

        state.open()
        assertThat(state.isOpen).isTrue()

        state.dismiss()
        assertThat(state.isOpen).isFalse()

        state.toggle()
        assertThat(state.isOpen).isTrue()

        state.toggle()
        assertThat(state.isOpen).isFalse()
    }

    @Test
    fun rememberQuickSettingsHostState_restoresStateAcrossSavedInstanceStateRecreation() {
        val restorationTester = StateRestorationTester(composeTestRule)
        var hostState: QuickSettingsHostState? = null

        restorationTester.setContent {
            hostState = rememberQuickSettingsHostState()
        }

        assertThat(hostState!!.isOpen).isFalse()

        composeTestRule.runOnIdle {
            hostState!!.open()
        }
        assertThat(hostState!!.isOpen).isTrue()

        hostState = null
        restorationTester.emulateSavedInstanceStateRestore()
        assertThat(hostState!!.isOpen).isTrue()

        composeTestRule.runOnIdle {
            hostState!!.dismiss()
        }
        hostState = null
        restorationTester.emulateSavedInstanceStateRestore()
        assertThat(hostState!!.isOpen).isFalse()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rememberQuickSettingsBottomSheetScaffoldState_supportsHiddenDefaultAndTransitions() {
        lateinit var scaffoldState: BottomSheetScaffoldState
        lateinit var scope: CoroutineScope

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            scaffoldState = rememberQuickSettingsBottomSheetScaffoldState()
        }

        assertThat(scaffoldState.bottomSheetState.currentValue).isEqualTo(SheetValue.Hidden)

        composeTestRule.runOnIdle {
            scope.launch { scaffoldState.bottomSheetState.expand() }
        }
        composeTestRule.waitForIdle()
        assertThat(scaffoldState.bottomSheetState.currentValue).isEqualTo(SheetValue.Expanded)

        composeTestRule.runOnIdle {
            scope.launch { scaffoldState.bottomSheetState.hide() }
        }
        composeTestRule.waitForIdle()
        assertThat(scaffoldState.bottomSheetState.currentValue).isEqualTo(SheetValue.Hidden)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rememberQuickSettingsBottomSheetScaffoldState_respectsInitialExpandedValue() {
        lateinit var scaffoldState: BottomSheetScaffoldState

        composeTestRule.setContent {
            scaffoldState = rememberQuickSettingsBottomSheetScaffoldState(
                initialValue = SheetValue.Expanded
            )
        }

        assertThat(scaffoldState.bottomSheetState.currentValue).isEqualTo(SheetValue.Expanded)
    }

    @Test
    fun quickSettingsOverlayCard_rtlLayoutDirection_mirrorsCardAlignmentAndRows() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        val flashTitleText = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.quick_settings_title_flash_mode)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MaterialTheme {
                    QuickSettingsOverlayCard(
                        isOpen = true,
                        onDismissRequest = {},
                        quickSettingsUiState = sampleAvailableUiState,
                        quickSettingsController = fakeQuickSettingsController
                    )
                }
            }
        }

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val cardBoundsLtr = composeTestRule
            .onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_TAG)
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

        assertThat(cardBoundsLtr.left).isEqualTo(16.dp)
        assertThat(cardBoundsLtr.right).isLessThan(rootBounds.width / 2)
        assertThat(cardBoundsLtr.top).isGreaterThan(rootBounds.height / 2)
        assertThat(cardBoundsLtr.bottom).isEqualTo(rootBounds.bottom - 16.dp)
        assertThat(flashTitleBoundsLtr.left).isLessThan(flashOffBoundsLtr.left)
        assertThat(flashOffBoundsLtr.left).isLessThan(flashAutoBoundsLtr.left)

        composeTestRule.runOnIdle {
            layoutDirection = LayoutDirection.Rtl
        }
        composeTestRule.waitForIdle()

        val cardBoundsRtl = composeTestRule
            .onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_TAG)
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

        assertThat(cardBoundsRtl.left).isGreaterThan(rootBounds.width / 2)
        assertThat(cardBoundsRtl.right).isEqualTo(rootBounds.right - 16.dp)
        assertThat(cardBoundsRtl.bottom).isEqualTo(rootBounds.bottom - 16.dp)
        assertThat(flashTitleBoundsRtl.left).isGreaterThan(flashOffBoundsRtl.left)
        assertThat(flashOffBoundsRtl.left).isGreaterThan(flashAutoBoundsRtl.left)
    }

    @Test
    fun quickSettingsOverlayCard_customContentAlignment_positionsCardAccordingly() {
        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsOverlayCard(
                    isOpen = true,
                    onDismissRequest = {},
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController,
                    contentAlignment = Alignment.TopEnd
                )
            }
        }

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val cardBounds = composeTestRule
            .onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_TAG)
            .getUnclippedBoundsInRoot()

        assertThat(cardBounds.left).isGreaterThan(rootBounds.width / 2)
        assertThat(cardBounds.right).isEqualTo(rootBounds.right - 16.dp)
        assertThat(cardBounds.top).isEqualTo(16.dp)
        assertThat(cardBounds.bottom).isLessThan(rootBounds.height / 2)
    }

    @Test
    fun quickSettingsOverlayCard_semantics_exposesPaneTitleHeadingAndCardFirstTraversal() {
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
                QuickSettingsOverlayCard(
                    isOpen = true,
                    onDismissRequest = {},
                    quickSettingsUiState = uiState,
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_TAG)
            .onParent()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, standardTitle))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, 0f))

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, 1f))

        composeTestRule.onNodeWithText(standardTitle)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

        composeTestRule.runOnIdle { captureMode = CaptureMode.IMAGE_ONLY }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, photoTitle))
        composeTestRule.onNodeWithText(photoTitle)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

        composeTestRule.runOnIdle { captureMode = CaptureMode.VIDEO_ONLY }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, videoTitle))
        composeTestRule.onNodeWithText(videoTitle)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun quickSettingsOverlayCard_escapeKey_dismissesOnKeyUp() {
        var dismissCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsOverlayCard(
                    isOpen = true,
                    onDismissRequest = { dismissCount++ },
                    quickSettingsUiState = sampleAvailableUiState,
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_TAG)
            .performKeyInput { keyDown(Key.Escape) }
        assertThat(dismissCount).isEqualTo(0)

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_TAG)
            .performKeyInput { keyUp(Key.Escape) }
        assertThat(dismissCount).isEqualTo(1)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun quickSettingsOverlayCard_emptyRows_requestsFocusCleanlyAndEscapeDismisses() {
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
                QuickSettingsOverlayCard(
                    isOpen = isOpen,
                    onDismissRequest = { isOpen = false },
                    quickSettingsUiState = emptyRowsState,
                    quickSettingsController = fakeQuickSettingsController,
                    showMoreSettingsButton = false
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_TAG)
            .assertIsDisplayed()
            .performKeyInput { pressKey(Key.Escape) }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_TAG).assertDoesNotExist()
        assertThat(isOpen).isFalse()
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp", fontScale = 2.0f)
    fun quickSettingsOverlayCard_fontScale2x_scrollsToBottomControlsWithoutOverlap() {
        var activeFontScale = 0f
        var navigatedToSettings = false
        val captureModeSubtitle = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.quick_settings_text_capture_mode_standard)

        composeTestRule.setContent {
            activeFontScale = LocalDensity.current.fontScale
            MaterialTheme {
                QuickSettingsOverlayCard(
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
}
