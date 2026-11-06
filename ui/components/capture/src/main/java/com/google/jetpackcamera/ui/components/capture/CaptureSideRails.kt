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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private object Tokens {
    val MinRailWidth = 72.dp
    val HorizontalPadding = 8.dp
    val StandardVerticalPadding = 16.dp
    val CompactVerticalPadding = 8.dp
    val StandardMinSectionGap = 12.dp
    val CompactMinSectionGap = 8.dp

    val StandardShutterClusterGap = 48.dp
    val CompactShutterClusterGap = 20.dp

    val PreviewStandardRailHeight = 800.dp
    val PreviewCompactRailHeight = 360.dp
    val PreviewBackgroundColor = Color(0xFF121212)
}

private enum class RailSectionId {
    Top,
    Center,
    Bottom
}

private enum class ShutterClusterSlotId {
    FlipCamera,
    Capture,
    ImageWell,
    PauseToggle
}

/**
 * Start-aligned vertical control rail for landscape, tabletop, and large-screen capture layouts.
 *
 * Tagged with [START_SIDE_RAIL_TAG] and provides a vertical [CaptureSlotScope] to its children.
 * Stacks [CaptureSlots.indicatorColumn] at the top, [CaptureSlots.zoomControls] in the center, and
 * [CaptureSlots.quickSettingsButton] at the bottom in strict top-to-bottom order across both LTR
 * and RTL locales while preserving the caller's `LocalLayoutDirection` inside each child slot.
 *
 * @param slots [CaptureSlots] supplying the indicator, zoom, and Quick Settings controls.
 * @param modifier [Modifier] applied to the rail container.
 * @param isCompactHeight Whether compact vertical padding, section gaps, and slot sizing hints are
 *   enabled for short windows.
 * @param isRecording Whether an active video recording session is in progress.
 * @param onDismissQuickSettings Callback propagated to [CaptureSlotScope.dismissQuickSettings].
 */
@Composable
fun StartSideRail(
    slots: CaptureSlots,
    modifier: Modifier = Modifier,
    isCompactHeight: Boolean = false,
    isRecording: Boolean = false,
    onDismissQuickSettings: () -> Unit = {}
) {
    val verticalPadding = if (isCompactHeight) {
        Tokens.CompactVerticalPadding
    } else {
        Tokens.StandardVerticalPadding
    }
    val minSectionGap = if (isCompactHeight) {
        Tokens.CompactMinSectionGap
    } else {
        Tokens.StandardMinSectionGap
    }

    VerticalRailLayout(
        modifier = modifier.testTag(START_SIDE_RAIL_TAG),
        verticalPadding = verticalPadding,
        horizontalPadding = Tokens.HorizontalPadding,
        minSectionGap = minSectionGap,
        topContent = {
            OverlapAwareCaptureSlot(
                orientation = Orientation.Vertical,
                isCompact = isCompactHeight,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings,
                content = slots.indicatorColumn
            )
        },
        centerContent = {
            OverlapAwareCaptureSlot(
                orientation = Orientation.Vertical,
                isCompact = isCompactHeight,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings,
                content = slots.zoomControls
            )
        },
        bottomContent = {
            OverlapAwareCaptureSlot(
                orientation = Orientation.Vertical,
                isCompact = isCompactHeight,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings,
                content = slots.quickSettingsButton
            )
        }
    )
}

/**
 * End-aligned vertical shutter and mode-selection rail for landscape, tabletop, and large-screen
 * capture layouts.
 *
 * Tagged with [END_SIDE_RAIL_TAG] and provides a vertical [CaptureSlotScope] to its children.
 * Places [CaptureSlots.elapsedTimeDisplay] at the top when [showElapsedTimeInRail] is `true`,
 * renders either [CaptureSlots.shutterStack] or a collision-free vertical cluster of
 * `flipCameraButton -> captureButton -> imageWell -> pauseToggleButton` in the center, and anchors
 * [CaptureSlots.captureModeToggle] at the bottom in strict top-to-bottom order across both LTR and
 * RTL locales.
 *
 * @param slots [CaptureSlots] supplying the shutter cluster, elapsed timer, and mode toggle.
 * @param modifier [Modifier] applied to the rail container.
 * @param isCompactHeight Whether compact vertical spacing and compact slot hints are active.
 * @param isRecording Whether an active video recording session is in progress.
 * @param showElapsedTimeInRail Whether [CaptureSlots.elapsedTimeDisplay] is hosted in the top slot
 *   of this rail.
 * @param onDismissQuickSettings Callback propagated to [CaptureSlotScope.dismissQuickSettings].
 */
