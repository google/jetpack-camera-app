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

import android.annotation.SuppressLint
import android.graphics.Rect
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import com.google.jetpackcamera.core.common.ignoreResult
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A cutout-aware linear layout that arranges children along [orientation] while flowing items
 * around any intersecting display cutout (such as a corner or hole-punch camera cutout).
 *
 * - When [orientation] is [Orientation.Horizontal], items flow from start to end (mirroring in
 *   RTL via `placeRelative`) and avoid cutouts intersecting the row's vertical bounds.
 * - When [orientation] is [Orientation.Vertical], items stack strictly top-to-bottom in physical
 *   coordinates (`y` is never inverted in RTL), avoiding cutouts that intersect the aligned
 *   pill/content horizontal bounds while respecting [horizontalAlignment] (`Start`/`End` mirror in
 *   RTL; [androidx.compose.ui.AbsoluteAlignment] pins to physical edges).
 *
 * When [pillPadding] is positive, contiguous active items that do not cross a cutout are grouped
 * into a shared rounded pill segment with [pillPadding] at the start and end of each segment; if
 * a cutout falls between active items, the pill splits into separate segments on each side. When
 * no items are visible, no pill segments are drawn.
 *
 * Before [onGloballyPositioned] resolves the container's window anchor (when [cutoutRectsOverride]
 * is `null`), the layout measures its bounds without placing children or drawing pills on frame 1
 * so indicators never flash on top of a cutout and jump on frame 2.
 *
 * Wraps measurement in a [LookaheadScope] so [androidx.compose.animation.AnimatedVisibility]
 * children expand/shrink without teleporting across a cutout mid-animation. Marks itself as a
 * semantics traversal group (`isTraversalGroup = true`) so screen readers traverse all items
 * together even when split across a display cutout.
 */
