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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.systemGestures
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.takeOrElse
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutDefaults
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutSolver
import com.google.jetpackcamera.ui.components.capture.layout.CameraRowIds
import com.google.jetpackcamera.ui.components.capture.layout.CameraWindow
import com.google.jetpackcamera.ui.components.capture.layout.LocalCameraLayoutSolution

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

/** Vertical gap between the zoom bar and the shutter row on large screens. */
private val ZOOM_TO_SHUTTER_GAP = 32.dp

/** Minimum safety gap between the zoom bar and the shutter row. */
private val MIN_ZOOM_TO_SHUTTER_GAP = 16.dp

/**
 * Vertical clearance between the zoom bar and the bottom edge of the 3:4 viewfinder,
 * providing comfortable breathing room inside the preview.
 */
private val ZOOM_VIEWFINDER_BOTTOM_PADDING = 20.dp

/** Maximum target vertical gap above and below the reserved middle slot. */
private val CONTROL_STACK_MAX_GAP = 24.dp

/** Minimum compressed vertical gap above and below the reserved middle slot. */
private val CONTROL_STACK_MIN_GAP = 12.dp

/**
 * Target clearance from the bottom of the display to the lower controls row, keeping the
 * controls at a consistent thumb resting position across navigation bar modes (gesture vs
 * 3-button navigation).
 */
private val TARGET_BOTTOM_CLEARANCE = 56.dp

/** Minimum safety margin between the lower controls row and the navigation bar. */
private val MIN_NAV_MARGIN = 8.dp

/** Minimum compressed bottom padding used on short screens. */
private val MIN_CONTROLS_BOTTOM_PADDING = 12.dp

