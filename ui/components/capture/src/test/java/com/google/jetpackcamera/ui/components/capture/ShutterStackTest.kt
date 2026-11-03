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
import android.net.Uri
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.MediaDescriptor
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [ShutterStack] verifying vertical and horizontal sizing, empty-slot zero-gap
 * collapse, posture-switch state preservation, independent per-slot overlap contrast resolution,
 * and RTL behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w600dp-h800dp")
class ShutterStackTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun verticalIdle_standardSpacing_measures86x294AndCentersFlankingSlots() {
        composeTestRule.setContent {
            ShutterStack(
                modifier = Modifier.testTag(STACK_TAG),
                orientation = Orientation.Vertical,
                compact = false,
                captureButton = { modifier ->
                    Box(modifier.size(86.dp).testTag(CAPTURE_TAG))
                },
                flipCameraButton = { modifier ->
                    Box(modifier.size(56.dp).testTag(FLIP_TAG))
                },
                imageWell = { modifier ->
                    Box(modifier.size(56.dp).testTag(WELL_TAG))
                }
            )
        }

        val stackBounds = composeTestRule.onNodeWithTag(STACK_TAG).getUnclippedBoundsInRoot()
        assertThat(stackBounds.width).isEqualTo(86.dp)
        assertThat(stackBounds.height).isEqualTo(294.dp)

        val flipBounds = composeTestRule.onNodeWithTag(FLIP_TAG).getUnclippedBoundsInRoot()
        val captureBounds = composeTestRule.onNodeWithTag(CAPTURE_TAG).getUnclippedBoundsInRoot()
        val wellBounds = composeTestRule.onNodeWithTag(WELL_TAG).getUnclippedBoundsInRoot()

