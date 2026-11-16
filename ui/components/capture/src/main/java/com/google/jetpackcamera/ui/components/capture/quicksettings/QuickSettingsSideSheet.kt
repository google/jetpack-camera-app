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
package com.google.jetpackcamera.ui.components.capture.quicksettings

import android.content.res.Configuration
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_DRAG_HANDLE
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_SCRIM
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_SIDE_SHEET_TAG
import com.google.jetpackcamera.ui.components.capture.R
import com.google.jetpackcamera.ui.controller.quicksettings.QuickSettingsController
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

private object QuickSettingsSideSheetTokens {
    val MaxSheetWidth = 412.dp
    val FoldableMaxSheetWidth = 380.dp
    val OuterPadding = 16.dp
    val ContentVerticalPadding = 20.dp
    val DragHandleWidth = 4.dp
    val DragHandleHeight = 32.dp
    val PredictiveBackMinScale = 0.95f

    /** Distance from the hidden anchor within which fling velocity toward it is damped (M3). */
    val FlingDampingZone = 125.dp

    /** Minimum velocity a damped fling keeps if it started above it (M3 sheet threshold). */
    val FlingVelocityThreshold = 125.dp
}

/** Whether a [QuickSettingsSideSheet] is off-screen or fully shown. */
internal enum class SideSheetValue {
    Hidden,
    Expanded
}

/** Horizontal edge a side sheet is attached to and slides in from. */
internal enum class SideSheetEdge {
    Left,
    Right
}

/**
 * State of a [QuickSettingsSideSheet]: a thin wrapper over a horizontal [AnchoredDraggableState],
 * the same drag primitive Material 3 sheets are built on, so the sheet can be dragged, flung and
 * animated between [SideSheetValue.Hidden] and [SideSheetValue.Expanded].
 */
@Stable
internal class SideSheetState(initialValue: SideSheetValue = SideSheetValue.Hidden) {
    internal val anchoredDraggableState = AnchoredDraggableState(initialValue)

    /** The settled value, or the value being animated away from. */
    val currentValue: SideSheetValue get() = anchoredDraggableState.currentValue

    /** The value the sheet is settling or animating toward. */
    val targetValue: SideSheetValue get() = anchoredDraggableState.targetValue

    /** Whether any part of the sheet is on screen, including during show and hide animations. */
    val isVisible: Boolean
        get() = currentValue != SideSheetValue.Hidden || targetValue != SideSheetValue.Hidden

    internal val hasAnchors: Boolean get() = anchoredDraggableState.anchors.size > 0

    /** Animates the sheet onto the screen. */
    suspend fun show(animationSpec: AnimationSpec<Float>) {
        anchoredDraggableState.animateTo(SideSheetValue.Expanded, animationSpec)
    }

    /** Animates the sheet off the screen toward its attached edge. */
    suspend fun hide(animationSpec: AnimationSpec<Float>) {
        anchoredDraggableState.animateTo(SideSheetValue.Hidden, animationSpec)
    }
}

