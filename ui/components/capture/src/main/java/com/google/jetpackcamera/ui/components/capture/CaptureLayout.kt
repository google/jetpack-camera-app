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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

/** Available height below which the control stack compresses its gaps. */
private val SHORT_SCREEN_THRESHOLD = 600.dp

/** Horizontal inset of the lower controls row and top indicator bar. */
private val LOWER_SECTION_HORIZONTAL_PADDING = 16.dp

/** Minimum height/width aspect ratio treated as a portrait handheld window (16:9 with float slack). */
private const val PORTRAIT_ASPECT_RATIO_THRESHOLD = 16f / 9f - 0.01f

/** Minimum bottom gesture inset floor used when solving the portrait control stack. */
private val MIN_GESTURE_INSET = 32.dp

/**
 * The top edge of the visible viewfinder, reported by a viewfinder that fills its slot and offsets
 * its own frame (such as [PreviewDisplay]).
 *
 * [PreviewLayout] reads this line to anchor viewfinder-relative content when the viewfinder slot
 * itself starts at the top of the window. A viewfinder that does not report the line is treated
 * as starting at its own top edge.
 */
internal val ViewfinderTopLine = HorizontalAlignmentLine { a, b -> minOf(a, b) }

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
 * never moves the surrounding controls. The content is centered on the slot and may be taller than
 * it, for example to provide a larger touch target, extending equally into the gaps above and
 * below the slot without overlapping the neighboring rows.
 * @param quickSettingsOverlay the quick settings overlay composable
 * @param debugOverlay the debug overlay composable
 * @param debugVisibilityWrapper A wrapper that conditionally hides its contents based on debug
 * settings
 * @param screenFlashOverlay the screen flash overlay composable, drawn above all other content
 *   (including the quick settings sheet) and across the entire window, including the system bar
 *   and display cutout areas
 * @param snackBar the snack bar composable for showing messages
 * @param topStartContent optional composable content placed at the top-start of the capture
 * screen, at a fixed position directly below the top status/indicator bar
 * @param viewfinderTopEndContent optional composable content anchored to the top-end corner of the
 * viewfinder. While [topStartContent] is inside the viewfinder, this content shares its vertical
 * origin directly below the top bar, so both top slots stay aligned. When the viewfinder shifts
 * below [topStartContent], this content moves down with the top edge of the viewfinder while
 * [topStartContent] stays in place.
 * @param onRequiresImmersiveChange invoked with `true` when the window is too short for the
 * tallest viewfinder to clear a persistent navigation bar, and with `false` when it no longer is
 * or when the adaptive portrait layout leaves the composition. The owner of the system bars should
 * hide the navigation bar while this is `true`, for example by passing
 * `keepNavigationBar = false` to [CameraSystemBarsEffect]. This layout does not change the system
 * bars itself.
 * @param errorDialog the camera error alert dialog composable
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Suppress("DEPRECATION")
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
    snackBar: @Composable (Modifier, snackbarHostState: SnackbarHostState) -> Unit,
    topStartContent: @Composable (Modifier) -> Unit = {},
    viewfinderTopEndContent: @Composable (Modifier) -> Unit = {},
    onRequiresImmersiveChange: (Boolean) -> Unit = {},
    errorDialog: @Composable (Modifier) -> Unit = {}
) {
    val overlapTargetBounds = remember { mutableStateOf(Rect.Zero) }
    val dismissQuickSettingsLabel =
        stringResource(R.string.quick_settings_btn_close_expanded_settings_description)

    CompositionLocalProvider(LocalOverlapTargetBounds provides overlapTargetBounds) {
        Box(modifier = modifier.fillMaxSize()) {
            BottomSheetScaffold(
                modifier = Modifier.fillMaxSize(),
                scaffoldState = scaffoldState,
                sheetPeekHeight = 0.dp,
                sheetDragHandle = {
                    BottomSheetDefaults.DragHandle(
                        modifier = Modifier
                            .testTag(QUICK_SETTINGS_DRAG_HANDLE)
                            .clickable(
                                role = Role.Button,
                                onClickLabel = dismissQuickSettingsLabel,
                                onClick = onDismissQuickSettings
                            )
                    )
                },
                sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                sheetContent = {
                    quickSettingsOverlay(Modifier)
                },
                snackbarHost = {}
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    // The adaptive portrait solver targets portrait handhelds (16:9 and taller,
                    // where a full-width 9:16 frame fits inside the window). Squarer windows
                    // (landscape, foldable inner displays, or short split-screen panes like
                    // 360x500) fall back to VerticalMaterialControls, which compresses below
                    // SHORT_SCREEN_THRESHOLD.
                    val isHandheldPortrait =
                        (maxHeight.value / maxWidth.value) >= PORTRAIT_ASPECT_RATIO_THRESHOLD
                    if (isHandheldPortrait) {
                        AdaptivePortraitCaptureLayout(
                            windowWidth = maxWidth,
                            windowHeight = maxHeight,
                            viewfinder = viewfinder,
                            topStartContent = topStartContent,
                            viewfinderTopEndContent = viewfinderTopEndContent,
                            onRequiresImmersiveChange = onRequiresImmersiveChange,
                            indicatorRow = indicatorRow,
                            elapsedTimeDisplay = elapsedTimeDisplay,
                            zoomControls = zoomLevelDisplay,
                            imageWell = imageWell,
                            captureButton = captureButton,
                            flipCameraButton = flipCameraButton,
                            captureModeCarousel = captureModeCarousel,
                            quickSettingsToggleButton = quickSettingsButton,
                            captureModeToggleSwitch = captureModeToggle,
                            debugVisibilityWrapper = debugVisibilityWrapper,
                            snackbarHost = { modifier ->
                                PillSnackbarHost(
                                    snackbarHostState = scaffoldState.snackbarHostState,
                                    modifier = modifier
                                )
                            }
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
                                        WindowInsets.displayCutout.only(
                                            WindowInsetsSides.Horizontal
                                        )
                                    )
                                    .padding(horizontal = LOWER_SECTION_HORIZONTAL_PADDING),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                indicatorRow(Modifier)
                            }
                            Box {
                                viewfinder(Modifier)
                                topStartContent(Modifier.align(Alignment.TopStart))
                                viewfinderTopEndContent(Modifier.align(Alignment.TopEnd))
                            }
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
                                    elapsedTimeDisplay = elapsedTimeDisplay,
                                    snackbarHost = { modifier ->
                                        PillSnackbarHost(
                                            snackbarHostState = scaffoldState.snackbarHostState,
                                            modifier = modifier
                                        )
                                    }
                                )
                            }
                            snackBar(Modifier, scaffoldState.snackbarHostState)
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
                                    onClickLabel = dismissQuickSettingsLabel,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onDismissQuickSettings
                                )
                        )
                    }
                    errorDialog(Modifier)
                }
            }
            // Drawn last and outside of both the inset-padded controls and the bottom sheet
            // so that the flash covers the entire window, including the system bar and display
            // cutout areas.
            screenFlashOverlay(Modifier)
        }
    }
}

