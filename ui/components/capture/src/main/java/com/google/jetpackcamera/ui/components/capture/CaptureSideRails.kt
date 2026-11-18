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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.unit.isSpecified
import kotlin.math.roundToInt

/**
 * Minimum intrinsic rail width shared by [StartSideRail] and [EndSideRail] before adding outer
 * posture horizontal padding.
 */
internal val MinSideRailWidth = 72.dp

private object Tokens {
    val HorizontalPadding = 8.dp
    val StandardVerticalPadding = 16.dp
    val CompactVerticalPadding = 8.dp
    val StandardMinSectionGap = 12.dp
    val CompactMinSectionGap = 8.dp

    val StandardShutterClusterGap = 48.dp
    val CompactShutterClusterGap = 20.dp
    val MinShutterClusterGap = 8.dp
    val MinSecondaryControlHeight = 48.dp

    val PreviewStandardRailHeight = 800.dp
    val PreviewTabletPortraitRailHeight = 1280.dp
    val PreviewCompactRailHeight = 360.dp
    val PreviewTabletPortraitTopPadding = 32.dp
    val PreviewTabletPortraitBottomPadding = 48.dp
    val PreviewTabletPortraitHorizontalPadding = 24.dp
    val PreviewTabletPortraitMinSectionGap = 16.dp
    const val PREVIEW_TABLET_PORTRAIT_CENTER_FRACTION = 0.70f
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
    ImageWell
}

/**
 * Start-aligned vertical control rail for landscape, foldable, and tablet capture layouts.
 *
 * Tagged with [START_SIDE_RAIL_TAG] and provides a vertical [PreviewOverlaySlotScope] (propagating
 * [Orientation.Vertical] and [controlSize] to [CaptureSlotScope]) to its children. Stacks
 * optional [CaptureSlots.topBarLeadingContent] directly above [CaptureSlots.topBar] in the top
 * section (callers own any vertical spacing between those two slots), [CaptureSlots.zoomControls]
 * in the center (at [centerVerticalFraction]), and [CaptureSlots.quickSettingsButton] at the bottom
 * in strict top-to-bottom order across both LTR and RTL locales while preserving the caller's
 * `LocalLayoutDirection` inside each child slot.
 *
 * @param slots [CaptureSlots] supplying the indicator, zoom, and Quick Settings controls.
 * @param modifier [Modifier] applied to the rail container. When given a bounded `maxHeight`
 *   (such as `.fillMaxHeight()` or `.height(...)`), the rail fills that height; when height is
 *   unbounded, the rail wraps the intrinsic heights and minimum gaps of its active sections.
 * @param isCompactHeight Whether compact vertical padding, section gaps, and slot sizing hints are
 *   enabled for short windows.
 * @param controlSize Control size variant requested for the rail's controls; defaults to
 *   [ControlSize.Mini] when [isCompactHeight] is `true` and [ControlSize.Standard] otherwise.
 * @param topPadding Top inset inside the rail before the top indicator section.
 * @param bottomPadding Bottom inset inside the rail below the Quick Settings button.
 * @param horizontalPadding Horizontal padding applied on both sides of the rail content.
 * @param railWidth Fixed rail width; when specified, controls are centered on the rail
 *   regardless of their measured width instead of the rail hugging its widest control.
 * @param minSectionGap Minimum collision-free vertical gap enforced between adjacent sections.
 * @param centerVerticalFraction Target vertical midpoint fraction (`0f..1f`) for the center zoom
 *   section (`0.5f` for vertically centered rails; `0.70f` for tall portrait tablet thumb zones),
 *   clamped within the remaining collision-free range between top and bottom sections.
 */
