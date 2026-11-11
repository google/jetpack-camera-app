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

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.core.graphics.createBitmap
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.jetpackcamera.core.camera.AudioStreamState
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DEFAULT_HDR_DYNAMIC_RANGE
import com.google.jetpackcamera.model.DEFAULT_HDR_IMAGE_OUTPUT
import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.MediaDescriptor
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutMode
import com.google.jetpackcamera.ui.components.capture.layout.LocalCameraLayoutMode
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.FlashModeIndicator
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.HdrIndicator
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.ToggleQuickSettingsButton
import com.google.jetpackcamera.ui.controller.testing.FakeQuickSettingsController
import com.google.jetpackcamera.ui.uistate.CustomSnackbarVisuals
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.AudioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.ElapsedTimeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState
import com.google.jetpackcamera.ui.uistate.capture.StabilizationUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric Compose UI tests verifying content gravity rotation:
 * - In-place glyph rotation (`Modifier.animateToUpright()`) on `1:1` controls and indicator icons.
 * - Single (`90°`, non-double-rotated) upright rotation for [CameraControlIconButton] placed in
 *   `topStartContent` of [CompactPortraitLayout].
 * - Swapped-constraint rectangular block rotation (`Modifier.rotateLayoutToGravity()`) on
 *   in-hierarchy [CameraErrorDialog], landscape `PillSnackbarHost` + `ElapsedTimeText` top-edge
 *   banner, and compact [CaptureScaffoldDefaults.QuickSettings] bottom sheet.
 * - Zero (`0°`) gravity rotation enforcement in [FoldableLayout], [TabletPortraitLayout],
 *   [TabletLandscapeLayout], and [CompactShortLayout].
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1600dp-h1400dp-mdpi")
class ContentGravityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun unclippedBoundsOf(tag: String): DpRect =
        composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun clippedBoundsOf(tag: String, useUnmergedTree: Boolean = false): DpRect =
        composeTestRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree).getBoundsInRoot()

    private fun sampleQuickSettingsUiState(): QuickSettingsUiState.Available =
        QuickSettingsUiState.Available(
            aspectRatioUiState = AspectRatioUiState.Available(
                selectedAspectRatio = AspectRatio.THREE_FOUR,
                availableAspectRatios = listOf(
                    SingleSelectableUiState.SelectableUi(AspectRatio.THREE_FOUR),
                    SingleSelectableUiState.SelectableUi(AspectRatio.ONE_ONE),
                    SingleSelectableUiState.SelectableUi(AspectRatio.NINE_SIXTEEN)
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
                selectedDynamicRange = DEFAULT_HDR_DYNAMIC_RANGE,
                selectedImageFormat = DEFAULT_HDR_IMAGE_OUTPUT,
                isSupported = true
            )
        )

    @Test
    fun squareControls_inCompactPortraitLandscape_preserveBoundsRotatingInPlace() {
        val gravityState = mutableFloatStateOf(0f)
        val measuredSlotSizes = mutableMapOf<String, IntSize>()
        val thumbnailBitmap = createBitmap(64, 64)
        val pauseSlotTag = "slot_pause_resume"
        val controlIconTag = "slot_control_icon"

        composeTestRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    LocalDisableAnimations provides true,
                    LocalGravityRotationState provides gravityState
                ) {
                    Column {
                        Row {
                            Box(Modifier.onSizeChanged { measuredSlotSizes["flip"] = it }) {
                                FlipCameraButton(
                                    modifier = Modifier.testTag(FLIP_CAMERA_BUTTON),
                                    flipLensUiState = FlipLensUiState.Available(
                                        selectedLensFacing = LensFacing.BACK,
                                        availableLensFacings = listOf(
                                            SingleSelectableUiState.SelectableUi(LensFacing.BACK),
                                            SingleSelectableUiState.SelectableUi(LensFacing.FRONT)
                                        )
                                    ),
                                    onClick = {}
                                )
                            }
                            Box(
                                Modifier.onSizeChanged { measuredSlotSizes["quickSettings"] = it }
                            ) {
                                ToggleQuickSettingsButton(
                                    isOpen = false,
                                    onClick = {}
                                )
                            }
                            Box(Modifier.onSizeChanged { measuredSlotSizes["imageWell"] = it }) {
                                ImageWell(
                                    imageWellUiState = ImageWellUiState.Content(
                                        mediaDescriptor = MediaDescriptor.Content.Image(
                                            uri = Uri.EMPTY,
                                            thumbnail = thumbnailBitmap
                                        )
                                    ),
                                    onClick = {}
                                )
                            }
                            Box(Modifier.onSizeChanged { measuredSlotSizes["pause"] = it }) {
                                PauseResumeToggleButton(
                                    modifier = Modifier.testTag(pauseSlotTag),
                                    onSetPause = {},
                                    currentRecordingStateProvider = {
                                        VideoRecordingState.Active.Recording(
                                            maxDurationMillis = 0L,
                                            audioStreamState = AudioStreamState.Disabled,
                                            elapsedTimeNanos = 1_000_000_000L
                                        )
                                    }
                                )
                            }
                            Box(Modifier.onSizeChanged { measuredSlotSizes["audio"] = it }) {
                                AmplitudeToggleButton(
                                    audioUiState = AudioUiState.Enabled.On(
                                        amplitude = 0.5,
                                        isAudioStreamActive = true
                                    ),
                                    onToggleAudio = {}
                                )
                            }
                            Box(Modifier.onSizeChanged { measuredSlotSizes["controlIcon"] = it }) {
                                CameraControlIconButton(
                                    onClick = {},
                                    modifier = Modifier.testTag(controlIconTag)
                                ) {
                                    Box(Modifier.size(20.dp))
                                }
                            }
                        }
                        Box(Modifier.onSizeChanged { measuredSlotSizes["indicatorPill"] = it }) {
                            IndicatorPill {
                                HdrIndicator(
                                    hdrUiState = HdrUiState.Available(
                                        selectedDynamicRange = DEFAULT_HDR_DYNAMIC_RANGE,
                                        selectedImageFormat = DEFAULT_HDR_IMAGE_OUTPUT,
                                        isSupported = true
                                    )
                                )
                                FlashModeIndicator(
                                    flashModeUiState = FlashModeUiState.Available(
                                        selectedFlashMode = FlashMode.ON,
                                        availableFlashModes = listOf(
                                            SingleSelectableUiState.SelectableUi(FlashMode.OFF),
                                            SingleSelectableUiState.SelectableUi(FlashMode.ON)
                                        ),
                                        isLowLightBoostActive = false
                                    )
                                )
                                StabilizationIcon(
                                    stabilizationUiState = StabilizationUiState.Specific(
                                        stabilizationMode = StabilizationMode.ON,
                                        active = true
                                    )
                                )
                                VideoQualityIcon(
                                    videoQuality = VideoQuality.FHD,
                                    modifier = Modifier.testTag(VIDEO_QUALITY_TAG)
                                )
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        val baselineMeasuredSizes = measuredSlotSizes.toMap()
        val baselineQuickSettingsSize =
            composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN).fetchSemanticsNode().size
        val baselineQuickSettingsBounds = clippedBoundsOf(QUICK_SETTINGS_DROP_DOWN)
        val squareTags = listOf(
            FLIP_CAMERA_BUTTON,
            IMAGE_WELL_TAG,
            pauseSlotTag,
            AUDIO_INPUT_TOGGLE,
            controlIconTag
        )
        val baselineSquareBounds = squareTags.associateWith { clippedBoundsOf(it) }
        val baselineVideoQualityBounds = clippedBoundsOf(VIDEO_QUALITY_TAG)

        squareTags.forEach { tag ->
            val bounds = baselineSquareBounds.getValue(tag)
            assertWithMessage("$tag should be square at 0f")
                .that(bounds.width.value)
                .isWithin(TOLERANCE)
                .of(bounds.height.value)
        }

        for (angle in listOf(90f, -90f)) {
            gravityState.floatValue = angle
            composeTestRule.waitForIdle()

            // Layout pass sizes of every slot remain 100% invariant (zero sibling shift).
            assertThat(measuredSlotSizes).isEqualTo(baselineMeasuredSizes)
            assertThat(
                composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN).fetchSemanticsNode().size
            ).isEqualTo(baselineQuickSettingsSize)

            // 1:1 square controls preserve identical square bounds in root across ±90f.
            squareTags.forEach { tag ->
                val rotatedBounds = clippedBoundsOf(tag)
                val initialBounds = baselineSquareBounds.getValue(tag)
                assertWithMessage("$tag width at $angle")
                    .that(rotatedBounds.width.value)
                    .isWithin(TOLERANCE)
                    .of(initialBounds.width.value)
                assertWithMessage("$tag height at $angle")
                    .that(rotatedBounds.height.value)
                    .isWithin(TOLERANCE)
                    .of(initialBounds.height.value)
            }

            // ToggleQuickSettingsButton (48x56dp narrow container) keeps its 48x56dp layout slot
            // while its layer rotates in-place to 56x48dp in root at ±90f.
            val rotatedQsBounds = clippedBoundsOf(QUICK_SETTINGS_DROP_DOWN)
            assertThat(rotatedQsBounds.width.value)
                .isWithin(TOLERANCE)
                .of(baselineQuickSettingsBounds.height.value)
            assertThat(rotatedQsBounds.height.value)
                .isWithin(TOLERANCE)
                .of(baselineQuickSettingsBounds.width.value)

            // IndicatorPill segment bounds remain completely unchanged while inner glyphs rotate.
            val rotatedVqBounds = clippedBoundsOf(VIDEO_QUALITY_TAG)
            assertThat(rotatedVqBounds.width.value)
                .isWithin(TOLERANCE)
                .of(baselineVideoQualityBounds.width.value)
            assertThat(rotatedVqBounds.height.value)
                .isWithin(TOLERANCE)
                .of(baselineVideoQualityBounds.height.value)
        }
    }

    @Test
    fun topStartCameraControlIconButton_inCompactPortraitLandscape_rotates90DegreesNot180() {
        val buttonTag = "top_start_control_btn"
        val topMarkerTag = "top_start_control_marker"
        var deviceRotation by mutableStateOf(DeviceRotation.Natural)

        composeTestRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    LocalDisableAnimations provides true
                ) {
                    Box(modifier = Modifier.size(width = 412.dp, height = 915.dp)) {
                        CompactPortraitLayout(
                            slots = CaptureSlots(
                                topStartContent = { mod ->
                                    CameraControlIconButton(
                                        onClick = {},
                                        modifier = mod.testTag(buttonTag)
                                    ) {
                                        Box(Modifier.fillMaxSize()) {
                                            Box(
                                                Modifier
                                                    .align(Alignment.TopCenter)
                                                    .size(width = 16.dp, height = 4.dp)
                                                    .testTag(topMarkerTag)
                                            )
                                        }
                                    }
                                }
                            ),
                            viewfinder = { mod -> Box(mod) },
                            deviceRotation = deviceRotation,
                            windowRotation = DeviceRotation.Natural
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        val portraitBtn = clippedBoundsOf(buttonTag)
        val portraitMarker = clippedBoundsOf(topMarkerTag, useUnmergedTree = true)
        val portraitBtnCenterY = (portraitBtn.top.value + portraitBtn.bottom.value) / 2f
        val portraitMarkerCenterY = (portraitMarker.top.value + portraitMarker.bottom.value) / 2f
        assertThat(portraitMarkerCenterY).isLessThan(portraitBtnCenterY - 4f)

        // Rotated270 -> +90° CW gravity: top-aligned marker rotates once (+90° CW) to the right
        // edge of CameraControlIconButton, NOT twice (+180°) to the bottom edge.
        deviceRotation = DeviceRotation.Rotated270
        composeTestRule.waitForIdle()

        val landscapeBtn = clippedBoundsOf(buttonTag)
        val landscapeMarker = clippedBoundsOf(topMarkerTag, useUnmergedTree = true)
        val landscapeBtnCenterX = (landscapeBtn.left.value + landscapeBtn.right.value) / 2f
        val landscapeBtnCenterY = (landscapeBtn.top.value + landscapeBtn.bottom.value) / 2f
        val landscapeMarkerCenterX = (landscapeMarker.left.value + landscapeMarker.right.value) / 2f
        val landscapeMarkerCenterY = (landscapeMarker.top.value + landscapeMarker.bottom.value) / 2f
        assertThat(landscapeMarkerCenterX).isGreaterThan(landscapeBtnCenterX + 4f)
        assertThat(landscapeMarkerCenterY).isWithin(TOLERANCE).of(landscapeBtnCenterY)
    }

    @Test
    fun rectangularBlocks_inCompactPortraitLandscape_swapConstraintsWithoutClipping() {
        // CameraErrorDialog in-hierarchy Surface inside a 360x740dp portrait container.
        val dialogGravity = mutableFloatStateOf(0f)
        var confirmClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    LocalDisableAnimations provides true,
                    LocalGravityRotationState provides dialogGravity
                ) {
                    Box(modifier = Modifier.size(width = 360.dp, height = 740.dp)) {
                        CameraErrorDialog(
                            title = "Camera unavailable",
                            body = "Close other camera apps and try again.",
                            confirmButtonText = "OK",
                            onConfirm = { confirmClicked = true }
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_TAG)
            .assertIsDisplayed()
            .assert(!isDialog())
            .assert(!isPopup())
            .assert(!hasAnyAncestor(isDialog()))
            .assert(!hasAnyAncestor(isPopup()))

        // At 90°, Surface swaps minWidth = 280.dp into vertical portrait height >= 280.dp while
        // fitting cleanly within the 360.dp width without clipping.
        dialogGravity.floatValue = 90f
        composeTestRule.waitForIdle()

        val titleLocalSize =
            composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_TITLE_TAG).fetchSemanticsNode().size
        val titleClipped = clippedBoundsOf(CAMERA_ERROR_DIALOG_TITLE_TAG)
        val bodyLocalSize =
            composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_BODY_TAG).fetchSemanticsNode().size
        val bodyClipped = clippedBoundsOf(CAMERA_ERROR_DIALOG_BODY_TAG)
        val buttonLocalSize =
            composeTestRule
                .onNodeWithTag(CAMERA_ERROR_DIALOG_CONFIRM_BUTTON_TAG)
                .fetchSemanticsNode()
                .size
        val buttonClipped = clippedBoundsOf(CAMERA_ERROR_DIALOG_CONFIRM_BUTTON_TAG)

        // Rotated 90° CW: clipped root width equals unrotated local height and clipped root height
        // equals unrotated local width (proving zero clipping after constraint swap).
        assertThat(titleClipped.width.value).isWithin(TOLERANCE).of(titleLocalSize.height.toFloat())
        assertThat(titleClipped.height.value).isWithin(TOLERANCE).of(titleLocalSize.width.toFloat())
        assertThat(bodyClipped.width.value).isWithin(TOLERANCE).of(bodyLocalSize.height.toFloat())
        assertThat(bodyClipped.height.value).isWithin(TOLERANCE).of(bodyLocalSize.width.toFloat())
        assertThat(buttonClipped.width.value)
            .isWithin(TOLERANCE)
            .of(buttonLocalSize.height.toFloat())
        assertThat(buttonClipped.height.value)
            .isWithin(TOLERANCE)
            .of(buttonLocalSize.width.toFloat())

        // Body spans >= 232.dp vertically (280.dp min card width - 48.dp card content padding),
        // and confirm button hit target works cleanly in rotated coordinates.
        assertThat(bodyClipped.height.value).isAtLeast(232f - TOLERANCE)
        assertThat(titleClipped.left.value).isAtLeast(0f)
        assertThat(titleClipped.right.value).isAtMost(360f)
        composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_CONFIRM_BUTTON_TAG).performClick()
        assertThat(confirmClicked).isTrue()
    }

    @Test
    fun landscapeTopEdgeBanner_inCompactPortrait_anchorsToTopEdgeInLtrAndRtl() {
        var activeRotation by mutableStateOf(DeviceRotation.Rotated270)
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)

        composeTestRule.setContent {
            val state = rememberCaptureScaffoldState()
            LaunchedEffect(state.snackbarHostState) {
                state.snackbarHostState.showSnackbar(
                    CustomSnackbarVisuals(
                        message = "Storage nearly full",
                        actionLabel = null,
                        withDismissAction = false,
                        duration = SnackbarDuration.Indefinite,
                        isError = false
                    )
                )
            }
            MaterialTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    LocalDisableAnimations provides true,
                    LocalLayoutDirection provides layoutDirection
                ) {
                    Box(modifier = Modifier.size(width = 740.dp, height = 360.dp)) {
                        CompactPortraitLayout(
                            slots = CaptureSlots(
                                topBar = { mod -> Box(mod.size(width = 120.dp, height = 36.dp)) },
                                zoomControls = { mod ->
                                    Box(mod.size(width = 160.dp, height = 48.dp))
                                },
                                quickSettingsButton = { mod -> Box(mod.size(48.dp)) },
                                captureButton = { mod -> Box(mod.size(80.dp)) },
                                flipCameraButton = { mod -> Box(mod.size(48.dp)) },
                                imageWell = { mod -> Box(mod.size(48.dp)) },
                                captureModeToggle = { mod ->
                                    Box(mod.size(width = 96.dp, height = 48.dp))
                                },
                                elapsedTimeDisplay = { mod ->
                                    ElapsedTimeText(
                                        modifier = mod,
                                        elapsedTimeUiStateProvider = {
                                            ElapsedTimeUiState.Enabled(
                                                elapsedTimeNanos = 15_000_000_000L
                                            )
                                        }
                                    )
                                }
                            ),
                            viewfinder = { mod -> Box(mod) },
                            state = state,
                            deviceRotation = activeRotation,
                            windowRotation = activeRotation
                        )
                    }
                }
            }
        }

        for (dir in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            for (rotation in listOf(DeviceRotation.Rotated270, DeviceRotation.Rotated90)) {
                layoutDirection = dir
                activeRotation = rotation
                composeTestRule.waitForIdle()

                val snackbarClipped = clippedBoundsOf(SNACKBAR_NODE_TAG)
                val snackbarUnclipped = unclippedBoundsOf(SNACKBAR_NODE_TAG)
                val elapsedClipped = clippedBoundsOf(ELAPSED_TIME_TAG)
                val elapsedUnclipped = unclippedBoundsOf(ELAPSED_TIME_TAG)

                // Banner sits at the physical top edge (y near 0.dp) of the 740x360dp landscape
                // window with PillSnackbarHost above ElapsedTimeText and zero clipping.
                assertWithMessage("Snackbar top at $rotation / $dir")
                    .that(snackbarUnclipped.top.value)
                    .isWithin(TOLERANCE)
                    .of(0f)
                assertWithMessage("Snackbar horizontal orientation at $rotation / $dir")
                    .that(snackbarUnclipped.width.value)
                    .isGreaterThan(snackbarUnclipped.height.value)
                assertWithMessage("ElapsedTime horizontal orientation at $rotation / $dir")
                    .that(elapsedUnclipped.width.value)
                    .isGreaterThan(elapsedUnclipped.height.value)
                assertWithMessage("Snackbar sits above ElapsedTime at $rotation / $dir")
                    .that(snackbarUnclipped.bottom.value)
                    .isAtMost(elapsedUnclipped.top.value + TOLERANCE)
                assertThat(snackbarClipped.width.value)
                    .isWithin(TOLERANCE)
                    .of(snackbarUnclipped.width.value)
                assertThat(elapsedClipped.width.value)
                    .isWithin(TOLERANCE)
                    .of(elapsedUnclipped.width.value)
            }
        }
    }

    @Test
    fun quickSettingsSheetContent_inLandscapeGravity_swapsConstraintsViaRotateLayoutToGravity() {
        var deviceRotation by mutableStateOf(DeviceRotation.Natural)
        val fakeController = FakeQuickSettingsController()
        var selectedFlash: FlashMode? = null
        fakeController.setFlashAction = { selectedFlash = it }

        composeTestRule.setContent {
            val state = remember { CaptureScaffoldState(isInitiallyExpanded = true) }
            MaterialTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    LocalDisableAnimations provides true
                ) {
                    Box(modifier = Modifier.size(width = 412.dp, height = 915.dp)) {
                        CompactPortraitLayout(
                            slots = CaptureSlots(
                                topBar = { mod -> Box(mod.size(width = 120.dp, height = 36.dp)) },
                                zoomControls = { mod ->
                                    Box(mod.size(width = 160.dp, height = 48.dp))
                                },
                                quickSettingsButton = { mod -> Box(mod.size(48.dp)) },
                                captureButton = { mod -> Box(mod.size(80.dp)) },
                                flipCameraButton = { mod -> Box(mod.size(48.dp)) },
                                imageWell = { mod -> Box(mod.size(48.dp)) },
                                captureModeToggle = { mod ->
                                    Box(mod.size(width = 96.dp, height = 48.dp))
                                },
                                quickSettingsOverlay = { mod ->
                                    CaptureScaffoldDefaults.QuickSettings(
                                        modifier = mod,
                                        uiState = sampleQuickSettingsUiState(),
                                        controller = fakeController,
                                        onNavigateToSettings = {}
                                    )
                                }
                            ),
                            viewfinder = { mod -> Box(mod) },
                            state = state,
                            deviceRotation = deviceRotation,
                            windowRotation = DeviceRotation.Natural
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET)
            .assertIsDisplayed()
            .assert(!isDialog())
            .assert(!isPopup())
        val portraitSheetBounds = clippedBoundsOf(QUICK_SETTINGS_BOTTOM_SHEET)
        assertThat(portraitSheetBounds.width.value).isWithin(TOLERANCE).of(412f)

        // Rotate device to landscape (Rotated270 -> +90° gravity): sheet swaps constraints so its
        // local width is capped at 380.dp, while rotated bounds in root fit within 412x380dp.
        deviceRotation = DeviceRotation.Rotated270
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertIsDisplayed()
        val landscapeSheetLocalSize =
            composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).fetchSemanticsNode().size
        val landscapeSheetBounds = clippedBoundsOf(QUICK_SETTINGS_BOTTOM_SHEET)
        assertThat(landscapeSheetLocalSize.width).isAtMost(380)
        assertThat(landscapeSheetBounds.width.value).isAtMost(412f + TOLERANCE)
        assertThat(landscapeSheetBounds.height.value).isAtMost(380f + TOLERANCE)

        // Flash row inside +90° rotated sheet runs vertically along the portrait root (upright for
        // the landscape user) and remains clickable.
        val flashRowBounds = clippedBoundsOf(ROW_QUICK_SETTINGS_FLASH)
        assertThat(flashRowBounds.height.value).isGreaterThan(flashRowBounds.width.value)
        composeTestRule.onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_ON).performClick()
        assertThat(selectedFlash).isEqualTo(FlashMode.ON)
    }

    @Test
    fun sideRailAndCompactShortLayouts_inLandscape_keepZeroGravityRotation() {
        val postures = listOf(
            "FoldableLayout" to DpSize(852.dp, 883.dp),
            "TabletPortraitLayout" to DpSize(800.dp, 1280.dp),
            "TabletLandscapeLayout" to DpSize(1280.dp, 800.dp),
            "CompactShortLayout" to DpSize(360.dp, 360.dp)
        )
        var activePostureIndex by mutableStateOf(0)
        var showErrorDialog by mutableStateOf(true)
        var observedRailGravity: Float? = null
        val fakeController = FakeQuickSettingsController()
        val state = CaptureScaffoldState(isInitiallyExpanded = false)

        @Composable
        fun buildRailSlots(isCompactShort: Boolean): CaptureSlots = PlaceholderCaptureSlots
            .sideRail(
                isCompactHeight = isCompactShort,
                includeElapsedTime = true
            )
            .copy(
                quickSettingsOverlay = { mod ->
                    CaptureScaffoldDefaults.QuickSettings(
                        modifier = mod,
                        uiState = sampleQuickSettingsUiState(),
                        controller = fakeController,
                        onNavigateToSettings = {}
                    )
                },
                errorDialog = { mod ->
                    if (showErrorDialog) {
                        CameraErrorDialog(
                            modifier = mod,
                            title = "Camera error",
                            body = "Try again",
                            confirmButtonText = "OK",
                            onConfirm = {}
                        )
                    }
                }
            )

        composeTestRule.setContent {
            val (layoutName, containerSize) = postures[activePostureIndex]
            val ambientGravity = remember { mutableFloatStateOf(90f) }
            MaterialTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    LocalDisableAnimations provides true,
                    LocalDeviceRotation provides DeviceRotation.Rotated270,
                    LocalWindowRotation provides DeviceRotation.Rotated270,
                    LocalGravityRotationState provides ambientGravity
                ) {
                    Box(modifier = Modifier.size(containerSize)) {
                        val slots = buildRailSlots(layoutName == "CompactShortLayout")
                        val vf: @Composable (Modifier) -> Unit = { mod ->
                            observedRailGravity = LocalGravityRotationState.current.value
                            PlaceholderViewfinder(modifier = mod)
                        }
                        when (layoutName) {
                            "FoldableLayout" -> FoldableLayout(
                                slots = slots,
                                viewfinder = vf,
                                state = state,
                                isErrorDialogShowing = showErrorDialog
                            )
                            "TabletPortraitLayout" -> TabletPortraitLayout(
                                slots = slots,
                                viewfinder = vf,
                                state = state,
                                isErrorDialogShowing = showErrorDialog
                            )
                            "TabletLandscapeLayout" -> TabletLandscapeLayout(
                                slots = slots,
                                viewfinder = vf,
                                state = state,
                                isErrorDialogShowing = showErrorDialog
                            )
                            else -> CompactShortLayout(
                                slots = slots,
                                viewfinder = vf,
                                state = state,
                                isErrorDialogShowing = showErrorDialog
                            )
                        }
                    }
                }
            }
        }

        postures.forEachIndexed { index, (layoutName, _) ->
            activePostureIndex = index
            showErrorDialog = true
            composeTestRule.runOnIdle { state.dismissQuickSettings() }
            composeTestRule.waitForIdle()

            // Even with ambient 90f gravity and Rotated270 device/window rotations, all rail
            // layouts enforce 0f gravity rotation.
            assertWithMessage("$layoutName gravity").that(observedRailGravity).isEqualTo(0f)

            val titleBounds = clippedBoundsOf(CAMERA_ERROR_DIALOG_TITLE_TAG)
            val bodyBounds = clippedBoundsOf(CAMERA_ERROR_DIALOG_BODY_TAG)
            val confirmBounds = clippedBoundsOf(CAMERA_ERROR_DIALOG_CONFIRM_BUTTON_TAG)
            assertThat(titleBounds.top.value).isLessThan(confirmBounds.top.value)
            assertThat(bodyBounds.width.value).isGreaterThan(bodyBounds.height.value)

            // Open Quick Settings floating card and verify it also remains unrotated (0°).
            showErrorDialog = false
            composeTestRule.runOnIdle { state.showQuickSettings() }
            composeTestRule.waitForIdle()

            composeTestRule
                .onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_PLACEHOLDER_TAG)
                .assertIsDisplayed()
            composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertIsDisplayed()
            val flashRowBounds = clippedBoundsOf(ROW_QUICK_SETTINGS_FLASH)
            val captureModeRowBounds = clippedBoundsOf(ROW_QUICK_SETTINGS_CAPTURE_MODE)
            assertThat(flashRowBounds.top.value).isLessThan(captureModeRowBounds.top.value)
            assertThat(flashRowBounds.width.value).isGreaterThan(flashRowBounds.height.value)
            composeTestRule.onNodeWithTag(SETTINGS_BUTTON).performScrollTo().assertIsDisplayed()
        }
    }

    @Config(qualifiers = "sw800dp-w1400dp-h1000dp-mdpi")
    @Test
    fun compactHorizontalSplitScreen_rotatesLayout90DegreesAndKeepsOverlaysUpright() {
        var observedMode: CameraLayoutMode? = null
        var observedActiveLayout: String? = null
        var observedInteriorGravity: Float? = null

        composeTestRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    LocalDisableAnimations provides true
                ) {
                    CaptureScaffoldLayout(
                        modifier = Modifier.size(width = 800.dp, height = 400.dp),
                        deviceRotation = DeviceRotation.Natural,
                        windowRotation = DeviceRotation.Natural,
                        viewfinder = { mod ->
                            observedMode = LocalCameraLayoutMode.current
                            observedActiveLayout = LocalActiveCaptureLayout.current
                            observedInteriorGravity = LocalGravityRotationState.current.value
                            PlaceholderViewfinder(modifier = mod)
                        },
                        captureButton = { mod ->
                            Box(mod.size(80.dp).testTag(CAPTURE_BUTTON))
                        },
                        imageWell = { mod ->
                            Box(mod.size(48.dp).testTag(IMAGE_WELL_TAG))
                        },
                        flipCameraButton = { mod ->
                            Box(mod.size(48.dp).testTag(FLIP_CAMERA_BUTTON))
                        },
                        zoomLevelDisplay = { mod ->
                            Box(
                                mod.size(
                                    width = 160.dp,
                                    height = 48.dp
                                ).testTag(ZOOM_BUTTON_ROW_TAG)
                            )
                        },
                        elapsedTimeDisplay = { mod ->
                            ElapsedTimeText(
                                modifier = mod,
                                elapsedTimeUiStateProvider = {
                                    ElapsedTimeUiState.Enabled(elapsedTimeNanos = 0L)
                                }
                            )
                        },
                        quickSettingsButton = { mod ->
                            Box(mod.size(48.dp).testTag(QUICK_SETTINGS_DROP_DOWN))
                        },
                        indicatorRow = {},
                        captureModeToggle = { mod ->
                            Box(mod.size(width = 96.dp, height = 48.dp))
                        },
                        quickSettingsOverlay = {},
                        overlay = {},
                        controlsVisible = true,
                        screenFlashOverlay = {},
                        snackBar = { _, _ -> },
                        errorDialog = { mod ->
                            CameraErrorDialog(
                                modifier = mod,
                                title = "Camera error",
                                body = "Try again",
                                confirmButtonText = "OK",
                                onConfirm = {}
                            )
                        }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        assertThat(observedMode).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(observedActiveLayout).isEqualTo("CompactPortraitLayout")
        assertThat(((observedInteriorGravity!! % 360f) + 360f) % 360f).isEqualTo(90f)

        // Inside the -90° CCW rotated CompactPortraitLayout, +90° CW interior gravity brings
        // rectangular overlays back to 0° upright in the 800x400dp screen window.
        val elapsedBounds = clippedBoundsOf(ELAPSED_TIME_TAG)
        assertThat(elapsedBounds.width.value).isGreaterThan(elapsedBounds.height.value)

        val titleBounds = clippedBoundsOf(CAMERA_ERROR_DIALOG_TITLE_TAG)
        val bodyBounds = clippedBoundsOf(CAMERA_ERROR_DIALOG_BODY_TAG)
        val confirmBounds = clippedBoundsOf(CAMERA_ERROR_DIALOG_CONFIRM_BUTTON_TAG)
        assertThat(titleBounds.top.value).isLessThan(confirmBounds.top.value)
        assertThat(bodyBounds.width.value).isGreaterThan(bodyBounds.height.value)
    }

    private companion object {
        const val TOLERANCE = 0.5f
    }
}
