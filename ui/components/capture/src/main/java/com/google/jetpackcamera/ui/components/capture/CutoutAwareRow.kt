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
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Measured
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
 * A horizontal row layout for top-bar status indicators that automatically flows items around any
 * intersecting display cutout (such as a corner or centered camera hole-punch).
 *
 * Unlike applying a uniform top inset for the full status bar or cutout height, this layout allows
 * the indicator row to occupy the status bar region (`y = 0`) while shifting individual items
 * horizontally past any cutout bounding rectangles that overlap the row's vertical bounds. Marks
 * itself as a semantics traversal group (`isTraversalGroup = true`) so screen readers traverse all
 * items in the row together even when split across a display cutout.
 *
 * When [pillHorizontalPadding] is positive, contiguous active items that do not cross a cutout are
 * grouped into a shared pill segment with [pillHorizontalPadding] at the start and end of each
 * segment; if a cutout falls between active items, the pill is split into separate segments on
 * each side of the cutout. When no items are visible, no pill segments are drawn.
 *
 * This layout wraps its measurement in a [LookaheadScope] and provides a [RowScope] receiver so
 * that [androidx.compose.animation.AnimatedVisibility] children use horizontal expand/shrink
 * transitions without jumping across a cutout mid-animation:
 * - During the lookahead pass (`isLookingAhead == true`), children are measured at their target
 *   widths to determine whether their final resting position lies past a cutout (`lockedMinX`).
 * - During the approach pass (`isLookingAhead == false`), entering and exiting children remain
 *   anchored past any cutout their target or prior visible state cleared, preventing teleportation
 *   while their animated width grows from or shrinks to `0`.
 *
 * @param modifier the [Modifier] to be applied to this row.
 * @param horizontalSpacing horizontal gap between adjacent visible items (or between split pills).
 * @param cutoutClearance additional horizontal padding kept clear on both sides of a display cutout.
 * @param pillHorizontalPadding horizontal padding applied at the start and end of each connected
 *   pill segment.
 * @param pillMinHeight minimum height of each connected pill segment when at least one child is
 *   visible.
 * @param pillBackgroundColor background color drawn for each connected pill segment, or
 *   [Color.Unspecified] to disable pill background drawing.
 * @param verticalAlignment vertical alignment of children within the row.
 * @param cutoutRectsOverride optional list of cutout bounding rectangles in the row's local
 *   coordinate space, used for deterministic testing. When `null`, cutout rects are queried from
 *   the host window's [android.view.DisplayCutout] and mapped into the row's local coordinates.
 * @param onPillSegmentsPlaced optional callback invoked with the physical local [IntRect] bounds of
 *   each active pill segment after layout, used for testing.
 * @param content the row children, scoped to [RowScope].
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
    content: @Composable RowScope.() -> Unit
) {
    val view = LocalView.current
    val cutoutInsets = WindowInsets.displayCutout
    var windowAnchor by remember { mutableStateOf<WindowAnchor?>(null) }
    val lockedMinX = remember { mutableMapOf<Int, Int>() }
    var pillSegments by remember { mutableStateOf<List<IntRect>>(emptyList()) }

    LookaheadScope {
        Layout(
            content = { CutoutRowScope.content() },
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

            val spacingPx = horizontalSpacing.roundToPx()
            val clearancePx = cutoutClearance.roundToPx()
            val pillPadPx = pillHorizontalPadding.roundToPx()
            val pillMinHeightPx = pillMinHeight.roundToPx()

            val childConstraints = Constraints(
                minWidth = 0,
                maxWidth = Constraints.Infinity,
                minHeight = 0,
                maxHeight = constraints.maxHeight
            )
            val placeables = measurables.map { it.measure(childConstraints) }

            val maxChildHeight = placeables.maxOfOrNull { it.height } ?: 0
            val hasAnyVisibleChild = placeables.any { it.width > 0 }
            val effectiveMinHeight = if (hasAnyVisibleChild) {
                max(constraints.minHeight, pillMinHeightPx)
            } else {
                constraints.minHeight
            }
            val rowHeight = max(maxChildHeight, effectiveMinHeight)
                .coerceAtMost(constraints.maxHeight)

            // Resolve cutout rects in the row's start-relative coordinate space so that
            // xPositions align with placeRelative in both LTR and RTL layouts.
            val isRtl = layoutDirection == LayoutDirection.Rtl
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

            // Convert vertically overlapping cutout rects into sorted horizontal keep-out ranges.
            val horizontalCutouts = localCutoutRects
                .filter { rect ->
                    rect.width > 0 && rect.height > 0 && rect.bottom > 0 &&
                        rect.top < rowHeight && rect.right + clearancePx > 0
                }
                .map { rect ->
                    IntRange(
                        start = (rect.left - clearancePx).coerceAtLeast(0),
                        endInclusive = rect.right + clearancePx
                    )
                }
                .sortedBy { it.first }

            val placement = calculateCutoutAxisPlacement(
                measurables = measurables,
                placeables = placeables,
                spacingPx = spacingPx,
                pillPadPx = pillPadPx,
                keepOutRanges = horizontalCutouts,
                isLookingAhead = isLookingAhead,
                lockedMinMain = lockedMinX,
                mainSize = { it.width },
                crossSize = { it.height },
                intrinsicMainSize = { it.maxIntrinsicWidth(constraints.maxHeight) },
                createPillSegment = { groupStartPx, groupEndPx, maxCrossPx ->
                    val pillHeight = max(maxCrossPx, pillMinHeightPx)
                        .coerceAtMost(rowHeight)
                    val pillTop = verticalAlignment.align(pillHeight, rowHeight)
                    IntRect(
                        left = groupStartPx,
                        top = pillTop,
                        right = groupEndPx,
                        bottom = pillTop + pillHeight
                    )
                }
            )
            val startRelativePills = placement.pillSegments
            val maxRight = placement.contentMainSize

            val rowWidth = if (isRtl && constraints.hasBoundedWidth) {
                max(maxRight, constraints.maxWidth)
            } else {
                max(maxRight, constraints.minWidth).coerceAtMost(constraints.maxWidth)
            }

            val resolvedPillSegments = if (isRtl) {
                startRelativePills.map { rect ->
                    IntRect(
                        left = rowWidth - rect.right,
                        top = rect.top,
                        right = rowWidth - rect.left,
                        bottom = rect.bottom
                    )
                }
            } else {
                startRelativePills
            }

            layout(rowWidth, rowHeight) {
                if (!isLookingAhead) {
                    if (pillSegments != resolvedPillSegments) {
                        pillSegments = resolvedPillSegments
                    }
                    onPillSegmentsPlaced?.invoke(resolvedPillSegments)
                }
                for (i in placeables.indices) {
                    if (!placement.isActive[i]) continue
                    val placeable = placeables[i]
                    val y = verticalAlignment.align(placeable.height, rowHeight)
                    placeable.placeRelative(placement.mainPositions[i], y)
                }
            }
        }
    }
}