@Composable
internal fun StartSideRail(
    slots: CaptureSlots,
    modifier: Modifier = Modifier,
    isCompactHeight: Boolean = false,
    controlSize: ControlSize = if (isCompactHeight) ControlSize.Mini else ControlSize.Standard,
    topPadding: Dp = if (isCompactHeight) {
        Tokens.CompactVerticalPadding
    } else {
        Tokens.StandardVerticalPadding
    },
    bottomPadding: Dp = if (isCompactHeight) {
        Tokens.CompactVerticalPadding
    } else {
        Tokens.StandardVerticalPadding
    },
    horizontalPadding: Dp = Tokens.HorizontalPadding,
    railWidth: Dp = Dp.Unspecified,
    minSectionGap: Dp = if (isCompactHeight) {
        Tokens.CompactMinSectionGap
    } else {
        Tokens.StandardMinSectionGap
    },
    centerVerticalFraction: Float = 0.5f
) {
    VerticalRailLayout(
        modifier = modifier.testTag(START_SIDE_RAIL_TAG),
        topPadding = topPadding,
        bottomPadding = bottomPadding,
        horizontalPadding = horizontalPadding,
        railWidth = railWidth,
        minSectionGap = minSectionGap,
        centerVerticalFraction = centerVerticalFraction,
        topContent = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val leadingContent = slots.topBarLeadingContent
                if (leadingContent != null) {
                    OverlapAwareCaptureSlot(
                        orientation = Orientation.Vertical,
                        controlSize = controlSize,
                        content = leadingContent
                    )
                }
                OverlapAwareCaptureSlot(
                    orientation = Orientation.Vertical,
                    controlSize = controlSize,
                    content = slots.topBar
                )
            }
        },
        centerContent = {
            OverlapAwareCaptureSlot(
                orientation = Orientation.Vertical,
                controlSize = controlSize,
                content = slots.zoomControls
            )
        },
        bottomContent = {
            OverlapAwareCaptureSlot(
                orientation = Orientation.Vertical,
                controlSize = controlSize,
                content = slots.quickSettingsButton
            )
        }
    )
}

/**
 * End-aligned vertical shutter and mode-selection rail for landscape, foldable, and tablet capture
 * layouts.
 *
 * Tagged with [END_SIDE_RAIL_TAG] and provides a vertical [PreviewOverlaySlotScope] (propagating
 * [Orientation.Vertical] and [controlSize] to [CaptureSlotScope]) to its children. Places
 * [CaptureSlots.elapsedTimeDisplay] at the top when [showElapsedTimeInRail] is `true`, renders a
 * collision-free vertical cluster of `flipCameraButton -> captureButton -> imageWell` around
 * [centerVerticalFraction], and anchors [CaptureSlots.captureModeToggle] at the bottom in strict
 * top-to-bottom order across both LTR and RTL locales.
 *
 * @param slots [CaptureSlots] supplying the shutter cluster, elapsed timer, and mode toggle.
 * @param modifier [Modifier] applied to the rail container. When given a bounded `maxHeight`
 *   (such as `.fillMaxHeight()` or `.height(...)`), the rail fills that height; when height is
 *   unbounded, the rail wraps the intrinsic heights and minimum gaps of its active sections.
 * @param isCompactHeight Whether compact vertical spacing and compact slot hints are active.
 * @param controlSize Control size variant requested for the rail's controls; defaults to
 *   [ControlSize.Mini] when [isCompactHeight] is `true` and [ControlSize.Standard] otherwise.
 * @param showElapsedTimeInRail Whether [CaptureSlots.elapsedTimeDisplay] is hosted in the top slot
 *   of this rail.
 * @param topPadding Top inset inside the rail before the optional top timer section.
 * @param bottomPadding Bottom inset inside the rail below the capture mode toggle.
 * @param horizontalPadding Horizontal padding applied on both sides of the rail content.
 * @param railWidth Fixed rail width; when specified, controls are centered on the rail
 *   regardless of their measured width instead of the rail hugging its widest control.
 * @param minSectionGap Minimum collision-free vertical gap enforced between adjacent sections.
 * @param centerVerticalFraction Target vertical midpoint fraction (`0f..1f`) for the shutter
 *   cluster (`0.5f` for vertically centered rails; `0.70f` for tall portrait tablet thumb zones),
 *   clamped within the remaining collision-free range between top and bottom sections.
 */
