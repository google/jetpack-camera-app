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
package com.google.jetpackcamera.ui.components.capture.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A closed vertical interval measured downward from the top of the device window.
 *
 * Used for both control rows and viewfinder frames so the solver can reason about them uniformly.
 */
data class DpRange(val top: Dp, val bottom: Dp) {
    val height: Dp get() = bottom - top

    init {
        require(bottom >= top) { "bottom ($bottom) must be >= top ($top)" }
    }
}

/**
 * The device window geometry the layout is solved against.
 *
 * All values are in density-independent pixels and describe the *window*, not the physical display,
 * so multi-window and split-screen configurations are handled by construction.
 *
 * @param width window width.
 * @param height window height.
 * @param topInset status bar or display cutout depth, whichever is deeper.
 * @param navInset navigation bar height, reported even when the bar is currently hidden.
 * @param gestureInset bottom system gesture zone depth.
 * @param cornerRadius hardware display corner radius, used to keep the preview corners concentric
 *   with the bezel. Does not affect vertical placement.
 */
data class CameraWindow(
    val width: Dp,
    val height: Dp,
    val topInset: Dp,
    val navInset: Dp,
    val gestureInset: Dp = 24.dp,
    val cornerRadius: Dp = 0.dp
) {
    init {
        require(width > 0.dp && height > 0.dp) { "window must have positive size" }
    }
}

/** An inter-row gap the solver is allowed to compress between [min] and [preferred]. */
data class GapRange(val min: Dp, val preferred: Dp) {
    init {
        require(min <= preferred) { "min ($min) must be <= preferred ($preferred)" }
    }
}

/**
 * A control row that a viewfinder edge must not intersect.
 *
 * The solver treats rows as opaque obstacles identified only by [id]; it has no knowledge of what
 * any particular row contains. Rows are supplied bottom-up, nearest the navigation bar first.
 *
 * @param id stable identifier, echoed back in [CameraLayoutSolution.rowBands] and [Collision].
 * @param height the *reserved* height of the row. A row whose content is conditionally hidden must
 *   still report its full height, so that showing or hiding that content never moves the rows
 *   around it. Content visibility is deliberately not an input to the solver: routing it through
 *   here is how a shutter button ends up shifting under the user's thumb when they switch modes.
 *   To remove a row entirely, omit it from [CameraLayoutSpec.rows].
 * @param collisionWeight relative cost of intruding on this row. Higher means the solver will
 *   sacrifice more elsewhere to keep this row clear. The shutter row is the most protected control
 *   on the screen and should carry the highest weight.
 * @param gapAbove spacing between this row and the row above it. The solver may compress this
 *   toward [GapRange.min] to resolve a collision. Unused on the top-most row.
 */
data class ControlRow(
    val id: String,
    val height: Dp,
    val collisionWeight: Float = 1f,
    val gapAbove: GapRange = GapRange(8.dp, 24.dp)
) {
    init {
        require(id.isNotBlank()) { "row id must not be blank" }
        require(height >= 0.dp) { "row height must not be negative" }
        require(collisionWeight > 0f) { "collisionWeight must be positive" }
    }
}

/**
 * How a viewfinder of a given aspect ratio prefers to be positioned before collision resolution.
 *
 * These policies are deliberately not generic. Each aspect ratio in a camera UI has a different
 * natural resting place, and flattening them into one rule produces layouts that are technically
 * collision-free but visually wrong.
 */
enum class PlacementPolicy {
    /**
     * Sit flush below the top bar. If the frame collides and a taller obstacle sits below it,
     * prefer dropping to that obstacle's lower edge rather than shifting an arbitrary distance.
     *
     * This is the `3:4` behaviour.
     */
    TOP_ALIGNED_PREFER_OBSTACLE_EDGE,

    /**
     * Centre within the frame area of a reference ratio, then shift to the nearest clean gap.
     * Pinning a square preview to the top bar leaves a lopsided black expanse below it.
     *
     * This is the `1:1` behaviour.
     */
    CENTERED_IN_REFERENCE,

    /**
     * Either sit flush below the top bar, or expand all the way to the top of the window. Partial
     * overlap with the top bar is never used, because a frame that starts part-way into the top bar
     * reads as a mistake rather than a decision.
     *
     * This is the `9:16` behaviour.
     */
    BINARY_TOP_EDGE
}

/**
 * One viewfinder frame the solver must find a home for.
 *
 * @param id stable identifier, echoed back in [CameraLayoutSolution.viewfinders].
 * @param aspectRatio width divided by height, so `3:4` is `0.75` and `9:16` is `0.5625`.
 * @param policy how this frame prefers to be positioned before collisions are resolved.
 * @param referenceId for [PlacementPolicy.CENTERED_IN_REFERENCE], the frame to centre within.
 */
