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

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.spring
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.model.CaptureSubModeId
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureSubModeUiState
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

private val CarouselHeight = 80.dp
private val PillHeight = 32.dp
private val ItemHorizontalPadding = 20.dp
private val ItemMinWidth = 80.dp

/** Release velocity (per second) above which a swipe advances to the next item. */
private val FlickVelocityThreshold = 400.dp

/** Release velocity (per second) above which a swipe uses free decay to pick its target. */
private val FastFlingVelocityThreshold = 5000.dp

private const val DISABLED_CONTENT_ALPHA = 0.38f
private const val SETTLE_DAMPING_RATIO = 0.8f
private const val SETTLE_STIFFNESS = 380f

/**
 * A swipeable and clickable capture sub-mode selector carousel.
 *
 * When [uiState] is [CaptureSubModeUiState.Unavailable], this composable does not render.
 * When [uiState] is [CaptureSubModeUiState.Available], the sub-mode labels are laid out in a
 * horizontal row inside an `80.dp` tall touch container. The row scrolls so that the selected
 * sub-mode is horizontally centered. A pill fixed at the center of the carousel highlights the
 * centered label, and its width interpolates between the widths of neighbouring labels while
 * the row moves.
 *
 * Dragging moves the row with the pointer. On release, the row settles on the nearest enabled
 * item (or the next item in the swipe direction for a flick). Tapping an item scrolls it to the
 * center. In both cases, [onSelectSubMode] is invoked only after the row has settled, so that the
 * resulting camera reconfiguration does not interrupt the scroll animation.
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
    val textMeasurer = rememberTextMeasurer()
    val geometry = remember(ids, labels, unselectedTextStyle, selectedTextStyle, density) {
        val horizontalPaddingPx = with(density) { ItemHorizontalPadding.toPx() }
        val minWidthPx = with(density) { ItemMinWidth.toPx() }
        CarouselGeometry(
            FloatArray(labels.size) { index ->
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
            }
        )
    }

    val selectedIndex = ids.indexOf(uiState.selectedSubMode).coerceAtLeast(0)
    val scrollState = remember { CarouselScrollState(geometry.centers[selectedIndex]) }

    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val decay = remember(density) { splineBasedDecay<Float>(density) }
    val flickVelocityPx = with(density) { FlickVelocityThreshold.toPx() }
    val fastFlingVelocityPx = with(density) { FastFlingVelocityThreshold.toPx() }

    val currentUiState by rememberUpdatedState(uiState)
    val currentOnSelectSubMode by rememberUpdatedState(onSelectSubMode)

    val centeredIndex by remember(geometry) {
        derivedStateOf { geometry.nearestIndex(scrollState.position) }
    }

    fun isEnabled(index: Int) = items[index] is SingleSelectableUiState.SelectableUi

    fun settleTo(index: Int, initialVelocity: Float, commit: Boolean) {
        scrollState.settleJob?.cancel()
        scrollState.isSettling = true
        scrollState.settleJob = scope.launch {
            val self = coroutineContext.job
            try {
                animate(
                    initialValue = scrollState.position,
                    targetValue = geometry.centers[index],
                    initialVelocity = initialVelocity,
                    animationSpec = spring(
                        dampingRatio = SETTLE_DAMPING_RATIO,
                        stiffness = SETTLE_STIFFNESS
                    )
                ) { value, _ -> scrollState.position = value }
                scrollState.isUserDriven = false
                val id = ids[index]
                if (commit && id != currentUiState.selectedSubMode) {
                    currentOnSelectSubMode(id)
                }
            } finally {
                if (scrollState.settleJob === self) {
                    scrollState.isSettling = false
                }
            }
        }
    }

    fun releaseTargetIndex(velocity: Float): Int {
        val position = scrollState.position
        val enabledIndices = items.indices.filter(::isEnabled)
        if (enabledIndices.isEmpty()) {
            return geometry.nearestIndex(position)
        }
        val speed = abs(velocity)
        if (speed >= flickVelocityPx && speed < fastFlingVelocityPx) {
            val next = if (velocity > 0) {
                enabledIndices.firstOrNull { geometry.centers[it] > position + 0.5f }
            } else {
                enabledIndices.lastOrNull { geometry.centers[it] < position - 0.5f }
            }
            if (next != null) return next
        }
        val projected = if (speed >= fastFlingVelocityPx) {
            decay.calculateTargetValue(position, velocity)
        } else {
            position
        }
        return enabledIndices.minBy { abs(geometry.centers[it] - projected) }
    }

    // Keep the row in sync with the externally selected sub-mode. A change to the set of items
    // snaps the row; a change to only the selection (or a rejected commit after settling) animates
    // it back to the selected item.
    LaunchedEffect(geometry, selectedIndex, scrollState.isSettling) {
        val target = geometry.centers[selectedIndex]
        if (scrollState.geometry !== geometry) {
            scrollState.geometry = geometry
            scrollState.settleJob?.cancel()
            scrollState.position = target
        } else if (!scrollState.isDragging &&
            !scrollState.isSettling &&
            scrollState.position != target
        ) {
            androidx.compose.runtime.withFrameNanos {}
            val currentSelectedIndex =
                ids.indexOf(currentUiState.selectedSubMode).coerceAtLeast(0)
            if (!scrollState.isDragging &&
                !scrollState.isSettling &&
                currentSelectedIndex == selectedIndex &&
                scrollState.position != target
            ) {
                settleTo(selectedIndex, initialVelocity = 0f, commit = false)
            }
        }
    }

    // Tick as each item crosses the center during user-driven motion.
    LaunchedEffect(geometry) {
        snapshotFlow { geometry.nearestIndex(scrollState.position) }
            .drop(1)
            .collect {
                if (scrollState.isUserDriven) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
    }

    val draggableState = rememberDraggableState { delta ->
        scrollState.position =
            (scrollState.position - delta).coerceIn(geometry.minPosition, geometry.maxPosition)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CarouselHeight)
            .testTag(CAPTURE_MODE_CAROUSEL)
            .selectableGroup()
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                reverseDirection = isRtl,
                startDragImmediately = scrollState.isSettling,
                onDragStarted = {
                    scrollState.settleJob?.cancel()
                    scrollState.isDragging = true
                    scrollState.isUserDriven = true
                },
                onDragStopped = { velocity ->
                    scrollState.isDragging = false
                    // Moving the pointer towards the start increases the scroll position.
                    val positionVelocity = -velocity
                    settleTo(
                        index = releaseTargetIndex(positionVelocity),
                        initialVelocity = positionVelocity,
                        commit = true
                    )
                }
            )
    ) {
        // Base layer: interactive, unselected-styled labels.
        CarouselRow(
            labels = labels,
            geometry = geometry,
            position = { scrollState.position },
            textStyle = unselectedTextStyle,
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
                    .testTag(captureSubModeOptionTag(ids[index]))
                    .selectable(
                        selected = index == centeredIndex,
                        enabled = isEnabled(index),
                        role = Role.Tab,
                        interactionSource = null,
                        indication = null
                    ) {
                        scrollState.isUserDriven = true
                        settleTo(index, initialVelocity = 0f, commit = true)
                    }
            }
        )

        // Highlight layer: selected-styled labels, visible only through the center pill.
        CarouselRow(
            labels = labels,
            geometry = geometry,
            position = { scrollState.position },
            textStyle = selectedTextStyle,
            contentColor = { selectedContentColor },
            modifier = Modifier
                .fillMaxSize()
                .clearAndSetSemantics {}
                .drawWithContent {
                    val pillWidth = geometry.pillWidth(scrollState.position)
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

/** Slot widths and centers of the carousel items, in pixels along the scroll axis. */
private class CarouselGeometry(val widths: FloatArray) {
    val centers: FloatArray = FloatArray(widths.size).also { centers ->
        var start = 0f
        widths.forEachIndexed { index, width ->
            centers[index] = start + width / 2f
            start += width
        }
    }

    val minPosition: Float get() = centers.first()
    val maxPosition: Float get() = centers.last()

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

/** Mutable scroll state of the carousel. */
private class CarouselScrollState(initialPosition: Float) {
    /** Content coordinate currently aligned with the center of the carousel. */
    var position by mutableFloatStateOf(initialPosition)
    var isSettling by mutableStateOf(false)
    var isDragging = false
    var isUserDriven = false
    var settleJob: Job? = null
    var geometry: CarouselGeometry? = null
}