/**
 * In-hierarchy side sheet for Quick Settings on adaptive medium and expanded layouts.
 *
 * The sheet attaches to a horizontal edge (the physical left by default, matching the start-side
 * rail of the large-screen mocks), slides in from that edge when opened, and can be dismissed by
 * swiping it back toward that edge, by its drag handle, by tapping outside it, by system back
 * (with predictive back) or by a hardware [Key.Escape] press.
 *
 * Renders nothing when [isOpen] is `false` (after the hide animation finishes) or
 * [quickSettingsUiState] is not [QuickSettingsUiState.Available]. While open, it displays
 * [QuickSettingsContent] inside a vertically scrollable [Surface] tagged with
 * [QUICK_SETTINGS_SIDE_SHEET_TAG] (while [QuickSettingsContent] retains
 * `QUICK_SETTINGS_BOTTOM_SHEET` until a shared `QUICK_SETTINGS_CONTENT_TAG` is split out).
 *
 * @param isOpen Whether the Quick Settings side sheet is open.
 * @param onDismissRequest Callback invoked when the user swipes the sheet away, taps its drag
 *   handle or the area outside it, or presses back or escape. Callers should set [isOpen] to
 *   `false` in response.
 * @param quickSettingsUiState The current [QuickSettingsUiState].
 * @param quickSettingsController The [QuickSettingsController] to handle setting changes.
 * @param modifier The [Modifier] applied to the full-size overlay container.
 * @param onNavigateToSettings Callback invoked when the "More settings" button is clicked.
 * @param showMoreSettingsButton Whether to display the "More settings" navigation button inside
 *   the sheet. Defaults to `false` for adaptive layouts that surface settings in persistent chrome.
 * @param contentAlignment Alignment of the sheet within the overlay container. Its horizontal
 *   component also picks the edge the sheet attaches to and slides from. Defaults to
 *   [AbsoluteAlignment.CenterLeft] so the sheet stays on the physical left across LTR and RTL
 *   while interior controls mirror for RTL.
 * @param sheetPadding Padding around the sheet surface (used by side-rail layouts to clear
 *   measured start and end rail widths without shifting the full-screen tap-outside area).
 * @param maxSheetWidth Maximum width cap applied to the sheet surface.
 * @param maxSheetHeight Optional maximum height cap applied to the sheet surface.
 * @param sheetContainerDirection Layout direction used to resolve [contentAlignment] and
 *   [sheetPadding] outside the sheet surface while restoring the caller's `LocalLayoutDirection`
 *   inside the sheet content.
 * @param windowInsets Window insets the sheet stays clear of, applied outside [sheetPadding].
 *   Defaults to [WindowInsets.Companion.safeDrawing].
 * @param scrimColor Color drawn behind the sheet. Transparent by default so the viewfinder is not
 *   dimmed, matching the mocks; the full-screen tap-to-dismiss area is present while the sheet is
 *   visible or opening.
 */
@Composable
internal fun QuickSettingsSideSheet(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    quickSettingsUiState: QuickSettingsUiState,
    quickSettingsController: QuickSettingsController,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    showMoreSettingsButton: Boolean = false,
    contentAlignment: Alignment = AbsoluteAlignment.CenterLeft,
    sheetPadding: PaddingValues = PaddingValues(QuickSettingsSideSheetTokens.OuterPadding),
    maxSheetWidth: Dp = QuickSettingsSideSheetTokens.MaxSheetWidth,
    maxSheetHeight: Dp = Dp.Unspecified,
    sheetContainerDirection: LayoutDirection = LocalLayoutDirection.current,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    scrimColor: Color = Color.Transparent
) {
    // Keep the last available state so the content stays rendered during the hide animation.
    var lastAvailableUiState by remember {
        mutableStateOf(quickSettingsUiState as? QuickSettingsUiState.Available)
    }
    if (quickSettingsUiState is QuickSettingsUiState.Available) {
        lastAvailableUiState = quickSettingsUiState
    }
    val availableUiState = lastAvailableUiState ?: return

    QuickSettingsSideSheet(
        isOpen = isOpen && quickSettingsUiState is QuickSettingsUiState.Available,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        paneTitle = stringResource(
            quickSettingsTitleRes(
                availableUiState.resolvedCaptureMode(),
                availableUiState.titleResId
            )
        ),
        contentAlignment = contentAlignment,
        sheetPadding = sheetPadding,
        maxSheetWidth = maxSheetWidth,
        maxSheetHeight = maxSheetHeight,
        sheetContainerDirection = sheetContainerDirection,
        windowInsets = windowInsets,
        scrimColor = scrimColor
    ) {
        QuickSettingsContent(
            quickSettingsUiState = availableUiState,
            quickSettingsController = quickSettingsController,
            onNavigateToSettings = onNavigateToSettings,
            showMoreSettingsButton = showMoreSettingsButton
        )
    }
}

