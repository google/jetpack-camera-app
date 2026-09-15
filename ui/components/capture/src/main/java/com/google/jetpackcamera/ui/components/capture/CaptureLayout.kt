/*
 * Copyright (C) 2025 The Android Open Source Project
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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.takeOrElse

/**
 * Height of the shutter row (capture button, image well and flip camera button) as specified by
 * the design spec.
 */
private val SHUTTER_STACK_HEIGHT = 86.dp

/**
 * Height of the slot reserved between the shutter row and the lower controls row. The design spec
 * reserves this space unconditionally so that optional content can appear there without shifting
 * any of the surrounding controls.
 */
private val MIDDLE_SLOT_HEIGHT = 32.dp

/**
 * Height of the lower controls row (quick settings and capture mode toggles).
 *
 * This is a hard height, and it currently has no slack: it is exactly the default height of the
 * toggle switch it contains. Keep it greater than or equal to that default, otherwise the row
 * will silently clip its contents.
 */
private val LOWER_SECTION_HEIGHT = 64.dp

/** Vertical gap between the zoom bar and the shutter row. */
private val ZOOM_TO_SHUTTER_GAP = 32.dp

/** Vertical gap above and below the reserved middle slot. */
private val CONTROL_STACK_GAP = 24.dp

/**
 * Target clearance from the bottom of the display to the lower controls row, keeping the
 * controls at a consistent thumb resting position across navigation bar modes (gesture vs
 * 3-button navigation).
 */
private val TARGET_BOTTOM_CLEARANCE = 56.dp

/** Minimum safety margin between the lower controls row and the navigation bar. */
private val MIN_NAV_MARGIN = 8.dp

/** Compressed [ZOOM_TO_SHUTTER_GAP] used on short screens. */
private val COMPACT_ZOOM_TO_SHUTTER_GAP = 16.dp

/** Compressed [CONTROL_STACK_GAP] used on short screens. */
private val COMPACT_CONTROL_STACK_GAP = 12.dp

/** Compressed bottom padding used on short screens. */
private val COMPACT_CONTROLS_BOTTOM_PADDING = 12.dp

/** Available height below which the control stack compresses its gaps. */
private val SHORT_SCREEN_THRESHOLD = 600.dp

/** Horizontal inset of the lower controls row. */
private val LOWER_SECTION_HORIZONTAL_PADDING = 16.dp

