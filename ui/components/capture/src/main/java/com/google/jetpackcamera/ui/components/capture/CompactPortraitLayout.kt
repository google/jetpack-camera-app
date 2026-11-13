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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.takeOrElse
import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutDefaults
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutSolution
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutSolver
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutSpec
import com.google.jetpackcamera.ui.components.capture.layout.CameraRowIds
import com.google.jetpackcamera.ui.components.capture.layout.CameraWindow
import com.google.jetpackcamera.ui.components.capture.layout.LocalCameraLayoutSolution

/**
 * CompositionLocal providing the exact active posture layout composable name rendered by
 * [CaptureScaffoldLayout] (`"CompactPortraitLayout"`, `"CompactShortLayout"`, `"FoldableLayout"`,
 * `"TabletPortraitLayout"`, or `"TabletLandscapeLayout"`).
 */
val LocalActiveCaptureLayout: ProvidableCompositionLocal<String> =
    compositionLocalOf { "CompactPortraitLayout" }

/**
 * Dimension and spacing constants used by [CompactPortraitLayout].
 */
internal object CompactPortraitTokens {
    val SheetPeekHeight = 0.dp
    val SheetTopCornerRadius = 28.dp
    val DefaultMinTouchTarget = 48.dp
    val ZeroInset = 0.dp

    /**
     * Height of the shutter row (capture button, image well and flip camera button) as specified
     * by the design spec.
     */
    val ShutterStackHeight = 86.dp

    /**
     * Height of the slot reserved between the shutter row and the lower controls row. Reserved
     * unconditionally so optional content can appear without shifting surrounding controls.
     */
    val MiddleSlotHeight = 32.dp

    /**
     * Height of the lower controls row (quick settings and capture mode toggles).
     */
    val LowerSectionHeight = 64.dp

    /** Vertical gap between the zoom bar and the shutter row on standard screens. */
    val ZoomToShutterGap = 32.dp

    /** Minimum safety gap between the zoom bar and the shutter row on short screens. */
    val MinZoomToShutterGap = 16.dp

    /** Maximum target vertical gap above and below the reserved middle slot. */
    val ControlStackMaxGap = 24.dp

    /** Minimum compressed vertical gap above and below the reserved middle slot. */
    val ControlStackMinGap = 12.dp

    /**
     * Target clearance from the bottom of the display to the lower controls row across navigation
     * bar modes.
     */
    val TargetBottomClearance = 56.dp

    /** Minimum safety margin between the lower controls row and the navigation bar. */
    val MinNavMargin = 8.dp

    /** Minimum compressed bottom padding used on short screens. */
    val MinControlsBottomPadding = 12.dp

    /** Available height below which the fallback control stack compresses its gaps. */
    val ShortScreenThreshold = 600.dp

    /** Horizontal inset of the lower controls row and top indicator bar. */
    val LowerSectionHorizontalPadding = 16.dp

    /** Minimum height/width aspect ratio treated as a portrait handheld window (16:9). */
    const val PORTRAIT_ASPECT_RATIO_THRESHOLD = 16f / 9f - 0.01f