@SuppressLint("AvoidNullableCollections", "NullableCollection")
@Composable
fun CutoutAwareLinearLayout(
    orientation: Orientation,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    cutoutClearance: Dp = 8.dp,
    pillPadding: Dp = 0.dp,
    pillMinCrossSize: Dp = 0.dp,
    pillBackgroundColor: Color = Color.Unspecified,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    cutoutRectsOverride: List<IntRect>? = null,
    onPillSegmentsPlaced: ((List<IntRect>) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val cutoutInsets = WindowInsets.displayCutout
    var windowAnchor by remember { mutableStateOf<WindowAnchor?>(null) }
    val lockedMinMain = remember { mutableMapOf<Int, Int>() }
    var pillSegments by remember { mutableStateOf<List<IntRect>>(emptyList()) }

    LookaheadScope {
        Layout(
            content = content,
            modifier = modifier
                .semantics { isTraversalGroup = true }
                .onGloballyPositioned { coordinates ->
                    val anchor = coordinates.toWindowAnchor()
                    if (windowAnchor != anchor) {
                        windowAnchor = anchor
                    }
                }
                .drawCutoutPillSegments(pillBackgroundColor) { pillSegments }
        ) { measurables, constraints ->
            // Read WindowInsets.displayCutout inside measure to subscribe to inset updates.
            cutoutInsets.getTop(this).ignoreResult()
            cutoutInsets.getBottom(this).ignoreResult()
            cutoutInsets.getLeft(this, layoutDirection).ignoreResult()
            cutoutInsets.getRight(this, layoutDirection).ignoreResult()

            val isHorizontal = orientation == Orientation.Horizontal
            val spacingPx = spacing.roundToPx()
            val clearancePx = cutoutClearance.roundToPx()
            val pillPadPx = pillPadding.roundToPx()
            val pillMinCrossPx = pillMinCrossSize.roundToPx()

            val childConstraints = if (isHorizontal) {
                Constraints(
                    minWidth = 0,
                    maxWidth = Constraints.Infinity,
                    minHeight = 0,
                    maxHeight = constraints.maxHeight
                )
            } else {
                Constraints(
                    minWidth = 0,
                    maxWidth = constraints.maxWidth,
                    minHeight = 0,
                    maxHeight = Constraints.Infinity
                )
            }
            val placeables = measurables.map { it.measure(childConstraints) }

            val maxChildCross = placeables.maxOfOrNull {
                if (isHorizontal) it.height else it.width
            } ?: 0
            val hasAnyVisibleChild = placeables.any {
                (if (isHorizontal) it.width else it.height) > 0
            }
            val minCrossConstraint = if (isHorizontal) constraints.minHeight else constraints.minWidth
            val maxCrossConstraint = if (isHorizontal) constraints.maxHeight else constraints.maxWidth
            val effectiveMinCross = if (hasAnyVisibleChild) {
                max(minCrossConstraint, pillMinCrossPx)
            } else {
                minCrossConstraint
            }
            val containerCrossSize = max(maxChildCross, effectiveMinCross)
                .coerceAtMost(maxCrossConstraint)

            val hasResolvedAnchor = cutoutRectsOverride != null || windowAnchor != null
            val isRtl = isHorizontal && layoutDirection == LayoutDirection.Rtl
            val rtlReferenceWidth = if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                placeables.sumOf { it.width }
            }
            val localCutoutRects = resolveLocalCutoutRects(
                cutoutRectsOverride = cutoutRectsOverride,
                windowCutoutRects = ViewCompat.getRootWindowInsets(view)
                    ?.displayCutout?.boundingRects.orEmpty(),
                windowAnchor = windowAnchor,
                isRtl = isRtl,
                rtlReferenceWidth = rtlReferenceWidth
            )

            val keepOutRanges = if (isHorizontal) {
                localCutoutRects
                    .filter { rect ->
                        rect.width > 0 && rect.height > 0 && rect.bottom > 0 &&
                            rect.top < containerCrossSize && rect.right + clearancePx > 0
                    }
                    .map { rect ->
                        IntRange(
                            start = (rect.left - clearancePx).coerceAtLeast(0),
                            endInclusive = rect.right + clearancePx
                        )
                    }
                    .sortedBy { it.first }
            } else {
                val maxSegmentWidth = max(maxChildCross, pillMinCrossPx)
                    .coerceAtMost(containerCrossSize)
                val alignedLeft = horizontalAlignment.align(
                    maxSegmentWidth,
                    containerCrossSize,
                    layoutDirection
                )
                val alignedRight = alignedLeft + maxSegmentWidth
                localCutoutRects
                    .filter { rect ->
                        rect.width > 0 && rect.height > 0 &&
                            rect.right > alignedLeft && rect.left < alignedRight &&
                            rect.bottom + clearancePx > 0
                    }
                    .map { rect ->
                        IntRange(
                            start = (rect.top - clearancePx).coerceAtLeast(0),
                            endInclusive = rect.bottom + clearancePx
                        )
                    }
                    .sortedBy { it.first }
            }

            val placement = calculateCutoutAxisPlacement(
                orientation = orientation,
                measurables = measurables,
                placeables = placeables,
                spacingPx = spacingPx,
                pillPadPx = pillPadPx,
                pillMinCrossPx = pillMinCrossPx,
                containerCrossSize = containerCrossSize,
                crossConstraintPx = maxCrossConstraint,
                keepOutRanges = keepOutRanges,
                isLookingAhead = isLookingAhead,
                lockedMinMain = lockedMinMain,
                verticalAlignment = verticalAlignment,
                horizontalAlignment = horizontalAlignment,
                layoutDirection = layoutDirection
            )

            val containerMainSize = if (isHorizontal) {
                if (isRtl && constraints.hasBoundedWidth) {
                    max(placement.contentMainSize, constraints.maxWidth)
                } else {
                    max(placement.contentMainSize, constraints.minWidth)
                        .coerceAtMost(constraints.maxWidth)
                }
            } else {
                max(placement.contentMainSize, constraints.minHeight)
                    .coerceAtMost(constraints.maxHeight)
            }

            val layoutWidth = if (isHorizontal) containerMainSize else containerCrossSize
            val layoutHeight = if (isHorizontal) containerCrossSize else containerMainSize

            val resolvedPillSegments = when {
                !hasResolvedAnchor -> emptyList()
                isRtl -> placement.pillSegments.map { rect ->
                    IntRect(
                        left = layoutWidth - rect.right,
                        top = rect.top,
                        right = layoutWidth - rect.left,
                        bottom = rect.bottom
                    )
                }
                else -> placement.pillSegments
            }

            layout(layoutWidth, layoutHeight) {
                if (!isLookingAhead) {
                    if (pillSegments != resolvedPillSegments) {
                        pillSegments = resolvedPillSegments
                    }
                    onPillSegmentsPlaced?.invoke(resolvedPillSegments)
                }
                if (!hasResolvedAnchor) return@layout
                for (i in placeables.indices) {
                    if (!placement.isActive[i]) continue
                    val placeable = placeables[i]
                    if (isHorizontal) {
                        val y = verticalAlignment.align(placeable.height, containerCrossSize)
                        placeable.placeRelative(placement.mainPositions[i], y)
                    } else {
                        val x = horizontalAlignment.align(
                            placeable.width,
                            containerCrossSize,
                            layoutDirection
                        )
                        placeable.place(x, placement.mainPositions[i])
                    }
                }
            }
        }
    }
}

