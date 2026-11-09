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
import android.net.Uri
import android.util.Range
import android.view.Surface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.AudioStreamState
import com.google.jetpackcamera.core.camera.InitialRecordingSettings
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.model.CameraError
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LensToZoom
import com.google.jetpackcamera.model.MediaDescriptor
import com.google.jetpackcamera.ui.controller.testing.FakeCameraController
import com.google.jetpackcamera.ui.controller.testing.FakeCaptureController
import com.google.jetpackcamera.ui.controller.testing.FakeQuickSettingsController
import com.google.jetpackcamera.ui.controller.testing.FakeSnackBarController
import com.google.jetpackcamera.ui.controller.testing.FakeZoomController
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.SnackBarUiState
import com.google.jetpackcamera.ui.uistate.SnackbarData
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.CameraErrorUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.ElapsedTimeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import java.util.LinkedList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDisplay

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class CaptureScaffoldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val availableQuickSettingsUiState = QuickSettingsUiState.Available(
        aspectRatioUiState = AspectRatioUiState.Unavailable,
        captureModeUiState = CaptureModeUiState.Unavailable,
        flashModeUiState = FlashModeUiState.Unavailable,
        flipLensUiState = FlipLensUiState.Unavailable,
        hdrUiState = HdrUiState.Unavailable
    )

    @Test
    fun captureScaffold_updatesCaptureButtonReactivelyWhenUiStateChanges() {
        var captureUiState by mutableStateOf(LOADING_CAPTURE_UI_STATE_SKELETON)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                CaptureScaffold(
                    captureUiState = { captureUiState },
                    surfaceRequest = null
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertIsNotEnabled()

        composeTestRule.runOnIdle {
            captureUiState = captureUiState.copy(
                captureButtonUiState = CaptureButtonUiState.Enabled.Idle(
                    captureMode = CaptureMode.STANDARD,
                    isEnabled = true
                )
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertIsEnabled()
    }

    @Test
    fun captureScaffold_hidesCaptureModeToggleAndQuickSettingsWhenRecordingActive() {
        var captureUiState by mutableStateOf(
            LOADING_CAPTURE_UI_STATE_SKELETON.copy(
                captureModeToggleUiState = CaptureModeToggleUiState.Available(
                    selectedCaptureMode = CaptureMode.IMAGE_ONLY,
                    imageOnlyUiState = SingleSelectableUiState.SelectableUi(CaptureMode.IMAGE_ONLY),
                    videoOnlyUiState = SingleSelectableUiState.SelectableUi(CaptureMode.VIDEO_ONLY)
                ),
                quickSettingsUiState = availableQuickSettingsUiState
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                CaptureScaffold(
                    captureUiState = { captureUiState },
                    surfaceRequest = null,
                    topStartContent = { slotModifier ->
                        if (!isRecording) {
                            Box(slotModifier.size(40.dp).testTag(TOP_START_BUTTON_TAG))
                        }
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TOP_START_BUTTON_TAG).assertIsDisplayed()

        composeTestRule.runOnIdle {
            captureUiState = captureUiState.copy(
                videoRecordingState = VideoRecordingState.Active.Recording(
                    maxDurationMillis = 0L,
                    audioStreamState = AudioStreamState.Active(0.0),
                    elapsedTimeNanos = 1_000_000_000L
                ),
                elapsedTimeUiState = ElapsedTimeUiState.Enabled(1_000_000_000L)
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).assertDoesNotExist()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TOP_START_BUTTON_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG).assertIsDisplayed()
    }

    @Test
    fun captureScaffold_rendersComponentsAndSupportsCustomQuickSettingsSlot() {
        val quickSettingsController = FakeQuickSettingsController()
        val uiState = LOADING_CAPTURE_UI_STATE_SKELETON.copy(
            flipLensUiState = FlipLensUiState.Available(
                selectedLensFacing = LensFacing.BACK,
                availableLensFacings = listOf(
                    SingleSelectableUiState.SelectableUi(LensFacing.BACK),
                    SingleSelectableUiState.SelectableUi(LensFacing.FRONT)
                )
            ),
            quickSettingsUiState = availableQuickSettingsUiState
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val scaffoldState = rememberCaptureScaffoldState(
                    isInitiallyExpanded = true
                )
                CaptureScaffold(
                    captureUiState = { uiState },
                    surfaceRequest = null,
                    state = scaffoldState,
                    quickSettingsController = quickSettingsController,
                    quickSettings = { slotModifier ->
                        CaptureScaffoldDefaults.QuickSettings(
                            uiState = uiState.quickSettingsUiState,
                            controller = quickSettingsController,
                            onNavigateToSettings = {},
                            modifier = slotModifier,
                            showMoreSettingsButton = false
                        )
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SETTINGS_BUTTON).assertDoesNotExist()
    }

    @Test
    fun captureScaffold_rendersTestableSnackbarWhenSnackBarQueueIsNonEmpty() {
        val snackBarController = FakeSnackBarController()
        val snackBarQueue = LinkedList(
            listOf(
                SnackbarData(
                    cookie = "test-cookie",
                    stringResource = R.string.camera_error_dialog_ok,
                    withDismissAction = true
                )
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                CaptureScaffold(
                    captureUiState = { LOADING_CAPTURE_UI_STATE_SKELETON },
                    surfaceRequest = null,
                    snackBarUiState = SnackBarUiState.Enabled(snackBarQueue),
                    snackBarController = snackBarController
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(SNACKBAR_NODE_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    }

    @Test
    fun captureScaffold_omitsImageWellByDefault_andRendersWhenProvided() {
        val thumbnail = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val imageWellContent = ImageWellUiState.Content(
            mediaDescriptor = MediaDescriptor.Content.Image(
                uri = Uri.EMPTY,
                thumbnail = thumbnail
            )
        )
        val uiState = LOADING_CAPTURE_UI_STATE_SKELETON.copy(
            imageWellUiState = imageWellContent
        )
        var showImageWell by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                CaptureScaffold(
                    captureUiState = { uiState },
                    surfaceRequest = null,
                    imageWell = if (showImageWell) {
                        { imageWellModifier ->
                            CaptureScaffoldDefaults.ImageWell(
                                uiState = uiState.imageWellUiState,
                                onClick = {},
                                modifier = imageWellModifier
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(IMAGE_WELL_TAG).assertDoesNotExist()

        composeTestRule.runOnIdle { showImageWell = true }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(IMAGE_WELL_TAG).assertIsDisplayed()
    }

    @Test
    fun captureScaffold_hidesControlsWhenControlsVisibleFalse() {
        var controlsVisible by mutableStateOf(true)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                CaptureScaffold(
                    captureUiState = { LOADING_CAPTURE_UI_STATE_SKELETON },
                    surfaceRequest = null,
                    controlsVisible = controlsVisible
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).assertIsDisplayed()

        composeTestRule.runOnIdle { controlsVisible = false }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).assertIsDisplayed()
    }

    @Test
    fun captureScaffold_errorDialogBlocksPreviewAndSupportsCustomErrorStrings() {
        var dismissed = false
        val uiState = LOADING_CAPTURE_UI_STATE_SKELETON.copy(
            cameraErrorUiState = CameraErrorUiState.Showing(
                error = CameraError.CameraInUse,
                blocksPreview = true
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                CaptureScaffold(
                    captureUiState = { uiState },
                    surfaceRequest = null,
                    errorDialog = { error, onDismiss ->
                        CaptureScaffoldDefaults.ErrorDialog(
                            error = error,
                            onDismiss = {
                                onDismiss()
                                dismissed = true
                            },
                            strings = {
                                CameraErrorStrings(
                                    title = "Custom Error Title",
                                    body = "Custom Error Body",
                                    confirmButtonText = "Confirm"
                                )
                            }
                        )
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).assertDoesNotExist()
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertDoesNotExist()
        composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_TITLE_TAG)
            .assertTextEquals("Custom Error Title")
        composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_BODY_TAG)
            .assertTextEquals("Custom Error Body")

        composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_CONFIRM_BUTTON_TAG).performClick()
        assertThat(dismissed).isTrue()
    }

    @Test
    fun captureScaffold_whenErrorDialogIsNull_hidesPreviewAndControlsWithoutShowingDialog() {
        val uiState = LOADING_CAPTURE_UI_STATE_SKELETON.copy(
            cameraErrorUiState = CameraErrorUiState.Showing(
                error = CameraError.CameraInUse,
                blocksPreview = true
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                CaptureScaffold(
                    captureUiState = { uiState },
                    surfaceRequest = null,
                    errorDialog = null
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).assertDoesNotExist()
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertDoesNotExist()
        composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_TAG).assertDoesNotExist()
    }

    @Test
    fun captureScaffold_quickSettingsButton_togglesQuickSettingsStateAndBottomSheet() {
        val quickSettingsController = FakeQuickSettingsController()
        val uiState = LOADING_CAPTURE_UI_STATE_SKELETON.copy(
            quickSettingsUiState = availableQuickSettingsUiState
        )
        lateinit var scaffoldState: CaptureScaffoldState

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                scaffoldState = rememberCaptureScaffoldState()
                CaptureScaffold(
                    captureUiState = { uiState },
                    surfaceRequest = null,
                    state = scaffoldState,
                    quickSettingsController = quickSettingsController
                )
            }
        }
        composeTestRule.waitForIdle()

        assertThat(scaffoldState.isQuickSettingsOpen).isFalse()

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN).performClick()
        composeTestRule.waitForIdle()

        assertThat(scaffoldState.isQuickSettingsOpen).isTrue()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertIsDisplayed()

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.waitForIdle()

        assertThat(scaffoldState.isQuickSettingsOpen).isFalse()
    }

    @Test
    fun captureScaffoldState_isModeAgnosticWithoutMountedBottomSheet() {
        val state = CaptureScaffoldState(isInitiallyExpanded = false)
        assertThat(state.isQuickSettingsOpen).isFalse()

        state.showQuickSettings()
        assertThat(state.isQuickSettingsOpen).isTrue()

        state.toggleQuickSettings()
        assertThat(state.isQuickSettingsOpen).isFalse()

        state.toggleQuickSettings()
        assertThat(state.isQuickSettingsOpen).isTrue()

        val restored = CaptureScaffoldState.Saver.restore(true)
        assertThat(restored?.isQuickSettingsOpen).isTrue()

        state.dismissQuickSettings()
        assertThat(state.isQuickSettingsOpen).isFalse()
    }

    @Test
    fun captureScaffold_restoresInitialRecordingSettingsAfterRecordingCompletesAcrossTicks() {
        val restoredAudioEnabled = mutableListOf<Boolean>()
        val restoredLensFacings = mutableListOf<LensFacing>()
        val restoredZooms = mutableMapOf<LensToZoom, Float>()

        val captureController = FakeCaptureController(
            setAudioEnabledAction = { restoredAudioEnabled += it }
        )
        val quickSettingsController = FakeQuickSettingsController(
            setLensFacingAction = { restoredLensFacings += it }
        )
        val zoomController = FakeZoomController(
            setZoomRatioAction = { zoomRatio ->
                restoredZooms[zoomRatio.changeType.lensToZoom] = zoomRatio.changeType.value
            }
        )

        var captureUiState by mutableStateOf(
            LOADING_CAPTURE_UI_STATE_SKELETON.copy(
                zoomUiState = ZoomUiState.Enabled(primaryZoomRange = Range(1f, 5f))
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                CaptureScaffold(
                    captureUiState = { captureUiState },
                    surfaceRequest = null,
                    captureController = captureController,
                    quickSettingsController = quickSettingsController,
                    zoomController = zoomController
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            captureUiState = captureUiState.copy(
                videoRecordingState = VideoRecordingState.Starting(
                    initialRecordingSettings = InitialRecordingSettings(
                        isAudioEnabled = true,
                        lensFacing = LensFacing.FRONT,
                        zoomRatios = mapOf(
                            LensFacing.FRONT to 1.5f,
                            LensFacing.BACK to 2.5f
                        )
                    )
                )
            )
        }
        composeTestRule.waitForIdle()

        // Multiple active recording ticks (elapsed time + audio amplitude updates) must not clear
        // or prematurely restore the saved initial settings.
        composeTestRule.runOnIdle {
            captureUiState = captureUiState.copy(
                videoRecordingState = VideoRecordingState.Active.Recording(
                    maxDurationMillis = 0L,
                    audioStreamState = AudioStreamState.Active(0.2),
                    elapsedTimeNanos = 1_000_000_000L
                )
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            captureUiState = captureUiState.copy(
                videoRecordingState = VideoRecordingState.Active.Recording(
                    maxDurationMillis = 0L,
                    audioStreamState = AudioStreamState.Active(0.8),
                    elapsedTimeNanos = 2_000_000_000L
                )
            )
        }
        composeTestRule.waitForIdle()

        assertThat(restoredAudioEnabled).isEmpty()
        assertThat(restoredLensFacings).isEmpty()
        assertThat(restoredZooms).isEmpty()

        composeTestRule.runOnIdle {
            captureUiState = captureUiState.copy(
                videoRecordingState = VideoRecordingState.Inactive()
            )
        }
        composeTestRule.waitForIdle()

        assertThat(restoredAudioEnabled).containsExactly(true)
        assertThat(restoredLensFacings).containsExactly(LensFacing.FRONT)
        assertThat(restoredZooms[LensToZoom.PRIMARY]).isEqualTo(1.5f)
        assertThat(restoredZooms[LensToZoom.SECONDARY]).isEqualTo(2.5f)
    }

    @Test
    fun cameraSessionEffect_startsAndStopsCameraWhenEnabledChanges() {
        var startCount = 0
        var stopCount = 0
        var sessionStartedCalls = 0
        var sessionStoppedCalls = 0
        val cameraController = FakeCameraController(
            startCameraAction = { startCount++ },
            stopCameraAction = { stopCount++ }
        )
        var enabled by mutableStateOf(false)

        composeTestRule.setContent {
            CameraSessionEffect(
                cameraController = cameraController,
                enabled = enabled,
                onSessionStarted = { sessionStartedCalls++ },
                onSessionStopped = { sessionStoppedCalls++ }
            )
        }
        composeTestRule.waitForIdle()

        assertThat(startCount).isEqualTo(0)
        assertThat(sessionStartedCalls).isEqualTo(0)
        assertThat(stopCount).isEqualTo(0)

        composeTestRule.runOnIdle { enabled = true }
        composeTestRule.waitForIdle()

        assertThat(startCount).isEqualTo(1)
        assertThat(sessionStartedCalls).isEqualTo(1)
        assertThat(stopCount).isEqualTo(0)

        composeTestRule.runOnIdle { enabled = false }
        composeTestRule.waitForIdle()

        assertThat(stopCount).isEqualTo(1)
        assertThat(sessionStoppedCalls).isEqualTo(1)
    }

    @Test
    fun cameraSessionEffect_restartsOnLifecycleStopAndStart_andForwardsRotation() {
        var startCount = 0
        var stopCount = 0
        var sessionStartedCalls = 0
        var sessionStoppedCalls = 0
        val reportedRotations = mutableListOf<DeviceRotation>()
        val cameraController = FakeCameraController(
            startCameraAction = { startCount++ },
            stopCameraAction = { stopCount++ },
            setDisplayRotationAction = { reportedRotations += it }
        )
        val lifecycleOwner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this)
            override val lifecycle: Lifecycle = registry
        }
        lifecycleOwner.registry.currentState = Lifecycle.State.STARTED

        val shadowDisplay = shadowOf(ShadowDisplay.getDefaultDisplay())
        shadowDisplay.setRotation(Surface.ROTATION_0)
        try {
            composeTestRule.setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                    CameraSessionEffect(
                        cameraController = cameraController,
                        enabled = true,
                        onSessionStarted = { sessionStartedCalls++ },
                        onSessionStopped = { sessionStoppedCalls++ }
                    )
                }
            }
            composeTestRule.waitForIdle()

            assertThat(startCount).isEqualTo(1)
            assertThat(sessionStartedCalls).isEqualTo(1)
            assertThat(stopCount).isEqualTo(0)
            assertThat(reportedRotations).containsExactly(DeviceRotation.Natural)

            composeTestRule.runOnIdle {
                lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            }
            composeTestRule.waitForIdle()

            assertThat(stopCount).isEqualTo(1)
            assertThat(sessionStoppedCalls).isEqualTo(1)

            // Rotate the display while stopped; restarting must seed from the new display rotation.
            shadowDisplay.setRotation(Surface.ROTATION_270)
            composeTestRule.runOnIdle {
                lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            }
            composeTestRule.waitForIdle()

            assertThat(startCount).isEqualTo(2)
            assertThat(sessionStartedCalls).isEqualTo(2)
            assertThat(reportedRotations.last()).isEqualTo(DeviceRotation.Rotated90)
        } finally {
            shadowDisplay.setRotation(Surface.ROTATION_0)
        }
    }

    @Test
    fun cameraControlIconButton_rendersAndInvokesOnClick() {
        var clicked = false

        composeTestRule.setContent {
            CameraControlIconButton(
                onClick = { clicked = true },
                modifier = Modifier.testTag(ICON_BUTTON_TAG)
            ) {
                Text("X")
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(ICON_BUTTON_TAG).assertIsDisplayed().performClick()
        assertThat(clicked).isTrue()
    }

    private companion object {
        const val TOP_START_BUTTON_TAG = "test_top_start_button"
        const val ICON_BUTTON_TAG = "test_icon_button"
    }
}