/**
 * Slot-based overload of [QuickSettingsSideSheet] hosting arbitrary Quick Settings [content]
 * (such as `CaptureScaffoldDefaults.QuickSettings`) inside the in-hierarchy side sheet, its
 * tap-outside area, focus group, drag, and escape/back handlers.
 *
 * Renders nothing while [isOpen] is `false` and the hide animation has finished.
 *
 * @param isOpen Whether the Quick Settings side sheet is open.
 * @param onDismissRequest Callback invoked when the user swipes the sheet away, taps its drag
 *   handle or the area outside it, or presses back or escape. Callers should set [isOpen] to
 *   `false` in response.
 * @param modifier The [Modifier] applied to the full-size overlay container.
 * @param paneTitle Accessibility pane title announced for the sheet surface.
 * @param contentAlignment Alignment of the sheet within the overlay container. Its horizontal
 *   component also picks the edge the sheet attaches to and slides from. Defaults to
 *   [AbsoluteAlignment.CenterLeft] so the sheet stays on the physical left across LTR and RTL
 *   while interior controls mirror for RTL.
 * @param sheetPadding Padding around the sheet surface (used by side-rail layouts to clear
 *   measured start and end rail widths without shifting the full-screen tap-outside area).
 * @param maxSheetWidth Maximum width cap applied to the sheet surface.
 * @param maxSheetHeight Optional maximum height cap applied to the sheet surface.
 * @param sheetContainerDirection Layout direction used to resolve [contentAlignment] and
 *   [sheetPadding] outside the sheet surface while restoring the caller's `LocalLayoutDirection`
 *   inside the sheet content.
 * @param windowInsets Window insets the sheet stays clear of, applied outside [sheetPadding].
 *   Defaults to [WindowInsets.Companion.safeDrawing].
 * @param scrimColor Color drawn behind the sheet. Transparent by default so the viewfinder is not
 *   dimmed, matching the mocks; the full-screen tap-to-dismiss area is present while the sheet is
 *   visible or opening.
 * @param content Composable Quick Settings content rendered inside the scrollable sheet surface.
 */
