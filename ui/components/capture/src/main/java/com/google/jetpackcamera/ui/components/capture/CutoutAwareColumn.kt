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

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.VerticalAlignmentLine
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

private object CutoutAwareColumnTokens {
    val DefaultVerticalSpacing = 8.dp
    val DefaultCutoutClearance = 8.dp
}

/**
 * A vertical column layout for side-rail status indicators that automatically flows items around
 * any intersecting display cutout (such as a corner or side-edge camera hole-punch).
 *
 * Items stack strictly top-to-bottom (`y` is never inverted in RTL), shifting individual items
 * downward past any cutout bounding rectangles that overlap the column's horizontal bounds. Marks
 * itself as a semantics traversal group (`isTraversalGroup = true`) so screen readers traverse
 * all items in the rail together even when split across a side cutout. The column resolves its
 * width identically in LTR and RTL without stretching to `constraints.maxWidth` in RTL, while
 * direction-relative [horizontalAlignment] values (`Start`/`End`) mirror across the column width
 * in RTL and [androidx.compose.ui.AbsoluteAlignment] values (`Left`/`Right`) remain pinned to
 * physical edges.
 *
 * Status icons stack top-to-bottom in declaration order (`Flash -> HDR -> VideoQuality ->
 * Stabilization`, non-inverted in RTL).
 *
 * When [pillVerticalPadding] is positive, contiguous active items that do not cross a cutout are
 * grouped into a shared vertical pill segment with [pillVerticalPadding] at the top and bottom of
 * each segment; if a cutout falls between active items, the pill splits into separate segments
 * above and below the cutout. When no items are visible, no pill segments are drawn.
 *
 * This layout wraps its measurement in a [LookaheadScope] and provides a [ColumnScope] receiver so
 * that [androidx.compose.animation.AnimatedVisibility] children use vertical expand/shrink
 * transitions without jumping across a cutout mid-animation:
 * - During the lookahead pass (`isLookingAhead == true`), children are measured at their target
 *   heights to determine whether their final resting position lies below a cutout (`lockedMinY`).
 * - During the approach pass (`isLookingAhead == false`), entering and exiting children remain
 *   anchored below any cutout their target or prior visible state cleared, preventing teleportation
 *   while their animated height grows from or shrinks to `0`.
 *
 * @param modifier the [Modifier] to be applied to this column.
 * @param verticalSpacing vertical gap between adjacent visible items (or between split pills).
 * @param cutoutClearance additional vertical padding kept clear above and below a display cutout.
 * @param pillVerticalPadding vertical padding applied at the top and bottom of each connected pill
 *   segment.
 * @param pillMinWidth minimum width of each connected pill segment when at least one child is
 *   visible.
 * @param pillBackgroundColor background color drawn for each connected pill segment, or
 *   [Color.Unspecified] to disable pill background drawing.
 * @param horizontalAlignment horizontal alignment of children and pill segments within the column
 *   (direction-relative `Start`/`End` mirror in RTL; use [androidx.compose.ui.AbsoluteAlignment]
 *   to pin to physical left/right edges).
 * @param cutoutRectsOverride optional list of cutout bounding rectangles in the column's local
 *   coordinate space, used for deterministic testing. When `null`, cutout rects are queried from
 *   the host window's [android.view.DisplayCutout] and mapped into the column's local coordinates.
 * @param onPillSegmentsPlaced optional callback invoked with the physical local [IntRect] bounds of
 *   each active pill segment after layout, used for testing.
 * @param content the column children, scoped to [ColumnScope].
 */