@Composable
fun EndSideRail(
    slots: CaptureSlots,
    modifier: Modifier = Modifier,
    isCompactHeight: Boolean = false,
    isRecording: Boolean = false,
    showElapsedTimeInRail: Boolean = false,
    onDismissQuickSettings: () -> Unit = {}
) {
    val verticalPadding = if (isCompactHeight) {
        Tokens.CompactVerticalPadding
    } else {
        Tokens.StandardVerticalPadding
    }
    val minSectionGap = if (isCompactHeight) {
        Tokens.CompactMinSectionGap
    } else {
        Tokens.StandardMinSectionGap
    }
    val customShutterStack = slots.shutterStack

    VerticalRailLayout(
        modifier = modifier.testTag(END_SIDE_RAIL_TAG),
        verticalPadding = verticalPadding,
        horizontalPadding = Tokens.HorizontalPadding,
        minSectionGap = minSectionGap,
        topContent = {
            if (showElapsedTimeInRail) {
                OverlapAwareCaptureSlot(
                    orientation = Orientation.Vertical,
                    isCompact = isCompactHeight,
                    isRecording = isRecording,
                    onDismissQuickSettings = onDismissQuickSettings,
                    content = slots.elapsedTimeDisplay
                )
            }
        },
        centerContent = {
            if (customShutterStack != null) {
                OverlapAwareCaptureSlot(
                    orientation = Orientation.Vertical,
                    isCompact = isCompactHeight,
                    isRecording = isRecording,
                    onDismissQuickSettings = onDismissQuickSettings
                ) { slotModifier ->
                    customShutterStack(slotModifier, isCompactHeight)
                }
            } else {
                VerticalRailShutterCluster(
                    slots = slots,
                    isCompactHeight = isCompactHeight,
                    isRecording = isRecording,
                    onDismissQuickSettings = onDismissQuickSettings
                )
            }
        },
        bottomContent = {
            OverlapAwareCaptureSlot(
                orientation = Orientation.Vertical,
                isCompact = isCompactHeight,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings,
                content = slots.captureModeToggle
            )
        }
    )
}

/**
 * Collision-free 3-band vertical rail layout (Top, Center, Bottom).
 *
 * Measures Top and Bottom first, constrains Center's `maxHeight` to the remaining vertical budget
 * minus [minSectionGap] buffers, and places all sections using explicit `place(x, y)` coordinates
 * so RTL layout direction never alters vertical ordering while child composables retain the
 * ambient `LocalLayoutDirection`.
 */