@Composable
internal fun QuickSettingsSideSheet(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    paneTitle: String = stringResource(R.string.quick_settings_title_photo_and_video_settings),
    contentAlignment: Alignment = AbsoluteAlignment.CenterLeft,
    sheetPadding: PaddingValues = PaddingValues(QuickSettingsSideSheetTokens.OuterPadding),
    maxSheetWidth: Dp = QuickSettingsSideSheetTokens.MaxSheetWidth,
    maxSheetHeight: Dp = Dp.Unspecified,
    sheetContainerDirection: LayoutDirection = LocalLayoutDirection.current,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    scrimColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    val sheetState = remember { SideSheetState() }
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val showSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val hideSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val settleSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    var isComposed by remember { mutableStateOf(isOpen) }

    LaunchedEffect(isOpen) {
        if (isOpen) {
            isComposed = true
            snapshotFlow { sheetState.hasAnchors }.first { it }
            sheetState.show(showSpec)
        } else {
            if (sheetState.hasAnchors) {
                sheetState.hide(hideSpec)
            }
            isComposed = false
        }
    }

    if (!isComposed) {
        return
    }

    val predictiveBackProgress = remember { Animatable(0f) }
    PredictiveBackHandler(enabled = isOpen) { progress ->
        try {
            progress.collect { backEvent -> predictiveBackProgress.snapTo(backEvent.progress) }
            currentOnDismissRequest()
        } catch (_: CancellationException) {
            predictiveBackProgress.animateTo(0f)
        }
    }
    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SideSheetValue.Hidden) {
            predictiveBackProgress.snapTo(0f)
        }
    }

    val contentLayoutDirection = LocalLayoutDirection.current
    val dismissDescription =
        stringResource(R.string.quick_settings_btn_close_expanded_settings_description)
    val inputModeManager = LocalInputModeManager.current
    val overlayFocusRequester = remember { FocusRequester() }
    val sheetFocusRequester = remember { FocusRequester() }
    val scrimFocusRequester = remember { FocusRequester() }
    var hasSheetFocus by remember { mutableStateOf(false) }
    var hasScrimFocus by remember { mutableStateOf(false) }

    LaunchedEffect(inputModeManager.inputMode) {
        if (!sheetFocusRequester.requestFocus() && !scrimFocusRequester.requestFocus()) {
            overlayFocusRequester.requestFocus()
        }
    }

    val edge = if (
        contentAlignment.align(IntSize.Zero, IntSize(2, 0), sheetContainerDirection).x == 2
    ) {
        SideSheetEdge.Right
    } else {
        SideSheetEdge.Left
    }
    val attachedEdgePadding = if (edge == SideSheetEdge.Left) {
        sheetPadding.calculateLeftPadding(sheetContainerDirection)
    } else {
        sheetPadding.calculateRightPadding(sheetContainerDirection)
    }
    val density = LocalDensity.current
    val attachedEdgeInsetPx = if (edge == SideSheetEdge.Left) {
        windowInsets.getLeft(density, sheetContainerDirection)
    } else {
        windowInsets.getRight(density, sheetContainerDirection)
    }
    // The attached side stays flat and only the free-edge corners are rounded, per the mocks.
    val sheetShape =
        QuickSettingsSheetDefaults.sideSheetShape(attachedToLeft = edge == SideSheetEdge.Left)

    val baseFlingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = sheetState.anchoredDraggableState,
        animationSpec = settleSpec
    )
    val flingBehavior = remember(baseFlingBehavior, sheetState) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                val anchors = sheetState.anchoredDraggableState.anchors
                val velocity = if (anchors.hasPositionFor(SideSheetValue.Hidden)) {
                    with(density) {
                        dampFlingTowardHidden(
                            velocity = initialVelocity,
                            offset = sheetState.anchoredDraggableState.requireOffset(),
                            hiddenOffset = anchors.positionOf(SideSheetValue.Hidden),
                            dampingZone = QuickSettingsSideSheetTokens.FlingDampingZone.toPx(),
                            velocityThreshold =
                            QuickSettingsSideSheetTokens.FlingVelocityThreshold.toPx()
                        )
                    }
                } else {
                    initialVelocity
                }
                try {
                    return with(baseFlingBehavior) { performFling(velocity) }
                } finally {
                    if (sheetState.currentValue == SideSheetValue.Hidden) {
                        currentOnDismissRequest()
                    }
                }
            }
        }
    }

    val heightCapModifier = if (maxSheetHeight != Dp.Unspecified) {
        Modifier.heightIn(max = maxSheetHeight)
    } else {
        Modifier
    }

    CompositionLocalProvider(LocalLayoutDirection provides sheetContainerDirection) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .onKeyEvent { event ->
                    if (event.key == Key.Escape) {
                        if (event.type == KeyEventType.KeyUp) {
                            currentOnDismissRequest()
                        }
                        event.type == KeyEventType.KeyDown || event.type == KeyEventType.KeyUp
                    } else {
                        false
                    }
                }
                .focusRequester(overlayFocusRequester)
                .focusProperties { canFocus = !hasSheetFocus && !hasScrimFocus }
                .focusTarget()
                .semantics { isTraversalGroup = true },
            contentAlignment = contentAlignment
        ) {
            // Shown only while the sheet is visible or opening, so a caller that ignores a
            // swipe-to-dismiss can't leave an invisible layer blocking taps.
            if (isOpen && sheetState.targetValue == SideSheetValue.Expanded) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(scrimColor)
                        .testTag(QUICK_SETTINGS_SCRIM)
                        .semantics {
                            contentDescription = dismissDescription
                            traversalIndex = 1f
                        }
                        .onFocusChanged { hasScrimFocus = it.hasFocus }
                        .focusRequester(scrimFocusRequester)
                        .clickable(
                            role = Role.Button,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { currentOnDismissRequest() }
                        )
                )
            }

            Surface(
                modifier = Modifier
                    .windowInsetsPadding(windowInsets)
                    .padding(sheetPadding)
                    .widthIn(max = maxSheetWidth)
                    .then(heightCapModifier)
                    .sideSheetAnchors(
                        state = sheetState,
                        edge = edge,
                        attachedEdgeDistancePx = {
                            attachedEdgeInsetPx + attachedEdgePadding.roundToPx()
                        }
                    )
                    .anchoredDraggable(
                        state = sheetState.anchoredDraggableState,
                        reverseDirection = false,
                        orientation = Orientation.Horizontal,
                        enabled = isOpen,
                        flingBehavior = flingBehavior
                    )
                    // Stretch toward the attached edge if a springy open overshoots, so no gap
                    // opens between the sheet and that edge (M3 sheets do the same vertically).
                    .graphicsLayer {
                        val overshoot = overshootPastExpanded(
                            sheetState.anchoredDraggableState.offset,
                            edge
                        )
                        scaleX = if (overshoot > 0f) (size.width + overshoot) / size.width else 1f
                        transformOrigin = TransformOrigin(
                            pivotFractionX = if (edge == SideSheetEdge.Left) 1f else 0f,
                            pivotFractionY = 0.5f
                        )
                    }
                    .graphicsLayer {
                        val scale = lerp(
                            1f,
                            QuickSettingsSideSheetTokens.PredictiveBackMinScale,
                            predictiveBackProgress.value
                        )
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(
                            pivotFractionX = if (edge == SideSheetEdge.Left) 0f else 1f,
                            pivotFractionY = 0.5f
                        )
                    }
                    .onFocusChanged { hasSheetFocus = it.hasFocus }
                    .focusRequester(sheetFocusRequester)
                    .focusGroup()
                    .semantics {
                        this.paneTitle = paneTitle
                        traversalIndex = 0f
                        dismiss(dismissDescription) {
                            currentOnDismissRequest()
                            true
                        }
                    }
                    .testTag(QUICK_SETTINGS_SIDE_SHEET_TAG),
                shape = sheetShape,
                color = QuickSettingsSheetDefaults.ContainerColor,
                contentColor = QuickSettingsSheetDefaults.ContentColor,
                shadowElevation = QuickSettingsSheetDefaults.ShadowElevation
            ) {
                // The drag handle sits on the free (non-attached) edge regardless of the
                // content's layout direction, so lay the row out in absolute LTR.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (edge == SideSheetEdge.Right) {
                            SideSheetDragHandle(
                                onClickLabel = dismissDescription,
                                onClick = { currentOnDismissRequest() }
                            )
                        }
                        CompositionLocalProvider(
                            LocalLayoutDirection provides contentLayoutDirection
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    // Undo the container's overshoot stretch for the content.
                                    .graphicsLayer {
                                        val overshoot = overshootPastExpanded(
                                            sheetState.anchoredDraggableState.offset,
                                            edge
                                        )
                                        val containerWidth = size.width + overshoot
                                        scaleX = if (overshoot > 0f) {
                                            size.width / containerWidth
                                        } else {
                                            1f
                                        }
                                    }
                                    .verticalScroll(rememberScrollState())
                                    .padding(
                                        vertical =
                                        QuickSettingsSideSheetTokens.ContentVerticalPadding
                                    )
                            ) {
                                content()
                            }
                        }
                        if (edge == SideSheetEdge.Left) {
                            SideSheetDragHandle(
                                onClickLabel = dismissDescription,
                                onClick = { currentOnDismissRequest() }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Measures the sheet, updates its horizontal anchors (expanded in place, hidden just past the
 * attached edge) and places it at the current drag offset.
 *
 * Offsets are physical pixels, so the sheet is placed with [Placeable.PlacementScope.place]
 * rather than mirrored for RTL.
 */
private fun Modifier.sideSheetAnchors(
    state: SideSheetState,
    edge: SideSheetEdge,
    attachedEdgeDistancePx: Density.() -> Int
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val hiddenDistance = placeable.width + attachedEdgeDistancePx()
    val hiddenOffset = if (edge == SideSheetEdge.Left) -hiddenDistance else hiddenDistance
    state.anchoredDraggableState.updateAnchors(
        DraggableAnchors {
            SideSheetValue.Hidden at hiddenOffset.toFloat()
            SideSheetValue.Expanded at 0f
        }
    )
    layout(placeable.width, placeable.height) {
        val offset = state.anchoredDraggableState.offset
        placeable.place(
            x = if (offset.isNaN()) hiddenOffset else offset.roundToInt(),
            y = 0
        )
    }
}

/**
 * How far, in pixels, [offset] has overshot the expanded anchor (0) away from the attached
 * [edge]; 0 when it has not.
 */
internal fun overshootPastExpanded(offset: Float, edge: SideSheetEdge): Float = when {
    offset.isNaN() -> 0f
    edge == SideSheetEdge.Left -> offset.coerceAtLeast(0f)
    else -> (-offset).coerceAtLeast(0f)
}

/**
 * Damps a fling toward the hidden anchor as the sheet nears it, mirroring Material 3 sheets, so a
 * fast fling eases out of view instead of snapping. Velocity is scaled by the square of the
 * remaining fraction of [dampingZone], but a fling that started at or above [velocityThreshold]
 * keeps at least that much velocity so it still settles hidden.
 *
 * @return the velocity to fling with.
 */
internal fun dampFlingTowardHidden(
    velocity: Float,
    offset: Float,
    hiddenOffset: Float,
    dampingZone: Float,
    velocityThreshold: Float
): Float {
    val towardHidden = hiddenOffset - offset
    if (velocity == 0f || towardHidden == 0f || (velocity > 0f) != (towardHidden > 0f)) {
        return velocity
    }
    val distance = abs(towardHidden)
    if (distance >= dampingZone) return velocity
    val factor = distance / dampingZone
    val speed = abs(velocity)
    var damped = speed * factor * factor
    if (speed >= velocityThreshold) damped = max(damped, velocityThreshold)
    return if (velocity > 0f) damped else -damped
}

/** Vertical drag handle drawn on the sheet's free edge; tapping it dismisses the sheet. */
@Composable
private fun SideSheetDragHandle(onClickLabel: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .testTag(QUICK_SETTINGS_DRAG_HANDLE)
            // Clickable wraps the minimum-size modifier so its semantics and hit bounds cover the
            // full 48dp touch target rather than only the 4x32dp pill.
            .clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick)
            .minimumInteractiveComponentSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = QuickSettingsSideSheetTokens.DragHandleWidth,
                    height = QuickSettingsSideSheetTokens.DragHandleHeight
                )
                .background(QuickSettingsSheetDefaults.DragHandleColor, CircleShape)
        )
    }
}

