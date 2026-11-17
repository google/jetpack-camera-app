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
import androidx.compose.animation.core.snap
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
import com.google.jetpackcamera.ui.components.capture.LocalDisableAnimations
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
    val MaxSheetWidth = 470.dp
    val MinSheetHeight = 570.dp
    val OuterVerticalPadding = 16.dp
    val ContentVerticalPadding = 20.dp
    val DragHandleWidth = 4.dp
    val DragHandleHeight = 32.dp
    val PredictiveBackMinScale = 0.95f

    // Fling damping near the hidden anchor; same values as Material 3 sheets.
    val FlingDampingZone = 125.dp
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

/** Drag state of a [QuickSettingsSideSheet], backed by a horizontal [AnchoredDraggableState]. */
@Stable
internal class SideSheetState(initialValue: SideSheetValue = SideSheetValue.Hidden) {
    internal val anchoredDraggableState = AnchoredDraggableState(initialValue)

    val currentValue: SideSheetValue get() = anchoredDraggableState.currentValue

    val targetValue: SideSheetValue get() = anchoredDraggableState.targetValue

    /** Whether the sheet is on screen, including while animating. */
    val isVisible: Boolean
        get() = currentValue != SideSheetValue.Hidden || targetValue != SideSheetValue.Hidden

    internal val hasAnchors: Boolean get() = anchoredDraggableState.anchors.size > 0

    suspend fun show(animationSpec: AnimationSpec<Float>) {
        anchoredDraggableState.animateTo(SideSheetValue.Expanded, animationSpec)
    }

    suspend fun hide(animationSpec: AnimationSpec<Float>) {
        anchoredDraggableState.animateTo(SideSheetValue.Hidden, animationSpec)
    }
}

/**
 * [QuickSettingsSideSheet] hosting [QuickSettingsContent]. Renders nothing unless
 * [quickSettingsUiState] is [QuickSettingsUiState.Available]; the pane title follows the capture
 * mode. Other parameters match the slot overload.
 *
 * @param showMoreSettingsButton Whether to show the "More settings" button. Off by default for
 *   layouts that surface settings elsewhere.
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
    sheetPadding: PaddingValues =
        PaddingValues(vertical = QuickSettingsSideSheetTokens.OuterVerticalPadding),
    maxSheetWidth: Dp = QuickSettingsSideSheetTokens.MaxSheetWidth,
    minSheetHeight: Dp = QuickSettingsSideSheetTokens.MinSheetHeight,
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
        minSheetHeight = minSheetHeight,
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
 * In-hierarchy Quick Settings sheet attached to a side edge for medium and expanded layouts.
 *
 * It slides in from its edge and is dismissed by swiping back toward it, the drag handle, the
 * Dismiss accessibility action, back (with predictive back), Escape, or tapping the scrim outside
 * the sheet. Host it in a `Box` over the content it covers. Leaves composition once the hide
 * animation finishes.
 *
 * @param onDismissRequest Called when the user dismisses the sheet; set [isOpen] to `false`.
 * @param contentAlignment Where the sheet sits; its horizontal side is also the edge it attaches
 *   to. Defaults to the physical left in both LTR and RTL.
 * @param sheetPadding Space around the sheet, e.g. to clear side rails.
 * @param sheetContainerDirection Direction used to resolve [contentAlignment] and [sheetPadding];
 *   the content keeps the caller's direction.
 * @param windowInsets Insets the sheet stays clear of, outside [sheetPadding].
 * @param scrimColor Color behind the sheet; transparent so the viewfinder is not dimmed.
 */
@Composable
internal fun QuickSettingsSideSheet(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    paneTitle: String = stringResource(R.string.quick_settings_title_photo_and_video_settings),
    contentAlignment: Alignment = AbsoluteAlignment.CenterLeft,
    sheetPadding: PaddingValues =
        PaddingValues(vertical = QuickSettingsSideSheetTokens.OuterVerticalPadding),
    maxSheetWidth: Dp = QuickSettingsSideSheetTokens.MaxSheetWidth,
    minSheetHeight: Dp = QuickSettingsSideSheetTokens.MinSheetHeight,
    maxSheetHeight: Dp = Dp.Unspecified,
    sheetContainerDirection: LayoutDirection = LocalLayoutDirection.current,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    scrimColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    val sheetState = remember { SideSheetState() }
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val disableAnimations = LocalDisableAnimations.current
    val showSpec: AnimationSpec<Float> =
        if (disableAnimations) snap() else MaterialTheme.motionScheme.defaultSpatialSpec()
    val hideSpec: AnimationSpec<Float> =
        if (disableAnimations) snap() else MaterialTheme.motionScheme.fastEffectsSpec()
    val settleSpec: AnimationSpec<Float> =
        if (disableAnimations) snap() else MaterialTheme.motionScheme.defaultSpatialSpec()
    var isComposed by remember { mutableStateOf(isOpen) }
    if (isOpen) {
        isComposed = true
    }

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
    var hasSheetFocus by remember { mutableStateOf(false) }

    LaunchedEffect(inputModeManager.inputMode) {
        if (!sheetFocusRequester.requestFocus()) {
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

    CompositionLocalProvider(LocalLayoutDirection provides sheetContainerDirection) {
        if (isOpen && sheetState.targetValue == SideSheetValue.Expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor)
                    .testTag(QUICK_SETTINGS_SCRIM)
                    .semantics {
                        contentDescription = dismissDescription
                        traversalIndex = 1f
                    }
                    .clickable(
                        role = Role.Button,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { currentOnDismissRequest() }
                    )
            )
        }

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
                .focusProperties { canFocus = !hasSheetFocus }
                .focusTarget()
                .semantics { isTraversalGroup = true },
            contentAlignment = contentAlignment
        ) {
            Surface(
                modifier = Modifier
                    .windowInsetsPadding(windowInsets)
                    .padding(sheetPadding)
                    .widthIn(max = maxSheetWidth)
                    .heightIn(min = minSheetHeight, max = maxSheetHeight)
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
                        scaleX = if (overshoot > 0f && size.width > 0f) {
                            (size.width + overshoot) / size.width
                        } else {
                            1f
                        }
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
                                        scaleX = if (overshoot > 0f && size.width > 0f) {
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

/** Sets anchors from the measured width and places the sheet at the drag offset (physical px). */
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

/** Pixels [offset] has moved past the expanded anchor away from [edge], or 0. */
internal fun overshootPastExpanded(offset: Float, edge: SideSheetEdge): Float = when {
    offset.isNaN() -> 0f
    edge == SideSheetEdge.Left -> offset.coerceAtLeast(0f)
    else -> (-offset).coerceAtLeast(0f)
}

/**
 * Damps a fling toward the hidden anchor inside [dampingZone], as Material 3 sheets do, keeping at
 * least [velocityThreshold] for flings that started above it.
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
    sheetPadding: PaddingValues =
        PaddingValues(vertical = QuickSettingsSideSheetTokens.OuterVerticalPadding),
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
    name = "Foldable (852x883dp)",
    widthDp = 852,
    heightDp = 883,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun QuickSettingsSideSheetFoldablePreview() {
    QuickSettingsSideSheetPreviewContent(showMoreSettingsButton = true)
}

@Preview(
    name = "Tablet (1280x800dp)",
    widthDp = 1280,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun QuickSettingsSideSheetTabletPreview() {
    QuickSettingsSideSheetPreviewContent(showMoreSettingsButton = true)
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