    /** Minimum bottom gesture inset floor used when solving the portrait control stack. */
    val MinGestureInset = 32.dp
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

/**
 * Prunes keyboard/rotary focus and Android accessibility traversal on background camera chrome
 * while an in-hierarchy modal surface (such as Quick Settings or an error dialog) is open above it,
 * without stripping Compose test tags or semantic actions inside [PreviewOverlaySlotScope] slots.
 */
internal fun Modifier.suppressBackgroundForModal(isModalOpen: Boolean): Modifier =
    if (isModalOpen) {
        this
            .focusProperties { canFocus = false }
            .semantics { hideFromAccessibility() }
    } else {
        this
    }

/**
 * Compact capture layout driven by a [CaptureSlots] slot table.
 *
 * Hosts the camera viewfinder, top status/indicator bar, bottom control stack, and an in-hierarchy
 * [BottomSheetScaffold] for Quick Settings controlled by [state] without spawning secondary
 * windows. Tags its root container with [COMPACT_CONTROLS_TAG], provides
 * [LocalActiveCaptureLayout] = `"CompactPortraitLayout"`, counter-rotates into canonical portrait
 * coordinates when `windowWidth > windowHeight` (`COMPACT` / `COMPACT_SLIM` horizontal postures),
 * and scopes [LocalCaptureRtlPolicy] (`mirrorCompactBottomBar`) exclusively to the horizontal
 * capture and bottom-toolbar rows while restoring the active locale's `LayoutDirection` inside
 * every child slot.
 *
 * @param slots [CaptureSlots] supplying the capture screen UI controls and overlays.
 * @param viewfinder Composable rendering the camera preview frame.
 * @param modifier [Modifier] applied to the outer layout container.
 * @param state Mode-agnostic [CaptureScaffoldState] controlling the Quick Settings bottom sheet and
 *   snackbar host.
 * @param deviceRotation Physical gravity orientation of the device.
 * @param windowRotation Display/window rotation reported by Android.
 * @param controlsVisible Whether the camera control slots should be composed and visible.
 * @param isErrorDialogShowing Whether a modal camera error dialog is currently visible above the
 *   capture chrome.
 * @param includeStatusBarsInTopBar Whether live visible `WindowInsets.statusBars` top padding is
 *   included in the top indicator bar so multi-window status bars never overlap status indicators.
 * @param onDismissQuickSettings Callback invoked when Quick Settings is dismissed via the drag
 *   handle or scrim.
 * @param onRequiresImmersiveChange Callback notified when the portrait layout solver requires or
 *   releases immersive navigation-bar hiding.
 * @param overlay Optional overlay composable drawn above the viewfinder and controls.
 * @param screenFlashOverlay Full-bleed screen flash composable drawn above all chrome and system
 *   bar areas.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CompactPortraitLayout(
    slots: CaptureSlots,
    viewfinder: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    state: CaptureScaffoldState = rememberCaptureScaffoldState(),
    deviceRotation: DeviceRotation = LocalDeviceRotation.current,
    windowRotation: DeviceRotation = LocalWindowRotation.current,
    controlsVisible: Boolean = true,
    isErrorDialogShowing: Boolean = false,
    includeStatusBarsInTopBar: Boolean = true,
    onDismissQuickSettings: () -> Unit = {},
    onRequiresImmersiveChange: (Boolean) -> Unit = {},
    overlay: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    screenFlashOverlay: @Composable (Modifier) -> Unit = {}
) {
    val captureButton = slots.captureButton
    val imageWell = slots.imageWell
    val flipCameraButton = slots.flipCameraButton
    val zoomLevelDisplay = slots.zoomControls
    val elapsedTimeDisplay = slots.elapsedTimeDisplay
    val quickSettingsButton = slots.quickSettingsButton
    val indicatorRow = slots.topBar
    val captureModeToggle = slots.captureModeToggle
    val captureModeCarousel = slots.captureModeCarousel
    val snackBar = slots.snackBar
    val topStartContent = slots.topStartContent
    val viewfinderTopEndContent = slots.viewfinderTopEndContent
    val topBarTrailingContent = slots.topBarTrailingContent
    val errorDialog = slots.errorDialog
    val bottomSheetState = rememberBottomSheetState(
        initialValue = if (state.isQuickSettingsOpen) SheetValue.Expanded else SheetValue.Hidden,
        confirmValueChange = { targetValue ->
            if (targetValue == SheetValue.Hidden) {
                state.dismissQuickSettings()
            } else if (targetValue == SheetValue.Expanded) {
                state.showQuickSettings()
            }
            true
        }
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState,
        snackbarHostState = state.snackbarHostState
    )
    LaunchedEffect(state.isQuickSettingsOpen) {
        if (state.isQuickSettingsOpen) {
            if (bottomSheetState.targetValue != SheetValue.Expanded) {
                bottomSheetState.expand()
            }
        } else {
            if (bottomSheetState.isVisible || bottomSheetState.targetValue != SheetValue.Hidden) {
                bottomSheetState.hide()
            }
        }
    }

    val overlapTargetBounds = remember { mutableStateOf(Rect.Zero) }
    val localeDirection = LocalLayoutDirection.current
    val dismissQuickSettingsLabel =
        stringResource(R.string.quick_settings_btn_close_expanded_settings_description)
    val handleDismissQuickSettings = {
        state.dismissQuickSettings()
        onDismissQuickSettings()
    }

    SeamlessRotationAnimationEffect()

    val isSheetVisible = state.isQuickSettingsOpen ||
        bottomSheetState.isVisible ||
        bottomSheetState.targetValue == SheetValue.Expanded
    var isErrorDialogPlaced by remember { mutableStateOf(false) }
    val isErrorModalActive = isErrorDialogShowing || isErrorDialogPlaced
    val isModalOpen = isSheetVisible || isErrorModalActive
    val externalVfMetrics = remember { ExternalViewfinderMetrics() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag(COMPACT_CONTROLS_TAG)
    ) {
        val windowWidth = maxWidth
        val windowHeight = maxHeight
        val shouldCounterRotate = windowWidth > windowHeight
        val effectiveDeviceRotation = if (
            deviceRotation == DeviceRotation.Natural &&
            windowRotation.isLandscape &&
            shouldCounterRotate
        ) {
            windowRotation
        } else {
            deviceRotation
        }
        val windowGravityDeg = effectiveDeviceRotation.toGravityRotationDegrees(windowRotation)
        val effectiveWindowRotation = when {
            !shouldCounterRotate -> DeviceRotation.Natural
            windowGravityDeg == 90 -> DeviceRotation.Rotated90
            windowGravityDeg == 270 -> DeviceRotation.Rotated270
            windowRotation.isLandscape -> windowRotation
            else -> DeviceRotation.Rotated270
        }
        val containerCounterDeg = effectiveWindowRotation.toWindowCounterRotationDegrees().toInt()
        val containerFrameRotation = DeviceRotation.snapFrom(
            ((windowRotation.toClockwiseRotationDegrees() - containerCounterDeg) % 360 + 360) % 360
        )
        val gravityRotationState = rememberGravityRotationState(
            deviceRotation = effectiveDeviceRotation,
            windowRotation = containerFrameRotation
        )
        val unrotatedGravityRotationState = rememberGravityRotationState(
            deviceRotation = effectiveDeviceRotation,
            windowRotation = windowRotation
        )

        val portraitWidth = if (shouldCounterRotate) windowHeight else windowWidth
        val portraitHeight = if (shouldCounterRotate) windowWidth else windowHeight
        val portraitAspectRatio = portraitHeight.value / portraitWidth.value
        val isHandheldPortrait =
            portraitAspectRatio >= CompactPortraitTokens.PORTRAIT_ASPECT_RATIO_THRESHOLD
        val minTouchTarget = LocalMinimumInteractiveComponentSize.current
            .takeOrElse { CompactPortraitTokens.DefaultMinTouchTarget }
        val (topBarHeight, topBarTopPadding) = computeTopBarMetrics(
            includeStatusBarsInTopBar = includeStatusBarsInTopBar,
            minTouchTarget = minTouchTarget,
            windowRotation = effectiveWindowRotation
        )
        val navInset = WindowInsets.navigationBarsIgnoringVisibility
            .toPortraitWindowInsets(effectiveWindowRotation)
            .asPaddingValues()
            .calculateBottomPadding()
        val rawGestureInset = WindowInsets.systemGestures
            .toPortraitWindowInsets(effectiveWindowRotation)
            .asPaddingValues()
            .calculateBottomPadding()
        val effectiveGestureInset = max(CompactPortraitTokens.MinGestureInset, rawGestureInset)
        val spec = remember(minTouchTarget) {
            CameraLayoutDefaults.spec(minInteractiveTouchTarget = minTouchTarget)
        }
        val portraitSolution = if (isHandheldPortrait) {
            val cameraWindow = remember(
                portraitWidth,
                portraitHeight,
                topBarHeight,
                navInset,
                effectiveGestureInset
            ) {
                CameraWindow(
                    width = portraitWidth,
                    height = portraitHeight,
                    topInset = topBarHeight,
                    navInset = navInset,
                    gestureInset = effectiveGestureInset
                )
            }
            remember(spec, cameraWindow) { CameraLayoutSolver.solve(spec, cameraWindow) }
        } else {
            null
        }

        CompositionLocalProvider(
            LocalOverlapTargetBounds provides overlapTargetBounds,
            LocalActiveCaptureLayout provides "CompactPortraitLayout",
            LocalDeviceRotation provides effectiveDeviceRotation,
            LocalWindowRotation provides if (shouldCounterRotate) {
                windowRotation
            } else {
                effectiveWindowRotation
            },
            LocalCompactCounterRotation provides effectiveWindowRotation,
            LocalGravityRotationState provides if (shouldCounterRotate) {
                unrotatedGravityRotationState
            } else {
                gravityRotationState
            },
            LocalCameraLayoutSolution provides portraitSolution,
            LocalLayoutDirection provides localeDirection
        ) {
            OuterViewfinderHost(
                viewfinder = viewfinder,
                solution = portraitSolution,
                spec = spec,
                topBarHeight = topBarHeight,
                shouldCounterRotate = shouldCounterRotate,
                counterRotation = effectiveWindowRotation,
                metrics = externalVfMetrics,
                modifier = Modifier
                    .fillMaxSize()
                    .suppressBackgroundForModal(isModalOpen)
            )
        }

        CompositionLocalProvider(
            LocalOverlapTargetBounds provides overlapTargetBounds,
            LocalActiveCaptureLayout provides "CompactPortraitLayout",
            LocalDeviceRotation provides effectiveDeviceRotation,
            LocalWindowRotation provides effectiveWindowRotation,
            LocalCompactCounterRotation provides effectiveWindowRotation,
            LocalGravityRotationState provides gravityRotationState
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .counterRotateToPortrait(effectiveWindowRotation)
            ) {
                BottomSheetScaffold(
                    modifier = Modifier.fillMaxSize(),
                    scaffoldState = scaffoldState,
                    containerColor = Color.Transparent,
                    sheetPeekHeight = CompactPortraitTokens.SheetPeekHeight,
                    sheetDragHandle = {
                        BottomSheetDefaults.DragHandle(
                            modifier = Modifier
                                .testTag(QUICK_SETTINGS_DRAG_HANDLE)
                                .minimumInteractiveComponentSize()
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = dismissQuickSettingsLabel,
                                    onClick = handleDismissQuickSettings
                                )
                        )
                    },
                    sheetShape = RoundedCornerShape(
                        topStart = CompactPortraitTokens.SheetTopCornerRadius,
                        topEnd = CompactPortraitTokens.SheetTopCornerRadius
                    ),
                    sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    sheetContent = {
                        CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                            OverlapAwareCaptureSlot(
                                orientation = Orientation.Vertical,
                                isCompact = false,
                                content = slots.quickSettingsOverlay
                            )
                        }
                    },
                    snackbarHost = {}
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val portraitViewfinderProxy: @Composable (Modifier) -> Unit = { proxyMod ->
                            ExternalViewfinderProxy(
                                metrics = externalVfMetrics,
                                modifier = proxyMod
                            )
                        }
                        if (isHandheldPortrait) {
                            AdaptivePortraitCaptureLayout(
                                windowWidth = maxWidth,
                                windowHeight = maxHeight,
                                windowRotation = effectiveWindowRotation,
                                includeStatusBarsInTopBar = includeStatusBarsInTopBar,
                                controlsVisible = controlsVisible,
                                precomputedSolution = portraitSolution,
                                modifier = Modifier.suppressBackgroundForModal(isModalOpen),
                                viewfinder = portraitViewfinderProxy,
                                topStartContent = topStartContent,
                                viewfinderTopEndContent = viewfinderTopEndContent,
                                onRequiresImmersiveChange = onRequiresImmersiveChange,
                                indicatorRow = indicatorRow,
                                topBarTrailingContent = topBarTrailingContent,
                                elapsedTimeDisplay = elapsedTimeDisplay,
                                zoomControls = zoomLevelDisplay,
                                imageWell = imageWell,
                                captureButton = captureButton,
                                flipCameraButton = flipCameraButton,
                                captureModeCarousel = captureModeCarousel,
                                quickSettingsToggleButton = quickSettingsButton,
                                captureModeToggleSwitch = captureModeToggle,
                                snackbarHost = { hostModifier ->
                                    PillSnackbarHost(
                                        snackbarHostState = state.snackbarHostState,
                                        modifier = hostModifier
                                    )
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .suppressBackgroundForModal(isModalOpen)
                                    .windowInsetsPadding(
                                        WindowInsets.systemBarsIgnoringVisibility
                                            .union(WindowInsets.displayCutout)
                                            .toPortraitWindowInsets(effectiveWindowRotation)
                                    )
                            ) {
                                CompositionLocalProvider(
                                    LocalLayoutDirection provides localeDirection
                                ) {
                                    snackBar(Modifier, state.snackbarHostState)
                                }
                            }
                        } else {
                            Column(modifier = Modifier.suppressBackgroundForModal(isModalOpen)) {
                                CompactTopBarBox(
                                    topBarHeight = topBarHeight,
                                    topBarTopPadding = topBarTopPadding,
                                    windowRotation = effectiveWindowRotation,
                                    indicatorRow = indicatorRow,
                                    topBarTrailingContent = topBarTrailingContent
                                )
                                Box {
                                    portraitViewfinderProxy(Modifier)
                                    OverlapAwareStyleProvider(Modifier.align(Alignment.TopStart)) {
                                        topStartContent(Modifier)
                                    }
                                    OverlapAwareStyleProvider(Modifier.align(Alignment.TopEnd)) {
                                        viewfinderTopEndContent(Modifier)
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .suppressBackgroundForModal(isModalOpen)
                                    .windowInsetsPadding(
                                        WindowInsets.systemBarsIgnoringVisibility
                                            .union(WindowInsets.displayCutout)
                                            .toPortraitWindowInsets(effectiveWindowRotation)
                                    )
                            ) {
                                if (controlsVisible) {
                                    VerticalMaterialControls(
                                        windowRotation = effectiveWindowRotation,
                                        captureButton = captureButton,
                                        imageWell = imageWell,
                                        flipCameraButton = flipCameraButton,
                                        quickSettingsToggleButton = quickSettingsButton,
                                        captureModeToggleSwitch = captureModeToggle,
                                        captureModeCarousel = captureModeCarousel,
                                        zoomControls = zoomLevelDisplay,
                                        elapsedTimeDisplay = elapsedTimeDisplay,
                                        snackbarHost = { hostModifier ->
                                            PillSnackbarHost(
                                                snackbarHostState = state.snackbarHostState,
                                                modifier = hostModifier
                                            )
                                        }
                                    )
                                }
                                CompositionLocalProvider(
                                    LocalLayoutDirection provides localeDirection
                                ) {
                                    snackBar(Modifier, state.snackbarHostState)
                                }
                            }
                        }

                        CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                            OverlapAwareStyleProvider(
                                modifier = Modifier.suppressBackgroundForModal(isModalOpen)
                            ) {
                                overlay(Modifier)
                            }
                        }

                        if (isSheetVisible && !isErrorModalActive) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag(QUICK_SETTINGS_SCRIM)
                                    .semantics {
                                        contentDescription = dismissQuickSettingsLabel
                                        traversalIndex = 1f
                                    }
                                    .clickable(
                                        role = Role.Button,
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = handleDismissQuickSettings
                                    )
                            )
                        }

                        CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                            Box(
                                modifier = Modifier.onSizeChanged { size ->
                                    isErrorDialogPlaced = size.width > 0 && size.height > 0
                                }
                            ) {
                                errorDialog(Modifier)
                            }
                        }
                    }
                }
                // Drawn last and outside of both the inset-padded controls and the bottom sheet so
                // the flash covers the entire window, including system bar and cutout areas.
                screenFlashOverlay(Modifier)
            }
        }
    }
}

private class ExternalViewfinderMetrics {
    var portraitWidthPx by mutableIntStateOf(0)
    var portraitHeightPx by mutableIntStateOf(0)
    var reportedTopPx by mutableIntStateOf(AlignmentLine.Unspecified)
}

@Composable
private fun OuterViewfinderHost(
    viewfinder: @Composable (Modifier) -> Unit,
    solution: CameraLayoutSolution?,
    spec: CameraLayoutSpec,
    topBarHeight: Dp,
    shouldCounterRotate: Boolean,
    counterRotation: DeviceRotation,
    metrics: ExternalViewfinderMetrics,
    modifier: Modifier = Modifier
) {
    Layout(
        modifier = modifier,
        content = { viewfinder(Modifier) }
    ) { measurables, constraints ->
        val measurable = measurables.firstOrNull()
        if (measurable == null) {
            layout(constraints.maxWidth, constraints.maxHeight) {}
        } else if (!shouldCounterRotate) {
            if (solution != null) {
                val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
                val vf = measurable.measure(looseConstraints)
                val reportedTop = vf[ViewfinderTopLine]
                val viewfinderY = if (vf.height in 1 until constraints.maxHeight) {
                    val ratio = vf.width.toFloat() / vf.height.toFloat()
                    solution.viewfinderBandFor(ratio, spec.viewfinders)?.top?.roundToPx()
                } else {
                    null
                } ?: 0
                val vfX = (constraints.maxWidth - vf.width) / 2
                if (metrics.portraitWidthPx != vf.width) metrics.portraitWidthPx = vf.width
                if (metrics.portraitHeightPx != vf.height) metrics.portraitHeightPx = vf.height
                if (metrics.reportedTopPx != reportedTop) metrics.reportedTopPx = reportedTop
                layout(constraints.maxWidth, constraints.maxHeight) {
                    vf.placeRelative(vfX, viewfinderY)
                }
            } else {
                val topBarPx = topBarHeight.roundToPx().coerceIn(0, constraints.maxHeight)
                val availHeight = (constraints.maxHeight - topBarPx).coerceAtLeast(0)
                val vf = measurable.measure(
                    Constraints(
                        minWidth = 0,
                        maxWidth = constraints.maxWidth,
                        minHeight = 0,
                        maxHeight = availHeight
                    )
                )
                if (metrics.portraitWidthPx != vf.width) metrics.portraitWidthPx = vf.width
                if (metrics.portraitHeightPx != vf.height) metrics.portraitHeightPx = vf.height
                if (metrics.reportedTopPx != AlignmentLine.Unspecified) {
                    metrics.reportedTopPx = AlignmentLine.Unspecified
                }
                layout(constraints.maxWidth, constraints.maxHeight) {
                    vf.placeRelative(0, topBarPx)
                }
            }
        } else if (solution != null) {
            val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
            val vf = measurable.measure(looseConstraints)
            val reportedTop = vf[ViewfinderTopLine]
            val xOffset = if (
                reportedTop == AlignmentLine.Unspecified &&
                vf.width in 1 until constraints.maxWidth &&
                vf.height > 0
            ) {
                val portraitRatio = vf.height.toFloat() / vf.width.toFloat()
                val bandTopPx = solution
                    .viewfinderBandFor(portraitRatio, spec.viewfinders)
                    ?.top
                    ?.roundToPx() ?: 0
                if (counterRotation == DeviceRotation.Rotated90) {
                    (constraints.maxWidth - bandTopPx - vf.width).coerceAtLeast(0)
                } else {
                    bandTopPx.coerceIn(0, (constraints.maxWidth - vf.width).coerceAtLeast(0))
                }
            } else {
                0
            }
            val yOffset = (constraints.maxHeight - vf.height) / 2
            if (metrics.portraitWidthPx != vf.height) metrics.portraitWidthPx = vf.height
            if (metrics.portraitHeightPx != vf.width) metrics.portraitHeightPx = vf.width
            if (metrics.reportedTopPx != reportedTop) metrics.reportedTopPx = reportedTop
            layout(constraints.maxWidth, constraints.maxHeight) {
                vf.place(xOffset, yOffset)
            }
        } else {
            val topBarPx = topBarHeight.roundToPx().coerceIn(0, constraints.maxWidth)
            val availWidth = (constraints.maxWidth - topBarPx).coerceAtLeast(0)
            val vf = measurable.measure(
                Constraints(
                    minWidth = 0,
                    maxWidth = availWidth,
                    minHeight = 0,
                    maxHeight = constraints.maxHeight
                )
            )
            val xOffset = if (counterRotation == DeviceRotation.Rotated90) {
                (availWidth - vf.width) / 2
            } else {
                topBarPx + (availWidth - vf.width) / 2
            }
            val yOffset = (constraints.maxHeight - vf.height) / 2
            if (metrics.portraitWidthPx != vf.height) metrics.portraitWidthPx = vf.height
            if (metrics.portraitHeightPx != vf.width) metrics.portraitHeightPx = vf.width
            if (metrics.reportedTopPx != AlignmentLine.Unspecified) {
                metrics.reportedTopPx = AlignmentLine.Unspecified
            }
            layout(constraints.maxWidth, constraints.maxHeight) {
                vf.place(xOffset, yOffset)
            }
        }
    }
}

@Composable
private fun ExternalViewfinderProxy(
    metrics: ExternalViewfinderMetrics,
    modifier: Modifier = Modifier
) {
    Layout(modifier = modifier) { _, constraints ->
        val w = metrics.portraitWidthPx.coerceIn(0, constraints.maxWidth)
        val h = metrics.portraitHeightPx.coerceIn(0, constraints.maxHeight)
        val reportedTop = metrics.reportedTopPx
        val lines: Map<AlignmentLine, Int> = if (reportedTop == AlignmentLine.Unspecified) {
            emptyMap()
        } else {
            mapOf(ViewfinderTopLine to reportedTop)
        }
        layout(w, h, alignmentLines = lines) {}
    }
}

@Composable
private fun computeTopBarMetrics(
    includeStatusBarsInTopBar: Boolean,
    minTouchTarget: Dp,
    windowRotation: DeviceRotation = LocalWindowRotation.current
): Pair<Dp, Dp> {
    val portraitCutoutInsets = WindowInsets.displayCutout.toPortraitWindowInsets(windowRotation)
    val cutoutTopInset = portraitCutoutInsets.asPaddingValues().calculateTopPadding()
    return if (includeStatusBarsInTopBar) {
        val portraitStatusBars = WindowInsets.statusBars.toPortraitWindowInsets(windowRotation)
        val statusBarTopInset = portraitStatusBars.asPaddingValues().calculateTopPadding()
        val combinedTopInset = portraitStatusBars
            .union(portraitCutoutInsets)
            .asPaddingValues()
            .calculateTopPadding()
        max(combinedTopInset, statusBarTopInset + minTouchTarget) to statusBarTopInset
    } else {
        max(cutoutTopInset, minTouchTarget) to CompactPortraitTokens.ZeroInset
    }
}

@Composable
private fun CompactTopBarBox(
    topBarHeight: Dp,
    topBarTopPadding: Dp,
    indicatorRow: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    topBarTrailingContent: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    modifier: Modifier = Modifier,
    windowRotation: DeviceRotation = LocalWindowRotation.current
) {
    val localeDirection = LocalLayoutDirection.current
    CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(topBarHeight)
                .padding(top = topBarTopPadding)
                .windowInsetsPadding(
                    WindowInsets.displayCutout
                        .toPortraitWindowInsets(windowRotation)
                        .only(WindowInsetsSides.Horizontal)
                )
                .padding(horizontal = CompactPortraitTokens.LowerSectionHorizontalPadding),
            contentAlignment = Alignment.CenterStart
        ) {
            OverlapAwareStyleProvider(orientation = Orientation.Horizontal) {
                indicatorRow(Modifier)
            }
            OverlapAwareStyleProvider(
                modifier = Modifier.align(Alignment.CenterEnd),
                orientation = Orientation.Horizontal
            ) {
                topBarTrailingContent(Modifier)
            }
        }
    }
}

@Composable
private fun CompactCaptureRow(
    bottomBarDirection: LayoutDirection,
    localeDirection: LayoutDirection,
    isCompact: Boolean,
    imageWell: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    captureButton: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    flipCameraButton: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides bottomBarDirection) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                        OverlapAwareStyleProvider(
                            orientation = Orientation.Horizontal,
                            isCompact = isCompact
                        ) {
                            imageWell(Modifier)
                        }
                    }
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                OverlapAwareStyleProvider(
                    orientation = Orientation.Horizontal,
                    isCompact = isCompact
                ) {
                    captureButton(Modifier)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                    OverlapAwareStyleProvider(
                        orientation = Orientation.Horizontal,
                        isCompact = isCompact
                    ) {
                        flipCameraButton(Modifier)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactBottomToolbar(
    bottomBarDirection: LayoutDirection,
    localeDirection: LayoutDirection,
    isCompact: Boolean,
    quickSettingsToggleButton: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    captureModeToggleSwitch: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides bottomBarDirection) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = CompactPortraitTokens.LowerSectionHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                    OverlapAwareStyleProvider(
                        orientation = Orientation.Horizontal,
                        isCompact = isCompact
                    ) {
                        quickSettingsToggleButton(Modifier)
                    }
                }
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                    OverlapAwareStyleProvider(
                        orientation = Orientation.Horizontal,
                        isCompact = isCompact
                    ) {
                        captureModeToggleSwitch(Modifier)
                    }
                }
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {}
        }
    }
}

/**
 * Constraint-solved portrait handheld layout placing the viewfinder and bottom control bands via
 * [CameraLayoutSolver].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AdaptivePortraitCaptureLayout(
    windowWidth: Dp,
    windowHeight: Dp,
    viewfinder: @Composable (Modifier) -> Unit,
    topStartContent: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    viewfinderTopEndContent: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    onRequiresImmersiveChange: (Boolean) -> Unit,
    indicatorRow: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    topBarTrailingContent: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    elapsedTimeDisplay: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    zoomControls: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    imageWell: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    captureButton: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    flipCameraButton: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    captureModeCarousel: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    quickSettingsToggleButton: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    captureModeToggleSwitch: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    controlsVisible: Boolean,
    modifier: Modifier = Modifier,
    windowRotation: DeviceRotation = LocalWindowRotation.current,
    includeStatusBarsInTopBar: Boolean = true,
    precomputedSolution: CameraLayoutSolution? = null,
    snackbarHost: @Composable (Modifier) -> Unit = {}
) {
    val localeDirection = LocalLayoutDirection.current
    val rtlPolicy = LocalCaptureRtlPolicy.current
    val bottomBarDirection = if (rtlPolicy.mirrorCompactBottomBar) {
        localeDirection
    } else {
        LayoutDirection.Ltr
    }

    val navInset = WindowInsets.navigationBarsIgnoringVisibility
        .toPortraitWindowInsets(windowRotation)
        .asPaddingValues()
        .calculateBottomPadding()
    val rawGestureInset = WindowInsets.systemGestures
        .toPortraitWindowInsets(windowRotation)
        .asPaddingValues()
        .calculateBottomPadding()
    val effectiveGestureInset = max(CompactPortraitTokens.MinGestureInset, rawGestureInset)
    val minTouchTarget = LocalMinimumInteractiveComponentSize.current
        .takeOrElse { CompactPortraitTokens.DefaultMinTouchTarget }
    val (topBarHeight, topBarTopPadding) = computeTopBarMetrics(
        includeStatusBarsInTopBar = includeStatusBarsInTopBar,
        minTouchTarget = minTouchTarget,
        windowRotation = windowRotation
    )

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
    // moves the rows around it.
    val spec = remember(minTouchTarget) {
        CameraLayoutDefaults.spec(minInteractiveTouchTarget = minTouchTarget)
    }
    val solution = precomputedSolution ?: remember(spec, window) {
        CameraLayoutSolver.solve(spec, window)
    }

    val currentOnRequiresImmersiveChange by rememberUpdatedState(onRequiresImmersiveChange)
    DisposableEffect(solution.requiresImmersive) {
        currentOnRequiresImmersiveChange(solution.requiresImmersive)
        onDispose { currentOnRequiresImmersiveChange(false) }
    }

    CompositionLocalProvider(LocalCameraLayoutSolution provides solution) {
        Layout(
            modifier = modifier.fillMaxSize(),
            content = {
                Box(modifier = Modifier.layoutId(CaptureSlotId.Viewfinder)) {
                    viewfinder(Modifier)
                }

                Box(modifier = Modifier.layoutId(CaptureSlotId.TopStartContent)) {
                    OverlapAwareStyleProvider(orientation = Orientation.Horizontal) {
                        topStartContent(Modifier)
                    }
                }

                Box(modifier = Modifier.layoutId(CaptureSlotId.ViewfinderTopEndContent)) {
                    OverlapAwareStyleProvider(orientation = Orientation.Horizontal) {
                        viewfinderTopEndContent(Modifier)
                    }
                }

                CompactTopBarBox(
                    topBarHeight = topBarHeight,
                    topBarTopPadding = topBarTopPadding,
                    windowRotation = windowRotation,
                    modifier = Modifier.layoutId(CaptureSlotId.TopBar),
                    indicatorRow = indicatorRow,
                    topBarTrailingContent = topBarTrailingContent
                )

                if (controlsVisible) {
                    Column(
                        modifier = Modifier.layoutId(CaptureSlotId.ElapsedTime),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        snackbarHost(Modifier)
                        OverlapAwareStyleProvider(orientation = Orientation.Horizontal) {
                            elapsedTimeDisplay(Modifier)
                        }
                    }

                    Box(
                        modifier = Modifier.layoutId(CaptureSlotId.ZoomBar),
                        contentAlignment = Alignment.Center
                    ) {
                        OverlapAwareStyleProvider(orientation = Orientation.Horizontal) {
                            zoomControls(Modifier)
                        }
                    }

                    CompactCaptureRow(
                        bottomBarDirection = bottomBarDirection,
                        localeDirection = localeDirection,
                        isCompact = false,
                        imageWell = imageWell,
                        captureButton = captureButton,
                        flipCameraButton = flipCameraButton,
                        modifier = Modifier.layoutId(CaptureSlotId.CaptureRow)
                    )

                    Box(
                        modifier = Modifier
                            .layoutId(CaptureSlotId.ModeSwitcher)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        OverlapAwareStyleProvider(orientation = Orientation.Horizontal) {
                            captureModeCarousel(Modifier)
                        }
                    }

                    CompactBottomToolbar(
                        bottomBarDirection = bottomBarDirection,
                        localeDirection = localeDirection,
                        isCompact = false,
                        quickSettingsToggleButton = quickSettingsToggleButton,
                        captureModeToggleSwitch = captureModeToggleSwitch,
                        modifier = Modifier.layoutId(CaptureSlotId.BottomToolbar)
                    )
                }
            }
        ) { measurables, constraints ->
            val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
            val fullWidthLooseHeight = constraints.copy(minHeight = 0)

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
            val modeSwitcher = measurables
                .firstOrNull { it.layoutId == CaptureSlotId.ModeSwitcher }
                ?.let { measurable ->
                    val band = bandOf(CameraRowIds.MODE_SWITCHER)
                    val clearance = min(
                        band.top - bandOf(CameraRowIds.CAPTURE_ROW).bottom,
                        bandOf(CameraRowIds.BOTTOM_TOOLBAR).top - band.bottom
                    ).coerceAtLeast(CompactPortraitTokens.ZeroInset)
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

            val viewfinderY = viewfinderPlaceable?.let { vf ->
                if (vf.height in 1 until constraints.maxHeight) {
                    val ratio = vf.width.toFloat() / vf.height.toFloat()
                    solution.viewfinderBandFor(ratio, spec.viewfinders)?.top?.roundToPx()
                } else {
                    null
                }
            } ?: 0

            val activeVfTopPx = viewfinderPlaceable?.let { vf ->
                val reportedTop = vf[ViewfinderTopLine]
                viewfinderY + if (reportedTop == AlignmentLine.Unspecified) 0 else reportedTop
            }

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

/**
 * Compressed-capable vertical bottom control stack used for non-handheld compact panes (such as
 * squarer split-screen windows narrower than `600.dp`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VerticalMaterialControls(
    modifier: Modifier = Modifier,
    windowRotation: DeviceRotation = LocalWindowRotation.current,
    captureButton: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    zoomControls: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    imageWell: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    flipCameraButton: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    quickSettingsToggleButton: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    captureModeToggleSwitch: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    captureModeCarousel: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    elapsedTimeDisplay: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    snackbarHost: @Composable (Modifier) -> Unit = {}
) {
    val localeDirection = LocalLayoutDirection.current
    val rtlPolicy = LocalCaptureRtlPolicy.current
    val bottomBarDirection = if (rtlPolicy.mirrorCompactBottomBar) {
        localeDirection
    } else {
        LayoutDirection.Ltr
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val navBarBottom = WindowInsets.navigationBarsIgnoringVisibility
            .toPortraitWindowInsets(windowRotation)
            .asPaddingValues()
            .calculateBottomPadding()
        val isShortScreen = maxHeight < CompactPortraitTokens.ShortScreenThreshold
        val maxBottomPad = max(
            CompactPortraitTokens.TargetBottomClearance - navBarBottom,
            CompactPortraitTokens.MinNavMargin
        )
        val bottomPad = if (isShortScreen) {
            min(CompactPortraitTokens.MinControlsBottomPadding, maxBottomPad)
        } else {
            maxBottomPad
        }
        val stackGap = if (isShortScreen) {
            CompactPortraitTokens.ControlStackMinGap
        } else {
            CompactPortraitTokens.ControlStackMaxGap
        }
        val zoomGap = if (isShortScreen) {
            CompactPortraitTokens.MinZoomToShutterGap
        } else {
            CompactPortraitTokens.ZoomToShutterGap
        }

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                snackbarHost(Modifier)

                OverlapAwareStyleProvider(
                    orientation = Orientation.Horizontal,
                    isCompact = isShortScreen
                ) {
                    elapsedTimeDisplay(Modifier)
                }

                OverlapAwareStyleProvider(
                    orientation = Orientation.Horizontal,
                    isCompact = isShortScreen
                ) {
                    zoomControls(Modifier)
                }

                Spacer(modifier = Modifier.height(zoomGap))

                Column {
                    CompactCaptureRow(
                        bottomBarDirection = bottomBarDirection,
                        localeDirection = localeDirection,
                        isCompact = isShortScreen,
                        imageWell = imageWell,
                        captureButton = captureButton,
                        flipCameraButton = flipCameraButton,
                        modifier = Modifier.height(CompactPortraitTokens.ShutterStackHeight)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CompactPortraitTokens.MiddleSlotHeight + stackGap * 2),
                    contentAlignment = Alignment.Center
                ) {
                    OverlapAwareStyleProvider(
                        orientation = Orientation.Horizontal,
                        isCompact = isShortScreen
                    ) {
                        captureModeCarousel(Modifier)
                    }
                }

                CompactBottomToolbar(
                    bottomBarDirection = bottomBarDirection,
                    localeDirection = localeDirection,
                    isCompact = isShortScreen,
                    quickSettingsToggleButton = quickSettingsToggleButton,
                    captureModeToggleSwitch = captureModeToggleSwitch,
                    modifier = Modifier.height(CompactPortraitTokens.LowerSectionHeight)
                )

                Spacer(modifier = Modifier.height(bottomPad))
            }
        }
    }
}

@Preview(
    name = "CompactPortrait - Phone Portrait (412x915dp)",
    device = "spec:width=412dp,height=915dp",
    showBackground = true
)
@Composable
private fun CompactPortraitStandardPhonePreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CompactPortraitLayout(
            slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true),
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) }
        )
    }
}

@Preview(
    name = "CompactPortrait - Short Phone (360x640dp)",
    device = "spec:width=360dp,height=640dp",
    showBackground = true
)
@Composable
private fun CompactPortraitShortPhonePreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CompactPortraitLayout(
            slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true),
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) }
        )
    }
}

@Preview(
    name = "CompactPortrait - Phone Horizontal (800x400dp)",
    device = "spec:width=800dp,height=400dp",
    showBackground = true
)
@Composable
private fun CompactPortraitHorizontalPhonePreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CompactPortraitLayout(
            slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true),
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) }
        )
    }
}

@Preview(
    name = "CompactPortrait - RTL Default Policy (Unmirrored Bar)",
    device = "spec:width=412dp,height=915dp",
    locale = "ar",
    showBackground = true
)
@Composable
private fun CompactPortraitRtlDefaultPolicyPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CompositionLocalProvider(
            LocalCaptureRtlPolicy provides CaptureRtlPolicy(mirrorCompactBottomBar = false)
        ) {
            CompactPortraitLayout(
                slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true),
                viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) }
            )
        }
    }
}

@Preview(
    name = "CompactPortrait - RTL Mirrored Policy",
    device = "spec:width=412dp,height=915dp",
    locale = "ar",
    showBackground = true
)
@Composable
private fun CompactPortraitRtlMirroredPolicyPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CompositionLocalProvider(
            LocalCaptureRtlPolicy provides CaptureRtlPolicy(mirrorCompactBottomBar = true)
        ) {
            CompactPortraitLayout(
                slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true),
                viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) }
            )
        }
    }
}