@Composable
private fun VerticalRailLayout(
    modifier: Modifier = Modifier,
    verticalPadding: Dp,
    horizontalPadding: Dp,
    minSectionGap: Dp,
    topContent: @Composable () -> Unit,
    centerContent: @Composable () -> Unit,
    bottomContent: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = {
            Box(modifier = Modifier.layoutId(RailSectionId.Top)) {
                topContent()
            }
            Box(modifier = Modifier.layoutId(RailSectionId.Center)) {
                centerContent()
            }
            Box(modifier = Modifier.layoutId(RailSectionId.Bottom)) {
                bottomContent()
            }
        }
    ) { measurables, constraints ->
        val verticalPadPx = verticalPadding.roundToPx()
        val horizontalPadPx = horizontalPadding.roundToPx()
        val gapPx = minSectionGap.roundToPx()

        val innerMaxWidth = if (constraints.hasBoundedWidth) {
            (constraints.maxWidth - horizontalPadPx * 2).coerceAtLeast(0)
        } else {
            Constraints.Infinity
        }

        val topMeasurable = measurables.firstOrNull { it.layoutId == RailSectionId.Top }
        val centerMeasurable = measurables.firstOrNull { it.layoutId == RailSectionId.Center }
        val bottomMeasurable = measurables.firstOrNull { it.layoutId == RailSectionId.Bottom }

        val topPlaceable: Placeable?
        val centerPlaceable: Placeable?
        val bottomPlaceable: Placeable?
        val railHeight: Int

        if (constraints.hasBoundedHeight) {
            railHeight = constraints.maxHeight
            val innerHeight = (railHeight - verticalPadPx * 2).coerceAtLeast(0)

            topPlaceable = topMeasurable?.measure(
                Constraints(
                    minWidth = 0,
                    maxWidth = innerMaxWidth,
                    minHeight = 0,
                    maxHeight = innerHeight
                )
            )?.takeIf { it.height > 0 }

            val topReserved = topPlaceable?.let { it.height + gapPx } ?: 0
            val remainingForBottom = (innerHeight - topReserved).coerceAtLeast(0)

            bottomPlaceable = bottomMeasurable?.measure(
                Constraints(
                    minWidth = 0,
                    maxWidth = innerMaxWidth,
                    minHeight = 0,
                    maxHeight = remainingForBottom
                )
            )?.takeIf { it.height > 0 }

            val bottomReserved = bottomPlaceable?.let { it.height + gapPx } ?: 0
            val availableCenterHeight = (remainingForBottom - bottomReserved).coerceAtLeast(0)

            centerPlaceable = centerMeasurable?.measure(
                Constraints(
                    minWidth = 0,
                    maxWidth = innerMaxWidth,
                    minHeight = 0,
                    maxHeight = availableCenterHeight
                )
            )?.takeIf { it.height > 0 }
        } else {
            val looseConstraints = Constraints(
                minWidth = 0,
                maxWidth = innerMaxWidth,
                minHeight = 0,
                maxHeight = Constraints.Infinity
            )
            topPlaceable = topMeasurable?.measure(looseConstraints)?.takeIf { it.height > 0 }
            bottomPlaceable = bottomMeasurable?.measure(looseConstraints)?.takeIf { it.height > 0 }
            centerPlaceable = centerMeasurable?.measure(looseConstraints)?.takeIf { it.height > 0 }

            val activeSections = listOfNotNull(topPlaceable, centerPlaceable, bottomPlaceable)
            val totalGapPx = (activeSections.size - 1).coerceAtLeast(0) * gapPx
            val intrinsicHeight = activeSections.sumOf { it.height } +
                totalGapPx +
                verticalPadPx * 2
            railHeight = intrinsicHeight.coerceAtLeast(constraints.minHeight)
        }

        val maxContentWidth = maxOf(
            topPlaceable?.width ?: 0,
            centerPlaceable?.width ?: 0,
            bottomPlaceable?.width ?: 0
        )
        val railWidth = maxOf(
            Tokens.MinRailWidth.roundToPx(),
            maxContentWidth + horizontalPadPx * 2
        ).coerceIn(constraints.minWidth, constraints.maxWidth)

        val topY = verticalPadPx
        val minBottomY = topPlaceable?.let { topY + it.height + gapPx } ?: verticalPadPx
        val bottomY = bottomPlaceable?.let { bottom ->
            (railHeight - verticalPadPx - bottom.height).coerceAtLeast(minBottomY)
        } ?: (railHeight - verticalPadPx)

        val minCenterY = topPlaceable?.let { topY + it.height + gapPx } ?: verticalPadPx
        val maxCenterY = centerPlaceable?.let { center ->
            if (bottomPlaceable != null) {
                bottomY - gapPx - center.height
            } else {
                railHeight - verticalPadPx - center.height
            }
        } ?: minCenterY

        layout(railWidth, railHeight) {
            topPlaceable?.let { placeable ->
                placeable.place(
                    x = (railWidth - placeable.width) / 2,
                    y = topY
                )
            }

            centerPlaceable?.let { placeable ->
                val idealCenterY = (railHeight - placeable.height) / 2
                val centerY = if (minCenterY <= maxCenterY) {
                    idealCenterY.coerceIn(minCenterY, maxCenterY)
                } else {
                    minCenterY
                }
                placeable.place(
                    x = (railWidth - placeable.width) / 2,
                    y = centerY
                )
            }

            bottomPlaceable?.let { placeable ->
                placeable.place(
                    x = (railWidth - placeable.width) / 2,
                    y = bottomY
                )
            }
        }
    }
}

/**
 * Default vertical shutter cluster for [EndSideRail], stacking
 * `flipCameraButton -> captureButton -> imageWell -> pauseToggleButton` strictly top-to-bottom.
 *
 * Collapses `0x0` omitted slots without adding extra gaps and compresses the inter-control gap
 * proportionally when constrained by short window heights.
 */