@Composable
internal fun EndSideRail(
    slots: CaptureSlots,
    modifier: Modifier = Modifier,
    isCompactHeight: Boolean = false,
    controlSize: ControlSize = if (isCompactHeight) ControlSize.Mini else ControlSize.Standard,
    showElapsedTimeInRail: Boolean = false,
    topPadding: Dp = if (isCompactHeight) {
        Tokens.CompactVerticalPadding
    } else {
        Tokens.StandardVerticalPadding
    },
    bottomPadding: Dp = if (isCompactHeight) {
        Tokens.CompactVerticalPadding
    } else {
        Tokens.StandardVerticalPadding
    },
    horizontalPadding: Dp = Tokens.HorizontalPadding,
    railWidth: Dp = Dp.Unspecified,
    minSectionGap: Dp = if (isCompactHeight) {
        Tokens.CompactMinSectionGap
    } else {
        Tokens.StandardMinSectionGap
    },
    centerVerticalFraction: Float = 0.5f
) {
    VerticalRailLayout(
        modifier = modifier.testTag(END_SIDE_RAIL_TAG),
        topPadding = topPadding,
        bottomPadding = bottomPadding,
        horizontalPadding = horizontalPadding,
        railWidth = railWidth,
        minSectionGap = minSectionGap,
        centerVerticalFraction = centerVerticalFraction,
        topContent = {
            if (showElapsedTimeInRail) {
                OverlapAwareCaptureSlot(
                    orientation = Orientation.Vertical,
                    controlSize = controlSize,
                    content = slots.elapsedTimeDisplay
                )
            }
        },
        centerContent = {
            VerticalRailShutterCluster(
                slots = slots,
                isCompactHeight = isCompactHeight,
                controlSize = controlSize
            )
        },
        bottomContent = {
            OverlapAwareCaptureSlot(
                orientation = Orientation.Vertical,
                controlSize = controlSize,
                content = slots.captureModeToggle
            )
        }
    )
}

/**
 * Collision-free 3-band vertical rail layout (Top, Center, Bottom).
 *
 * When `constraints.hasBoundedHeight` is `true`, the rail fills `constraints.maxHeight`, measures
 * Top and Bottom first, constrains Center's `maxHeight` to the remaining vertical budget minus
 * [minSectionGap] buffers, and positions Center around [centerVerticalFraction] clamped to
 * `[minCenterY, maxCenterY]`. When height is unbounded, the rail wraps the intrinsic heights of
 * active sections plus [minSectionGap] and vertical padding. Places all sections using explicit
 * `place(x, y)` coordinates so RTL layout direction never alters vertical ordering while child
 * composables retain the ambient `LocalLayoutDirection`.
 */
@Composable
private fun VerticalRailLayout(
    modifier: Modifier = Modifier,
    topPadding: Dp,
    bottomPadding: Dp,
    horizontalPadding: Dp,
    railWidth: Dp,
    minSectionGap: Dp,
    centerVerticalFraction: Float,
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
        val topPadPx = topPadding.roundToPx()
        val bottomPadPx = bottomPadding.roundToPx()
        val horizontalPadPx = horizontalPadding.roundToPx()
        val gapPx = minSectionGap.roundToPx()
        val clampedFraction = centerVerticalFraction.coerceIn(0f, 1f)

        val fixedRailWidthPx = if (railWidth.isSpecified) railWidth.roundToPx() else null
        // A fixed-width rail centers its controls on a stable column regardless of their measured
        // footprint, so controls are measured without a width cap and may overflow symmetrically.
        val innerMaxWidth = if (fixedRailWidthPx == null && constraints.hasBoundedWidth) {
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
            val innerHeight = (railHeight - topPadPx - bottomPadPx).coerceAtLeast(0)

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
                topPadPx +
                bottomPadPx
            railHeight = intrinsicHeight.coerceAtLeast(constraints.minHeight)
        }

        val maxContentWidth = maxOf(
            topPlaceable?.width ?: 0,
            centerPlaceable?.width ?: 0,
            bottomPlaceable?.width ?: 0
        )
        val railWidthPx = (
            fixedRailWidthPx ?: maxOf(
                MinSideRailWidth.roundToPx(),
                maxContentWidth + horizontalPadPx * 2
            )
            ).coerceIn(constraints.minWidth, constraints.maxWidth)

        val topY = topPadPx
        val minBottomY = topPlaceable?.let { topY + it.height + gapPx } ?: topPadPx
        val bottomY = bottomPlaceable?.let { bottom ->
            (railHeight - bottomPadPx - bottom.height).coerceAtLeast(minBottomY)
        } ?: (railHeight - bottomPadPx)

        val minCenterY = topPlaceable?.let { topY + it.height + gapPx } ?: topPadPx
        val maxCenterY = centerPlaceable?.let { center ->
            if (bottomPlaceable != null) {
                bottomY - gapPx - center.height
            } else {
                railHeight - bottomPadPx - center.height
            }
        } ?: minCenterY

        layout(railWidthPx, railHeight) {
            topPlaceable?.let { placeable ->
                placeable.place(
                    x = (railWidthPx - placeable.width) / 2,
                    y = topY
                )
            }

            centerPlaceable?.let { placeable ->
                val idealCenterY =
                    (railHeight * clampedFraction - placeable.height / 2f).roundToInt()
                val centerY = if (minCenterY <= maxCenterY) {
                    idealCenterY.coerceIn(minCenterY, maxCenterY)
                } else {
                    minCenterY
                }
                placeable.place(
                    x = (railWidthPx - placeable.width) / 2,
                    y = centerY
                )
            }

            bottomPlaceable?.let { placeable ->
                placeable.place(
                    x = (railWidthPx - placeable.width) / 2,
                    y = bottomY
                )
            }
        }
    }
}