        assertThat(flipBounds.left).isEqualTo(15.dp)
        assertThat(flipBounds.top).isEqualTo(0.dp)
        assertThat(captureBounds.left).isEqualTo(0.dp)
        assertThat(captureBounds.top).isEqualTo(104.dp)
        assertThat(wellBounds.left).isEqualTo(15.dp)
        assertThat(wellBounds.top).isEqualTo(238.dp)
    }

    @Test
    fun verticalCompact_measures86x238WithCompactSpacing() {
        composeTestRule.setContent {
            ShutterStack(
                modifier = Modifier.testTag(STACK_TAG),
                orientation = Orientation.Vertical,
                compact = true,
                captureButton = { modifier ->
                    Box(modifier.size(86.dp).testTag(CAPTURE_TAG))
                },
                flipCameraButton = { modifier ->
                    Box(modifier.size(56.dp).testTag(FLIP_TAG))
                },
                imageWell = { modifier ->
                    Box(modifier.size(56.dp).testTag(WELL_TAG))
                }
            )
        }

        val stackBounds = composeTestRule.onNodeWithTag(STACK_TAG).getUnclippedBoundsInRoot()
        assertThat(stackBounds.width).isEqualTo(86.dp)
        assertThat(stackBounds.height).isEqualTo(238.dp)

        assertThat(composeTestRule.onNodeWithTag(FLIP_TAG).getUnclippedBoundsInRoot().top)
            .isEqualTo(0.dp)
        assertThat(composeTestRule.onNodeWithTag(CAPTURE_TAG).getUnclippedBoundsInRoot().top)
            .isEqualTo(76.dp)
        assertThat(composeTestRule.onNodeWithTag(WELL_TAG).getUnclippedBoundsInRoot().top)
            .isEqualTo(182.dp)
    }

    @Test
    fun verticalRecording_withPauseResume_measures86x398() {
        composeTestRule.setContent {
            ShutterStack(
                modifier = Modifier.testTag(STACK_TAG),
                orientation = Orientation.Vertical,
                captureButton = { modifier ->
                    Box(modifier.size(86.dp).testTag(CAPTURE_TAG))
                },
                flipCameraButton = { modifier ->
                    Box(modifier.size(56.dp).testTag(FLIP_TAG))
                },
                imageWell = { modifier ->
                    Box(modifier.size(56.dp).testTag(WELL_TAG))
                },
                pauseResumeButton = { modifier ->
                    Box(modifier.size(56.dp).testTag(PAUSE_TAG))
                }
            )
        }

        val stackBounds = composeTestRule.onNodeWithTag(STACK_TAG).getUnclippedBoundsInRoot()
        assertThat(stackBounds.width).isEqualTo(86.dp)
        assertThat(stackBounds.height).isEqualTo(398.dp)

        assertThat(composeTestRule.onNodeWithTag(FLIP_TAG).getUnclippedBoundsInRoot().top)
            .isEqualTo(0.dp)
        assertThat(composeTestRule.onNodeWithTag(CAPTURE_TAG).getUnclippedBoundsInRoot().top)
            .isEqualTo(104.dp)
        assertThat(composeTestRule.onNodeWithTag(WELL_TAG).getUnclippedBoundsInRoot().top)
            .isEqualTo(238.dp)
        val pauseBounds = composeTestRule.onNodeWithTag(PAUSE_TAG).getUnclippedBoundsInRoot()
        assertThat(pauseBounds.left).isEqualTo(15.dp)
        assertThat(pauseBounds.top).isEqualTo(342.dp)
    }

    @Test
    fun vertical_optionalSlotsEmitNothingInsideProvider_collapsesTo86x86WithZeroGaps() {
        composeTestRule.setContent {
            ShutterStack(
                modifier = Modifier.testTag(STACK_TAG),
                orientation = Orientation.Vertical,
                captureButton = { modifier ->
                    Box(modifier.size(86.dp).testTag(CAPTURE_TAG))
                },
                flipCameraButton = {
                    // Emits nothing inside OverlapAwareStyleProvider (e.g., Unavailable lens)
                },
                imageWell = {
                    // Emits nothing inside OverlapAwareStyleProvider (e.g., Unavailable media)
                },
                pauseResumeButton = {
                    // Emits nothing inside OverlapAwareStyleProvider (e.g., Inactive recording)
                }
            )
        }

        val stackBounds = composeTestRule.onNodeWithTag(STACK_TAG).getUnclippedBoundsInRoot()
        assertThat(stackBounds.width).isEqualTo(86.dp)
        assertThat(stackBounds.height).isEqualTo(86.dp)

        val captureBounds = composeTestRule.onNodeWithTag(CAPTURE_TAG).getUnclippedBoundsInRoot()
        assertThat(captureBounds.left).isEqualTo(0.dp)
        assertThat(captureBounds.top).isEqualTo(0.dp)
        assertThat(captureBounds.width).isEqualTo(86.dp)
        assertThat(captureBounds.height).isEqualTo(86.dp)
    }

    @Test
    fun vertical_middleOptionalSlotEmitsNothing_skipsExtraSpacingGap() {
        composeTestRule.setContent {
            ShutterStack(
                modifier = Modifier.testTag(STACK_TAG),
                orientation = Orientation.Vertical,
                captureButton = { modifier ->
                    Box(modifier.size(86.dp).testTag(CAPTURE_TAG))
                },
                flipCameraButton = { modifier ->
                    Box(modifier.size(56.dp).testTag(FLIP_TAG))
                },
                imageWell = {
                    // Empty middle slot must not contribute an extra 48.dp gap
                },
                pauseResumeButton = { modifier ->
                    Box(modifier.size(56.dp).testTag(PAUSE_TAG))
                }
            )
        }

        val stackBounds = composeTestRule.onNodeWithTag(STACK_TAG).getUnclippedBoundsInRoot()
        assertThat(stackBounds.width).isEqualTo(86.dp)
        assertThat(stackBounds.height).isEqualTo(294.dp)
        assertThat(composeTestRule.onNodeWithTag(PAUSE_TAG).getUnclippedBoundsInRoot().top)
            .isEqualTo(238.dp)
    }

    @Test
    fun postureSwitch_verticalToHorizontalAndBack_preservesCaptureButtonStateWithoutDisposal() {
        var orientation by mutableStateOf(Orientation.Vertical)
        var mountCount = 0
        var disposeCount = 0
        var initialRememberedToken: Any? = null
        var latestRememberedToken: Any? = null

        composeTestRule.setContent {
            ShutterStack(
                modifier = Modifier.testTag(STACK_TAG),
                orientation = orientation,
                captureButton = { modifier ->
                    val token = remember { Any() }
                    if (initialRememberedToken == null) {
                        initialRememberedToken = token
                    }
                    latestRememberedToken = token
                    DisposableEffect(Unit) {
                        mountCount++
                        onDispose { disposeCount++ }
                    }
                    Box(modifier.size(86.dp).testTag(CAPTURE_TAG))
                },
                flipCameraButton = { modifier ->
                    Box(modifier.size(56.dp).testTag(FLIP_TAG))
                },
                imageWell = { modifier ->
                    Box(modifier.size(56.dp).testTag(WELL_TAG))
                },
                pauseResumeButton = { modifier ->
                    Box(modifier.size(56.dp).testTag(PAUSE_TAG))
                }
            )
        }

        composeTestRule.waitForIdle()
        assertThat(mountCount).isEqualTo(1)
        assertThat(disposeCount).isEqualTo(0)
        assertThat(composeTestRule.onNodeWithTag(FLIP_TAG).getUnclippedBoundsInRoot().top)
            .isEqualTo(0.dp)
        assertThat(composeTestRule.onNodeWithTag(CAPTURE_TAG).getUnclippedBoundsInRoot().top)
            .isEqualTo(104.dp)
        assertThat(composeTestRule.onNodeWithTag(WELL_TAG).getUnclippedBoundsInRoot().top)
            .isEqualTo(238.dp)
        assertThat(composeTestRule.onNodeWithTag(PAUSE_TAG).getUnclippedBoundsInRoot().top)
            .isEqualTo(342.dp)

        // Switch dynamically from Vertical to Horizontal
        orientation = Orientation.Horizontal
        composeTestRule.waitForIdle()

        assertThat(mountCount).isEqualTo(1)
        assertThat(disposeCount).isEqualTo(0)
        assertThat(latestRememberedToken).isSameInstanceAs(initialRememberedToken)

        val horizontalStackBounds =
            composeTestRule.onNodeWithTag(STACK_TAG).getUnclippedBoundsInRoot()
        assertThat(horizontalStackBounds.width).isEqualTo(350.dp)
        assertThat(horizontalStackBounds.height).isEqualTo(86.dp)

        // Horizontal LTR order: ImageWell -> CaptureButton -> FlipCamera -> PauseResume
        val wellHorizontal = composeTestRule.onNodeWithTag(WELL_TAG).getUnclippedBoundsInRoot()
        val captureHorizontal =
            composeTestRule.onNodeWithTag(CAPTURE_TAG).getUnclippedBoundsInRoot()
        val flipHorizontal = composeTestRule.onNodeWithTag(FLIP_TAG).getUnclippedBoundsInRoot()
        val pauseHorizontal = composeTestRule.onNodeWithTag(PAUSE_TAG).getUnclippedBoundsInRoot()

        assertThat(wellHorizontal.left).isEqualTo(0.dp)
        assertThat(wellHorizontal.top).isEqualTo(15.dp)
        assertThat(captureHorizontal.left).isEqualTo(88.dp)
        assertThat(captureHorizontal.top).isEqualTo(0.dp)
        assertThat(flipHorizontal.left).isEqualTo(206.dp)
        assertThat(flipHorizontal.top).isEqualTo(15.dp)
        assertThat(pauseHorizontal.left).isEqualTo(294.dp)
        assertThat(pauseHorizontal.top).isEqualTo(15.dp)

        // Switch dynamically back to Vertical
        orientation = Orientation.Vertical
        composeTestRule.waitForIdle()

        assertThat(mountCount).isEqualTo(1)
        assertThat(disposeCount).isEqualTo(0)
        assertThat(latestRememberedToken).isSameInstanceAs(initialRememberedToken)
        assertThat(composeTestRule.onNodeWithTag(STACK_TAG).getUnclippedBoundsInRoot().height)
            .isEqualTo(398.dp)
        assertThat(composeTestRule.onNodeWithTag(FLIP_TAG).getUnclippedBoundsInRoot().top)
            .isEqualTo(0.dp)
        assertThat(composeTestRule.onNodeWithTag(WELL_TAG).getUnclippedBoundsInRoot().top)
            .isEqualTo(238.dp)
    }

    @Test
    fun partialOverlap_resolvesIndependentBackgroundStylePerSlot() {
        var flipStyle: CameraControlBackgroundStyle? = null
        var captureStyle: CameraControlBackgroundStyle? = null
        var wellStyle: CameraControlBackgroundStyle? = null
        var pauseStyle: CameraControlBackgroundStyle? = null
        val targetBoundsState = mutableStateOf(Rect.Zero)

        var topHalfRectPx = Rect.Zero
        var bottomHalfRectPx = Rect.Zero

        composeTestRule.setContent {
            val density = LocalDensity.current
            topHalfRectPx = with(density) {
                Rect(0f, 0f, 100.dp.toPx(), 160.dp.toPx())
            }
            bottomHalfRectPx = with(density) {
                Rect(0f, 220.dp.toPx(), 100.dp.toPx(), 400.dp.toPx())
            }
            Box(Modifier.size(500.dp)) {
                CompositionLocalProvider(LocalOverlapTargetBounds provides targetBoundsState) {
                    ShutterStack(
                        orientation = Orientation.Vertical,
                        captureButton = { modifier ->
                            captureStyle = LocalCameraControlBackgroundStyle.current
                            Box(modifier.size(86.dp).testTag(CAPTURE_TAG))
                        },
                        flipCameraButton = { modifier ->
                            flipStyle = LocalCameraControlBackgroundStyle.current
                            Box(modifier.size(56.dp).testTag(FLIP_TAG))
                        },
                        imageWell = { modifier ->
                            wellStyle = LocalCameraControlBackgroundStyle.current
                            Box(modifier.size(56.dp).testTag(WELL_TAG))
                        },
                        pauseResumeButton = { modifier ->
                            pauseStyle = LocalCameraControlBackgroundStyle.current
                            Box(modifier.size(56.dp).testTag(PAUSE_TAG))
                        }
                    )
                }
            }
        }

        // Overlap top two slots (Flip [0..56dp] = 100%, Capture [104..190dp] = 56/86dp > 50%)
        targetBoundsState.value = topHalfRectPx
        composeTestRule.waitForIdle()

        assertThat(flipStyle).isEqualTo(CameraControlBackgroundStyle.BLACK_60)
        assertThat(captureStyle).isEqualTo(CameraControlBackgroundStyle.BLACK_60)
        assertThat(wellStyle).isEqualTo(CameraControlBackgroundStyle.WHITE_20)
        assertThat(pauseStyle).isEqualTo(CameraControlBackgroundStyle.WHITE_20)

        // Shift overlap to bottom two slots (Well [238..294dp] = 100%, Pause [342..398dp] = 100%)
        targetBoundsState.value = bottomHalfRectPx
        composeTestRule.waitForIdle()

        assertThat(flipStyle).isEqualTo(CameraControlBackgroundStyle.WHITE_20)
        assertThat(captureStyle).isEqualTo(CameraControlBackgroundStyle.WHITE_20)
        assertThat(wellStyle).isEqualTo(CameraControlBackgroundStyle.BLACK_60)
        assertThat(pauseStyle).isEqualTo(CameraControlBackgroundStyle.BLACK_60)
    }

    @Test
    fun verticalRtl_doesNotInvertTopToBottomOrder() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ShutterStack(
                    modifier = Modifier.testTag(STACK_TAG),
                    orientation = Orientation.Vertical,
                    captureButton = { modifier ->
                        Box(modifier.size(86.dp).testTag(CAPTURE_TAG))
                    },
                    flipCameraButton = { modifier ->
                        Box(modifier.size(56.dp).testTag(FLIP_TAG))
                    },
                    imageWell = { modifier ->
                        Box(modifier.size(56.dp).testTag(WELL_TAG))
                    },
                    pauseResumeButton = { modifier ->
                        Box(modifier.size(56.dp).testTag(PAUSE_TAG))
                    }
                )
            }
        }

        val flipBounds = composeTestRule.onNodeWithTag(FLIP_TAG).getUnclippedBoundsInRoot()
        val captureBounds = composeTestRule.onNodeWithTag(CAPTURE_TAG).getUnclippedBoundsInRoot()
        val wellBounds = composeTestRule.onNodeWithTag(WELL_TAG).getUnclippedBoundsInRoot()
        val pauseBounds = composeTestRule.onNodeWithTag(PAUSE_TAG).getUnclippedBoundsInRoot()

        assertThat(flipBounds.top).isEqualTo(0.dp)
        assertThat(flipBounds.left).isEqualTo(15.dp)
        assertThat(captureBounds.top).isEqualTo(104.dp)
        assertThat(captureBounds.left).isEqualTo(0.dp)
        assertThat(wellBounds.top).isEqualTo(238.dp)
        assertThat(wellBounds.left).isEqualTo(15.dp)
        assertThat(pauseBounds.top).isEqualTo(342.dp)
        assertThat(pauseBounds.left).isEqualTo(15.dp)
    }

    @Test
    fun horizontalRtl_mirrorsStartToEndOrder() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                ShutterStack(
                    modifier = Modifier.testTag(STACK_TAG),
                    orientation = Orientation.Horizontal,
                    captureButton = { modifier ->
                        Box(modifier.size(86.dp).testTag(CAPTURE_TAG))
                    },
                    flipCameraButton = { modifier ->
                        Box(modifier.size(56.dp).testTag(FLIP_TAG))
                    },
                    imageWell = { modifier ->
                        Box(modifier.size(56.dp).testTag(WELL_TAG))
                    },
                    pauseResumeButton = { modifier ->
                        Box(modifier.size(56.dp).testTag(PAUSE_TAG))
                    }
                )
            }
        }

        composeTestRule.waitForIdle()
        // LTR Start -> End: Well [0..56], Capture [88..174], Flip [206..262], Pause [294..350]
        assertThat(composeTestRule.onNodeWithTag(WELL_TAG).getUnclippedBoundsInRoot().left)
            .isEqualTo(0.dp)
        assertThat(composeTestRule.onNodeWithTag(CAPTURE_TAG).getUnclippedBoundsInRoot().left)
            .isEqualTo(88.dp)
        assertThat(composeTestRule.onNodeWithTag(FLIP_TAG).getUnclippedBoundsInRoot().left)
            .isEqualTo(206.dp)
        assertThat(composeTestRule.onNodeWithTag(PAUSE_TAG).getUnclippedBoundsInRoot().left)
            .isEqualTo(294.dp)

        // RTL Start -> End mirrors right-to-left within 350.dp width:
        // Pause [0..56], Flip [88..144], Capture [176..262], Well [294..350]
        layoutDirection = LayoutDirection.Rtl
        composeTestRule.waitForIdle()

        val stackBounds = composeTestRule.onNodeWithTag(STACK_TAG).getUnclippedBoundsInRoot()
        assertThat(stackBounds.width).isEqualTo(350.dp)
        assertThat(stackBounds.height).isEqualTo(86.dp)

        assertThat(composeTestRule.onNodeWithTag(PAUSE_TAG).getUnclippedBoundsInRoot().left)
            .isEqualTo(0.dp)
        assertThat(composeTestRule.onNodeWithTag(FLIP_TAG).getUnclippedBoundsInRoot().left)
            .isEqualTo(88.dp)
        assertThat(composeTestRule.onNodeWithTag(CAPTURE_TAG).getUnclippedBoundsInRoot().left)
            .isEqualTo(176.dp)
        assertThat(composeTestRule.onNodeWithTag(WELL_TAG).getUnclippedBoundsInRoot().left)
            .isEqualTo(294.dp)
    }

    @Test
    fun horizontalCompactTrue_defaultsToHorizontalSpacingAndRespectsCustomOverride() {
        var customSpacing by mutableStateOf<Dp?>(null)

        composeTestRule.setContent {
            val spacingOverride = customSpacing
            if (spacingOverride == null) {
                ShutterStack(
                    modifier = Modifier.testTag(STACK_TAG),
                    orientation = Orientation.Horizontal,
                    compact = true,
                    captureButton = { modifier ->
                        Box(modifier.size(86.dp).testTag(CAPTURE_TAG))
                    },
                    flipCameraButton = { modifier ->
                        Box(modifier.size(56.dp).testTag(FLIP_TAG))
                    },
                    imageWell = { modifier ->
                        Box(modifier.size(56.dp).testTag(WELL_TAG))
                    }
                )
            } else {
                ShutterStack(
                    modifier = Modifier.testTag(STACK_TAG),
                    orientation = Orientation.Horizontal,
                    compact = true,
                    spacing = spacingOverride,
                    captureButton = { modifier ->
                        Box(modifier.size(86.dp).testTag(CAPTURE_TAG))
                    },
                    flipCameraButton = { modifier ->
                        Box(modifier.size(56.dp).testTag(FLIP_TAG))
                    },
                    imageWell = { modifier ->
                        Box(modifier.size(56.dp).testTag(WELL_TAG))
                    }
                )
            }
        }

        // Default Horizontal spacing (32.dp) even when compact = true: 56 + 32 + 86 + 32 + 56 = 262
        assertThat(composeTestRule.onNodeWithTag(STACK_TAG).getUnclippedBoundsInRoot().width)
            .isEqualTo(262.dp)

        // Custom spacing override (12.dp): 56 + 12 + 86 + 12 + 56 = 222
        customSpacing = 12.dp
        composeTestRule.waitForIdle()
        assertThat(composeTestRule.onNodeWithTag(STACK_TAG).getUnclippedBoundsInRoot().width)
            .isEqualTo(222.dp)
    }

    @Test
    fun traversalSemantics_postureSwitch_exposesGroupAndUpdatesPerSlotTraversalIndex() {
        var orientation by mutableStateOf(Orientation.Vertical)

        composeTestRule.setContent {
            ShutterStack(
                modifier = Modifier.testTag(STACK_TAG),
                orientation = orientation,
                captureButton = { modifier ->
                    Box(modifier.size(86.dp).testTag(CAPTURE_TAG))
                },
                flipCameraButton = { modifier ->
                    Box(modifier.size(56.dp).testTag(FLIP_TAG))
                },
                imageWell = { modifier ->
                    Box(modifier.size(56.dp).testTag(WELL_TAG))
                },
                pauseResumeButton = { modifier ->
                    Box(modifier.size(56.dp).testTag(PAUSE_TAG))
                }
            )
        }

        composeTestRule.onNodeWithTag(STACK_TAG).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true)
        )
        assertThat(slotTraversalIndex(FLIP_TAG)).isEqualTo(0f)
        assertThat(slotTraversalIndex(CAPTURE_TAG)).isEqualTo(1f)
        assertThat(slotTraversalIndex(WELL_TAG)).isEqualTo(2f)
        assertThat(slotTraversalIndex(PAUSE_TAG)).isEqualTo(3f)

        orientation = Orientation.Horizontal
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(STACK_TAG).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true)
        )
        assertThat(slotTraversalIndex(WELL_TAG)).isEqualTo(0f)
        assertThat(slotTraversalIndex(CAPTURE_TAG)).isEqualTo(1f)
        assertThat(slotTraversalIndex(FLIP_TAG)).isEqualTo(2f)
        assertThat(slotTraversalIndex(PAUSE_TAG)).isEqualTo(3f)
    }

    @Test
    fun focusOrder_moveFocusNext_traversesSlotsInVerticalAndHorizontalLtrAndRtl() {
        var orientation by mutableStateOf(Orientation.Vertical)
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        val entryFocusRequester = FocusRequester()
        lateinit var focusManager: FocusManager

        composeTestRule.setContent {
            focusManager = LocalFocusManager.current
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box {
                    Box(
                        Modifier
                            .size(48.dp)
                            .focusRequester(entryFocusRequester)
                            .testTag(ENTRY_FOCUS_TAG)
                            .focusable()
                    )
                    ShutterStack(
                        modifier = Modifier.testTag(STACK_TAG),
                        orientation = orientation,
                        captureButton = { modifier ->
                            Box(modifier.size(86.dp).testTag(CAPTURE_TAG).focusable())
                        },
                        flipCameraButton = { modifier ->
                            Box(modifier.size(56.dp).testTag(FLIP_TAG).focusable())
                        },
                        imageWell = { modifier ->
                            Box(modifier.size(56.dp).testTag(WELL_TAG).focusable())
                        },
                        pauseResumeButton = { modifier ->
                            Box(modifier.size(56.dp).testTag(PAUSE_TAG).focusable())
                        }
                    )
                    Box(
                        Modifier
                            .size(48.dp)
                            .testTag(EXIT_FOCUS_TAG)
                            .focusable()
                    )
                }
            }
        }

        assertNextFocusSequence(
            entryFocusRequester = entryFocusRequester,
            focusManager = focusManager,
            expectedTags = listOf(FLIP_TAG, CAPTURE_TAG, WELL_TAG, PAUSE_TAG)
        )

        orientation = Orientation.Horizontal
        layoutDirection = LayoutDirection.Ltr
        composeTestRule.waitForIdle()
        assertNextFocusSequence(
            entryFocusRequester = entryFocusRequester,
            focusManager = focusManager,
            expectedTags = listOf(WELL_TAG, CAPTURE_TAG, FLIP_TAG, PAUSE_TAG)
        )

        layoutDirection = LayoutDirection.Rtl
        composeTestRule.waitForIdle()
        assertNextFocusSequence(
            entryFocusRequester = entryFocusRequester,
            focusManager = focusManager,
            expectedTags = listOf(WELL_TAG, CAPTURE_TAG, FLIP_TAG, PAUSE_TAG)
        )
    }

    @Test
    fun lightTheme_providesWhiteLocalContentColorInsideShutterStack() {
        var captureContentColor: Color? = null
        var flipContentColor: Color? = null
        var wellContentColor: Color? = null
        var pauseContentColor: Color? = null

        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ShutterStack(
                    captureButton = { modifier ->
                        captureContentColor = LocalContentColor.current
                        Box(modifier.size(86.dp).testTag(CAPTURE_TAG))
                    },
                    flipCameraButton = { modifier ->
                        flipContentColor = LocalContentColor.current
                        Box(modifier.size(56.dp).testTag(FLIP_TAG))
                    },
                    imageWell = { modifier ->
                        wellContentColor = LocalContentColor.current
                        Box(modifier.size(56.dp).testTag(WELL_TAG))
                    },
                    pauseResumeButton = { modifier ->
                        pauseContentColor = LocalContentColor.current
                        Box(modifier.size(56.dp).testTag(PAUSE_TAG))
                    }
                )
            }
        }

        assertThat(captureContentColor).isEqualTo(Color.White)
        assertThat(flipContentColor).isEqualTo(Color.White)
        assertThat(wellContentColor).isEqualTo(Color.White)
        assertThat(pauseContentColor).isEqualTo(Color.White)
    }

    @Test
    fun collapsedOptionalSlots_emitZeroChildSemanticsNodes() {
        composeTestRule.setContent {
            ShutterStack(
                modifier = Modifier.testTag(STACK_TAG),
                orientation = Orientation.Vertical,
                captureButton = { modifier ->
                    Box(modifier.size(86.dp).testTag(CAPTURE_TAG))
                },
                flipCameraButton = { modifier ->
                    FlipCameraButton(
                        flipLensUiState = FlipLensUiState.Unavailable,
                        onClick = {},
                        modifier = modifier.testTag(FLIP_TAG)
                    )
                },
                imageWell = { modifier ->
                    ImageWell(
                        imageWellUiState = ImageWellUiState.Unavailable,
                        onClick = {},
                        modifier = modifier.testTag(WELL_TAG)
                    )
                },
                pauseResumeButton = {
                    // Emits nothing when recording is inactive
                }
            )
        }

        val stackChildren = composeTestRule.onNodeWithTag(STACK_TAG, useUnmergedTree = true)
            .fetchSemanticsNode().children
        assertThat(stackChildren).hasSize(1)
        composeTestRule.onNodeWithTag(FLIP_TAG, useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag(WELL_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun horizontalConstrained328dp_fourSlots_compressesSpacingWithoutClippingInLtrAndRtl() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box(modifier = Modifier.size(width = 328.dp, height = 86.dp)) {
                    ShutterStack(
                        modifier = Modifier.testTag(STACK_TAG),
                        orientation = Orientation.Horizontal,
                        captureButton = { modifier ->
                            Box(
                                modifier
                                    .size(width = 118.25.dp, height = 86.dp)
                                    .testTag(CAPTURE_TAG)
                            )
                        },
                        flipCameraButton = { modifier ->
                            Box(modifier.size(56.dp).testTag(FLIP_TAG))
                        },
                        imageWell = { modifier ->
                            Box(modifier.size(56.dp).testTag(WELL_TAG))
                        },
                        pauseResumeButton = { modifier ->
                            Box(modifier.size(56.dp).testTag(PAUSE_TAG))
                        }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        assertConstrainedHorizontalBoundsWithin(maxWidth = 328.dp)

        val wellLtr = composeTestRule.onNodeWithTag(WELL_TAG).getUnclippedBoundsInRoot()
        val captureLtr = composeTestRule.onNodeWithTag(CAPTURE_TAG).getUnclippedBoundsInRoot()
        val flipLtr = composeTestRule.onNodeWithTag(FLIP_TAG).getUnclippedBoundsInRoot()
        val pauseLtr = composeTestRule.onNodeWithTag(PAUSE_TAG).getUnclippedBoundsInRoot()
        assertThat(wellLtr.right).isAtMost(captureLtr.left)
        assertThat(captureLtr.right).isAtMost(flipLtr.left)
        assertThat(flipLtr.right).isAtMost(pauseLtr.left)

        layoutDirection = LayoutDirection.Rtl
        composeTestRule.waitForIdle()
        assertConstrainedHorizontalBoundsWithin(maxWidth = 328.dp)

        val pauseRtl = composeTestRule.onNodeWithTag(PAUSE_TAG).getUnclippedBoundsInRoot()
        val flipRtl = composeTestRule.onNodeWithTag(FLIP_TAG).getUnclippedBoundsInRoot()
        val captureRtl = composeTestRule.onNodeWithTag(CAPTURE_TAG).getUnclippedBoundsInRoot()
        val wellRtl = composeTestRule.onNodeWithTag(WELL_TAG).getUnclippedBoundsInRoot()
        assertThat(pauseRtl.right).isAtMost(flipRtl.left)
        assertThat(flipRtl.right).isAtMost(captureRtl.left)
        assertThat(captureRtl.right).isAtMost(wellRtl.left)
    }

    @Test
    fun verticalConstrained360dp_fourSlots_compressesSpacingWithoutClipping() {
        composeTestRule.setContent {
            Box(modifier = Modifier.size(width = 86.dp, height = 360.dp)) {
                ShutterStack(
                    modifier = Modifier.testTag(STACK_TAG),
                    orientation = Orientation.Vertical,
                    captureButton = { modifier ->
                        Box(modifier.size(86.dp).testTag(CAPTURE_TAG))
                    },
                    flipCameraButton = { modifier ->
                        Box(modifier.size(56.dp).testTag(FLIP_TAG))
                    },
                    imageWell = { modifier ->
                        Box(modifier.size(56.dp).testTag(WELL_TAG))
                    },
                    pauseResumeButton = { modifier ->
                        Box(modifier.size(56.dp).testTag(PAUSE_TAG))
                    }
                )
            }
        }

        for (tag in listOf(FLIP_TAG, WELL_TAG, PAUSE_TAG)) {
            val unclipped = composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            val clipped = composeTestRule.onNodeWithTag(tag).getBoundsInRoot()
            assertThat(unclipped.height).isEqualTo(56.dp)
            assertThat(clipped.height).isEqualTo(56.dp)
            assertThat(unclipped.top).isAtLeast(0.dp)
            assertThat(unclipped.bottom).isAtMost(360.dp)
        }
    }

    @Test
    fun imageWell_exposesButtonRoleAndContentDescriptionWithAndWithoutThumbnail() {
        val expectedDescription = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.image_well_content_description)
        val bitmap = createBitmap(32, 32)
        var uiState by mutableStateOf<ImageWellUiState>(
            ImageWellUiState.Content(
                mediaDescriptor = MediaDescriptor.Content.Image(
                    uri = Uri.fromParts("content", "capture1", null),
                    thumbnail = bitmap
                )
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                ImageWell(
                    imageWellUiState = uiState,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(IMAGE_WELL_TAG)
            .assertContentDescriptionEquals(expectedDescription)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        composeTestRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),
            useUnmergedTree = true
        ).assertCountEquals(1)

        uiState = ImageWellUiState.Content(
            mediaDescriptor = MediaDescriptor.Content.Image(
                uri = Uri.fromParts("content", "capture2", null),
                thumbnail = null
            )
        )
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(IMAGE_WELL_TAG)
            .assertContentDescriptionEquals(expectedDescription)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    private fun slotTraversalIndex(tag: String): Float {
        val slotNode = composeTestRule.onNodeWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNode()
        assertThat(slotNode.config.getOrNull(SemanticsProperties.IsTraversalGroup)).isTrue()
        return checkNotNull(slotNode.config.getOrNull(SemanticsProperties.TraversalIndex))
    }

    private fun assertNextFocusSequence(
        entryFocusRequester: FocusRequester,
        focusManager: FocusManager,
        expectedTags: List<String>
    ) {
        composeTestRule.runOnIdle {
            entryFocusRequester.requestFocus()
        }
        composeTestRule.onNodeWithTag(ENTRY_FOCUS_TAG).assertIsFocused()
        for (tag in expectedTags) {
            composeTestRule.runOnIdle {
                assertThat(focusManager.moveFocus(FocusDirection.Next)).isTrue()
            }
            composeTestRule.onNodeWithTag(tag).assertIsFocused()
        }
        composeTestRule.runOnIdle {
            assertThat(focusManager.moveFocus(FocusDirection.Next)).isTrue()
        }
        composeTestRule.onNodeWithTag(EXIT_FOCUS_TAG).assertIsFocused()
    }

    private fun assertConstrainedHorizontalBoundsWithin(maxWidth: Dp) {
        val stackBounds = composeTestRule.onNodeWithTag(STACK_TAG).getUnclippedBoundsInRoot()
        assertThat(stackBounds.left).isAtLeast(0.dp)
        assertThat(stackBounds.right).isAtMost(maxWidth)

        val captureBounds = composeTestRule.onNodeWithTag(CAPTURE_TAG).getUnclippedBoundsInRoot()
        assertThat(captureBounds.left).isAtLeast(0.dp)
        assertThat(captureBounds.right).isAtMost(maxWidth)

        for (tag in listOf(WELL_TAG, FLIP_TAG, PAUSE_TAG)) {
            val unclipped = composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            val clipped = composeTestRule.onNodeWithTag(tag).getBoundsInRoot()
            assertThat(unclipped.width).isEqualTo(56.dp)
            assertThat(clipped.width).isEqualTo(56.dp)
            assertThat(unclipped.left).isAtLeast(0.dp)
            assertThat(unclipped.right).isAtMost(maxWidth)
        }
    }

    private companion object {
        const val STACK_TAG = "stack_shutter"
        const val CAPTURE_TAG = "btn_shutter_capture"
        const val FLIP_TAG = "btn_shutter_flip"
        const val WELL_TAG = "btn_shutter_well"
        const val PAUSE_TAG = "btn_shutter_pause"
        const val ENTRY_FOCUS_TAG = "focus_entry_sentinel"
        const val EXIT_FOCUS_TAG = "focus_exit_sentinel"
    }
}
