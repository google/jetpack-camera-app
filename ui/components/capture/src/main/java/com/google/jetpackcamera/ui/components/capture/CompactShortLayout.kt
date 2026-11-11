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

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min

private object CompactShortTokens {
    val ZeroSize = 0.dp
    val CondensedChromePadding = 8.dp
    val CondensedMinSectionGap = 8.dp
    val RailTotalVerticalPadding = 16.dp
    val RailHorizontalPaddingTotal = 16.dp
    val MinRailSlotWidthFloor = 36.dp
    val MinCenterHudWidthFloor = 48.dp
    val QuickSettingsCardMaxWidth = 320.dp

    // Proportional height fractions of the usable inner rail height so controls scale down
    // harmoniously without clipping or overlap in condensed short windows (< 480dp on both axes,
    // such as 360x360dp or 320x340dp with visible multi-window status bars).
    const val RAIL_WIDTH_FRACTION = 0.22f
    const val INDICATOR_MAX_HEIGHT_FRACTION = 0.20f
    const val ZOOM_MAX_HEIGHT_FRACTION = 0.45f
    const val QUICK_SETTINGS_BUTTON_FRACTION = 0.16f
    const val SHUTTER_DIAMETER_FRACTION = 0.22f
    const val SECONDARY_ACTION_FRACTION = 0.15f
    const val MODE_TOGGLE_HEIGHT_FRACTION = 0.15f
    const val CENTER_HUD_HEIGHT_FRACTION = 0.12f
}

/**
 * Condensed-height side-rail capture layout for short compact windows (`widthDp < 480.dp` and
 * `heightDp < 480.dp`, covering both `COMPACT_SHORT` square multi-window panes / clamshell cover
 * displays such as `360x360dp` and `COMPACT_SHORT_SLIM` panes such as `320x340dp`).
 *
 * Places [viewfinder] in a full-bleed centered background box, computes the remaining safe width
 * and height after live `WindowInsets.systemBars.union(WindowInsets.displayCutout)` inside
 * [BoxWithConstraints], wraps [slots] with proportional `Modifier.sizeIn` caps so controls never
 * clip or overlap vertically, provides [LocalActiveCaptureLayout] = `"CompactShortLayout"`, and
 * delegates to [SideRailCaptureScaffold] with `isCompactHeight = true`, condensed `8.dp` chrome
 * padding, scoped [LocalCaptureRtlPolicy] (`mirrorSideRails`), and an in-hierarchy floating Quick
 * Settings card (`maxCardWidth <= 320.dp`).
 *
 * @param slots [CaptureSlots] supplying the side-rail controls, HUD overlays, Quick Settings
 *   content, snackbar, and error dialog.
 * @param viewfinder Composable rendering the camera preview frame.
 * @param modifier [Modifier] applied to the outer layout container.
 * @param state Mode-agnostic [CaptureScaffoldState] controlling Quick Settings visibility and the
 *   snackbar host.
 * @param controlsVisible Whether the camera control slots should be composed and visible.
 * @param isErrorDialogShowing Whether a modal camera error dialog is currently visible above the
 *   capture chrome.
 * @param onDismissQuickSettings Callback invoked when Quick Settings is dismissed via scrim tap or
 *   system back/escape.
 * @param overlay Optional overlay composable drawn above the viewfinder and controls.
 * @param screenFlashOverlay Full-bleed screen flash composable drawn above all chrome and system
 *   bar areas.
 */