/**
 * Vertical shutter cluster for [EndSideRail], stacking
 * `flipCameraButton -> captureButton -> imageWell` strictly top-to-bottom.
 *
 * Collapses `0x0` omitted slots without adding extra gaps and compresses the inter-control gap
 * proportionally down to [Tokens.MinShutterClusterGap] (`8.dp`) when constrained by short window
 * heights, omitting `imageWell` and then `flipCameraButton` if remaining height falls below
 * [Tokens.MinSecondaryControlHeight] (`48.dp`). Caps `captureButton`'s width contribution to
 * `clusterWidth` at [CaptureButtonDefaults.diameterFor].
 */
@Composable
private fun VerticalRailShutterCluster(
    slots: CaptureSlots,
    isCompactHeight: Boolean,
    controlSize: ControlSize,
    modifier: Modifier = Modifier
) {
    Layout(
        modifier = modifier,
        content = {
            OverlapAwareCaptureSlot(
                modifier = Modifier.layoutId(ShutterClusterSlotId.FlipCamera),
                orientation = Orientation.Vertical,
                controlSize = controlSize,
                content = slots.flipCameraButton
            )
            OverlapAwareCaptureSlot(
                modifier = Modifier.layoutId(ShutterClusterSlotId.Capture),
                orientation = Orientation.Vertical,
                controlSize = controlSize,
                content = slots.captureButton
            )
            OverlapAwareCaptureSlot(
                modifier = Modifier.layoutId(ShutterClusterSlotId.ImageWell),
                orientation = Orientation.Vertical,
                controlSize = controlSize,
                content = slots.imageWell
            )
        }
    ) { measurables, constraints ->
        val targetGapPx = if (isCompactHeight) {
            Tokens.CompactShutterClusterGap
        } else {
            Tokens.StandardShutterClusterGap
        }.roundToPx()
        val minGapPx = Tokens.MinShutterClusterGap.roundToPx()
        val minSecondaryHeightPx = Tokens.MinSecondaryControlHeight.roundToPx()
        val nominalCaptureDiameterPx = CaptureButtonDefaults.diameterFor(controlSize).roundToPx()

        val capturePlaceable: Placeable?
        val activePlaceables = if (constraints.hasBoundedHeight) {
            var remainingHeight = constraints.maxHeight
            var placedCount = 0
            fun measureSlot(id: ShutterClusterSlotId, isPrimary: Boolean): Placeable? {
                val measurable = measurables.firstOrNull { it.layoutId == id } ?: return null
                val reservedGapPx = if (placedCount == 0) 0 else minGapPx
                val maxSlotHeight = (remainingHeight - reservedGapPx).coerceAtLeast(0)
                if (!isPrimary && maxSlotHeight < minSecondaryHeightPx) {
                    return null
                }
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = 0,
                        maxWidth = constraints.maxWidth,
                        minHeight = 0,
                        maxHeight = maxSlotHeight
                    )
                )
                if (placeable.height > 0) {
                    remainingHeight =
                        (remainingHeight - placeable.height - reservedGapPx).coerceAtLeast(0)
                    placedCount++
                    return placeable
                }
                return null
            }

            // Measure primary shutter first so it retains priority in extreme micro-heights, then
            // assemble in strict Flip -> Capture -> ImageWell visual order.
            capturePlaceable = measureSlot(ShutterClusterSlotId.Capture, isPrimary = true)
            val flipPlaceable = measureSlot(ShutterClusterSlotId.FlipCamera, isPrimary = false)
            val imageWellPlaceable = measureSlot(ShutterClusterSlotId.ImageWell, isPrimary = false)
            listOfNotNull(
                flipPlaceable,
                capturePlaceable,
                imageWellPlaceable
            )
        } else {
            val looseConstraints = Constraints(
                minWidth = 0,
                maxWidth = constraints.maxWidth,
                minHeight = 0,
                maxHeight = Constraints.Infinity
            )
            val flipPlaceable = measurables.firstOrNull {
                it.layoutId == ShutterClusterSlotId.FlipCamera
            }?.measure(looseConstraints)?.takeIf { it.height > 0 }
            capturePlaceable = measurables.firstOrNull {
                it.layoutId == ShutterClusterSlotId.Capture
            }?.measure(looseConstraints)?.takeIf { it.height > 0 }
            val imageWellPlaceable = measurables.firstOrNull {
                it.layoutId == ShutterClusterSlotId.ImageWell
            }?.measure(looseConstraints)?.takeIf { it.height > 0 }
            listOfNotNull(
                flipPlaceable,
                capturePlaceable,
                imageWellPlaceable
            )
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

        // Cap the capture button's width contribution at its nominal ring diameter so its
        // invisible 1.375x drag-to-lock track overflows symmetrically without widening the rail.
        val clusterWidth = (
            activePlaceables.maxOfOrNull { placeable ->
                if (placeable === capturePlaceable) {
                    minOf(placeable.width, nominalCaptureDiameterPx)
                } else {
                    placeable.width
                }
            } ?: 0
            ).coerceIn(constraints.minWidth, constraints.maxWidth)
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
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(modifier = Modifier.background(Tokens.PreviewBackgroundColor)) {
            StartSideRail(
                slots = PlaceholderCaptureSlots.sideRail(isCompactHeight = false),
                modifier = Modifier.height(Tokens.PreviewStandardRailHeight)
            )
        }
    }
}

