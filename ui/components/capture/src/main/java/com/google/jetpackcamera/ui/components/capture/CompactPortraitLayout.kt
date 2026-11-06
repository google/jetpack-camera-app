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
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.takeOrElse
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutDefaults
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutSolver
import com.google.jetpackcamera.ui.components.capture.layout.CameraRowIds
import com.google.jetpackcamera.ui.components.capture.layout.CameraWindow
import com.google.jetpackcamera.ui.components.capture.layout.LocalCameraLayoutSolution

private object CompactPortraitTokens {
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
 * Remembers a [BottomSheetScaffoldState] configured for [CompactPortraitLayout] and
 * [PreviewLayout], backed by the non-deprecated Material 3 [rememberBottomSheetState] API.
 *
 * @param initialValue Initial [SheetValue] of the Quick Settings bottom sheet; defaults to
 *   [SheetValue.Hidden].
 * @return A remembered [BottomSheetScaffoldState] supporting hidden, partially expanded, and
 *   expanded sheet states.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberCompactPortraitScaffoldState(
    initialValue: SheetValue = SheetValue.Hidden
): BottomSheetScaffoldState = rememberBottomSheetScaffoldState(
    bottomSheetState = rememberBottomSheetState(initialValue = initialValue)
)

/**
 * Suppresses keyboard/rotary focus and accessibility semantics on background camera chrome while an
 * in-hierarchy modal surface (such as Quick Settings or an error dialog) is open above it.
 */
internal fun Modifier.suppressBackgroundForModal(isModalOpen: Boolean): Modifier =
    if (isModalOpen) {
        this
            .focusProperties { canFocus = false }
            .clearAndSetSemantics {}
    } else {
        this
    }

/**
 * Compact portrait capture layout driven by a [CaptureSlots] slot table.
 *
 * Hosts the camera viewfinder, top status/indicator bar, bottom control stack, and an in-hierarchy
 * [BottomSheetScaffold] for Quick Settings without spawning secondary windows. Tags its root
 * container with [COMPACT_CONTROLS_TAG] and scopes [LocalCaptureRtlPolicy]
 * (`mirrorCompactBottomBar`) exclusively to the horizontal capture and bottom-toolbar rows while
 * restoring the active locale's `LayoutDirection` inside every child slot.
 *
 * @param slots [CaptureSlots] supplying the capture screen UI controls and overlays.
 * @param viewfinder Composable rendering the camera preview frame.
 * @param modifier [Modifier] applied to the outer layout container.
 * @param scaffoldState [BottomSheetScaffoldState] controlling the Quick Settings sheet and snackbar
 *   host.
 * @param isRecording Whether an active video recording session is in progress.
 * @param isErrorDialogShowing Whether a modal camera error dialog is currently visible above the
 *   capture chrome.
 * @param includeStatusBarsInTopBar Whether live visible `WindowInsets.statusBars` top padding is
 *   included in the top indicator bar so multi-window status bars never overlap status indicators.
 * @param onDismissQuickSettings Callback invoked when the user taps the Quick Settings drag handle
 *   or scrim, or triggers dismissal from a slot.
 * @param onRequiresImmersiveChange Callback notified when the portrait layout solver requires or
 *   releases immersive navigation-bar hiding.
 * @param debugOverlay Untouched pass-through composable for debug UI overlays.
 * @param debugVisibilityWrapper Wrapper conditionally hiding camera chrome when debug component
 *   hiding is enabled.
 * @param screenFlashOverlay Full-bleed screen flash composable drawn above all chrome and system
 *   bar areas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactPortraitLayout(
    slots: CaptureSlots,
    viewfinder: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState = rememberCompactPortraitScaffoldState(),
    isRecording: Boolean = false,
    isErrorDialogShowing: Boolean = false,
    includeStatusBarsInTopBar: Boolean = true,
    onDismissQuickSettings: () -> Unit = {},
    onRequiresImmersiveChange: (Boolean) -> Unit = {},
    debugOverlay: @Composable (Modifier) -> Unit = {},
    debugVisibilityWrapper: @Composable (@Composable () -> Unit) -> Unit = { it() },
    screenFlashOverlay: @Composable (Modifier) -> Unit = {}
) {
    CompactPortraitLayout(
        modifier = modifier,
        scaffoldState = scaffoldState,
        onDismissQuickSettings = onDismissQuickSettings,
        isErrorDialogShowing = isErrorDialogShowing,
        includeStatusBarsInTopBar = includeStatusBarsInTopBar,
        viewfinder = viewfinder,
        captureButton = { slotModifier ->
            OverlapAwareCaptureSlot(
                modifier = slotModifier,
                orientation = Orientation.Horizontal,
                isCompact = false,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings,
                content = slots.captureButton
            )
        },
        imageWell = { slotModifier ->
            OverlapAwareCaptureSlot(
                modifier = slotModifier,
                orientation = Orientation.Horizontal,
                isCompact = false,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings,
                content = slots.imageWell
            )
        },
        flipCameraButton = { slotModifier ->
            OverlapAwareCaptureSlot(
                modifier = slotModifier,
                orientation = Orientation.Horizontal,
                isCompact = false,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings,
                content = slots.flipCameraButton
            )
        },
        zoomLevelDisplay = { slotModifier ->
            OverlapAwareCaptureSlot(
                modifier = slotModifier,
                orientation = Orientation.Horizontal,
                isCompact = false,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings,
                content = slots.zoomControls
            )
        },
        elapsedTimeDisplay = { slotModifier ->
            OverlapAwareCaptureSlot(
                modifier = slotModifier,
                orientation = Orientation.Horizontal,
                isCompact = false,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings,
                content = slots.elapsedTimeDisplay
            )
        },
        quickSettingsButton = { slotModifier ->
            OverlapAwareCaptureSlot(
                modifier = slotModifier,
                orientation = Orientation.Horizontal,
                isCompact = false,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings,
                content = slots.quickSettingsButton
            )
        },
        indicatorRow = { slotModifier ->
            OverlapAwareCaptureSlot(
                modifier = slotModifier,
                orientation = Orientation.Horizontal,
                isCompact = false,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings,
                content = slots.topBar
            )
        },
        captureModeToggle = { slotModifier ->
            OverlapAwareCaptureSlot(
                modifier = slotModifier,
                orientation = Orientation.Horizontal,
                isCompact = false,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings,
                content = slots.captureModeToggle
            )
        },
        captureModeCarousel = { slotModifier ->
            OverlapAwareCaptureSlot(
                modifier = slotModifier,
                orientation = Orientation.Horizontal,
                isCompact = false,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings,
                content = slots.captureModeCarousel
            )
        },
        quickSettingsOverlay = { slotModifier ->
            OverlapAwareCaptureSlot(
                modifier = slotModifier,
                orientation = Orientation.Vertical,
                isCompact = false,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings,
                content = slots.quickSettingsOverlay
            )
        },
        debugOverlay = debugOverlay,
        debugVisibilityWrapper = debugVisibilityWrapper,
        screenFlashOverlay = screenFlashOverlay,
        snackBar = slots.snackBar,
        topStartContent = { slotModifier ->
            val slotScope = rememberCaptureSlotScope(
                orientation = Orientation.Horizontal,
                isCompact = false,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings
            )
            slotScope.(slots.viewfinderTopStart)(slotModifier)
        },
        viewfinderTopEndContent = { slotModifier ->
            val slotScope = rememberCaptureSlotScope(
                orientation = Orientation.Horizontal,
                isCompact = false,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings
            )
            slotScope.(slots.viewfinderTopEnd)(slotModifier)
        },
        topBarTrailingContent = { slotModifier ->
            OverlapAwareCaptureSlot(
                modifier = slotModifier,
                orientation = Orientation.Horizontal,
                isCompact = false,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings,
                content = slots.topBarTrailingContent
            )
        },
        onRequiresImmersiveChange = onRequiresImmersiveChange,
        errorDialog = slots.errorDialog
    )
}

/**
 * Compact portrait capture layout accepting individual control composables.
 *
 * Uses [AdaptivePortraitCaptureLayout] for handheld portrait aspect ratios (16:9 and taller) and
 * falls back to [VerticalMaterialControls] for squarer compact panes (such as split-screen
 * multi-window). Hosts Quick Settings inside an in-hierarchy [BottomSheetScaffold] and tags its
 * root container with [COMPACT_CONTROLS_TAG].
 *
 * @param modifier [Modifier] applied to the outer layout container.
 * @param scaffoldState [BottomSheetScaffoldState] controlling the Quick Settings sheet and snackbar
 *   host.
 * @param onDismissQuickSettings Callback invoked when the user taps the Quick Settings drag handle
 *   or scrim.
 * @param isErrorDialogShowing Whether a modal camera error dialog is currently visible above the
 *   capture chrome.
 * @param includeStatusBarsInTopBar Whether live visible `WindowInsets.statusBars` top padding is
 *   included in the top indicator bar (`true` by default for multi-window clearance; `false` when
 *   called from legacy [PreviewLayout] to keep top-bar coordinates invariant across status-bar
 *   visibility transitions).
 * @param viewfinder Composable rendering the camera preview frame.
 * @param captureButton Primary photo/video shutter button composable.
 * @param imageWell Media thumbnail button composable.
 * @param flipCameraButton Lens-facing toggle button composable.
 * @param zoomLevelDisplay Zoom control bar composable.
 * @param elapsedTimeDisplay Recording duration readout composable.
 * @param quickSettingsButton Button toggling the Quick Settings bottom sheet.
 * @param indicatorRow Status indicator bar content placed in the top bar.
 * @param captureModeToggle Photo/video mode toggle switch composable.
 * @param captureModeCarousel Secondary sub-mode selector carousel placed in the reserved middle
 *   band.
 * @param quickSettingsOverlay Content rendered inside the Quick Settings bottom sheet.
 * @param debugOverlay Untouched pass-through composable for debug UI overlays.
 * @param debugVisibilityWrapper Wrapper conditionally hiding camera chrome when debug component
 *   hiding is enabled.
 * @param screenFlashOverlay Full-bleed screen flash composable drawn above all other content and
 *   system bar areas.
 * @param snackBar Transient message composable paired with a [SnackbarHostState].
 * @param topStartContent Optional content anchored directly below the top bar at the start edge.
 * @param viewfinderTopEndContent Optional content anchored to the top-end corner of the viewfinder.
 * @param topBarTrailingContent Optional trailing content aligned to the end of the top bar.
 * @param onRequiresImmersiveChange Callback notified when the portrait solver requires or releases
 *   immersive navigation-bar hiding.
 * @param errorDialog In-hierarchy camera error dialog composable.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CompactPortraitLayout(
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState = rememberCompactPortraitScaffoldState(),
    onDismissQuickSettings: () -> Unit = {},
    isErrorDialogShowing: Boolean = false,
    includeStatusBarsInTopBar: Boolean = true,
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
    debugOverlay: @Composable (Modifier) -> Unit = {},
    debugVisibilityWrapper: @Composable (@Composable () -> Unit) -> Unit = { it() },
    screenFlashOverlay: @Composable (Modifier) -> Unit = {},
    snackBar: @Composable (Modifier, snackbarHostState: SnackbarHostState) -> Unit = { _, _ -> },
    topStartContent: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    viewfinderTopEndContent: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    topBarTrailingContent: @Composable (Modifier) -> Unit = {},
    onRequiresImmersiveChange: (Boolean) -> Unit = {},
    errorDialog: @Composable (Modifier) -> Unit = {}
) {
    val overlapTargetBounds = remember { mutableStateOf(Rect.Zero) }
    val localeDirection = LocalLayoutDirection.current
    val dismissQuickSettingsLabel =
        stringResource(R.string.quick_settings_btn_close_expanded_settings_description)
    val isSheetVisible = scaffoldState.bottomSheetState.isVisible ||
        scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded
    val isModalOpen = isSheetVisible || isErrorDialogShowing

    CompositionLocalProvider(LocalOverlapTargetBounds provides overlapTargetBounds) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag(COMPACT_CONTROLS_TAG)
        ) {
            BottomSheetScaffold(
                modifier = Modifier.fillMaxSize(),
                scaffoldState = scaffoldState,
                sheetPeekHeight = CompactPortraitTokens.SheetPeekHeight,
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
                sheetShape = RoundedCornerShape(
                    topStart = CompactPortraitTokens.SheetTopCornerRadius,
                    topEnd = CompactPortraitTokens.SheetTopCornerRadius
                ),
                sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                sheetContent = {
                    CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                        quickSettingsOverlay(Modifier)
                    }
                },
                snackbarHost = {}
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    val windowAspectRatio = maxHeight.value / maxWidth.value
                    val isHandheldPortrait =
                        windowAspectRatio >= CompactPortraitTokens.PORTRAIT_ASPECT_RATIO_THRESHOLD
                    if (isHandheldPortrait) {
                        AdaptivePortraitCaptureLayout(
                            windowWidth = maxWidth,
                            windowHeight = maxHeight,
                            includeStatusBarsInTopBar = includeStatusBarsInTopBar,
                            isSheetVisible = isSheetVisible,
                            isModalOpen = isModalOpen,
                            viewfinder = viewfinder,
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
                            debugVisibilityWrapper = debugVisibilityWrapper,
                            snackbarHost = { hostModifier ->
                                PillSnackbarHost(
                                    snackbarHostState = scaffoldState.snackbarHostState,
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
                                )
                        ) {
                            CompositionLocalProvider(
                                LocalLayoutDirection provides localeDirection
                            ) {
                                snackBar(Modifier, scaffoldState.snackbarHostState)
                            }
                        }
                    } else {
                        Column {
                            val minTouchTarget = LocalMinimumInteractiveComponentSize.current
                                .takeOrElse { CompactPortraitTokens.DefaultMinTouchTarget }
                            val (topBarHeight, topBarTopPadding) = computeTopBarMetrics(
                                includeStatusBarsInTopBar = includeStatusBarsInTopBar,
                                minTouchTarget = minTouchTarget
                            )
                            CompactTopBarBox(
                                topBarHeight = topBarHeight,
                                topBarTopPadding = topBarTopPadding,
                                modifier = Modifier.suppressBackgroundForModal(isModalOpen),
                                indicatorRow = indicatorRow,
                                topBarTrailingContent = topBarTrailingContent
                            )
                            Box {
                                Box(modifier = Modifier.suppressBackgroundForModal(isModalOpen)) {
                                    viewfinder(Modifier)
                                }
                                OverlapAwareStyleProvider(
                                    Modifier
                                        .align(Alignment.TopStart)
                                        .suppressBackgroundForModal(isSheetVisible)
                                ) {
                                    topStartContent(Modifier)
                                }
                                OverlapAwareStyleProvider(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .suppressBackgroundForModal(isModalOpen)
                                ) {
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
                                    snackbarHost = { hostModifier ->
                                        PillSnackbarHost(
                                            snackbarHostState = scaffoldState.snackbarHostState,
                                            modifier = hostModifier
                                        )
                                    }
                                )
                            }
                            CompositionLocalProvider(
                                LocalLayoutDirection provides localeDirection
                            ) {
                                snackBar(Modifier, scaffoldState.snackbarHostState)
                            }
                        }
                    }

                    CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                        debugOverlay(Modifier)
                    }

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

                    CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                        errorDialog(Modifier)
                    }
                }
            }
            // Drawn last and outside of both the inset-padded controls and the bottom sheet so the
            // flash covers the entire window, including system bar and display cutout areas.
            screenFlashOverlay(Modifier)
        }
    }
}

@Composable
private fun computeTopBarMetrics(
    includeStatusBarsInTopBar: Boolean,
    minTouchTarget: Dp
): Pair<Dp, Dp> {
    val cutoutTopInset = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    return if (includeStatusBarsInTopBar) {
        val statusBarTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val combinedTopInset = WindowInsets.statusBars
            .union(WindowInsets.displayCutout)
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
    indicatorRow: @Composable (Modifier) -> Unit,
    topBarTrailingContent: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val localeDirection = LocalLayoutDirection.current
    CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(topBarHeight)
                .padding(top = topBarTopPadding)
                .windowInsetsPadding(
                    WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)
                )
                .padding(horizontal = CompactPortraitTokens.LowerSectionHorizontalPadding),
            contentAlignment = Alignment.CenterStart
        ) {
            indicatorRow(Modifier)
            topBarTrailingContent(Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun CompactCaptureRow(
    bottomBarDirection: LayoutDirection,
    localeDirection: LayoutDirection,
    imageWell: @Composable (Modifier) -> Unit,
    captureButton: @Composable (Modifier) -> Unit,
    flipCameraButton: @Composable (Modifier) -> Unit,
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
                        OverlapAwareStyleProvider {
                            imageWell(Modifier)
                        }
                    }
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                OverlapAwareStyleProvider {
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
                    OverlapAwareStyleProvider {
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
    quickSettingsToggleButton: @Composable (Modifier) -> Unit,
    captureModeToggleSwitch: @Composable (Modifier) -> Unit,
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
                    OverlapAwareStyleProvider {
                        quickSettingsToggleButton(Modifier)
                    }
                }
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                    OverlapAwareStyleProvider {
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
    indicatorRow: @Composable (Modifier) -> Unit,
    topBarTrailingContent: @Composable (Modifier) -> Unit,
    elapsedTimeDisplay: @Composable (Modifier) -> Unit,
    zoomControls: @Composable (Modifier) -> Unit,
    imageWell: @Composable (Modifier) -> Unit,
    captureButton: @Composable (Modifier) -> Unit,
    flipCameraButton: @Composable (Modifier) -> Unit,
    captureModeCarousel: @Composable (Modifier) -> Unit,
    quickSettingsToggleButton: @Composable (Modifier) -> Unit,
    captureModeToggleSwitch: @Composable (Modifier) -> Unit,
    debugVisibilityWrapper: @Composable (@Composable () -> Unit) -> Unit,
    includeStatusBarsInTopBar: Boolean = true,
    isSheetVisible: Boolean = false,
    isModalOpen: Boolean = false,
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
        .asPaddingValues()
        .calculateBottomPadding()
    val rawGestureInset = WindowInsets.systemGestures
        .asPaddingValues()
        .calculateBottomPadding()
    val effectiveGestureInset = max(CompactPortraitTokens.MinGestureInset, rawGestureInset)
    val minTouchTarget = LocalMinimumInteractiveComponentSize.current
        .takeOrElse { CompactPortraitTokens.DefaultMinTouchTarget }
    val (topBarHeight, topBarTopPadding) = computeTopBarMetrics(
        includeStatusBarsInTopBar = includeStatusBarsInTopBar,
        minTouchTarget = minTouchTarget
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
    val solution = remember(spec, window) { CameraLayoutSolver.solve(spec, window) }

    val currentOnRequiresImmersiveChange by rememberUpdatedState(onRequiresImmersiveChange)
    DisposableEffect(solution.requiresImmersive) {
        currentOnRequiresImmersiveChange(solution.requiresImmersive)
        onDispose { currentOnRequiresImmersiveChange(false) }
    }

    CompositionLocalProvider(LocalCameraLayoutSolution provides solution) {
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                Box(
                    modifier = Modifier
                        .layoutId(CaptureSlotId.Viewfinder)
                        .suppressBackgroundForModal(isModalOpen)
                ) {
                    viewfinder(Modifier)
                }

                Box(
                    modifier = Modifier
                        .layoutId(CaptureSlotId.TopStartContent)
                        .suppressBackgroundForModal(isSheetVisible)
                ) {
                    OverlapAwareStyleProvider {
                        topStartContent(Modifier)
                    }
                }

                Box(
                    modifier = Modifier
                        .layoutId(CaptureSlotId.ViewfinderTopEndContent)
                        .suppressBackgroundForModal(isModalOpen)
                ) {
                    OverlapAwareStyleProvider {
                        viewfinderTopEndContent(Modifier)
                    }
                }

                CompactTopBarBox(
                    topBarHeight = topBarHeight,
                    topBarTopPadding = topBarTopPadding,
                    modifier = Modifier
                        .layoutId(CaptureSlotId.TopBar)
                        .suppressBackgroundForModal(isModalOpen),
                    indicatorRow = indicatorRow,
                    topBarTrailingContent = topBarTrailingContent
                )

                debugVisibilityWrapper {
                    Column(
                        modifier = Modifier
                            .layoutId(CaptureSlotId.ElapsedTime)
                            .suppressBackgroundForModal(isModalOpen),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        snackbarHost(Modifier)
                        elapsedTimeDisplay(Modifier)
                    }

                    Box(
                        modifier = Modifier
                            .layoutId(CaptureSlotId.ZoomBar)
                            .suppressBackgroundForModal(isModalOpen),
                        contentAlignment = Alignment.Center
                    ) {
                        OverlapAwareStyleProvider {
                            zoomControls(Modifier)
                        }
                    }

                    CompactCaptureRow(
                        bottomBarDirection = bottomBarDirection,
                        localeDirection = localeDirection,
                        imageWell = imageWell,
                        captureButton = captureButton,
                        flipCameraButton = flipCameraButton,
                        modifier = Modifier
                            .layoutId(CaptureSlotId.CaptureRow)
                            .suppressBackgroundForModal(isModalOpen)
                    )

                    Box(
                        modifier = Modifier
                            .layoutId(CaptureSlotId.ModeSwitcher)
                            .fillMaxWidth()
                            .suppressBackgroundForModal(isModalOpen),
                        contentAlignment = Alignment.Center
                    ) {
                        OverlapAwareStyleProvider {
                            captureModeCarousel(Modifier)
                        }
                    }

                    CompactBottomToolbar(
                        bottomBarDirection = bottomBarDirection,
                        localeDirection = localeDirection,
                        quickSettingsToggleButton = quickSettingsToggleButton,
                        captureModeToggleSwitch = captureModeToggleSwitch,
                        modifier = Modifier
                            .layoutId(CaptureSlotId.BottomToolbar)
                            .suppressBackgroundForModal(isModalOpen)
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
    captureButton: @Composable (Modifier) -> Unit,
    zoomControls: @Composable (Modifier) -> Unit,
    imageWell: @Composable (Modifier) -> Unit,
    flipCameraButton: @Composable (Modifier) -> Unit,
    quickSettingsToggleButton: @Composable (Modifier) -> Unit,
    captureModeToggleSwitch: @Composable (Modifier) -> Unit,
    captureModeCarousel: @Composable (Modifier) -> Unit = {},
    elapsedTimeDisplay: @Composable (Modifier) -> Unit,
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

                elapsedTimeDisplay(Modifier)

                OverlapAwareStyleProvider {
                    zoomControls(Modifier)
                }

                Spacer(modifier = Modifier.height(zoomGap))

                Column {
                    CompactCaptureRow(
                        bottomBarDirection = bottomBarDirection,
                        localeDirection = localeDirection,
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
                    OverlapAwareStyleProvider {
                        captureModeCarousel(Modifier)
                    }
                }

                CompactBottomToolbar(
                    bottomBarDirection = bottomBarDirection,
                    localeDirection = localeDirection,
                    quickSettingsToggleButton = quickSettingsToggleButton,
                    captureModeToggleSwitch = captureModeToggleSwitch,
                    modifier = Modifier.height(CompactPortraitTokens.LowerSectionHeight)
                )

                Spacer(modifier = Modifier.height(bottomPad))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "CompactPortrait - Phone Portrait (412x915dp)",
    device = "spec:width=412dp,height=915dp",
    showBackground = true
)
@Composable
private fun CompactPortraitStandardPhonePreview() {
    MaterialTheme {
        CompactPortraitLayout(
            slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true),
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "CompactPortrait - Short Phone (360x640dp)",
    device = "spec:width=360dp,height=640dp",
    showBackground = true
)
@Composable
private fun CompactPortraitShortPhonePreview() {
    MaterialTheme {
        CompactPortraitLayout(
            slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true),
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "CompactPortrait - RTL Default Policy (Unmirrored Bar)",
    device = "spec:width=412dp,height=915dp",
    locale = "ar",
    showBackground = true
)
@Composable
private fun CompactPortraitRtlDefaultPolicyPreview() {
    MaterialTheme {
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "CompactPortrait - RTL Mirrored Policy",
    device = "spec:width=412dp,height=915dp",
    locale = "ar",
    showBackground = true
)
@Composable
private fun CompactPortraitRtlMirroredPolicyPreview() {
    MaterialTheme {
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
