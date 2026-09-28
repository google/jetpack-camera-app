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
 * The control row vocabulary of a conventional bottom-stacked camera UI.
 *
 * These ids are what [CameraLayoutSolution.rowBands] is keyed by. They are plain strings rather
 * than an enum so that an app with a different control anatomy can define its own rows without
 * forking this file.
 */
object CameraRowIds {
    /** Zoom ratio selector, sitting above the shutter. */
    const val ZOOM_BAR = "zoomBar"

    /** Shutter button flanked by the image well and the lens flip control. */
    const val CAPTURE_ROW = "captureRow"

    /** Photo / video mode carousel. */
    const val MODE_SWITCHER = "modeSwitcher"

    /** Quick settings and capture mode toggle. */
    const val BOTTOM_TOOLBAR = "bottomToolbar"
}

/** The viewfinder aspect ratios a conventional camera UI offers. */
object CameraViewfinderIds {
    const val RATIO_3_4 = "3:4"
    const val RATIO_1_1 = "1:1"
    const val RATIO_9_16 = "9:16"
}

/**
 * Default layout inputs matching the app's design specification.
 *
 * Every value here is a design decision rather than an arbitrary constant, so each is commented
 * with what it protects. Callers are expected to copy and adjust rather than mutate.
 */
object CameraLayoutDefaults {

    /** Shutter button plus the image well and flip control either side of it. */
    val CaptureRowHeight = 86.dp

    /** Mode carousel. Reserved even in video mode, where the carousel is hidden. */
    val ModeSwitcherHeight = 32.dp

    /** Quick settings row. Equal to the toggle switch's own height, so it has no internal slack. */
    val BottomToolbarHeight = 64.dp

    /** Zoom ratio selector. */
    val ZoomBarHeight = 48.dp

    /** Standard gap between adjacent control rows. */
    val StandardGap = 24.dp

    /**
     * Gap between the zoom bar and the shutter row.
     *
     * Wider than [StandardGap] because the zoom bar usually sits over the live preview while the
     * shutter row sits on black, and the extra space stops the two reading as one block.
     */
    val ZoomToCaptureGap = 32.dp

    /**
     * Gap to aim for between the zoom bar and the shutter row when no viewfinder border separates
     * them. Keeps the zoom bar grouped with the shutter instead of floating toward the centre of
     * the photo.
     */
    val ZoomToCaptureGroupedGap = 12.dp

    /** Floor for any inter-row gap under compression. */
    val MinGap = 8.dp

    /** Preferred padding below the bottom toolbar. */
    val BottomPadding = 30.dp

    /** Floor for the padding below the bottom toolbar. */
    val MinBottomPadding = 8.dp

    /** Required daylight between a viewfinder edge and any control row. */
    val MinControlClearance = 4.dp

    /**
     * How far the stack may be raised as a last resort before the layout is declared
     * infeasible.
     */
    val MaxStackLift = 72.dp

    // Relative cost of a viewfinder edge intruding on each row. The shutter is the control the user
    // reaches for without looking, so it is protected hardest; the mode switcher is the most
    // expendable because it is transient and self-explanatory.
    private const val CAPTURE_ROW_WEIGHT = 10f
    private const val BOTTOM_TOOLBAR_WEIGHT = 5f
    private const val ZOOM_BAR_WEIGHT = 3f
    private const val MODE_SWITCHER_WEIGHT = 2f

    /**
     * Control rows in solver order: bottom-up, nearest the navigation bar first.
     *
     * Each row's `gapAbove` is the space between it and the row above it, so the top-most row's
     * value is unused.
     *
     * Note that [ModeSwitcherHeight] is reserved unconditionally. The mode carousel is hidden in
     * video mode, but its slot is not released, so the shutter button stays exactly where the
     * user's thumb left it when they switch modes.
     */
    fun rows(
        bottomToolbarHeight: Dp = BottomToolbarHeight,
        modeSwitcherHeight: Dp = ModeSwitcherHeight,
        captureRowHeight: Dp = CaptureRowHeight,
        zoomBarHeight: Dp = ZoomBarHeight
    ): List<ControlRow> = listOf(
        ControlRow(
            id = CameraRowIds.BOTTOM_TOOLBAR,
            height = bottomToolbarHeight,
            collisionWeight = BOTTOM_TOOLBAR_WEIGHT,
            gapAbove = GapRange(MinGap, StandardGap)
        ),
        ControlRow(
            id = CameraRowIds.MODE_SWITCHER,
            height = modeSwitcherHeight,
            collisionWeight = MODE_SWITCHER_WEIGHT,
            gapAbove = GapRange(MinGap, StandardGap)
        ),
        ControlRow(
            id = CameraRowIds.CAPTURE_ROW,
            height = captureRowHeight,
            collisionWeight = CAPTURE_ROW_WEIGHT,
            gapAbove = GapRange(MinGap, ZoomToCaptureGap)
        ),
        ControlRow(
            id = CameraRowIds.ZOOM_BAR,
            height = zoomBarHeight,
            collisionWeight = ZOOM_BAR_WEIGHT,
            gapAbove = GapRange(MinGap, StandardGap)
        )
    )

    /**
     * The three offered aspect ratios.
     *
     * Order matters: `3:4` is solved first because `1:1` is positioned relative to it.
     */
    fun viewfinders(): List<ViewfinderSlot> = listOf(
        ViewfinderSlot(
            id = CameraViewfinderIds.RATIO_3_4,
            aspectRatio = 3f / 4f,
            policy = PlacementPolicy.TOP_ALIGNED_PREFER_OBSTACLE_EDGE
        ),
        ViewfinderSlot(
            id = CameraViewfinderIds.RATIO_1_1,
            aspectRatio = 1f,
            policy = PlacementPolicy.CENTERED_IN_REFERENCE,
            referenceId = CameraViewfinderIds.RATIO_3_4
        ),
        ViewfinderSlot(
            id = CameraViewfinderIds.RATIO_9_16,
            aspectRatio = 9f / 16f,
            policy = PlacementPolicy.BINARY_TOP_EDGE
        )
    )

    /**
     * The default specification.
     *
     * @param toolbarCompaction how to handle screens where the black bar below a full-width `9:16`
     *   frame is slightly too short for a full-height toolbar.
     */
    fun spec(
        toolbarCompaction: ToolbarCompaction = ToolbarCompaction.COMPACT_TOOLBAR,
        bottomToolbarHeight: Dp = BottomToolbarHeight,
        modeSwitcherHeight: Dp = ModeSwitcherHeight,
        captureRowHeight: Dp = CaptureRowHeight,
        zoomBarHeight: Dp = ZoomBarHeight,
        minInteractiveTouchTarget: Dp = 48.dp
    ): CameraLayoutSpec = CameraLayoutSpec(
        rows = rows(
            bottomToolbarHeight = bottomToolbarHeight,
            modeSwitcherHeight = modeSwitcherHeight,
            captureRowHeight = captureRowHeight,
            zoomBarHeight = zoomBarHeight
        ),
        viewfinders = viewfinders(),
        minControlClearance = MinControlClearance,
        minInteractiveTouchTarget = minInteractiveTouchTarget,
        bottomPadding = GapRange(MinBottomPadding, BottomPadding),
        maxStackLift = MaxStackLift,
        enforceTopWeighting = true,
        toolbarCompaction = toolbarCompaction,
        groupingRule = GroupingRule(
            rowId = CameraRowIds.CAPTURE_ROW,
            tightenedGapAbove = ZoomToCaptureGroupedGap
        )
    )
}