@Composable
internal fun CompactShortLayout(
    slots: CaptureSlots,
    viewfinder: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    state: CaptureScaffoldState = rememberCaptureScaffoldState(),
    controlsVisible: Boolean = true,
    isErrorDialogShowing: Boolean = false,
    onDismissQuickSettings: () -> Unit = {},
    overlay: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    screenFlashOverlay: @Composable (Modifier) -> Unit = {}
) {
    val localeDirection = LocalLayoutDirection.current
    val safePadding = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalInsets = safePadding.calculateLeftPadding(localeDirection) +
            safePadding.calculateRightPadding(localeDirection)
        val verticalInsets = safePadding.calculateTopPadding() +
            safePadding.calculateBottomPadding()
        val availableWidth =
            (maxWidth - horizontalInsets).coerceAtLeast(CompactShortTokens.ZeroSize)
        val availableHeight =
            (maxHeight - verticalInsets).coerceAtLeast(CompactShortTokens.ZeroSize)

        val railSlotMaxWidth = min(
            CaptureSlotTokens.ModeToggleCompactWidth,
            (availableWidth * CompactShortTokens.RAIL_WIDTH_FRACTION)
                .coerceAtLeast(CompactShortTokens.MinRailSlotWidthFloor)
        )
        val estimatedRailFootprint =
            railSlotMaxWidth + CompactShortTokens.RailHorizontalPaddingTotal
        val condensedSlots = remember(slots, availableWidth, availableHeight) {
            slots.withCondensedSizeCaps(
                availableWidth = availableWidth,
                availableHeight = availableHeight,
                railSlotMaxWidth = railSlotMaxWidth,
                estimatedRailFootprint = estimatedRailFootprint
            )
        }
        val cardMaxWidth = min(
            CompactShortTokens.QuickSettingsCardMaxWidth,
            (availableWidth - CompactShortTokens.CondensedChromePadding * 2)
                .coerceAtLeast(CompactShortTokens.ZeroSize)
        )
        val cardMaxHeight = (availableHeight - CompactShortTokens.CondensedChromePadding * 2)
            .coerceAtLeast(CompactShortTokens.ZeroSize)

        val config = remember(cardMaxWidth, cardMaxHeight) {
            SideRailPostureConfig(
                activeLayoutName = "CompactShortLayout",
                isCompactHeight = true,
                railTopPadding = CompactShortTokens.CondensedChromePadding,
                railBottomPadding = CompactShortTokens.CondensedChromePadding,
                railHorizontalPadding = CompactShortTokens.CondensedChromePadding,
                railMinSectionGap = CompactShortTokens.CondensedMinSectionGap,
                centerVerticalFraction = 0.50f,
                hudVerticalPadding = CompactShortTokens.CondensedChromePadding,
                quickSettingsCardMaxWidth = cardMaxWidth,
                quickSettingsCardMaxHeight = cardMaxHeight,
                clearRailsForQuickSettings = false
            )
        }

        SideRailCaptureScaffold(
            slots = condensedSlots,
            viewfinder = viewfinder,
            modifier = Modifier.fillMaxSize(),
            state = state,
            config = config,
            controlsVisible = controlsVisible,
            isErrorDialogShowing = isErrorDialogShowing,
            onDismissQuickSettings = onDismissQuickSettings,
            overlay = overlay,
            screenFlashOverlay = screenFlashOverlay
        )
    }
}