@Composable
private fun QuickSettingsSideSheetPreviewContent(
    showMoreSettingsButton: Boolean = false,
    sheetPadding: PaddingValues = PaddingValues(QuickSettingsSideSheetTokens.OuterPadding),
    maxSheetWidth: Dp = QuickSettingsSideSheetTokens.MaxSheetWidth
) {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        QuickSettingsSideSheet(
            isOpen = true,
            onDismissRequest = {},
            quickSettingsUiState = QuickSettingsUiState.Available(
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
            ),
            quickSettingsController = NoOpQuickSettingsController(),
            onNavigateToSettings = {},
            showMoreSettingsButton = showMoreSettingsButton,
            sheetPadding = sheetPadding,
            maxSheetWidth = maxSheetWidth
        )
    }
}

@Preview(
    name = "LTR - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "RTL - Dark",
    locale = "ar",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "CompactShort (360dp height)",
    heightDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun QuickSettingsSideSheetPreview() {
    QuickSettingsSideSheetPreviewContent(showMoreSettingsButton = false)
}

@Preview(
    name = "Foldable Rail-Cleared (852x883dp)",
    widthDp = 852,
    heightDp = 883,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun QuickSettingsSideSheetFoldablePreview() {
    QuickSettingsSideSheetPreviewContent(
        showMoreSettingsButton = true,
        sheetPadding = PaddingValues(start = 92.dp, end = 92.dp, top = 24.dp, bottom = 24.dp),
        maxSheetWidth = QuickSettingsSideSheetTokens.FoldableMaxSheetWidth
    )
}

@Preview(
    name = "Tablet Rail-Cleared (1280x800dp)",
    widthDp = 1280,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun QuickSettingsSideSheetTabletPreview() {
    QuickSettingsSideSheetPreviewContent(
        showMoreSettingsButton = true,
        sheetPadding = PaddingValues(start = 100.dp, end = 100.dp, top = 24.dp, bottom = 28.dp),
        maxSheetWidth = QuickSettingsSideSheetTokens.MaxSheetWidth
    )
}

@Preview(
    name = "With More Settings - LTR - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "With More Settings - RTL - Dark",
    locale = "ar",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun QuickSettingsSideSheetWithMoreSettingsPreview() {
    QuickSettingsSideSheetPreviewContent(showMoreSettingsButton = true)
}