@Preview(name = "StartSideRail - Standard (800dp) RTL", locale = "ar", showBackground = true)
@Composable
private fun StartSideRailStandardRtlPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(modifier = Modifier.background(Tokens.PreviewBackgroundColor)) {
            StartSideRail(
                slots = PlaceholderCaptureSlots.sideRail(isCompactHeight = false),
                modifier = Modifier.height(Tokens.PreviewStandardRailHeight)
            )
        }
    }
}

@Preview(name = "StartSideRail - Tablet Portrait (1280dp, 0.70f)", showBackground = true)
@Composable
private fun StartSideRailTabletPortraitPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(modifier = Modifier.background(Tokens.PreviewBackgroundColor)) {
            StartSideRail(
                slots = PlaceholderCaptureSlots.sideRail(isCompactHeight = false),
                modifier = Modifier.height(Tokens.PreviewTabletPortraitRailHeight),
                topPadding = Tokens.PreviewTabletPortraitTopPadding,
                bottomPadding = Tokens.PreviewTabletPortraitBottomPadding,
                horizontalPadding = Tokens.PreviewTabletPortraitHorizontalPadding,
                minSectionGap = Tokens.PreviewTabletPortraitMinSectionGap,
                centerVerticalFraction = Tokens.PREVIEW_TABLET_PORTRAIT_CENTER_FRACTION
            )
        }
    }
}