private fun CaptureSlots.withCondensedSizeCaps(
    availableWidth: Dp,
    availableHeight: Dp,
    railSlotMaxWidth: Dp,
    estimatedRailFootprint: Dp
): CaptureSlots {
    val innerRailHeight = (availableHeight - CompactShortTokens.RailTotalVerticalPadding)
        .coerceAtLeast(CompactShortTokens.ZeroSize)

    val indicatorMaxHeight = min(
        CaptureSlotTokens.IndicatorCompactMainAxisSize,
        innerRailHeight * CompactShortTokens.INDICATOR_MAX_HEIGHT_FRACTION
    )
    val zoomMaxHeight = min(
        CaptureSlotTokens.ZoomCompactMainAxisSize,
        innerRailHeight * CompactShortTokens.ZOOM_MAX_HEIGHT_FRACTION
    )
    val quickSettingsButtonMaxSize = min(
        min(
            CaptureSlotTokens.IconTouchTargetSize,
            innerRailHeight * CompactShortTokens.QUICK_SETTINGS_BUTTON_FRACTION
        ),
        railSlotMaxWidth
    )
    val shutterMaxDiameter = min(
        min(
            CaptureSlotTokens.ShutterCompactDiameter,
            innerRailHeight * CompactShortTokens.SHUTTER_DIAMETER_FRACTION
        ),
        railSlotMaxWidth
    )
    val secondaryActionMaxSize = min(
        min(
            CaptureSlotTokens.SecondaryActionCompactSize,
            innerRailHeight * CompactShortTokens.SECONDARY_ACTION_FRACTION
        ),
        railSlotMaxWidth
    )
    val modeToggleMaxHeight = min(
        CaptureSlotTokens.ModeToggleCompactHeight,
        innerRailHeight * CompactShortTokens.MODE_TOGGLE_HEIGHT_FRACTION
    )

    val centerHudMaxWidth = (availableWidth - estimatedRailFootprint * 2)
        .coerceAtLeast(CompactShortTokens.MinCenterHudWidthFloor)
    val centerHudMaxHeight = min(
        CaptureSlotTokens.HorizontalTopBarHeight,
        innerRailHeight * CompactShortTokens.CENTER_HUD_HEIGHT_FRACTION
    )

    val original = this

    return original.copy(
        topBar = { mod ->
            ConstrainedSlot(
                modifier = mod,
                maxWidth = railSlotMaxWidth,
                maxHeight = indicatorMaxHeight,
                slot = original.topBar
            )
        },
        zoomControls = { mod ->
            ConstrainedSlot(
                modifier = mod,
                maxWidth = railSlotMaxWidth,
                maxHeight = zoomMaxHeight,
                slot = original.zoomControls
            )
        },
        quickSettingsButton = { mod ->
            ConstrainedSlot(
                modifier = mod,
                maxWidth = quickSettingsButtonMaxSize,
                maxHeight = quickSettingsButtonMaxSize,
                slot = original.quickSettingsButton
            )
        },
        captureButton = { mod ->
            ConstrainedSlot(
                modifier = mod,
                maxWidth = shutterMaxDiameter,
                maxHeight = shutterMaxDiameter,
                slot = original.captureButton
            )
        },
        flipCameraButton = { mod ->
            ConstrainedSlot(
                modifier = mod,
                maxWidth = secondaryActionMaxSize,
                maxHeight = secondaryActionMaxSize,
                slot = original.flipCameraButton
            )
        },
        imageWell = { mod ->
            ConstrainedSlot(
                modifier = mod,
                maxWidth = secondaryActionMaxSize,
                maxHeight = secondaryActionMaxSize,
                slot = original.imageWell
            )
        },
        captureModeToggle = { mod ->
            ConstrainedSlot(
                modifier = mod,
                maxWidth = railSlotMaxWidth,
                maxHeight = modeToggleMaxHeight,
                slot = original.captureModeToggle
            )
        },
        captureModeCarousel = { mod ->
            ConstrainedSlot(
                modifier = mod,
                maxWidth = centerHudMaxWidth,
                maxHeight = centerHudMaxHeight,
                slot = original.captureModeCarousel
            )
        },
        elapsedTimeDisplay = { mod ->
            ConstrainedSlot(
                modifier = mod,
                maxWidth = centerHudMaxWidth,
                maxHeight = centerHudMaxHeight,
                slot = original.elapsedTimeDisplay
            )
        },
        topStartContent = { mod ->
            ConstrainedSlot(
                modifier = mod,
                maxWidth = centerHudMaxWidth,
                maxHeight = secondaryActionMaxSize,
                slot = original.topStartContent
            )
        },
        viewfinderTopEndContent = { mod ->
            ConstrainedSlot(
                modifier = mod,
                maxWidth = centerHudMaxWidth,
                maxHeight = secondaryActionMaxSize,
                slot = original.viewfinderTopEndContent
            )
        }
    )
}