data class ViewfinderSlot(
    val id: String,
    val aspectRatio: Float,
    val policy: PlacementPolicy,
    val referenceId: String? = null
) {
    init {
        require(aspectRatio > 0f) { "aspectRatio must be positive" }
        require(policy != PlacementPolicy.CENTERED_IN_REFERENCE || referenceId != null) {
            "CENTERED_IN_REFERENCE requires a referenceId"
        }
    }
}

/**
 * How to resolve the case where the black bar below a full-width tall frame is slightly too short
 * to hold a full-height bottom toolbar.
 */
enum class ToolbarCompaction {
    /** Raise the control stack so the full-size toolbar steps inside the viewfinder. */
    LIFT_INTO_VIEWFINDER,

    /**
     * Scale the toolbar's drawn height down so it fits in the black bar below the viewfinder.
     * Touch targets are unaffected; callers are expected to keep a minimum interactive size.
     */
    COMPACT_TOOLBAR
}

/**
 * Pulls a row closer to the row above it when no viewfinder border runs between them.
 *
 * On mid-height screens a frame edge often lands below the identified row, putting that row inside
 * the preview. With no border separating it from the row above, the designed gap reads as a gap
 * between two unrelated things floating over the photo. Tightening it keeps them reading as one
 * group. When a border *does* pass between them the designed gap is kept, because the border
 * already provides the visual separation.
 *
 * @param rowId the row whose gap above may be tightened.
 * @param tightenedGapAbove the gap to aim for when the rule applies.
 */
data class GroupingRule(val rowId: String, val tightenedGapAbove: Dp)

/**
 * The complete input to a layout solve.
 *
 * @param rows control rows, bottom-up.
 * @param viewfinders the frames that must all be placed against the same stationary [rows].
 * @param minControlClearance required daylight between any viewfinder edge and any control row.
 * @param bottomPadding padding below the bottom-most row, between the two bounds given.
 * @param maxStackLift how far the solver may raise the whole stack as a last resort.
 * @param enforceTopWeighting cap a downward shift so the black band above a frame never exceeds the
 *   band below it.
 * @param toolbarCompaction strategy for the mid-height toolbar case.
 * @param groupingRule optional visual grouping adjustment applied while ranking repair candidates.
 */
data class CameraLayoutSpec(
    val rows: List<ControlRow>,
    val viewfinders: List<ViewfinderSlot>,
    val minControlClearance: Dp = 4.dp,
    val bottomPadding: GapRange = GapRange(8.dp, 30.dp),
    val maxStackLift: Dp = 72.dp,
    val enforceTopWeighting: Boolean = true,
    val toolbarCompaction: ToolbarCompaction = ToolbarCompaction.COMPACT_TOOLBAR,
    val groupingRule: GroupingRule? = null
) {
    init {
        require(rows.isNotEmpty()) { "at least one control row is required" }
        require(rows.distinctBy { it.id }.size == rows.size) { "row ids must be unique" }
        require(viewfinders.isNotEmpty()) { "at least one viewfinder slot is required" }
        require(viewfinders.distinctBy { it.id }.size == viewfinders.size) {
            "viewfinder ids must be unique"
        }
    }
}

/** A viewfinder edge that intrudes on a control row, or on its keep-out margin. */
data class Collision(val viewfinderId: String, val rowId: String)

/**
 * The solved layout.
 *
 * [rowBands] is shared by every entry in [viewfinders]: the whole point of the solve is that the
 * control stack does not move when the user switches aspect ratio.
 *
 * @param rowBands solved vertical band per control row id.
 * @param viewfinders solved vertical band per viewfinder id.
 * @param requiresImmersive whether the system bars must be hidden for this window. Decided once per
 *   window and applied to every aspect ratio, so switching ratios never resizes the window.
 * @param toolbarHeight drawn height of the bottom-most row after any compaction.
 * @param resolvedBottomPadding padding the solver settled on below the bottom-most row.
 * @param resolvedGaps inter-row gaps the solver settled on, bottom-up.
 * @param minClearance smallest daylight between any viewfinder edge and any control row.
 * @param collisions empty when the solve succeeded. Non-empty means no collision-free layout exists
 *   for this geometry and the best available was returned instead.
 * @param liftedStack whether the stack had to be raised above its preferred bottom padding.
 */
data class CameraLayoutSolution(
    val rowBands: Map<String, DpRange>,
    val viewfinders: Map<String, DpRange>,
    val requiresImmersive: Boolean,
    val toolbarHeight: Dp,
    val resolvedBottomPadding: Dp,
    val resolvedGaps: List<Dp>,
    val minClearance: Dp,
    val collisions: List<Collision>,
    val liftedStack: Boolean
) {
    /** True when every viewfinder edge clears every control row by the required margin. */
    val isCollisionFree: Boolean get() = collisions.isEmpty()
}