@Preview(name = "StartSideRail - Compact (360dp) LTR", showBackground = true)
@Composable
private fun StartSideRailCompactLtrPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(modifier = Modifier.background(Tokens.PreviewBackgroundColor)) {
            StartSideRail(
                slots = PlaceholderCaptureSlots.sideRail(isCompactHeight = true),
                modifier = Modifier.height(Tokens.PreviewCompactRailHeight),
                isCompactHeight = true
            )
        }
    }
}

@Preview(name = "StartSideRail - Compact (360dp) RTL", locale = "ar", showBackground = true)
@Composable
private fun StartSideRailCompactRtlPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(modifier = Modifier.background(Tokens.PreviewBackgroundColor)) {
            StartSideRail(
                slots = PlaceholderCaptureSlots.sideRail(isCompactHeight = true),
                modifier = Modifier.height(Tokens.PreviewCompactRailHeight),
                isCompactHeight = true
            )
        }
    }
}

@Preview(name = "EndSideRail - Standard (800dp) LTR", showBackground = true)
@Composable
private fun EndSideRailStandardLtrPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(modifier = Modifier.background(Tokens.PreviewBackgroundColor)) {
            EndSideRail(
                slots = PlaceholderCaptureSlots.sideRail(isCompactHeight = false),
                modifier = Modifier.height(Tokens.PreviewStandardRailHeight)
            )
        }
    }
}

@Preview(name = "EndSideRail - Standard (800dp) RTL", locale = "ar", showBackground = true)
@Composable
private fun EndSideRailStandardRtlPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(modifier = Modifier.background(Tokens.PreviewBackgroundColor)) {
            EndSideRail(
                slots = PlaceholderCaptureSlots.sideRail(isCompactHeight = false),
                modifier = Modifier.height(Tokens.PreviewStandardRailHeight)
            )
        }
    }
}

@Preview(name = "EndSideRail - Tablet Portrait (1280dp, 0.70f)", showBackground = true)
@Composable
private fun EndSideRailTabletPortraitPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(modifier = Modifier.background(Tokens.PreviewBackgroundColor)) {
            EndSideRail(
                slots = PlaceholderCaptureSlots.sideRail(isCompactHeight = false),
                modifier = Modifier.height(Tokens.PreviewTabletPortraitRailHeight),
                topPadding = Tokens.PreviewTabletPortraitTopPadding,
                bottomPadding = Tokens.PreviewTabletPortraitBottomPadding,
                horizontalPadding = Tokens.PreviewTabletPortraitHorizontalPadding,
                minSectionGap = Tokens.PreviewTabletPortraitMinSectionGap,
                centerVerticalFraction = Tokens.PREVIEW_TABLET_PORTRAIT_CENTER_FRACTION
            )
        }
    }
}

@Preview(name = "EndSideRail - Compact (360dp) Recording LTR", showBackground = true)
@Composable
private fun EndSideRailCompactLtrPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(modifier = Modifier.background(Tokens.PreviewBackgroundColor)) {
            EndSideRail(
                slots = PlaceholderCaptureSlots.sideRail(
                    isCompactHeight = true,
                    includeImageWell = true,
                    includeElapsedTime = true
                ),
                modifier = Modifier.height(Tokens.PreviewCompactRailHeight),
                isCompactHeight = true,
                showElapsedTimeInRail = true
            )
        }
    }
}

@Preview(name = "EndSideRail - Compact (360dp) Recording RTL", locale = "ar", showBackground = true)
@Composable
private fun EndSideRailCompactRtlPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(modifier = Modifier.background(Tokens.PreviewBackgroundColor)) {
            EndSideRail(
                slots = PlaceholderCaptureSlots.sideRail(
                    isCompactHeight = true,
                    includeImageWell = true,
                    includeElapsedTime = true
                ),
                modifier = Modifier.height(Tokens.PreviewCompactRailHeight),
                isCompactHeight = true,
                showElapsedTimeInRail = true
            )
        }
    }
}
