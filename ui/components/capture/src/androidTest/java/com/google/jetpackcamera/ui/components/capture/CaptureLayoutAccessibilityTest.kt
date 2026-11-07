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

import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.jetpackcamera.core.camera.AudioStreamState
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.MediaDescriptor
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.ui.components.capture.quicksettings.NoOpQuickSettingsController
import com.google.jetpackcamera.ui.components.capture.quicksettings.QuickSettingsScaffoldContent
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.ToggleQuickSettingsButton
import com.google.jetpackcamera.ui.uistate.CustomSnackbarVisuals
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.AudioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.ElapsedTimeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState
import com.google.jetpackcamera.ui.uistate.capture.StabilizationUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class CaptureLayoutAccessibilityTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(
            AccessibilityValidator()
                .setRunChecksFromRootView(true)
                .setThrowExceptionFor(AccessibilityCheckResultType.ERROR)
        )
    }

    @Test
    fun previewLayout_idleCaptureControls_meetAccessibilityChecks() {
        val thumbnail = createBitmap(64, 64).apply { eraseColor(AndroidColor.BLUE) }

        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                CompositionLocalProvider(LocalDisableAnimations provides true) {
                    PreviewLayout(
                        viewfinder = { modifier -> Box(modifier.fillMaxSize()) },
                        captureButton = { modifier ->
                            CaptureButton(
                                modifier = modifier,
                                captureButtonUiState = CaptureButtonUiState.Enabled.Idle(
                                    CaptureMode.IMAGE_ONLY
                                )
                            )
                        },
                        imageWell = { modifier ->
                            ImageWell(
                                modifier = modifier,
                                imageWellUiState = ImageWellUiState.Content(
                                    mediaDescriptor = MediaDescriptor.Content.Image(
                                        uri = Uri.EMPTY,
                                        thumbnail = thumbnail
                                    )
                                ),
                                onClick = {}
                            )
                        },
                        flipCameraButton = { modifier ->
                            FlipCameraButton(
                                modifier = modifier,
                                flipLensUiState = FlipLensUiState.Available(
                                    selectedLensFacing = LensFacing.BACK,
                                    availableLensFacings = listOf(
                                        SingleSelectableUiState.SelectableUi(LensFacing.BACK),
                                        SingleSelectableUiState.SelectableUi(LensFacing.FRONT)
                                    )
                                ),
                                onClick = {}
                            )
                        },
                        zoomLevelDisplay = { modifier ->
                            ZoomBar(
                                modifier = modifier,
                                zoomControlUiState = ZoomControlUiState.Enabled(
                                    zoomLevels = listOf(0.5f, 1.0f, 2.0f, 5.0f),
                                    primaryLensFacing = LensFacing.BACK,
                                    primaryZoomRatio = 1.0f
                                ),
                                onChangeZoom = {}
                            )
                        },
                        elapsedTimeDisplay = {},
                        quickSettingsButton = { modifier ->
                            ToggleQuickSettingsButton(
                                modifier = modifier,
                                isOpen = false,
                                onClick = {}
                            )
                        },
                        indicatorRow = { modifier ->
                            IndicatorPill(modifier = modifier) {
                                StabilizationIcon(
                                    stabilizationUiState = StabilizationUiState.Specific(
                                        stabilizationMode = StabilizationMode.ON
                                    )
                                )
                                VideoQualityIcon(videoQuality = VideoQuality.FHD)
                            }
                        },
                        captureModeToggle = { modifier ->
                            CaptureModeToggleButton(
                                modifier = modifier,
                                uiState = CaptureModeToggleUiState.Available(
                                    selectedCaptureMode = CaptureMode.IMAGE_ONLY,
                                    imageOnlyUiState = SingleSelectableUiState.SelectableUi(
                                        CaptureMode.IMAGE_ONLY
                                    ),
                                    videoOnlyUiState = SingleSelectableUiState.SelectableUi(
                                        CaptureMode.VIDEO_ONLY
                                    )
                                ),
                                onChangeCaptureMode = {},
                                onToggleWhenDisabled = {}
                            )
                        },
                        quickSettingsOverlay = {},
                        debugOverlay = {},
                        debugVisibilityWrapper = { it() },
                        screenFlashOverlay = {},
                        snackBar = { _, _ -> }
                    )
                }
            }
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun previewLayout_pressedRecordingControls_meetAccessibilityChecks() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                CompositionLocalProvider(LocalDisableAnimations provides true) {
                    PreviewLayout(
                        viewfinder = { modifier -> Box(modifier.fillMaxSize()) },
                        captureButton = { modifier ->
                            CaptureButton(
                                modifier = modifier,
                                captureButtonUiState =
                                CaptureButtonUiState.Enabled.Recording.PressedRecording
                            )
                        },
                        imageWell = { modifier ->
                            PauseResumeToggleButton(
                                modifier = modifier,
                                onSetPause = {},
                                currentRecordingStateProvider = {
                                    VideoRecordingState.Active.Recording(
                                        maxDurationMillis = 0L,
                                        audioStreamState = AudioStreamState.Active(0.5),
                                        elapsedTimeNanos = 5_000_000_000L
                                    )
                                }
                            )
                        },
                        flipCameraButton = { modifier ->
                            Row(
                                modifier = modifier,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AmplitudeToggleButton(
                                    audioUiState = AudioUiState.Enabled.On(
                                        amplitude = 0.5,
                                        isAudioStreamActive = true
                                    ),
                                    onToggleAudio = {}
                                )
                            }
                        },
                        zoomLevelDisplay = { modifier ->
                            ZoomBar(
                                modifier = modifier,
                                zoomControlUiState = ZoomControlUiState.Enabled(
                                    zoomLevels = listOf(1.0f, 2.0f),
                                    primaryLensFacing = LensFacing.BACK,
                                    primaryZoomRatio = 1.0f
                                ),
                                onChangeZoom = {}
                            )
                        },
                        elapsedTimeDisplay = { modifier ->
                            ElapsedTimeText(
                                modifier = modifier,
                                elapsedTimeUiStateProvider = {
                                    ElapsedTimeUiState.Enabled(elapsedTimeNanos = 5_000_000_000L)
                                }
                            )
                        },
                        quickSettingsButton = {},
                        indicatorRow = {},
                        captureModeToggle = {},
                        quickSettingsOverlay = {},
                        debugOverlay = {},
                        debugVisibilityWrapper = { it() },
                        screenFlashOverlay = {},
                        snackBar = { _, _ -> }
                    )
                }
            }
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun previewLayout_expandedQuickSettings_meetsAccessibilityChecks() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberStandardBottomSheetState(
                        initialValue = SheetValue.Expanded,
                        skipHiddenState = false
                    )
                )
                PreviewLayout(
                    scaffoldState = scaffoldState,
                    viewfinder = { modifier -> Box(modifier.fillMaxSize()) },
                    captureButton = {},
                    imageWell = {},
                    flipCameraButton = {},
                    zoomLevelDisplay = {},
                    elapsedTimeDisplay = {},
                    quickSettingsButton = { modifier ->
                        ToggleQuickSettingsButton(
                            modifier = modifier,
                            isOpen = true,
                            onClick = {}
                        )
                    },
                    indicatorRow = {},
                    captureModeToggle = {},
                    quickSettingsOverlay = { modifier ->
                        QuickSettingsScaffoldContent(
                            modifier = modifier,
                            quickSettingsUiState = QuickSettingsUiState.Available(
                                aspectRatioUiState = AspectRatioUiState.Available(
                                    selectedAspectRatio = AspectRatio.THREE_FOUR,
                                    availableAspectRatios = listOf(
                                        SingleSelectableUiState.SelectableUi(
                                            AspectRatio.THREE_FOUR
                                        ),
                                        SingleSelectableUiState.SelectableUi(AspectRatio.ONE_ONE),
                                        SingleSelectableUiState.SelectableUi(
                                            AspectRatio.NINE_SIXTEEN
                                        )
                                    )
                                ),
                                captureModeUiState = CaptureModeUiState.Available(
                                    selectedCaptureMode = CaptureMode.STANDARD,
                                    availableCaptureModes = listOf(
                                        SingleSelectableUiState.SelectableUi(CaptureMode.STANDARD),
                                        SingleSelectableUiState.SelectableUi(
                                            CaptureMode.VIDEO_ONLY
                                        ),
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
                                    selectedDynamicRange = DynamicRange.SDR
                                )
                            ),
                            onNavigateToSettings = {},
                            quickSettingsController = NoOpQuickSettingsController()
                        )
                    },
                    debugOverlay = {},
                    debugVisibilityWrapper = { it() },
                    screenFlashOverlay = {},
                    snackBar = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DRAG_HANDLE).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun previewLayout_errorDialogAndSnackbar_meetAccessibilityChecks() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberStandardBottomSheetState(
                        initialValue = SheetValue.Hidden,
                        skipHiddenState = false
                    )
                )
                LaunchedEffect(Unit) {
                    scaffoldState.snackbarHostState.showSnackbar(
                        CustomSnackbarVisuals(
                            message = "Image capture failed",
                            actionLabel = "Retry",
                            withDismissAction = true,
                            duration = SnackbarDuration.Indefinite,
                            isError = true
                        )
                    )
                }
                PreviewLayout(
                    scaffoldState = scaffoldState,
                    viewfinder = { modifier -> Box(modifier.fillMaxSize()) },
                    captureButton = {},
                    imageWell = {},
                    flipCameraButton = {},
                    zoomLevelDisplay = {},
                    elapsedTimeDisplay = {},
                    quickSettingsButton = {},
                    indicatorRow = {},
                    captureModeToggle = {},
                    quickSettingsOverlay = {},
                    debugOverlay = {},
                    debugVisibilityWrapper = { it() },
                    screenFlashOverlay = {},
                    snackBar = { _, _ -> },
                    errorDialog = { modifier ->
                        CameraErrorDialog(
                            modifier = modifier,
                            title = "Can't open camera",
                            body = "Try restarting your device.",
                            confirmButtonText = "OK",
                            onConfirm = {}
                        )
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_CONFIRM_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}
