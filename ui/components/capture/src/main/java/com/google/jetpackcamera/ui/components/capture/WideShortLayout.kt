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
import androidx.compose.material3.SnackbarHostState
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

private object WideShortTokens {
    val ZeroSize = 0.dp
    val CondensedChromePadding = 8.dp
    val RailTotalVerticalPadding = 16.dp
    val RailHorizontalPaddingTotal = 16.dp
    val MinRailSlotWidthFloor = 36.dp
    val MinCenterHudWidthFloor = 48.dp
    val QuickSettingsCardMaxWidth = 320.dp

    // Proportional height fractions of the usable inner rail height so controls scale down
    // harmoniously without clipping or overlap in condensed windows (< 480dp, e.g. 800x400dp or
    // 360x360dp with visible multi-window status bars).
    const val RAIL_WIDTH_FRACTION = 0.22f
    const val INDICATOR_MAX_HEIGHT_FRACTION = 0.20f
    const val ZOOM_MAX_HEIGHT_FRACTION = 0.45f
    const val QUICK_SETTINGS_BUTTON_FRACTION = 0.16f
    const val SHUTTER_DIAMETER_FRACTION = 0.22f
    const val SECONDARY_ACTION_FRACTION = 0.15f
    const val MODE_TOGGLE_HEIGHT_FRACTION = 0.15f
    const val SHUTTER_STACK_HEIGHT_FRACTION = 0.78f
    const val CENTER_HUD_HEIGHT_FRACTION = 0.12f
}

/**
 * Condensed-height side-rail capture layout for short windows (`heightDp < 480.dp`, covering both
 * `WIDE_SHORT` landscape/split-screen postures such as `800x400dp` and `COMPACT_SHORT` square
 * multi-window panes such as `360x360dp`).
 *
 * Places [viewfinder] in a full-bleed centered background box, computes the remaining safe width
 * and height after live `WindowInsets.systemBars.union(WindowInsets.displayCutout)` inside
 * [BoxWithConstraints], wraps [slots] with proportional `Modifier.sizeIn` caps so controls never
 * clip or overlap vertically, and delegates to [StartSideRail] and [EndSideRail] with
 * `isCompactHeight = true`, `showElapsedTimeInRail = false`, condensed `8.dp` chrome padding,
 * scoped [LocalCaptureRtlPolicy] (`mirrorSideRails`), and an in-hierarchy floating Quick Settings
 * card.
 *
 * @param slots [CaptureSlots] supplying the side-rail controls, HUD overlays, Quick Settings
 *   content, snackbar, and error dialog.
 * @param viewfinder Composable rendering the camera preview frame.
 * @param modifier [Modifier] applied to the outer layout container.
 * @param snackbarHostState [SnackbarHostState] driving the top-center [PillSnackbarHost] and passed
 *   to [CaptureSlots.snackBar].
 * @param isQuickSettingsOpen Whether the in-hierarchy Quick Settings scrim and floating card are
 *   displayed.
 * @param isRecording Whether an active video recording session is in progress.
 * @param isErrorDialogShowing Whether a modal camera error dialog is currently visible above the
 *   capture chrome.
 * @param tagQuickSettingsCard Whether the outer floating Quick Settings card carries
 *   [QUICK_SETTINGS_BOTTOM_SHEET]; defaults to `true`.
 * @param onDismissQuickSettings Callback invoked on scrim tap, system back press while Quick
 *   Settings is open, or [CaptureSlotScope.dismissQuickSettings].
 * @param debugOverlay Untouched pass-through composable for debug UI overlays.
 * @param debugVisibilityWrapper Wrapper conditionally hiding camera chrome when debug component
 *   hiding is enabled.
 * @param screenFlashOverlay Full-bleed screen flash composable drawn above all chrome and system
 *   bar areas.
 */
