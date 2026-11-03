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

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.components.capture.quicksettings.QuickSettingsOverlayCard
import com.google.jetpackcamera.ui.components.capture.quicksettings.QuickSettingsScaffoldContent
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DEBUG_OVERLAY_TAG = "box_debug_overlay"

@RunWith(AndroidJUnit4::class)
class CaptureLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(
            AccessibilityValidator().setRunChecksFromRootView(true).also {
                it.setThrowExceptionFor(AccessibilityCheckResultType.ERROR)
            }
        )
    }

    private fun getResString(@StringRes resId: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(resId)

    private fun sampleQuickSettingsUiState(): QuickSettingsUiState.Available =
        QuickSettingsUiState.Available(
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TestPreviewLayout(
        modifier: Modifier = Modifier,
        scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
        ),
        onDismissQuickSettings: () -> Unit = {},
        captureButton: @Composable (Modifier) -> Unit = {},
        flipCameraButton: @Composable (Modifier) -> Unit = {},
        indicatorRow: @Composable (Modifier) -> Unit = {},
        debugOverlay: @Composable (Modifier) -> Unit = {},
        quickSettingsOverlay: @Composable (Modifier) -> Unit = {},
        viewfinder: @Composable (Modifier) -> Unit = {},
        errorDialog: @Composable (Modifier) -> Unit = {},
        isErrorDialogShowing: Boolean = false
    ) {
        PreviewLayout(
            modifier = modifier,
            scaffoldState = scaffoldState,
            onDismissQuickSettings = onDismissQuickSettings,
            viewfinder = viewfinder,
            captureButton = captureButton,
            imageWell = {},
            flipCameraButton = flipCameraButton,
            zoomLevelDisplay = {},
            elapsedTimeDisplay = {},
            quickSettingsButton = {},
            indicatorRow = indicatorRow,
            captureModeToggle = {},
            quickSettingsOverlay = quickSettingsOverlay,
            debugOverlay = debugOverlay,
            debugVisibilityWrapper = { it() },
            screenFlashOverlay = {},
            snackBar = { _, _ -> },
            errorDialog = errorDialog,
            isErrorDialogShowing = isErrorDialogShowing
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun previewLayout_dragHandle_hasButtonRoleAndAccessibilityLabel() {
        composeTestRule.setContent {
            val scaffoldState = rememberBottomSheetScaffoldState(
                bottomSheetState = rememberBottomSheetState(initialValue = SheetValue.Expanded)
            )
            TestPreviewLayout(scaffoldState = scaffoldState)
        }

        val targetDescription = getResString(
            R.string.quick_settings_btn_close_expanded_settings_description
        )

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DRAG_HANDLE)
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
            )
            .assert(
                SemanticsMatcher("onClickLabel equals $targetDescription") { node ->
                    node.config.getOrNull(SemanticsActions.OnClick)?.label == targetDescription
                }
            )

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun previewLayout_clickDragHandle_callsOnDismissQuickSettings() {
        var onDismissCalled = false

        composeTestRule.setContent {
            val scaffoldState = rememberBottomSheetScaffoldState(
                bottomSheetState = rememberBottomSheetState(initialValue = SheetValue.Expanded)
            )
            TestPreviewLayout(
                scaffoldState = scaffoldState,
                onDismissQuickSettings = { onDismissCalled = true }
            )
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DRAG_HANDLE).performClick()
        assertThat(onDismissCalled).isTrue()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun previewLayout_scrim_displayedWhenExpanded_andClickCallsDismiss() {
        var onDismissCalled = false
        val targetDescription = getResString(
            R.string.quick_settings_btn_close_expanded_settings_description
        )

        composeTestRule.setContent {
            val scaffoldState = rememberBottomSheetScaffoldState(
                bottomSheetState = rememberBottomSheetState(initialValue = SheetValue.Expanded)
            )
            TestPreviewLayout(
                scaffoldState = scaffoldState,
                onDismissQuickSettings = { onDismissCalled = true }
            )
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM)
            .assertIsDisplayed()
            .assertContentDescriptionEquals(targetDescription)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, 1f))
            .performClick()

        assertThat(onDismissCalled).isTrue()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun previewLayout_scrim_interceptsTouchEventsFromUnderlyingControls() {
        var underlyingViewfinderClicked = false
        var onDismissCalled = false

        composeTestRule.setContent {
            val scaffoldState = rememberBottomSheetScaffoldState(
                bottomSheetState = rememberBottomSheetState(initialValue = SheetValue.Expanded)
            )
            TestPreviewLayout(
                scaffoldState = scaffoldState,
                onDismissQuickSettings = { onDismissCalled = true },
                viewfinder = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { underlyingViewfinderClicked = true }
                    )
                }
            )
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM).performClick()

        assertThat(underlyingViewfinderClicked).isFalse()
        assertThat(onDismissCalled).isTrue()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun previewLayout_scrim_doesNotExistWhenHidden() {
        composeTestRule.setContent {
            val scaffoldState = rememberBottomSheetScaffoldState(
                bottomSheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
            )
            TestPreviewLayout(scaffoldState = scaffoldState)
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM)
            .assertDoesNotExist()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun previewLayout_whenQuickSettingsExpanded_prunesBackgroundControlsFromAccessibilityTree() {
        lateinit var scaffoldState: BottomSheetScaffoldState
        lateinit var scope: CoroutineScope
        val capturePhotoLabel = getResString(R.string.capture_button_capture_photo)
        val backCameraLabel = getResString(R.string.quick_settings_back_camera_description)
        val hdQualityLabel = getResString(R.string.video_quality_description_hd)

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            scaffoldState = rememberBottomSheetScaffoldState(
                bottomSheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
            )
            MaterialTheme(colorScheme = darkColorScheme()) {
                TestPreviewLayout(
                    scaffoldState = scaffoldState,
                    captureButton = { modifier ->
                        Box(
                            modifier = modifier
                                .size(48.dp)
                                .testTag(CAPTURE_BUTTON)
                                .semantics { contentDescription = capturePhotoLabel }
                                .clickable(role = Role.Button) {}
                        )
                    },
                    flipCameraButton = { modifier ->
                        Box(
                            modifier = modifier
                                .size(48.dp)
                                .testTag(FLIP_CAMERA_BUTTON)
                                .semantics { contentDescription = backCameraLabel }
                                .clickable(role = Role.Button) {}
                        )
                    },
                    indicatorRow = { modifier ->
                        Box(
                            modifier = modifier
                                .size(48.dp)
                                .testTag(VIDEO_QUALITY_TAG)
                                .semantics { contentDescription = hdQualityLabel }
                        )
                    },
                    debugOverlay = { modifier ->
                        Box(
                            modifier = modifier
                                .size(48.dp)
                                .testTag(TEST_DEBUG_OVERLAY_TAG)
                        )
                    },
                    quickSettingsOverlay = { modifier ->
                        QuickSettingsScaffoldContent(
                            modifier = modifier,
                            quickSettingsUiState = sampleQuickSettingsUiState(),
                            onNavigateToSettings = {},
                            quickSettingsController = FakeQuickSettingsController(),
                            showMoreSettingsButton = false
                        )
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(VIDEO_QUALITY_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_DEBUG_OVERLAY_TAG).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        composeTestRule.runOnIdle {
            scope.launch { scaffoldState.bottomSheetState.expand() }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertDoesNotExist()
        composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).assertDoesNotExist()
        composeTestRule.onNodeWithTag(VIDEO_QUALITY_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TEST_DEBUG_OVERLAY_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertIsDisplayed()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun previewLayout_whenErrorDialogOpen_prunesBackgroundControlsAndKeepsDialogAccessible() {
        var showError by mutableStateOf(false)
        val capturePhotoLabel = getResString(R.string.capture_button_capture_photo)
        val backCameraLabel = getResString(R.string.quick_settings_back_camera_description)

        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                TestPreviewLayout(
                    captureButton = { modifier ->
                        Box(
                            modifier = modifier
                                .size(48.dp)
                                .testTag(CAPTURE_BUTTON)
                                .semantics { contentDescription = capturePhotoLabel }
                                .clickable(role = Role.Button) {}
                        )
                    },
                    flipCameraButton = { modifier ->
                        Box(
                            modifier = modifier
                                .size(48.dp)
                                .testTag(FLIP_CAMERA_BUTTON)
                                .semantics { contentDescription = backCameraLabel }
                                .clickable(role = Role.Button) {}
                        )
                    },
                    errorDialog = { modifier ->
                        if (showError) {
                            CameraErrorDialog(
                                title = "Camera unavailable",
                                body = "Close other camera apps and try again",
                                confirmButtonText = "OK",
                                onConfirm = { showError = false },
                                modifier = modifier
                            )
                        }
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_TAG).assertDoesNotExist()

        composeTestRule.runOnIdle { showError = true }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertDoesNotExist()
        composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).assertDoesNotExist()
        composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_CONFIRM_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun quickSettingsOverlayCard_lightTheme_passesAccessibilityChecks() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    QuickSettingsOverlayCard(
                        isOpen = true,
                        onDismissRequest = {},
                        quickSettingsUiState = sampleQuickSettingsUiState(),
                        quickSettingsController = FakeQuickSettingsController()
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun quickSettingsOverlayCard_darkTheme_passesAccessibilityChecks() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    QuickSettingsOverlayCard(
                        isOpen = true,
                        onDismissRequest = {},
                        quickSettingsUiState = sampleQuickSettingsUiState(),
                        quickSettingsController = FakeQuickSettingsController()
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}
