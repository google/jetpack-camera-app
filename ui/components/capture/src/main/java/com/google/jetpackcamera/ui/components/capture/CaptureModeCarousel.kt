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

import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.model.CaptureSubModeId
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureSubModeUiState
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Height of [CaptureModeCarousel], and of the slot that [PreviewLayout] reserves for it so that
 * showing or hiding the carousel does not move the surrounding controls.
 */
internal val CaptureModeCarouselHeight = 50.dp
private val PillHeight = 32.dp
private val ItemHorizontalPadding = 12.dp
private val ItemMinWidth = 72.dp
private val ItemSpacing = 4.dp

/** Shadow behind unselected labels while the carousel is drawn over the viewfinder. */
private val OverViewfinderTextShadowBlur = 4.dp
private val OverViewfinderTextShadowOffsetY = 1.dp

private const val DISABLED_CONTENT_ALPHA = 0.38f
private const val SETTLE_DAMPING_RATIO = 0.8f
private const val SETTLE_STIFFNESS = 380f

/**
 * How long the carousel waits, after committing a sub-mode, for the selected sub-mode to change to
 * it before returning to the selected sub-mode.
 */
private const val SELECTION_CONFIRMATION_TIMEOUT_MS = 500L

/**
 * A swipeable and clickable capture sub-mode selector carousel.
 *
 * When [uiState] is [CaptureSubModeUiState.Unavailable], this composable does not render.
 * When [uiState] is [CaptureSubModeUiState.Available], the sub-mode labels are laid out in a
 * horizontal row inside a [CaptureModeCarouselHeight] tall touch container. The row scrolls so
 * that the selected sub-mode is horizontally centered. A pill fixed at the center of the carousel
 * highlights the centered label, and its width interpolates between the widths of neighbouring
 * labels while the row moves.
 *
 * Dragging moves the row with the pointer. On release, the row settles on the nearest enabled
 * item, or on the next enabled item in the swipe direction for a flick. Tapping an item scrolls it
 * to the center. In both cases, [onSelectSubMode] is invoked only after the row has settled, so
 * that the resulting camera reconfiguration does not interrupt the scroll animation. If the
 * selected sub-mode in [uiState] does not change to the committed sub-mode shortly afterwards, the
 * row returns to the selected sub-mode.
 *
 * When the carousel is drawn over the viewfinder, as reported by [OverlapAwareStyleProvider], the
 * unselected labels are drawn with a dark shadow so that they stay legible on bright scenes.
 */