@Composable
fun WideShortLayout(
    slots: CaptureSlots,
    viewfinder: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    isQuickSettingsOpen: Boolean = false,
    isRecording: Boolean = false,
    isErrorDialogShowing: Boolean = false,
    tagQuickSettingsCard: Boolean = true,
    onDismissQuickSettings: () -> Unit = {},
    debugOverlay: @Composable (Modifier) -> Unit = {},
    debugVisibilityWrapper: @Composable (@Composable () -> Unit) -> Unit = { it() },
    screenFlashOverlay: @Composable (Modifier) -> Unit = {}
) {
    val localeDirection = LocalLayoutDirection.current
    val safePadding = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalInsets = safePadding.calculateLeftPadding(localeDirection) +
            safePadding.calculateRightPadding(localeDirection)
        val verticalInsets = safePadding.calculateTopPadding() +
            safePadding.calculateBottomPadding()
        val availableWidth = (maxWidth - horizontalInsets).coerceAtLeast(WideShortTokens.ZeroSize)
        val availableHeight = (maxHeight - verticalInsets).coerceAtLeast(WideShortTokens.ZeroSize)

        val railSlotMaxWidth = min(
            CaptureSlotTokens.ModeToggleCompactWidth,
            (availableWidth * WideShortTokens.RAIL_WIDTH_FRACTION)
                .coerceAtLeast(WideShortTokens.MinRailSlotWidthFloor)
        )
        val estimatedRailFootprint = railSlotMaxWidth + WideShortTokens.RailHorizontalPaddingTotal
        val condensedSlots = remember(slots, availableWidth, availableHeight) {
            slots.withCondensedSizeCaps(
                availableWidth = availableWidth,
                availableHeight = availableHeight,
                railSlotMaxWidth = railSlotMaxWidth,
                estimatedRailFootprint = estimatedRailFootprint
            )
        }
        val cardMaxWidth = min(
            WideShortTokens.QuickSettingsCardMaxWidth,
            (availableWidth - WideShortTokens.CondensedChromePadding * 2)
                .coerceAtLeast(WideShortTokens.ZeroSize)
        )
        val cardMaxHeight = (availableHeight - WideShortTokens.CondensedChromePadding * 2)
            .coerceAtLeast(WideShortTokens.ZeroSize)

        SideRailCaptureScaffold(
            slots = condensedSlots,
            viewfinder = viewfinder,
            modifier = Modifier.fillMaxSize(),
            snackbarHostState = snackbarHostState,
            isCompactHeight = true,
            chromePadding = WideShortTokens.CondensedChromePadding,
            topOverlayHorizontalClearance = estimatedRailFootprint,
            quickSettingsCardMaxWidth = cardMaxWidth,
            quickSettingsCardMaxHeight = cardMaxHeight,
            isQuickSettingsOpen = isQuickSettingsOpen,
            isRecording = isRecording,
            isErrorDialogShowing = isErrorDialogShowing,
            tagQuickSettingsCard = tagQuickSettingsCard,
            onDismissQuickSettings = onDismissQuickSettings,
            debugOverlay = debugOverlay,
            debugVisibilityWrapper = debugVisibilityWrapper,
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
    val innerRailHeight = (availableHeight - WideShortTokens.RailTotalVerticalPadding)
        .coerceAtLeast(WideShortTokens.ZeroSize)

    val indicatorMaxHeight = min(
        CaptureSlotTokens.IndicatorCompactMainAxisSize,
        innerRailHeight * WideShortTokens.INDICATOR_MAX_HEIGHT_FRACTION
    )
    val zoomMaxHeight = min(
        CaptureSlotTokens.ZoomCompactMainAxisSize,
        innerRailHeight * WideShortTokens.ZOOM_MAX_HEIGHT_FRACTION
    )
    val quickSettingsButtonMaxSize = min(
        min(
            CaptureSlotTokens.IconTouchTargetSize,
            innerRailHeight * WideShortTokens.QUICK_SETTINGS_BUTTON_FRACTION
        ),
        railSlotMaxWidth
    )
    val shutterMaxDiameter = min(
        min(
            CaptureSlotTokens.ShutterCompactDiameter,
            innerRailHeight * WideShortTokens.SHUTTER_DIAMETER_FRACTION
        ),
        railSlotMaxWidth
    )
    val secondaryActionMaxSize = min(
        min(
            CaptureSlotTokens.SecondaryActionCompactSize,
            innerRailHeight * WideShortTokens.SECONDARY_ACTION_FRACTION
        ),
        railSlotMaxWidth
    )
    val modeToggleMaxHeight = min(
        CaptureSlotTokens.ModeToggleCompactHeight,
        innerRailHeight * WideShortTokens.MODE_TOGGLE_HEIGHT_FRACTION
    )
    val shutterStackMaxHeight = innerRailHeight * WideShortTokens.SHUTTER_STACK_HEIGHT_FRACTION

    val centerHudMaxWidth = (availableWidth - estimatedRailFootprint * 2)
        .coerceAtLeast(WideShortTokens.MinCenterHudWidthFloor)
    val centerHudMaxHeight = min(
        CaptureSlotTokens.HorizontalTopBarHeight,
        innerRailHeight * WideShortTokens.CENTER_HUD_HEIGHT_FRACTION
    )
    val cardInnerMaxWidth = min(
        WideShortTokens.QuickSettingsCardMaxWidth,
        (availableWidth - WideShortTokens.CondensedChromePadding * 2)
            .coerceAtLeast(WideShortTokens.ZeroSize)
    )
    val cardInnerMaxHeight = (availableHeight - WideShortTokens.CondensedChromePadding * 2)
        .coerceAtLeast(WideShortTokens.ZeroSize)

    val original = this
    val originalShutterStack = original.shutterStack

    return original.copy(
        indicatorColumn = { mod ->
            ConstrainedSlot(
                modifier = mod,
                maxWidth = railSlotMaxWidth,
                maxHeight = indicatorMaxHeight,
                slot = original.indicatorColumn
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
        pauseToggleButton = { mod ->
            ConstrainedSlot(
                modifier = mod,
                maxWidth = secondaryActionMaxSize,
                maxHeight = secondaryActionMaxSize,
                slot = original.pauseToggleButton
            )
        },
        audioToggleButton = { mod ->
            ConstrainedSlot(
                modifier = mod,
                maxWidth = secondaryActionMaxSize,
                maxHeight = secondaryActionMaxSize,
                slot = original.audioToggleButton
            )
        },
        shutterStack = if (originalShutterStack != null) {
            { mod, _ ->
                ConstrainedSlot(
                    modifier = mod,
                    maxWidth = railSlotMaxWidth,
                    maxHeight = shutterStackMaxHeight
                ) { cappedMod ->
                    originalShutterStack(cappedMod, true)
                }
            }
        } else {
            null
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
        viewfinderTopStart = { mod ->
            ConstrainedSlot(
                modifier = mod,
                maxWidth = centerHudMaxWidth,
                maxHeight = secondaryActionMaxSize,
                slot = original.viewfinderTopStart
            )
        },
        viewfinderTopEnd = { mod ->
            ConstrainedSlot(
                modifier = mod,
                maxWidth = centerHudMaxWidth,
                maxHeight = secondaryActionMaxSize,
                slot = original.viewfinderTopEnd
            )
        },
        quickSettingsOverlay = { mod ->
            ConstrainedSlot(
                modifier = mod,
                maxWidth = cardInnerMaxWidth,
                maxHeight = cardInnerMaxHeight,
                slot = original.quickSettingsOverlay
            )
        }
    )
}

@Composable
private fun CaptureSlotScope.ConstrainedSlot(
    modifier: Modifier,
    maxWidth: Dp,
    maxHeight: Dp,
    slot: @Composable CaptureSlotScope.(Modifier) -> Unit
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
    name = "WideShort - Landscape (800x400dp)",
    device = "spec:width=800dp,height=400dp",
    showBackground = true
)
@Composable
private fun WideShortLandscapePreview() {
    MaterialTheme {
        WideShortLayout(
            slots = PlaceholderCaptureSlots.sideRail(
                isCompactHeight = true,
                includePauseSlot = true,
                includeElapsedTime = true
            ),
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
            isRecording = true
        )
    }
}

@Preview(
    name = "CompactShort - Multi-Window Square (360x360dp)",
    device = "spec:width=360dp,height=360dp",
    showBackground = true
)
@Composable
private fun CompactShortSquarePreview() {
    MaterialTheme {
        WideShortLayout(
            slots = PlaceholderCaptureSlots.sideRail(
                isCompactHeight = true,
                includePauseSlot = true,
                includeElapsedTime = true
            ),
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
            isRecording = true
        )
    }
}

@Preview(
    name = "WideShort - RTL Default Policy (800x400dp)",
    device = "spec:width=800dp,height=400dp",
    locale = "ar",
    showBackground = true
)
@Composable
private fun WideShortRtlDefaultPolicyPreview() {
    MaterialTheme {
        CompositionLocalProvider(
            LocalCaptureRtlPolicy provides CaptureRtlPolicy(mirrorSideRails = false)
        ) {
            WideShortLayout(
                slots = PlaceholderCaptureSlots.sideRail(
                    isCompactHeight = true,
                    includeElapsedTime = true
                ),
                viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) }
            )
        }
    }
}

@Preview(
    name = "WideShort - RTL Mirrored Policy (800x400dp)",
    device = "spec:width=800dp,height=400dp",
    locale = "ar",
    showBackground = true
)
@Composable
private fun WideShortRtlMirroredPolicyPreview() {
    MaterialTheme {
        CompositionLocalProvider(
            LocalCaptureRtlPolicy provides CaptureRtlPolicy(mirrorSideRails = true)
        ) {
            WideShortLayout(
                slots = PlaceholderCaptureSlots.sideRail(
                    isCompactHeight = true,
                    includeElapsedTime = true
                ),
                viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) }
            )
        }
    }
}