private enum class CaptureSlotId {
    Viewfinder,
    TopStartContent,
    ViewfinderTopEndContent,
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
    topStartContent: @Composable (Modifier) -> Unit,
    viewfinderTopEndContent: @Composable (Modifier) -> Unit,
    onRequiresImmersiveChange: (Boolean) -> Unit,
    indicatorRow: @Composable (Modifier) -> Unit,
    elapsedTimeDisplay: @Composable (Modifier) -> Unit,
    zoomControls: @Composable (Modifier) -> Unit,
    imageWell: @Composable (Modifier) -> Unit,
    captureButton: @Composable (Modifier) -> Unit,
    flipCameraButton: @Composable (Modifier) -> Unit,
    captureModeCarousel: @Composable (Modifier) -> Unit,
    quickSettingsToggleButton: @Composable (Modifier) -> Unit,
    captureModeToggleSwitch: @Composable (Modifier) -> Unit,
    debugVisibilityWrapper: (@Composable (@Composable () -> Unit) -> Unit),
    snackbarHost: @Composable (Modifier) -> Unit = {}
) {
    val cutoutTopInset = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBarsIgnoringVisibility
        .asPaddingValues()
        .calculateBottomPadding()
    val rawGestureInset = WindowInsets.systemGestures
        .asPaddingValues()
        .calculateBottomPadding()
    val effectiveGestureInset = max(MIN_GESTURE_INSET, rawGestureInset)
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

    // Report whether the navigation bar has to be hidden rather than hiding it here, so that a
    // single owner controls system bar visibility. Leaving the composition reports `false`, so the
    // request never outlives this layout.
    val currentOnRequiresImmersiveChange by rememberUpdatedState(onRequiresImmersiveChange)
    DisposableEffect(solution.requiresImmersive) {
        currentOnRequiresImmersiveChange(solution.requiresImmersive)
        onDispose { currentOnRequiresImmersiveChange(false) }
    }

    CompositionLocalProvider(LocalCameraLayoutSolution provides solution) {
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                Box(modifier = Modifier.layoutId(CaptureSlotId.Viewfinder)) {
                    viewfinder(Modifier)
                }

                Box(modifier = Modifier.layoutId(CaptureSlotId.TopStartContent)) {
                    topStartContent(Modifier)
                }

                Box(modifier = Modifier.layoutId(CaptureSlotId.ViewfinderTopEndContent)) {
                    viewfinderTopEndContent(Modifier)
                }

                Box(
                    modifier = Modifier
                        .layoutId(CaptureSlotId.TopBar)
                        .fillMaxWidth()
                        .height(topBarHeight)
                        .windowInsetsPadding(
                            WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)
                        )
                        .padding(horizontal = LOWER_SECTION_HORIZONTAL_PADDING),
                    contentAlignment = Alignment.CenterStart
                ) {
                    indicatorRow(Modifier)
                }

                debugVisibilityWrapper {
                    Column(
                        modifier = Modifier.layoutId(CaptureSlotId.ElapsedTime),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        snackbarHost(Modifier)
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
                                OverlapAwareStyleProvider {
                                    imageWell(Modifier)
                                }
                            }
                        }

                        OverlapAwareStyleProvider {
                            captureButton(Modifier)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            OverlapAwareStyleProvider {
                                flipCameraButton(Modifier)
                            }
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
                            OverlapAwareStyleProvider {
                                quickSettingsToggleButton(Modifier)
                            }
                        }

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            OverlapAwareStyleProvider {
                                captureModeToggleSwitch(Modifier)
                            }
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
            val topStartPlaceable = measurables
                .firstOrNull { it.layoutId == CaptureSlotId.TopStartContent }
                ?.measure(looseConstraints)
            val elapsedTimePlaceable = measurables
                .firstOrNull { it.layoutId == CaptureSlotId.ElapsedTime }
                ?.measure(looseConstraints)

            val zoomBar = measureInBand(CaptureSlotId.ZoomBar, CameraRowIds.ZOOM_BAR)
            val captureRow = measureInBand(CaptureSlotId.CaptureRow, CameraRowIds.CAPTURE_ROW)
            // The mode switcher's band is the switcher's reserved visual height. Its content may
            // be taller, for example a carousel with a larger touch target, so it is measured up to
            // the band plus the smaller of the two gaps around it, once on each side, and centered
            // on the band. The content then never overlaps the neighboring rows and never moves
            // them.
            val modeSwitcher = measurables
                .firstOrNull { it.layoutId == CaptureSlotId.ModeSwitcher }
                ?.let { measurable ->
                    val band = bandOf(CameraRowIds.MODE_SWITCHER)
                    val clearance = min(
                        band.top - bandOf(CameraRowIds.CAPTURE_ROW).bottom,
                        bandOf(CameraRowIds.BOTTOM_TOOLBAR).top - band.bottom
                    ).coerceAtLeast(0.dp)
                    val bandHeightPx = band.height.roundToPx()
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = constraints.maxWidth,
                            maxWidth = constraints.maxWidth,
                            minHeight = bandHeightPx,
                            maxHeight = (band.height + clearance * 2).roundToPx()
                        )
                    )
                    placeable to band.top.roundToPx() - (placeable.height - bandHeightPx) / 2
                }
            val bottomToolbar =
                measureInBand(CaptureSlotId.BottomToolbar, CameraRowIds.BOTTOM_TOOLBAR)

            val viewfinderPlaceable = measurables
                .firstOrNull { it.layoutId == CaptureSlotId.Viewfinder }
                ?.measure(looseConstraints)

            // A viewfinder that is exactly as tall as one of the solved frames is placed at that
            // frame's band. A viewfinder that fills the slot (such as PreviewDisplay) positions its
            // own frame and is placed at the top.
            val viewfinderY = viewfinderPlaceable?.let { vf ->
                if (vf.height in 1 until constraints.maxHeight) {
                    val ratio = vf.width.toFloat() / vf.height.toFloat()
                    solution.viewfinderBandFor(ratio, spec.viewfinders)?.top?.roundToPx()
                } else {
                    null
                }
            } ?: 0
            // The top edge of the visible frame, in this layout's coordinates. Viewfinders that
            // position their own frame report it through ViewfinderTopLine.
            val activeVfTopPx = viewfinderPlaceable?.let { vf ->
                val reportedTop = vf[ViewfinderTopLine]
                viewfinderY + if (reportedTop == AlignmentLine.Unspecified) 0 else reportedTop
            }

            // While topStartContent is inside the viewfinder (full-bleed, flush with the top bar,
            // or a frame that passes behind it), viewfinderTopEndContent shares its vertical
            // origin. Once the frame shifts below topStartContent, it follows the frame's top edge.
            val topBarHeightPx = topBarHeight.roundToPx()
            val viewfinderTopEndY = if (
                activeVfTopPx == null ||
                activeVfTopPx <= (topBarHeight + spec.topStartHeight).roundToPx()
            ) {
                topBarHeightPx
            } else {
                activeVfTopPx
            }
            val viewfinderEnd = viewfinderPlaceable?.let { vf ->
                (constraints.maxWidth + vf.width) / 2
            } ?: constraints.maxWidth
            // While the two top slots overlap vertically, viewfinderTopEndContent is limited to
            // the width left beside topStartContent so that the two never overlap.
            val sharesTopStartRows = topStartPlaceable != null &&
                viewfinderTopEndY < topBarHeightPx + topStartPlaceable.height
            val viewfinderTopEndMaxWidth = if (sharesTopStartRows) {
                viewfinderEnd - topStartPlaceable.width
            } else {
                viewfinderEnd
            }.coerceAtLeast(0)
            val viewfinderTopEndPlaceable = measurables
                .firstOrNull { it.layoutId == CaptureSlotId.ViewfinderTopEndContent }
                ?.measure(looseConstraints.copy(maxWidth = viewfinderTopEndMaxWidth))

            layout(constraints.maxWidth, constraints.maxHeight) {
                viewfinderPlaceable?.let { vf ->
                    val vfX = (constraints.maxWidth - vf.width) / 2
                    vf.placeRelative(vfX, viewfinderY)
                }

                topStartPlaceable?.placeRelative(0, topBarHeightPx)
                viewfinderTopEndPlaceable?.let { topEnd ->
                    topEnd.placeRelative(viewfinderEnd - topEnd.width, viewfinderTopEndY)
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
    elapsedTimeDisplay: @Composable (Modifier) -> Unit,
    snackbarHost: @Composable (Modifier) -> Unit = {}
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // This layout is only used for windows that are not portrait handhelds (see
        // PreviewLayout), so it does not track the viewfinder. Short windows, such as landscape
        // phones, compress the stack to its minimum spacing.
        val navBarBottom = WindowInsets.navigationBarsIgnoringVisibility
            .asPaddingValues()
            .calculateBottomPadding()
        val isShortScreen = maxHeight < SHORT_SCREEN_THRESHOLD
        val maxBottomPad = max(TARGET_BOTTOM_CLEARANCE - navBarBottom, MIN_NAV_MARGIN)
        val bottomPad = if (isShortScreen) {
            min(MIN_CONTROLS_BOTTOM_PADDING, maxBottomPad)
        } else {
            maxBottomPad
        }
        val stackGap = if (isShortScreen) CONTROL_STACK_MIN_GAP else CONTROL_STACK_MAX_GAP
        val zoomGap = if (isShortScreen) MIN_ZOOM_TO_SHUTTER_GAP else ZOOM_TO_SHUTTER_GAP

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                snackbarHost(Modifier)

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
                                OverlapAwareStyleProvider {
                                    imageWell(Modifier)
                                }
                            }
                        }

                        OverlapAwareStyleProvider {
                            captureButton(Modifier)
                        }

                        // right capturebutton item
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            OverlapAwareStyleProvider {
                                flipCameraButton(Modifier)
                            }
                        }
                    }
                }

                // Slot reserved by the design spec between the shutter row and the lower controls
                // row, together with the gaps above and below it. Its height is fixed whether or
                // not it has content, so the shutter row never moves when content appears here.
                // Content is centered, so content taller than MIDDLE_SLOT_HEIGHT, such as a carousel
                // with a larger touch target, extends equally into both gaps.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MIDDLE_SLOT_HEIGHT + stackGap * 2),
                    contentAlignment = Alignment.Center
                ) {
                    captureModeCarousel(Modifier)
                }

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
                        OverlapAwareStyleProvider {
                            quickSettingsToggleButton(Modifier)
                        }
                    }

                    // capture mode toggle switch center
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        OverlapAwareStyleProvider {
                            captureModeToggleSwitch(Modifier)
                        }
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