@Composable
fun CaptureModeCarousel(
    uiState: CaptureSubModeUiState,
    onSelectSubMode: (CaptureSubModeId) -> Unit,
    modifier: Modifier = Modifier,
    selectedContainerColor: Color = MaterialTheme.colorScheme.secondaryFixed,
    selectedContentColor: Color = MaterialTheme.colorScheme.onSecondaryFixed,
    unselectedContentColor: Color = Color.White
) {
    if (uiState !is CaptureSubModeUiState.Available || uiState.availableSubModes.isEmpty()) {
        return
    }

    val items = uiState.availableSubModes
    val labels = items.map { stringResource(it.value.labelResId) }
    val ids = items.map { it.value.id }

    val baseTextStyle = MaterialTheme.typography.labelLarge
    val unselectedTextStyle = baseTextStyle.copy(fontWeight = FontWeight.Medium)
    val selectedTextStyle = baseTextStyle.copy(fontWeight = FontWeight.SemiBold)

    val density = LocalDensity.current
    val isOverViewfinder =
        LocalCameraControlBackgroundStyle.current == CameraControlBackgroundStyle.BLACK_60
    // The shadow does not affect text size, so it is applied only when drawing. Measuring with
    // the shadow-free style keeps the geometry, and therefore the drag state, unchanged when the
    // overlap changes.
    val unselectedDrawStyle = remember(unselectedTextStyle, isOverViewfinder, density) {
        if (isOverViewfinder) {
            with(density) {
                unselectedTextStyle.copy(
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(0f, OverViewfinderTextShadowOffsetY.toPx()),
                        blurRadius = OverViewfinderTextShadowBlur.toPx()
                    )
                )
            }
        } else {
            unselectedTextStyle
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val geometry = remember(ids, labels, unselectedTextStyle, selectedTextStyle, density) {
        val horizontalPaddingPx = with(density) { ItemHorizontalPadding.toPx() }
        val minWidthPx = with(density) { ItemMinWidth.toPx() }
        CarouselGeometry(
            widths = FloatArray(labels.size) { index ->
                val textWidth = max(
                    textMeasurer.measure(
                        text = labels[index],
                        style = unselectedTextStyle,
                        maxLines = 1,
                        softWrap = false
                    ).size.width,
                    textMeasurer.measure(
                        text = labels[index],
                        style = selectedTextStyle,
                        maxLines = 1,
                        softWrap = false
                    ).size.width
                )
                max(textWidth + 2 * horizontalPaddingPx, minWidthPx)
            },
            spacing = with(density) { ItemSpacing.toPx() }
        )
    }

    val selectedIndex = ids.indexOf(uiState.selectedSubMode).coerceAtLeast(0)
    val selectedId = ids[selectedIndex]

    fun isEnabled(index: Int) = items[index] is SingleSelectableUiState.SelectableUi

    // Each item is anchored at the offset that centers it. Disabled items are laid out but are not
    // anchors, so the row never comes to rest on them. The selected item is always an anchor so
    // that the row can rest on it.
    val anchoredIndices = items.indices.filter { isEnabled(it) || it == selectedIndex }
    val anchors = remember(geometry, anchoredIndices) {
        DraggableAnchors {
            for (index in anchoredIndices) {
                ids[index] at -geometry.centers[index]
            }
        }
    }

    // A change to the items replaces the state. This ends any drag or animation of the previous
    // items and centers the new row on the selected item.
    val state = remember(anchors) {
        AnchoredDraggableState(initialValue = selectedId, anchors = anchors)
    }
    val settleSpec = remember {
        spring<Float>(dampingRatio = SETTLE_DAMPING_RATIO, stiffness = SETTLE_STIFFNESS)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val currentSelectedId by rememberUpdatedState(selectedId)
    val currentOnSelectSubMode by rememberUpdatedState(onSelectSubMode)

    // The content coordinate that is aligned with the horizontal center of the carousel.
    val position: () -> Float = { -state.requireOffset() }
    val centeredIndex by remember(state, geometry) {
        derivedStateOf { geometry.nearestIndex(position()) }
    }

    // Commit the item that the row comes to rest on. If the selection does not follow, for
    // example because the camera rejected the sub-mode, return to the selected item.
    LaunchedEffect(state) {
        snapshotFlow { state.settledValue }.collectLatest { settledId ->
            if (settledId == currentSelectedId) return@collectLatest
            currentOnSelectSubMode(settledId)
            val followed = withTimeoutOrNull(SELECTION_CONFIRMATION_TIMEOUT_MS) {
                snapshotFlow { currentSelectedId }.first { it == settledId }
            }
            if (followed == null) {
                snapshotFlow { isDragged }.first { !it }
                // Launched separately so that a drag that interrupts the animation does not end
                // this collection.
                scope.launch { state.animateTo(currentSelectedId, settleSpec) }
            }
        }
    }

    // Follow selection changes made outside of the carousel, once the user stops dragging.
    LaunchedEffect(state, selectedId) {
        snapshotFlow { isDragged }.first { !it }
        if (state.settledValue != selectedId) {
            state.animateTo(selectedId, settleSpec)
        }
    }

    // Tick as each item crosses the center while the user drags the row.
    LaunchedEffect(state, geometry) {
        snapshotFlow { geometry.nearestIndex(position()) }
            .drop(1)
            .collect {
                if (isDragged) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CaptureModeCarouselHeight)
            .testTag(CAPTURE_MODE_CAROUSEL)
            .selectableGroup()
            // In right-to-left layouts, the drag direction is reversed to match the mirrored row.
            .anchoredDraggable(
                state = state,
                orientation = Orientation.Horizontal,
                interactionSource = interactionSource,
                flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                    state = state,
                    animationSpec = settleSpec
                )
            )
    ) {
        // Base layer: interactive, unselected-styled labels.
        CarouselRow(
            labels = labels,
            geometry = geometry,
            position = position,
            textStyle = unselectedDrawStyle,
            contentColor = { index ->
                if (isEnabled(index)) {
                    unselectedContentColor
                } else {
                    unselectedContentColor.copy(alpha = DISABLED_CONTENT_ALPHA)
                }
            },
            modifier = Modifier.fillMaxSize(),
            itemModifier = { index ->
                Modifier
                    .testTag(ids[index].carouselOptionTag)
                    .selectable(
                        selected = index == centeredIndex,
                        enabled = isEnabled(index),
                        role = Role.Tab,
                        interactionSource = null,
                        indication = null
                    ) {
                        scope.launch { state.animateTo(ids[index], settleSpec) }
                    }
            }
        )

        // Highlight layer: selected-styled labels, visible only through the center pill.
        CarouselRow(
            labels = labels,
            geometry = geometry,
            position = position,
            textStyle = selectedTextStyle,
            contentColor = { selectedContentColor },
            modifier = Modifier
                .fillMaxSize()
                .clearAndSetSemantics {}
                .drawWithContent {
                    val pillWidth = geometry.pillWidth(position())
                    val pillHeight = PillHeight.toPx()
                    val pillRect = Rect(
                        offset = Offset(
                            x = (size.width - pillWidth) / 2f,
                            y = (size.height - pillHeight) / 2f
                        ),
                        size = Size(pillWidth, pillHeight)
                    )
                    val pillPath = Path().apply {
                        addRoundRect(RoundRect(pillRect, CornerRadius(pillHeight / 2f)))
                    }
                    clipPath(pillPath) {
                        drawRect(selectedContainerColor)
                        this@drawWithContent.drawContent()
                    }
                }
        )
    }
}

/**
 * Lays out [labels] in a single row of fixed-width slots so that the content coordinate
 * [position] is aligned with the horizontal center of this layout.
 */
@Composable
private fun CarouselRow(
    labels: List<String>,
    geometry: CarouselGeometry,
    position: () -> Float,
    textStyle: TextStyle,
    contentColor: (Int) -> Color,
    modifier: Modifier = Modifier,
    itemModifier: (Int) -> Modifier = { Modifier }
) {
    Layout(
        content = {
            labels.forEachIndexed { index, label ->
                Box(modifier = itemModifier(index), contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        color = contentColor(index),
                        style = textStyle,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        modifier = modifier
    ) { measurables, constraints ->
        val height = constraints.maxHeight
        val placeables = measurables.mapIndexed { index, measurable ->
            measurable.measure(Constraints.fixed(ceil(geometry.widths[index]).toInt(), height))
        }
        layout(constraints.maxWidth, height) {
            // Reading the position here limits scroll-driven invalidation to layout and draw.
            val origin = constraints.maxWidth / 2f - position()
            placeables.forEachIndexed { index, placeable ->
                val left = geometry.centers[index] - geometry.widths[index] / 2f
                placeable.placeRelative((origin + left).roundToInt(), 0)
            }
        }
    }
}

/**
 * Slot widths and centers of the carousel items, in pixels along the scroll axis. Adjacent slots
 * are separated by [spacing].
 */
private class CarouselGeometry(val widths: FloatArray, spacing: Float) {
    val centers: FloatArray = FloatArray(widths.size).also { centers ->
        var start = 0f
        widths.forEachIndexed { index, width ->
            centers[index] = start + width / 2f
            start += width + spacing
        }
    }

    fun nearestIndex(position: Float): Int = centers.indices.minBy { abs(centers[it] - position) }

    /** The pill width at [position], interpolated between the two neighbouring slot widths. */
    fun pillWidth(position: Float): Float {
        if (position <= centers.first()) return widths.first()
        if (position >= centers.last()) return widths.last()
        val right = centers.indexOfFirst { it >= position }
        val left = right - 1
        val fraction = (position - centers[left]) / (centers[right] - centers[left])
        return widths[left] + (widths[right] - widths[left]) * fraction
    }
}