// TODO: Confirm final vertical status indicator stacking order with UX.
@Composable
internal fun CutoutAwareColumn(
    modifier: Modifier = Modifier,
    verticalSpacing: Dp = CutoutAwareColumnTokens.DefaultVerticalSpacing,
    cutoutClearance: Dp = CutoutAwareColumnTokens.DefaultCutoutClearance,
    pillVerticalPadding: Dp = 0.dp,
    pillMinWidth: Dp = 0.dp,
    pillBackgroundColor: Color = Color.Unspecified,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    cutoutRectsOverride: List<IntRect>? = null,
    onPillSegmentsPlaced: ((List<IntRect>) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val view = LocalView.current
    val cutoutInsets = WindowInsets.displayCutout
    var windowAnchor by remember { mutableStateOf<WindowAnchor?>(null) }
    val lockedMinY = remember { mutableMapOf<Int, Int>() }
    var pillSegments by remember { mutableStateOf<List<IntRect>>(emptyList()) }

    LookaheadScope {
        Layout(
            content = { CutoutColumnScope.content() },
            modifier = modifier
                .semantics { isTraversalGroup = true }
                .onGloballyPositioned { coordinates ->
                    val anchor = coordinates.toWindowAnchor()
                    if (windowAnchor != anchor) {
                        windowAnchor = anchor
                    }
                }
                .drawBehind {
                    if (pillBackgroundColor != Color.Unspecified) {
                        for (segment in pillSegments) {
                            if (segment.width > 0 && segment.height > 0) {
                                val radius = min(segment.width, segment.height) / 2f
                                drawRoundRect(
                                    color = pillBackgroundColor,
                                    topLeft = Offset(
                                        segment.left.toFloat(),
                                        segment.top.toFloat()
                                    ),
                                    size = Size(
                                        segment.width.toFloat(),
                                        segment.height.toFloat()
                                    ),
                                    cornerRadius = CornerRadius(radius, radius)
                                )
                            }
                        }
                    }
                }
        ) { measurables, constraints ->
            // Read WindowInsets.displayCutout inside measure to subscribe to inset updates.
            cutoutInsets.getTop(this).ignoreResult()
            cutoutInsets.getBottom(this).ignoreResult()
            cutoutInsets.getLeft(this, layoutDirection).ignoreResult()
            cutoutInsets.getRight(this, layoutDirection).ignoreResult()

            val spacingPx = verticalSpacing.roundToPx()
            val clearancePx = cutoutClearance.roundToPx()
            val pillPadPx = pillVerticalPadding.roundToPx()
            val pillMinWidthPx = pillMinWidth.roundToPx()

            val childConstraints = Constraints(
                minWidth = 0,
                maxWidth = constraints.maxWidth,
                minHeight = 0,
                maxHeight = Constraints.Infinity
            )
            val placeables = measurables.map { it.measure(childConstraints) }

            val maxChildWidth = placeables.maxOfOrNull { it.width } ?: 0
            val hasAnyVisibleChild = placeables.any { it.height > 0 }
            val effectiveMinWidth = if (hasAnyVisibleChild) {
                max(constraints.minWidth, pillMinWidthPx)
            } else {
                constraints.minWidth
            }
            val columnWidth = max(maxChildWidth, effectiveMinWidth)
                .coerceAtMost(constraints.maxWidth)

            val localCutoutRects = resolveLocalCutoutRects(
                cutoutRectsOverride = cutoutRectsOverride,
                windowCutoutRects = ViewCompat.getRootWindowInsets(view)
                    ?.displayCutout?.boundingRects.orEmpty(),
                windowAnchor = windowAnchor,
                isRtl = layoutDirection == LayoutDirection.Rtl,
                rtlReferenceWidth = columnWidth
            )
            val verticalCutouts = computeVerticalKeepOutRanges(
                localCutoutRects = localCutoutRects,
                columnWidth = columnWidth,
                clearancePx = clearancePx
            )
            val placement = calculateVerticalCutoutPlacement(
                measurables = measurables,
                placeables = placeables,
                maxChildWidthConstraint = constraints.maxWidth,
                columnWidth = columnWidth,
                spacingPx = spacingPx,
                pillPadPx = pillPadPx,
                pillMinWidthPx = pillMinWidthPx,
                horizontalAlignment = horizontalAlignment,
                layoutDirection = layoutDirection,
                verticalCutouts = verticalCutouts,
                isLookingAhead = isLookingAhead,
                lockedMinY = lockedMinY
            )

            val columnHeight = max(placement.contentHeight, constraints.minHeight)
                .coerceAtMost(constraints.maxHeight)

            layout(columnWidth, columnHeight) {
                if (!isLookingAhead) {
                    if (pillSegments != placement.pillSegments) {
                        pillSegments = placement.pillSegments
                    }
                    onPillSegmentsPlaced?.invoke(placement.pillSegments)
                }
                for (i in placeables.indices) {
                    if (!placement.isActive[i]) continue
                    val placeable = placeables[i]
                    val x = horizontalAlignment.align(
                        placeable.width,
                        columnWidth,
                        layoutDirection
                    )
                    placeable.place(x, placement.yPositions[i])
                }
            }
        }
    }
}

private class VerticalCutoutPlacement(
    val yPositions: IntArray,
    val isActive: BooleanArray,
    val pillSegments: List<IntRect>,
    val contentHeight: Int
)

private fun computeVerticalKeepOutRanges(
    localCutoutRects: List<IntRect>,
    columnWidth: Int,
    clearancePx: Int
): List<IntRange> {
    if (localCutoutRects.isEmpty()) return emptyList()
    return localCutoutRects
        .filter { rect ->
            rect.width > 0 && rect.height > 0 && rect.right > 0 &&
                rect.left < columnWidth && rect.bottom + clearancePx > 0
        }
        .map { rect ->
            IntRange(
                start = (rect.top - clearancePx).coerceAtLeast(0),
                endInclusive = rect.bottom + clearancePx
            )
        }
        .sortedBy { it.first }
}

private fun calculateVerticalCutoutPlacement(
    measurables: List<Measurable>,
    placeables: List<Placeable>,
    maxChildWidthConstraint: Int,
    columnWidth: Int,
    spacingPx: Int,
    pillPadPx: Int,
    pillMinWidthPx: Int,
    horizontalAlignment: Alignment.Horizontal,
    layoutDirection: LayoutDirection,
    verticalCutouts: List<IntRange>,
    isLookingAhead: Boolean,
    lockedMinY: MutableMap<Int, Int>
): VerticalCutoutPlacement {
    if (!isLookingAhead) {
        lockedMinY.keys.removeAll { it >= placeables.size }
    }

    val yPositions = IntArray(placeables.size)
    val isActive = BooleanArray(placeables.size)
    val resolvedPillSegments = mutableListOf<IntRect>()

    var collisionCursor = 0
    var placeCursor = 0
    var inOpenGroup = false
    var currentGroupStartPx = 0
    var currentGroupVisibleBottomPx = -1
    var currentGroupMaxWidthPx = 0
    var maxBottom = 0

    fun closeCurrentGroup() {
        if (inOpenGroup) {
            if (currentGroupVisibleBottomPx >= 0 && pillPadPx > 0) {
                val groupEndPx = currentGroupVisibleBottomPx + pillPadPx
                val pillWidth = max(currentGroupMaxWidthPx, pillMinWidthPx)
                    .coerceAtMost(columnWidth)
                val pillLeft = horizontalAlignment.align(
                    pillWidth,
                    columnWidth,
                    layoutDirection
                )
                resolvedPillSegments.add(
                    IntRect(
                        left = pillLeft,
                        top = currentGroupStartPx,
                        right = pillLeft + pillWidth,
                        bottom = groupEndPx
                    )
                )
                maxBottom = max(maxBottom, groupEndPx)
            }
            collisionCursor += pillPadPx
            inOpenGroup = false
            currentGroupVisibleBottomPx = -1
            currentGroupMaxWidthPx = 0
        }
    }

    for (i in placeables.indices) {
        val placeable = placeables[i]
        // Query maxIntrinsicHeight so children wrapped in AnimatedVisibility
        // (whose ExpandShrinkModifier passes intrinsics through to the unclipped child)
        // evaluate cutout collisions against their full target height throughout
        // both enter (expandVertically) and exit (shrinkVertically) animations,
        // including the initial 0-height frame of an enter transition.
        val collisionHeight = max(
            placeable.height,
            measurables[i].maxIntrinsicHeight(maxChildWidthConstraint)
        )
        if (collisionHeight <= 0) {
            yPositions[i] = placeCursor
            if (!isLookingAhead) {
                lockedMinY.remove(i)
            }
            continue
        }
        isActive[i] = true

        val minY = if (!isLookingAhead) (lockedMinY[i] ?: 0) else 0

        if (inOpenGroup) {
            val candidateCollisionY = collisionCursor + spacingPx
            val collidesWithCutout = verticalCutouts.any { cut ->
                candidateCollisionY + collisionHeight + pillPadPx > cut.first &&
                    collisionCursor < cut.last
            }
            if (candidateCollisionY < minY || collidesWithCutout) {
                closeCurrentGroup()
            }
        }

        if (!inOpenGroup) {
            var groupStart = if (collisionCursor > 0) collisionCursor + spacingPx else 0
            if (!isLookingAhead && minY > 0) {
                groupStart = max(groupStart, minY - pillPadPx)
            }
            val neededGroupHeight = collisionHeight + 2 * pillPadPx
            for (cut in verticalCutouts) {
                if (groupStart + neededGroupHeight > cut.first && groupStart < cut.last) {
                    groupStart = max(groupStart, cut.last)
                }
            }
            inOpenGroup = true
            currentGroupStartPx = groupStart
            currentGroupVisibleBottomPx = -1
            currentGroupMaxWidthPx = 0
            collisionCursor = groupStart + pillPadPx
            placeCursor = groupStart + pillPadPx
        } else {
            collisionCursor += spacingPx
        }

        if (isLookingAhead) {
            val groupStartForCutout = currentGroupStartPx
            val requiredMinY = verticalCutouts
                .filter { groupStartForCutout >= it.last }
                .maxOfOrNull { it.last + pillPadPx } ?: 0
            lockedMinY[i] = requiredMinY
        }

        collisionCursor += collisionHeight
        yPositions[i] = placeCursor
        if (placeable.height > 0) {
            val itemBottom = placeCursor + placeable.height
            currentGroupVisibleBottomPx = itemBottom
            currentGroupMaxWidthPx = max(currentGroupMaxWidthPx, placeable.width)
            maxBottom = max(maxBottom, itemBottom)
            placeCursor = itemBottom + spacingPx
        }
    }
    closeCurrentGroup()

    return VerticalCutoutPlacement(
        yPositions = yPositions,
        isActive = isActive,
        pillSegments = resolvedPillSegments,
        contentHeight = maxBottom
    )
}

private object CutoutColumnScope : ColumnScope {
    override fun Modifier.weight(weight: Float, fill: Boolean): Modifier = this
    override fun Modifier.align(alignment: Alignment.Horizontal): Modifier = this
    override fun Modifier.alignBy(alignmentLine: VerticalAlignmentLine): Modifier = this
    override fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int): Modifier = this
}