/**
 * The base layout for the camera capture screen.
 *
 * @param modifier the modifier for this component
 * @param scaffoldState the bottom sheet scaffold state
 * @param onDismissQuickSettings callback to dismiss quick settings when clicking the drag handle
 * @param viewfinder the viewfinder composable
 * @param captureButton the capture button composable
 * @param imageWell the image well composable
 * @param flipCameraButton the flip camera button composable
 * @param zoomLevelDisplay the zoom level display composable
 * @param elapsedTimeDisplay the elapsed time display composable
 * @param quickSettingsButton the quick settings button composable
 * @param indicatorRow the indicator row composable
 * @param captureModeToggle the capture mode toggle composable
 * @param captureModeCarousel content for the fixed-height slot reserved between the shutter row
 * and the lower controls row. The slot is always reserved, so supplying or omitting content here
 * never moves the surrounding controls.
 * @param quickSettingsOverlay the quick settings overlay composable
 * @param debugOverlay the debug overlay composable
 * @param debugVisibilityWrapper A wrapper that conditionally hides its contents based on debug settings
 * @param screenFlashOverlay the screen flash overlay composable
 * @param snackBar the snack bar composable for showing messages
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PreviewLayout(
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false
        )
    ),
    onDismissQuickSettings: () -> Unit = {},
    viewfinder: @Composable (Modifier) -> Unit,
    captureButton: @Composable (Modifier) -> Unit,
    imageWell: @Composable (Modifier) -> Unit,
    flipCameraButton: @Composable (Modifier) -> Unit,
    zoomLevelDisplay: @Composable (Modifier) -> Unit,
    elapsedTimeDisplay: @Composable (Modifier) -> Unit,
    quickSettingsButton: @Composable (Modifier) -> Unit,
    indicatorRow: @Composable (Modifier) -> Unit,
    captureModeToggle: @Composable (Modifier) -> Unit,
    captureModeCarousel: @Composable (Modifier) -> Unit = {},
    quickSettingsOverlay: @Composable (Modifier) -> Unit,
    debugOverlay: @Composable (Modifier) -> Unit,
    debugVisibilityWrapper: (@Composable (@Composable () -> Unit) -> Unit),
    screenFlashOverlay: @Composable (Modifier) -> Unit,
    snackBar: @Composable (Modifier, snackbarHostState: SnackbarHostState) -> Unit
) {
    val overlapTargetBounds = remember { mutableStateOf(Rect.Zero) }

    CompositionLocalProvider(LocalOverlapTargetBounds provides overlapTargetBounds) {
        BottomSheetScaffold(
            modifier = modifier.fillMaxSize(),
            scaffoldState = scaffoldState,
            sheetPeekHeight = 0.dp,
            sheetDragHandle = {
                BottomSheetDefaults.DragHandle(
                    modifier = Modifier
                        .testTag(QUICK_SETTINGS_DRAG_HANDLE)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = stringResource(
                                R.string.quick_settings_btn_close_expanded_settings_description
                            ),
                            onClick = onDismissQuickSettings
                        )
                )
            },
            sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            sheetContent = {
                quickSettingsOverlay(Modifier)
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = scaffoldState.snackbarHostState,
                    modifier = Modifier.testTag(SNACKBAR_NODE_TAG)
                )
            }
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                Column {
                    // The indicator row occupies the top bar real estate in the hidden status bar
                    // region. We size the top bar to accommodate any top display cutout or the
                    // minimum interactive touch target (defaulting to 48dp), while CutoutAwareRow
                    // shifts individual indicator icons around any intersecting cutout bounds.
                    val cutoutTopInset =
                        WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
                    val minTouchTarget =
                        LocalMinimumInteractiveComponentSize.current.takeOrElse { 48.dp }
                    val topBarHeight = max(cutoutTopInset, minTouchTarget)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(topBarHeight)
                            .windowInsetsPadding(
                                WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)
                            )
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        indicatorRow(Modifier)
                    }
                    viewfinder(Modifier)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(
                            WindowInsets.systemBarsIgnoringVisibility
                                .union(WindowInsets.displayCutout)
                        )

                ) {
                    debugVisibilityWrapper {
                        VerticalMaterialControls(
                            captureButton = captureButton,
                            imageWell = imageWell,
                            flipCameraButton = flipCameraButton,
                            quickSettingsToggleButton = quickSettingsButton,
                            captureModeToggleSwitch = captureModeToggle,
                            captureModeCarousel = captureModeCarousel,
                            zoomControls = zoomLevelDisplay,
                            elapsedTimeDisplay = elapsedTimeDisplay
                        )
                    }
                    // controls overlay
                    snackBar(Modifier, scaffoldState.snackbarHostState)
                    screenFlashOverlay(Modifier)
                }
                debugOverlay(Modifier)

                val isSheetVisible = scaffoldState.bottomSheetState.isVisible ||
                    scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded
                if (isSheetVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(QUICK_SETTINGS_SCRIM)
                            .clickable(
                                onClickLabel = stringResource(
                                    R.string.quick_settings_btn_close_expanded_settings_description
                                ),
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismissQuickSettings
                            )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VerticalMaterialControls(
    modifier: Modifier = Modifier,
    captureButton: @Composable (Modifier) -> Unit,
    zoomControls: @Composable (Modifier) -> Unit,
    imageWell: @Composable (Modifier) -> Unit,
    flipCameraButton: @Composable (Modifier) -> Unit,
    quickSettingsToggleButton: @Composable (Modifier) -> Unit,
    captureModeToggleSwitch: @Composable (Modifier) -> Unit,
    captureModeCarousel: @Composable (Modifier) -> Unit,
    elapsedTimeDisplay: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // On short screens only the gaps and the bottom padding compress; the fixed element
        // heights are preserved so the stack keeps the proportions of the design spec.
        val isPortrait = maxHeight > maxWidth
        val compact = maxHeight < SHORT_SCREEN_THRESHOLD
        val stackGap = if (compact) COMPACT_CONTROL_STACK_GAP else CONTROL_STACK_GAP
        val navBarBottom = WindowInsets.navigationBarsIgnoringVisibility
            .asPaddingValues()
            .calculateBottomPadding()
        val bottomPad = if (compact) {
            COMPACT_CONTROLS_BOTTOM_PADDING
        } else {
            max(TARGET_BOTTOM_CLEARANCE - navBarBottom, MIN_NAV_MARGIN)
        }

        // Shutter top measured up from the bottom of this controls Box
        val shutterTopFromBottom = bottomPad + LOWER_SECTION_HEIGHT + stackGap +
            MIDDLE_SLOT_HEIGHT + stackGap + SHUTTER_STACK_HEIGHT

        val zoomGap = if (compact) {
            COMPACT_ZOOM_TO_SHUTTER_GAP
        } else if (isPortrait) {
            // In standard portrait orientation, anchor the zoom bar to the 3:4 viewfinder
            // bottom baseline (matching reference camera app behavior).
            // This places the zoom bar cleanly inside the 3:4 preview, keeps it locked at the
            // same physical coordinate when switching between 3:4 and 9:16 aspect ratios,
            // and adapts across device screen heights.
            val topInset = max(
                WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding(),
                WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
            )
            val topBarHeight = max(topInset, 48.dp)
            val viewfinder34Height = maxWidth * 4f / 3f
            val viewfinder34BottomFromTop = topBarHeight + viewfinder34Height
            val viewfinder34BottomFromBoxBottom = maxHeight - (viewfinder34BottomFromTop - topInset)
            val targetGap = viewfinder34BottomFromBoxBottom - shutterTopFromBottom
            max(targetGap, ZOOM_TO_SHUTTER_GAP)
        } else {
            ZOOM_TO_SHUTTER_GAP
        }

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                elapsedTimeDisplay(Modifier)

                // zoom controls row
                zoomControls(Modifier)

                Spacer(modifier = Modifier.height(zoomGap))

                // capture button row
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(SHUTTER_STACK_HEIGHT),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Row that holds flip camera, capture button, and audio
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                            // animation fades in/out this component based on quick settings
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                imageWell(Modifier)
                            }
                        }

                        OverlapAwareStyleProvider(
                            overlapThreshold = 0.5f
                        ) {
                            captureButton(Modifier)
                        }

                        // right capturebutton item
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            flipCameraButton(Modifier)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(stackGap))

                // Slot reserved by the design spec between the shutter row and the lower controls
                // row. Its height is fixed whether or not it has content, so the shutter row never
                // moves when content appears here.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MIDDLE_SLOT_HEIGHT),
                    contentAlignment = Alignment.Center
                ) {
                    captureModeCarousel(Modifier)
                }

                Spacer(modifier = Modifier.height(stackGap))

                // bottom controls row
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(LOWER_SECTION_HEIGHT)
                        .padding(horizontal = LOWER_SECTION_HORIZONTAL_PADDING),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Row that holds toggle buttons for quick settings and capture mode
                    // quick settings toggle switch item to the left
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        quickSettingsToggleButton(Modifier)
                    }

                    // capture mode toggle switch center
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        captureModeToggleSwitch(Modifier)
                    }

                    // right toggle switch item to the right
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd
                    ) {}
                }

                Spacer(modifier = Modifier.height(bottomPad))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun CaptureLayoutPreview() {
    PreviewLayout(
        modifier = Modifier.background(Color.Black),
        viewfinder = { modifier ->
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .background(Color.DarkGray)
            )
        },
        captureButton = { modifier ->
            Box(
                modifier = modifier
                    .size(80.dp)
                    .background(Color.White)
            )
        },
        flipCameraButton = { modifier ->
            Box(
                modifier = modifier
                    .size(48.dp)
                    .background(Color.Cyan)
            )
        },
        imageWell = { modifier ->
            Box(
                modifier = modifier
                    .size(48.dp)
                    .background(Color.Cyan)
            )
        },
        zoomLevelDisplay = { modifier ->
            Box(
                modifier = modifier
                    .height(48.dp)
                    .fillMaxWidth()
                    .background(Color.Magenta)
            )
        },
        elapsedTimeDisplay = { modifier ->
            Box(
                modifier = modifier
                    .height(24.dp)
                    .fillMaxWidth(0.5f)
                    .background(Color.Red)
            )
        },
        quickSettingsButton = { modifier ->
            Box(
                modifier = modifier
                    .size(48.dp)
                    .background(Color.Yellow)
            )
        },
        indicatorRow = { modifier ->
            Box(
                modifier = modifier
                    .height(48.dp)
                    .fillMaxWidth()
                    .background(Color.Green)
            )
        },
        captureModeToggle = { modifier ->
            Box(
                modifier = modifier
                    .height(48.dp)
                    .fillMaxWidth(0.5f)
                    .background(Color.Blue)
            )
        },
        quickSettingsOverlay = {
            // No-op for preview
        },
        debugOverlay = {
            // No-op for preview
        },
        screenFlashOverlay = {
            // No-op for preview
        },
        snackBar = { _, _ ->
            // No-op for preview
        },
        debugVisibilityWrapper = { content -> content() }
    )
}