@Composable
private fun PreviewOverlaySlotScope.ConstrainedSlot(
    modifier: Modifier,
    maxWidth: Dp,
    maxHeight: Dp,
    slot: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit
) {
    val cappedModifier = modifier.sizeIn(maxWidth = maxWidth, maxHeight = maxHeight)
    Layout(
        content = { slot(cappedModifier) }
    ) { measurables, constraints ->
        if (measurables.isEmpty()) {
            layout(0, 0) {}
        } else {
            val cappedMaxWidthPx = minOf(constraints.maxWidth, maxWidth.roundToPx())
                .coerceAtLeast(0)
            val cappedMaxHeightPx = minOf(constraints.maxHeight, maxHeight.roundToPx())
                .coerceAtLeast(0)
            val childConstraints = Constraints(
                minWidth = 0,
                maxWidth = cappedMaxWidthPx,
                minHeight = 0,
                maxHeight = cappedMaxHeightPx
            )
            val placeables = measurables.map { it.measure(childConstraints) }
            val resolvedWidth = (placeables.maxOfOrNull { it.width } ?: 0)
                .coerceIn(constraints.minWidth, constraints.maxWidth)
            val resolvedHeight = (placeables.maxOfOrNull { it.height } ?: 0)
                .coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(resolvedWidth, resolvedHeight) {
                placeables.forEach { placeable ->
                    placeable.place(
                        x = (resolvedWidth - placeable.width) / 2,
                        y = (resolvedHeight - placeable.height) / 2
                    )
                }
            }
        }
    }
}

@Preview(
    name = "CompactShort - Multi-Window Square (360x360dp)",
    device = "spec:width=360dp,height=360dp",
    showBackground = true
)
@Composable
private fun CompactShortSquarePreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CompactShortLayout(
            slots = PlaceholderCaptureSlots.sideRail(
                isCompactHeight = true,
                includeElapsedTime = true
            ),
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) }
        )
    }
}

@Preview(
    name = "CompactShortSlim - Ultra-Compact (320x340dp)",
    device = "spec:width=320dp,height=340dp",
    showBackground = true
)
@Composable
private fun CompactShortSlimPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CompactShortLayout(
            slots = PlaceholderCaptureSlots.sideRail(
                isCompactHeight = true,
                includeElapsedTime = true
            ),
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) }
        )
    }
}

@Preview(
    name = "CompactShort - Quick Settings Open (360x360dp)",
    device = "spec:width=360dp,height=360dp",
    showBackground = true
)
@Composable
private fun CompactShortQuickSettingsOpenPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CompactShortLayout(
            slots = PlaceholderCaptureSlots.sideRail(
                isCompactHeight = true,
                includeElapsedTime = true,
                tagQuickSettingsOverlay = true
            ),
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
            state = rememberCaptureScaffoldState(isInitiallyExpanded = true)
        )
    }
}

@Preview(
    name = "CompactShort - RTL Default Policy (Unmirrored Rails)",
    device = "spec:width=360dp,height=360dp",
    locale = "ar",
    showBackground = true
)
@Composable
private fun CompactShortRtlDefaultPolicyPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CompositionLocalProvider(
            LocalCaptureRtlPolicy provides CaptureRtlPolicy(mirrorSideRails = false)
        ) {
            CompactShortLayout(
                slots = PlaceholderCaptureSlots.sideRail(
                    isCompactHeight = true,
                    includeElapsedTime = true,
                    tagQuickSettingsOverlay = true
                ),
                viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
                state = rememberCaptureScaffoldState(isInitiallyExpanded = true)
            )
        }
    }
}

@Preview(
    name = "CompactShort - RTL Mirrored Policy",
    device = "spec:width=360dp,height=360dp",
    locale = "ar",
    showBackground = true
)
@Composable
private fun CompactShortRtlMirroredPolicyPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CompositionLocalProvider(
            LocalCaptureRtlPolicy provides CaptureRtlPolicy(mirrorSideRails = true)
        ) {
            CompactShortLayout(
                slots = PlaceholderCaptureSlots.sideRail(
                    isCompactHeight = true,
                    includeElapsedTime = true,
                    tagQuickSettingsOverlay = true
                ),
                viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
                state = rememberCaptureScaffoldState(isInitiallyExpanded = true)
            )
        }
    }
}