/**
 * A horizontal row layout that flows items around any intersecting display cutout.
 *
 * Thin wrapper around [CutoutAwareLinearLayout] with [Orientation.Horizontal].
 */
@SuppressLint("AvoidNullableCollections", "NullableCollection")
@Composable
fun CutoutAwareRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 8.dp,
    cutoutClearance: Dp = 8.dp,
    pillHorizontalPadding: Dp = 0.dp,
    pillMinHeight: Dp = 0.dp,
    pillBackgroundColor: Color = Color.Unspecified,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    cutoutRectsOverride: List<IntRect>? = null,
    onPillSegmentsPlaced: ((List<IntRect>) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    CutoutAwareLinearLayout(
        orientation = Orientation.Horizontal,
        modifier = modifier,
        spacing = horizontalSpacing,
        cutoutClearance = cutoutClearance,
        pillPadding = pillHorizontalPadding,
        pillMinCrossSize = pillMinHeight,
        pillBackgroundColor = pillBackgroundColor,
        verticalAlignment = verticalAlignment,
        cutoutRectsOverride = cutoutRectsOverride,
        onPillSegmentsPlaced = onPillSegmentsPlaced,
        content = content
    )
}

/**
 * A vertical column layout that stacks items top-to-bottom in physical coordinates around any
 * intersecting display cutout.
 *
 * Thin wrapper around [CutoutAwareLinearLayout] with [Orientation.Vertical].
 */