@Composable
private fun VerticalRailShutterCluster(
    slots: CaptureSlots,
    isCompactHeight: Boolean,
    isRecording: Boolean,
    onDismissQuickSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Layout(
        modifier = modifier,
        content = {
            OverlapAwareCaptureSlot(
                modifier = Modifier.layoutId(ShutterClusterSlotId.FlipCamera),
                orientation = Orientation.Vertical,
                isCompact = isCompactHeight,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings,
                content = slots.flipCameraButton
            )
            OverlapAwareCaptureSlot(
                modifier = Modifier.layoutId(ShutterClusterSlotId.Capture),
                orientation = Orientation.Vertical,
                isCompact = isCompactHeight,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings,
                content = slots.captureButton
            )
            OverlapAwareCaptureSlot(
                modifier = Modifier.layoutId(ShutterClusterSlotId.ImageWell),
                orientation = Orientation.Vertical,
                isCompact = isCompactHeight,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings,
                content = slots.imageWell
            )
            OverlapAwareCaptureSlot(
                modifier = Modifier.layoutId(ShutterClusterSlotId.PauseToggle),
                orientation = Orientation.Vertical,
                isCompact = isCompactHeight,
                isRecording = isRecording,
                onDismissQuickSettings = onDismissQuickSettings,
                content = slots.pauseToggleButton
            )
        }
    ) { measurables, constraints ->
        val targetGapPx = if (isCompactHeight) {
            Tokens.CompactShutterClusterGap
        } else {
            Tokens.StandardShutterClusterGap
        }.roundToPx()

        val activePlaceables = if (constraints.hasBoundedHeight) {
            var remainingHeight = constraints.maxHeight
            fun measureSlot(id: ShutterClusterSlotId): Placeable? {
                val measurable = measurables.firstOrNull { it.layoutId == id } ?: return null
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = 0,
                        maxWidth = constraints.maxWidth,
                        minHeight = 0,
                        maxHeight = remainingHeight
                    )
                )
                if (placeable.height > 0) {
                    remainingHeight = (remainingHeight - placeable.height).coerceAtLeast(0)
                    return placeable
                }
                return null
            }

            // Measure primary shutter first so it retains priority in extreme micro-heights, then
            // assemble in strict Flip -> Capture -> ImageWell -> Pause visual order.
            val capturePlaceable = measureSlot(ShutterClusterSlotId.Capture)
            val flipPlaceable = measureSlot(ShutterClusterSlotId.FlipCamera)
            val imageWellPlaceable = measureSlot(ShutterClusterSlotId.ImageWell)
            val pausePlaceable = measureSlot(ShutterClusterSlotId.PauseToggle)
            listOfNotNull(
                flipPlaceable,
                capturePlaceable,
                imageWellPlaceable,
                pausePlaceable
            )
        } else {
            val looseConstraints = Constraints(
                minWidth = 0,
                maxWidth = constraints.maxWidth,
                minHeight = 0,
                maxHeight = Constraints.Infinity
            )
            ShutterClusterSlotId.entries.mapNotNull { id ->
                measurables.firstOrNull { it.layoutId == id }
                    ?.measure(looseConstraints)
                    ?.takeIf { it.height > 0 }
            }
        }

        val gapCount = (activePlaceables.size - 1).coerceAtLeast(0)
        val totalChildrenHeight = activePlaceables.sumOf { it.height }
        val targetTotalGapPx = targetGapPx * gapCount

        val actualTotalGapPx = if (constraints.hasBoundedHeight && gapCount > 0) {
            val maxAllowedGapPx = (constraints.maxHeight - totalChildrenHeight).coerceAtLeast(0)
            minOf(targetTotalGapPx, maxAllowedGapPx)
        } else {
            targetTotalGapPx
        }

        val baseGapPx = if (gapCount > 0) actualTotalGapPx / gapCount else 0
        val extraGapPixels = if (gapCount > 0) actualTotalGapPx % gapCount else 0

        val clusterWidth = (activePlaceables.maxOfOrNull { it.width } ?: 0)
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val clusterHeight = (totalChildrenHeight + actualTotalGapPx)
            .coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(clusterWidth, clusterHeight) {
            var currentY = (clusterHeight - (totalChildrenHeight + actualTotalGapPx))
                .coerceAtLeast(0) / 2
            activePlaceables.forEachIndexed { index, placeable ->
                placeable.place(
                    x = (clusterWidth - placeable.width) / 2,
                    y = currentY
                )
                currentY += placeable.height
                if (index < gapCount) {
                    currentY += baseGapPx + if (index < extraGapPixels) 1 else 0
                }
            }
        }
    }
}