/**
 * Window-space anchor of a cutout-aware container, capturing horizontal bounds and top offset so
 * vertical height animations do not invalidate cutout coordinate resolution.
 *
 * @property left physical left edge of the container in window coordinates (pixels).
 * @property top physical top edge of the container in window coordinates (pixels).
 * @property width physical width of the container in window coordinates (pixels).
 * @property right physical right edge of the container in window coordinates (pixels).
 * @property bottom physical bottom edge of the container in window coordinates (pixels).
 */
@Immutable
internal data class WindowAnchor(
    val left: Int,
    val top: Int,
    val width: Int,
    val right: Int = left + width,
    val bottom: Int = top
)

/**
 * Computes the [WindowAnchor] for these [LayoutCoordinates] in window pixel coordinates.
 */
internal fun LayoutCoordinates.toWindowAnchor(): WindowAnchor {
    val bounds = boundsInWindow()
    val left = bounds.left.roundToInt()
    val top = bounds.top.roundToInt()
    val right = bounds.right.roundToInt()
    val bottom = bounds.bottom.roundToInt()
    return WindowAnchor(
        left = left,
        top = top,
        width = right - left,
        right = right,
        bottom = bottom
    )
}

/**
 * Resolves display cutout bounding rectangles into a container's start-relative local coordinate
 * space, using [cutoutRectsOverride] when provided or offsetting [windowCutoutRects] by
 * [windowAnchor], and mirroring horizontal coordinates in RTL while keeping vertical coordinates
 * top-to-bottom.
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

/**
 * Placement result produced by [calculateCutoutAxisPlacement] along a cutout-aware container's
 * primary axis.
 */
internal class CutoutAxisPlacement(
    val mainPositions: IntArray,
    val isActive: BooleanArray,
    val pillSegments: List<IntRect>,
    val contentMainSize: Int
)

/**
 * Draws rounded capsule segments behind a cutout-aware container when [pillBackgroundColor] is
 * specified.
 */
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

/**
 * Packs [placeables] into contiguous pill groups along a single axis while avoiding
 * [keepOutRanges] and maintaining lookahead-locked minimum positions ([lockedMinMain]) across
 * enter/exit animations.
 */
internal fun calculateCutoutAxisPlacement(
    measurables: List<Measurable>,
    placeables: List<Placeable>,
    spacingPx: Int,
    pillPadPx: Int,
    keepOutRanges: List<IntRange>,
    isLookingAhead: Boolean,
    lockedMinMain: MutableMap<Int, Int>,
    mainSize: (Placeable) -> Int,
    crossSize: (Placeable) -> Int,
    intrinsicMainSize: (Measurable) -> Int,
    createPillSegment: (groupStartPx: Int, groupEndPx: Int, maxCrossPx: Int) -> IntRect
): CutoutAxisPlacement {
    if (!isLookingAhead) {
        lockedMinMain.keys.removeAll { it >= placeables.size }
    }

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
                resolvedPillSegments.add(
                    createPillSegment(currentGroupStartPx, groupEndPx, currentGroupMaxCrossPx)
                )
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
        val itemMainSize = mainSize(placeable)
        // Query intrinsic main size so children wrapped in AnimatedVisibility (whose
        // ExpandShrinkModifier passes intrinsics through to the unclipped child) evaluate cutout
        // collisions against their full target size throughout both enter and exit animations,
        // including the initial 0-size frame of an enter transition.
        val collisionMainSize = max(itemMainSize, intrinsicMainSize(measurables[i]))
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
            currentGroupMaxCrossPx = max(currentGroupMaxCrossPx, crossSize(placeable))
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

private object CutoutRowScope : RowScope {
    override fun Modifier.weight(weight: Float, fill: Boolean): Modifier = this
    override fun Modifier.align(alignment: Alignment.Vertical): Modifier = this
    override fun Modifier.alignBy(alignmentLine: HorizontalAlignmentLine): Modifier = this
    override fun Modifier.alignByBaseline(): Modifier = this
    override fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int): Modifier = this
}