@SuppressLint("AvoidNullableCollections", "NullableCollection")
@Composable
fun CutoutAwareColumn(
    modifier: Modifier = Modifier,
    verticalSpacing: Dp = 8.dp,
    cutoutClearance: Dp = 8.dp,
    pillVerticalPadding: Dp = 0.dp,
    pillMinWidth: Dp = 0.dp,
    pillBackgroundColor: Color = Color.Unspecified,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    cutoutRectsOverride: List<IntRect>? = null,
    onPillSegmentsPlaced: ((List<IntRect>) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    CutoutAwareLinearLayout(
        orientation = Orientation.Vertical,
        modifier = modifier,
        spacing = verticalSpacing,
        cutoutClearance = cutoutClearance,
        pillPadding = pillVerticalPadding,
        pillMinCrossSize = pillMinWidth,
        pillBackgroundColor = pillBackgroundColor,
        horizontalAlignment = horizontalAlignment,
        cutoutRectsOverride = cutoutRectsOverride,
        onPillSegmentsPlaced = onPillSegmentsPlaced,
        content = content
    )
}

/**
 * Window-space anchor of a cutout-aware container, capturing horizontal bounds and top offset so
 * vertical height animations do not invalidate cutout coordinate resolution.
 */
@Immutable
internal data class WindowAnchor(
    val left: Int,
    val top: Int,
    val width: Int
)

/**
 * Computes the [WindowAnchor] for these [LayoutCoordinates] in window pixel coordinates.
 */
internal fun LayoutCoordinates.toWindowAnchor(): WindowAnchor {
    val bounds = boundsInWindow()
    val left = bounds.left.roundToInt()
    val top = bounds.top.roundToInt()
    val right = bounds.right.roundToInt()
    return WindowAnchor(
        left = left,
        top = top,
        width = right - left
    )
}

/**
 * Resolves display cutout bounding rectangles into a container's local coordinate space, using
 * [cutoutRectsOverride] when provided or offsetting [windowCutoutRects] by [windowAnchor]. When
 * [isRtl] is `true`, horizontal bounds are mirrored into start-relative coordinates; vertical
 * layouts pass `isRtl = false` to keep coordinates in physical space.
 */
internal fun resolveLocalCutoutRects(
    cutoutRectsOverride: List<IntRect>?,
    windowCutoutRects: List<Rect>,
    windowAnchor: WindowAnchor?,
    isRtl: Boolean,
    rtlReferenceWidth: Int
): List<IntRect> {
    val ltrRects = cutoutRectsOverride ?: run {
        val anchor = windowAnchor ?: return emptyList()
        val offsetX = anchor.left
        val offsetY = anchor.top
        windowCutoutRects.map { rect ->
            IntRect(
                left = rect.left - offsetX,
                top = rect.top - offsetY,
                right = rect.right - offsetX,
                bottom = rect.bottom - offsetY
            )
        }
    }
    return if (isRtl) {
        val startEdgeX = if (cutoutRectsOverride == null && windowAnchor != null) {
            windowAnchor.width
        } else {
            rtlReferenceWidth
        }
        ltrRects.map { rect ->
            IntRect(
                left = startEdgeX - rect.right,
                top = rect.top,
                right = startEdgeX - rect.left,
                bottom = rect.bottom
            )
        }
    } else {
        ltrRects
    }
}

internal class CutoutAxisPlacement(
    val mainPositions: IntArray,
    val isActive: BooleanArray,
    val pillSegments: List<IntRect>,
    val contentMainSize: Int
)

internal fun Modifier.drawCutoutPillSegments(
    pillBackgroundColor: Color,
    pillSegments: () -> List<IntRect>
): Modifier = drawBehind {
    if (pillBackgroundColor != Color.Unspecified) {
        for (segment in pillSegments()) {
            if (segment.width > 0 && segment.height > 0) {
                val radius = min(segment.width, segment.height) / 2f
                drawRoundRect(
                    color = pillBackgroundColor,
                    topLeft = Offset(segment.left.toFloat(), segment.top.toFloat()),
                    size = Size(segment.width.toFloat(), segment.height.toFloat()),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
        }
    }
}

/**
 * Packs [placeables] into contiguous pill groups along [orientation] while avoiding
 * [keepOutRanges].
 *
 * Uses two cursors during the sweep: `collisionCursor` advances by each item's target intrinsic
 * size so enter/exit `AnimatedVisibility` children reserve their full resting footprint, while
 * `placeCursor` advances by the current animated `placeable` size so visible items pack smoothly.
 * During lookahead (`isLookingAhead == true`), [lockedMinMain] records the minimum start position
 * past any cleared cutout per child index; during the approach pass, items stay anchored at or
 * past `lockedMinMain` so they do not teleport across a cutout while growing from `0`. When a
 * contiguous group closes (`closeCurrentGroup`), a rounded pill segment [IntRect] is emitted if
 * [pillPadPx] is positive and at least one item in the group had non-zero size.
 */
internal fun calculateCutoutAxisPlacement(
    orientation: Orientation,
    measurables: List<Measurable>,
    placeables: List<Placeable>,
    spacingPx: Int,
    pillPadPx: Int,
    pillMinCrossPx: Int,
    containerCrossSize: Int,
    crossConstraintPx: Int,
    keepOutRanges: List<IntRange>,
    isLookingAhead: Boolean,
    lockedMinMain: MutableMap<Int, Int>,
    verticalAlignment: Alignment.Vertical,
    horizontalAlignment: Alignment.Horizontal,
    layoutDirection: LayoutDirection
): CutoutAxisPlacement {
    if (!isLookingAhead) {
        lockedMinMain.keys.removeAll { it >= placeables.size }
    }

    val isHorizontal = orientation == Orientation.Horizontal
    val mainPositions = IntArray(placeables.size)
    val isActive = BooleanArray(placeables.size)
    val resolvedPillSegments = mutableListOf<IntRect>()

    var collisionCursor = 0
    var placeCursor = 0
    var inOpenGroup = false
    var currentGroupStartPx = 0
    var currentGroupVisibleEndPx = -1
    var currentGroupMaxCrossPx = 0
    var maxEnd = 0

    fun closeCurrentGroup() {
        if (inOpenGroup) {
            if (currentGroupVisibleEndPx >= 0 && pillPadPx > 0) {
                val groupEndPx = currentGroupVisibleEndPx + pillPadPx
                val pillCross = max(currentGroupMaxCrossPx, pillMinCrossPx)
                    .coerceAtMost(containerCrossSize)
                val segment = if (isHorizontal) {
                    val pillTop = verticalAlignment.align(pillCross, containerCrossSize)
                    IntRect(
                        left = currentGroupStartPx,
                        top = pillTop,
                        right = groupEndPx,
                        bottom = pillTop + pillCross
                    )
                } else {
                    val pillLeft = horizontalAlignment.align(
                        pillCross,
                        containerCrossSize,
                        layoutDirection
                    )
                    IntRect(
                        left = pillLeft,
                        top = currentGroupStartPx,
                        right = pillLeft + pillCross,
                        bottom = groupEndPx
                    )
                }
                resolvedPillSegments.add(segment)
                maxEnd = max(maxEnd, groupEndPx)
            }
            collisionCursor += pillPadPx
            inOpenGroup = false
            currentGroupVisibleEndPx = -1
            currentGroupMaxCrossPx = 0
        }
    }

    for (i in placeables.indices) {
        val placeable = placeables[i]
        val itemMainSize = if (isHorizontal) placeable.width else placeable.height
        val itemCrossSize = if (isHorizontal) placeable.height else placeable.width
        val intrinsicMain = if (isHorizontal) {
            measurables[i].maxIntrinsicWidth(crossConstraintPx)
        } else {
            measurables[i].maxIntrinsicHeight(crossConstraintPx)
        }
        val collisionMainSize = max(itemMainSize, intrinsicMain)
        if (collisionMainSize <= 0) {
            mainPositions[i] = placeCursor
            if (!isLookingAhead) {
                lockedMinMain.remove(i)
            }
            continue
        }
        isActive[i] = true

        val minMain = if (!isLookingAhead) (lockedMinMain[i] ?: 0) else 0

        if (inOpenGroup) {
            val candidateCollisionMain = collisionCursor + spacingPx
            val collidesWithCutout = keepOutRanges.any { cut ->
                candidateCollisionMain + collisionMainSize + pillPadPx > cut.first &&
                    collisionCursor < cut.last
            }
            if (candidateCollisionMain < minMain || collidesWithCutout) {
                closeCurrentGroup()
            }
        }

        if (!inOpenGroup) {
            var groupStart = if (collisionCursor > 0) collisionCursor + spacingPx else 0
            if (!isLookingAhead && minMain > 0) {
                groupStart = max(groupStart, minMain - pillPadPx)
            }
            val neededGroupMain = collisionMainSize + 2 * pillPadPx
            for (cut in keepOutRanges) {
                if (groupStart + neededGroupMain > cut.first && groupStart < cut.last) {
                    groupStart = max(groupStart, cut.last)
                }
            }
            inOpenGroup = true
            currentGroupStartPx = groupStart
            currentGroupVisibleEndPx = -1
            currentGroupMaxCrossPx = 0
            collisionCursor = groupStart + pillPadPx
            placeCursor = groupStart + pillPadPx
        } else {
            collisionCursor += spacingPx
        }

        if (isLookingAhead) {
            val groupStartForCutout = currentGroupStartPx
            val requiredMinMain = keepOutRanges
                .filter { groupStartForCutout >= it.last }
                .maxOfOrNull { it.last + pillPadPx } ?: 0
            lockedMinMain[i] = requiredMinMain
        }

        collisionCursor += collisionMainSize
        mainPositions[i] = placeCursor
        if (itemMainSize > 0) {
            val itemEnd = placeCursor + itemMainSize
            currentGroupVisibleEndPx = itemEnd
            currentGroupMaxCrossPx = max(currentGroupMaxCrossPx, itemCrossSize)
            maxEnd = max(maxEnd, itemEnd)
            placeCursor = itemEnd + spacingPx
        }
    }
    closeCurrentGroup()

    return CutoutAxisPlacement(
        mainPositions = mainPositions,
        isActive = isActive,
        pillSegments = resolvedPillSegments,
        contentMainSize = maxEnd
    )
}