/** Available height below which the control stack compresses its gaps in landscape. */
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
            BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                // The adaptive portrait solver targets portrait handhelds (16:9 and taller, where
                // a full-width 9:16 frame fits inside the window). Squarer windows (landscape,
                // foldable inner displays, or short split-screen panes like 360x500) fall back to
                // VerticalMaterialControls, which compresses below SHORT_SCREEN_THRESHOLD.
                val isHandheldPortrait = (maxHeight / maxWidth) >= (16f / 9f - 0.01f)
                if (isHandheldPortrait) {
                    AdaptivePortraitCaptureLayout(
                        windowWidth = maxWidth,
                        windowHeight = maxHeight,
                        viewfinder = viewfinder,
                        indicatorRow = indicatorRow,
                        elapsedTimeDisplay = elapsedTimeDisplay,
                        zoomControls = zoomLevelDisplay,
                        imageWell = imageWell,
                        captureButton = captureButton,
                        flipCameraButton = flipCameraButton,
                        captureModeCarousel = captureModeCarousel,
                        quickSettingsToggleButton = quickSettingsButton,
                        captureModeToggleSwitch = captureModeToggle,
                        debugVisibilityWrapper = debugVisibilityWrapper
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(
                                WindowInsets.systemBarsIgnoringVisibility
                                    .union(WindowInsets.displayCutout)
                            )
                    ) {
                        snackBar(Modifier, scaffoldState.snackbarHostState)
                        screenFlashOverlay(Modifier)
                    }
                } else {
                    Column {
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
                        snackBar(Modifier, scaffoldState.snackbarHostState)
                        screenFlashOverlay(Modifier)
                    }
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

private enum class CaptureSlotId {
    Viewfinder,
    TopBar,
    ElapsedTime,
    ZoomBar,
    CaptureRow,
    ModeSwitcher,
    BottomToolbar
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdaptivePortraitCaptureLayout(
    windowWidth: Dp,
    windowHeight: Dp,
    viewfinder: @Composable (Modifier) -> Unit,
    indicatorRow: @Composable (Modifier) -> Unit,
    elapsedTimeDisplay: @Composable (Modifier) -> Unit,
    zoomControls: @Composable (Modifier) -> Unit,
    imageWell: @Composable (Modifier) -> Unit,
    captureButton: @Composable (Modifier) -> Unit,
    flipCameraButton: @Composable (Modifier) -> Unit,
    captureModeCarousel: @Composable (Modifier) -> Unit,
    quickSettingsToggleButton: @Composable (Modifier) -> Unit,
    captureModeToggleSwitch: @Composable (Modifier) -> Unit,
    debugVisibilityWrapper: (@Composable (@Composable () -> Unit) -> Unit)
) {
    val cutoutTopInset = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBarsIgnoringVisibility
        .asPaddingValues()
        .calculateBottomPadding()
    val rawGestureInset = WindowInsets.systemGestures
        .asPaddingValues()
        .calculateBottomPadding()
    val effectiveGestureInset = max(24.dp, rawGestureInset)
    val minTouchTarget = LocalMinimumInteractiveComponentSize.current.takeOrElse { 48.dp }
    val topBarHeight = max(cutoutTopInset, minTouchTarget)

    val window = remember(
        windowWidth,
        windowHeight,
        topBarHeight,
        navInset,
        effectiveGestureInset
    ) {
        CameraWindow(
            width = windowWidth,
            height = windowHeight,
            topInset = topBarHeight,
            navInset = navInset,
            gestureInset = effectiveGestureInset
        )
    }

    // The solve depends only on the window geometry, the insets, and the accessibility touch
    // target. Row heights are deliberately *not* measured and fed back in: ControlRow.height is
    // contractually the row's *reserved* height, so that showing or hiding a row's content never
    // moves the rows around it. Measuring the children and re-solving would both break that
    // guarantee and turn the solve into a per-measure-pass cost.
    val spec = remember(minTouchTarget) {
        CameraLayoutDefaults.spec(minInteractiveTouchTarget = minTouchTarget)
    }
    val solution = remember(spec, window) { CameraLayoutSolver.solve(spec, window) }

    ImmersiveNavigationBarEffect(requiresImmersive = solution.requiresImmersive)

    CompositionLocalProvider(LocalCameraLayoutSolution provides solution) {
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                Box(modifier = Modifier.layoutId(CaptureSlotId.Viewfinder)) {
                    viewfinder(Modifier)
                }

                Box(
                    modifier = Modifier
                        .layoutId(CaptureSlotId.TopBar)
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

                debugVisibilityWrapper {
                    Box(
                        modifier = Modifier.layoutId(CaptureSlotId.ElapsedTime),
                        contentAlignment = Alignment.Center
                    ) {
                        elapsedTimeDisplay(Modifier)
                    }

                    Box(
                        modifier = Modifier.layoutId(CaptureSlotId.ZoomBar),
                        contentAlignment = Alignment.Center
                    ) {
                        zoomControls(Modifier)
                    }

                    Row(
                        modifier = Modifier
                            .layoutId(CaptureSlotId.CaptureRow)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                imageWell(Modifier)
                            }
                        }

                        OverlapAwareStyleProvider(overlapThreshold = 0.5f) {
                            captureButton(Modifier)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            flipCameraButton(Modifier)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .layoutId(CaptureSlotId.ModeSwitcher)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        captureModeCarousel(Modifier)
                    }

                    Row(
                        modifier = Modifier
                            .layoutId(CaptureSlotId.BottomToolbar)
                            .fillMaxWidth()
                            .padding(horizontal = LOWER_SECTION_HORIZONTAL_PADDING),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            quickSettingsToggleButton(Modifier)
                        }

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            captureModeToggleSwitch(Modifier)
                        }

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterEnd
                        ) {}
                    }
                }
            }
        ) { measurables, constraints ->
            val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
            val fullWidthLooseHeight = constraints.copy(minHeight = 0)

            // Each control row is measured against the band the solver reserved for it, so a row's
            // content can never push its neighbours around. The solver has already accounted for
            // toolbar compaction, so BOTTOM_TOOLBAR's band is the compacted height where that
            // applies.
            fun bandOf(rowId: String) = solution.rowBands.getValue(rowId)

            fun measureInBand(slot: CaptureSlotId, rowId: String): Pair<Placeable, Int>? {
                val measurable = measurables.firstOrNull { it.layoutId == slot } ?: return null
                val band = bandOf(rowId)
                val placeable = measurable.measure(
                    Constraints.fixed(constraints.maxWidth, band.height.roundToPx())
                )
                return placeable to band.top.roundToPx()
            }

            val topBarPlaceable = measurables
                .firstOrNull { it.layoutId == CaptureSlotId.TopBar }
                ?.measure(fullWidthLooseHeight)
            val elapsedTimePlaceable = measurables
                .firstOrNull { it.layoutId == CaptureSlotId.ElapsedTime }
                ?.measure(looseConstraints)

            val zoomBar = measureInBand(CaptureSlotId.ZoomBar, CameraRowIds.ZOOM_BAR)
            val captureRow = measureInBand(CaptureSlotId.CaptureRow, CameraRowIds.CAPTURE_ROW)
            val modeSwitcher = measureInBand(CaptureSlotId.ModeSwitcher, CameraRowIds.MODE_SWITCHER)
            val bottomToolbar =
                measureInBand(CaptureSlotId.BottomToolbar, CameraRowIds.BOTTOM_TOOLBAR)

            val viewfinderPlaceable = measurables
                .firstOrNull { it.layoutId == CaptureSlotId.Viewfinder }
                ?.measure(looseConstraints)

            layout(constraints.maxWidth, constraints.maxHeight) {
                viewfinderPlaceable?.let { vf ->
                    val vfY = if (vf.height in 1 until constraints.maxHeight) {
                        val ratio = vf.width.toFloat() / vf.height.toFloat()
                        solution.viewfinderBandFor(ratio, spec.viewfinders)?.top?.roundToPx() ?: 0
                    } else {
                        0
                    }
                    val vfX = (constraints.maxWidth - vf.width) / 2
                    vf.placeRelative(vfX, vfY)
                }

                topBarPlaceable?.placeRelative(0, 0)

                // The elapsed time readout is not a solver row: it is an optional caption that
                // hangs above the zoom bar, so it is placed relative to that band rather than
                // reserving a band of its own.
                zoomBar?.let { (_, zoomTop) ->
                    elapsedTimePlaceable?.let { elapsed ->
                        val elapsedX = (constraints.maxWidth - elapsed.width) / 2
                        elapsed.placeRelative(elapsedX, zoomTop - elapsed.height)
                    }
                }

                zoomBar?.let { (placeable, top) -> placeable.placeRelative(0, top) }
                captureRow?.let { (placeable, top) -> placeable.placeRelative(0, top) }
                modeSwitcher?.let { (placeable, top) -> placeable.placeRelative(0, top) }
                bottomToolbar?.let { (placeable, top) -> placeable.placeRelative(0, top) }
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
        val isPortrait = (maxHeight / maxWidth) >= (16f / 9f - 0.01f)
        val navBarBottom = WindowInsets.navigationBarsIgnoringVisibility
            .asPaddingValues()
            .calculateBottomPadding()

        val topInset = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
        val minTouchTarget = LocalMinimumInteractiveComponentSize.current.takeOrElse { 48.dp }
        val topBarHeight = max(topInset, minTouchTarget)
        val viewfinder34Height = maxWidth * 4f / 3f
        val viewfinder34BottomFromTop = topBarHeight + viewfinder34Height
        // Available space below the 3:4 viewfinder baseline
        val spaceBelowViewfinder34 = maxHeight - (viewfinder34BottomFromTop - topInset)

        // Dynamic bounds computed on the fly from constituent element constraints
        val maxBottomPad = max(TARGET_BOTTOM_CLEARANCE - navBarBottom, MIN_NAV_MARGIN)
        val minBottomPad = min(MIN_CONTROLS_BOTTOM_PADDING, maxBottomPad)

        val minStackHeight = SHUTTER_STACK_HEIGHT +
            MIDDLE_SLOT_HEIGHT +
            LOWER_SECTION_HEIGHT +
            (CONTROL_STACK_MIN_GAP * 2) +
            minBottomPad

        val maxStackHeight = SHUTTER_STACK_HEIGHT +
            MIDDLE_SLOT_HEIGHT +
            LOWER_SECTION_HEIGHT +
            (CONTROL_STACK_MAX_GAP * 2) +
            maxBottomPad

        // In portrait, distribute the padding space linearly with the available space below the
        // 3:4 viewfinder so the shutter button never overlaps the live preview while smoothly
        // expanding to the target design spec spacing on taller screens.
        val fraction = if (isPortrait) {
            if (maxStackHeight > minStackHeight) {
                ((spaceBelowViewfinder34 - minStackHeight) / (maxStackHeight - minStackHeight))
                    .coerceIn(0f, 1f)
            } else {
                1f
            }
        } else {
            if (maxHeight < SHORT_SCREEN_THRESHOLD) 0f else 1f
        }

        val stackGap = lerp(CONTROL_STACK_MIN_GAP, CONTROL_STACK_MAX_GAP, fraction)
        val bottomPad = lerp(minBottomPad, maxBottomPad, fraction)

        // Shutter top measured up from the bottom of this controls Box
        val shutterTopFromBottom = bottomPad + LOWER_SECTION_HEIGHT + stackGap +
            MIDDLE_SLOT_HEIGHT + stackGap + SHUTTER_STACK_HEIGHT

        val zoomGap = if (isPortrait) {
            // In standard portrait orientation, anchor the zoom bar to the 3:4 viewfinder
            // bottom baseline (matching reference camera app behavior).
            // This places the zoom bar cleanly inside the 3:4 preview, keeps it locked at the
            // same physical coordinate when switching between 3:4 and 9:16 aspect ratios,
            // and adapts across device screen heights while respecting a minimum safety gap
            // above the shutter button.
            val targetGap = (spaceBelowViewfinder34 + ZOOM_VIEWFINDER_BOTTOM_PADDING) -
                shutterTopFromBottom
            max(targetGap, MIN_ZOOM_TO_SHUTTER_GAP)
        } else {
            if (maxHeight < SHORT_SCREEN_THRESHOLD) {
                MIN_ZOOM_TO_SHUTTER_GAP
            } else {
                ZOOM_TO_SHUTTER_GAP
            }
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
@PreviewPortraitDevices
@Composable
private fun CaptureLayoutPreview() {
    PreviewLayout(
        modifier = Modifier.background(Color.Black),
        viewfinder = { modifier ->
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
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