@Preview(name = "StartSideRail - Standard (800dp) LTR", showBackground = true)
@Composable
private fun StartSideRailStandardLtrPreview() {
    Box(modifier = Modifier.background(Tokens.PreviewBackgroundColor)) {
        StartSideRail(
            slots = PlaceholderCaptureSlots.sideRail(isCompactHeight = false),
            modifier = Modifier.height(Tokens.PreviewStandardRailHeight)
        )
    }
}

@Preview(name = "StartSideRail - Standard (800dp) RTL", locale = "ar", showBackground = true)
@Composable
private fun StartSideRailStandardRtlPreview() {
    Box(modifier = Modifier.background(Tokens.PreviewBackgroundColor)) {
        StartSideRail(
            slots = PlaceholderCaptureSlots.sideRail(isCompactHeight = false),
            modifier = Modifier.height(Tokens.PreviewStandardRailHeight)
        )
    }
}

@Preview(name = "StartSideRail - Compact (360dp) LTR", showBackground = true)
@Composable
private fun StartSideRailCompactLtrPreview() {
    Box(modifier = Modifier.background(Tokens.PreviewBackgroundColor)) {
        StartSideRail(
            slots = PlaceholderCaptureSlots.sideRail(isCompactHeight = true),
            modifier = Modifier.height(Tokens.PreviewCompactRailHeight),
            isCompactHeight = true
        )
    }
}

@Preview(name = "StartSideRail - Compact (360dp) RTL", locale = "ar", showBackground = true)
@Composable
private fun StartSideRailCompactRtlPreview() {
    Box(modifier = Modifier.background(Tokens.PreviewBackgroundColor)) {
        StartSideRail(
            slots = PlaceholderCaptureSlots.sideRail(isCompactHeight = true),
            modifier = Modifier.height(Tokens.PreviewCompactRailHeight),
            isCompactHeight = true
        )
    }
}

@Preview(name = "EndSideRail - Standard (800dp) LTR", showBackground = true)
@Composable
private fun EndSideRailStandardLtrPreview() {
    Box(modifier = Modifier.background(Tokens.PreviewBackgroundColor)) {
        EndSideRail(
            slots = PlaceholderCaptureSlots.sideRail(isCompactHeight = false),
            modifier = Modifier.height(Tokens.PreviewStandardRailHeight)
        )
    }
}

@Preview(name = "EndSideRail - Standard (800dp) RTL", locale = "ar", showBackground = true)
@Composable
private fun EndSideRailStandardRtlPreview() {
    Box(modifier = Modifier.background(Tokens.PreviewBackgroundColor)) {
        EndSideRail(
            slots = PlaceholderCaptureSlots.sideRail(isCompactHeight = false),
            modifier = Modifier.height(Tokens.PreviewStandardRailHeight)
        )
    }
}

@Preview(name = "EndSideRail - Compact (360dp) Recording LTR", showBackground = true)
@Composable
private fun EndSideRailCompactLtrPreview() {
    Box(modifier = Modifier.background(Tokens.PreviewBackgroundColor)) {
        EndSideRail(
            slots = PlaceholderCaptureSlots.sideRail(
                isCompactHeight = true,
                includeImageWell = true,
                includePauseSlot = true,
                includeElapsedTime = true
            ),
            modifier = Modifier.height(Tokens.PreviewCompactRailHeight),
            isCompactHeight = true,
            isRecording = true,
            showElapsedTimeInRail = true
        )
    }
}

@Preview(name = "EndSideRail - Compact (360dp) Recording RTL", locale = "ar", showBackground = true)
@Composable
private fun EndSideRailCompactRtlPreview() {
    Box(modifier = Modifier.background(Tokens.PreviewBackgroundColor)) {
        EndSideRail(
            slots = PlaceholderCaptureSlots.sideRail(
                isCompactHeight = true,
                includeImageWell = true,
                includePauseSlot = true,
                includeElapsedTime = true
            ),
            modifier = Modifier.height(Tokens.PreviewCompactRailHeight),
            isCompactHeight = true,
            isRecording = true,
            showElapsedTimeInRail = true
        )
    }
}
