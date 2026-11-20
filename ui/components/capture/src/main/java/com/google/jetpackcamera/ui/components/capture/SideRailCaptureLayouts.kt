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

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.max
import com.google.jetpackcamera.ui.components.capture.quicksettings.QuickSettingsSideSheet

/**
 * Posture-specific padding, vertical alignment, and control sizing configuration for
 * [SideRailCaptureScaffold].
 *
 * [controlSize] overrides the [ControlSize] requested for rail and HUD controls; when `null` it is
 * derived from [isCompactHeight] (see [resolvedControlSize]). [railWidth] fixes each rail's width
 * so controls stay on a stable column anchored to the window edges.
 */
@Immutable
internal data class SideRailPostureConfig(
    val activeLayoutName: String,
    val isCompactHeight: Boolean,
    val railTopPadding: Dp,
    val railBottomPadding: Dp,
    val railHorizontalPadding: Dp,
    val railMinSectionGap: Dp,
    val centerVerticalFraction: Float,
    val hudVerticalPadding: Dp,
    val controlSize: ControlSize? = null,
    val railWidth: Dp = Dp.Unspecified,
    val useSideSheet: Boolean = !isCompactHeight
) {
    /**
     * The [ControlSize] requested for rail and HUD controls: [controlSize] when set, otherwise
     * [ControlSize.Mini] for compact-height windows and [ControlSize.Standard] elsewhere.
     */
    val resolvedControlSize: ControlSize
        get() = controlSize ?: if (isCompactHeight) ControlSize.Mini else ControlSize.Standard
}

/**
 * Shared dimension, clearance, and posture-preset constants for side-rail capture layouts.
 */
internal object SideRailTokens {
    val TopOverlayRailClearanceGap = 12.dp
    val TopHudSlotGap = 8.dp

    val FoldableConfig = SideRailPostureConfig(
        activeLayoutName = "FoldableLayout",
        isCompactHeight = false,
        railTopPadding = 24.dp,
        railBottomPadding = 24.dp,
        railHorizontalPadding = 16.dp,
        railMinSectionGap = 12.dp,
        centerVerticalFraction = 0.50f,
        hudVerticalPadding = 16.dp,
        controlSize = ControlSize.Medium,
        // Fixed rail width keeps the rails anchored to the window edges regardless of how wide the
        // black bars beside the viewfinder get. 96dp clears a 3:4 viewfinder on ~852x883dp
        // foldables.
        // TODO: Explore deriving this from the 3:4 viewfinder clearance, similar to how
        // CameraLayoutSolver sizes the compact control bands, instead of using a fixed value.
        railWidth = 96.dp
    )

    val TabletLandscapeConfig = SideRailPostureConfig(
        activeLayoutName = "TabletLandscapeLayout",
        isCompactHeight = false,
        railTopPadding = 24.dp,
        railBottomPadding = 28.dp,
        railHorizontalPadding = 20.dp,
        railMinSectionGap = 16.dp,
        centerVerticalFraction = 0.50f,
        hudVerticalPadding = 20.dp
    )

    val TabletPortraitConfig = SideRailPostureConfig(
        activeLayoutName = "TabletPortraitLayout",
        isCompactHeight = false,
        railTopPadding = 32.dp,
        railBottomPadding = 48.dp,
        railHorizontalPadding = 24.dp,
        railMinSectionGap = 16.dp,
        centerVerticalFraction = 0.70f,
        hudVerticalPadding = 24.dp
    )
}

/**
 * Unfolded book-style foldable capture layout (`~673x841dp` to `~852x883dp`) anchoring balanced
 * vertical control rails (`centerVerticalFraction = 0.50f`) to the start and end edges over a
 * full-bleed camera viewfinder. Quick Settings opens in an in-hierarchy [QuickSettingsSideSheet].
 *
 * Provides [LocalActiveCaptureLayout] = `"FoldableLayout"`.
 */
@Composable
internal fun FoldableLayout(
    slots: CaptureSlots,
    viewfinder: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    state: CaptureScaffoldState = rememberCaptureScaffoldState(),
    controlsVisible: Boolean = true,
    isErrorDialogShowing: Boolean = false,
    onDismissQuickSettings: () -> Unit = {},
    onRequiresImmersiveChange: (Boolean) -> Unit = {},
    overlay: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    screenFlashOverlay: @Composable (Modifier) -> Unit = {}
) {
    SideRailCaptureScaffold(
        slots = slots,
        viewfinder = viewfinder,
        modifier = modifier,
        state = state,
        config = SideRailTokens.FoldableConfig,
        controlsVisible = controlsVisible,
        isErrorDialogShowing = isErrorDialogShowing,
        onDismissQuickSettings = onDismissQuickSettings,
        onRequiresImmersiveChange = onRequiresImmersiveChange,
        overlay = overlay,
        screenFlashOverlay = screenFlashOverlay
    )
}

/**
 * Landscape tablet capture layout (`~1280x800dp`) anchoring generous vertical control rails
 * (`centerVerticalFraction = 0.50f`, `20.dp` horizontal padding, `16.dp` section gap) to the start
 * and end edges over a full-bleed viewfinder. Quick Settings opens in an in-hierarchy
 * [QuickSettingsSideSheet].
 *
 * Provides [LocalActiveCaptureLayout] = `"TabletLandscapeLayout"`.
 */
@Composable
internal fun TabletLandscapeLayout(
    slots: CaptureSlots,
    viewfinder: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    state: CaptureScaffoldState = rememberCaptureScaffoldState(),
    controlsVisible: Boolean = true,
    isErrorDialogShowing: Boolean = false,
    onDismissQuickSettings: () -> Unit = {},
    onRequiresImmersiveChange: (Boolean) -> Unit = {},
    overlay: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    screenFlashOverlay: @Composable (Modifier) -> Unit = {}
) {
    SideRailCaptureScaffold(
        slots = slots,
        viewfinder = viewfinder,
        modifier = modifier,
        state = state,
        config = SideRailTokens.TabletLandscapeConfig,
        controlsVisible = controlsVisible,
        isErrorDialogShowing = isErrorDialogShowing,
        onDismissQuickSettings = onDismissQuickSettings,
        onRequiresImmersiveChange = onRequiresImmersiveChange,
        overlay = overlay,
        screenFlashOverlay = screenFlashOverlay
    )
}

/**
 * Portrait tablet capture layout (`~800x1280dp`) shifting the zoom controls and shutter cluster
 * into the lower ergonomic thumb zone (`centerVerticalFraction = 0.70f`, `bottomPadding = 48.dp`,
 * `horizontalPadding = 24.dp`) over a full-bleed viewfinder. Quick Settings opens in an
 * in-hierarchy [QuickSettingsSideSheet] biased toward the lower third.
 *
 * Provides [LocalActiveCaptureLayout] = `"TabletPortraitLayout"`.
 */
@Composable
internal fun TabletPortraitLayout(
    slots: CaptureSlots,
    viewfinder: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    state: CaptureScaffoldState = rememberCaptureScaffoldState(),
    controlsVisible: Boolean = true,
    isErrorDialogShowing: Boolean = false,
    onDismissQuickSettings: () -> Unit = {},
    onRequiresImmersiveChange: (Boolean) -> Unit = {},
    overlay: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    screenFlashOverlay: @Composable (Modifier) -> Unit = {}
) {
    SideRailCaptureScaffold(
        slots = slots,
        viewfinder = viewfinder,
        modifier = modifier,
        state = state,
        config = SideRailTokens.TabletPortraitConfig,
        controlsVisible = controlsVisible,
        isErrorDialogShowing = isErrorDialogShowing,
        onDismissQuickSettings = onDismissQuickSettings,
        onRequiresImmersiveChange = onRequiresImmersiveChange,
        overlay = overlay,
        screenFlashOverlay = screenFlashOverlay
    )
}

/**
 * Shared full-bleed viewfinder and dual-rail capture scaffold backing [FoldableLayout],
 * [TabletLandscapeLayout], [TabletPortraitLayout], and [CompactShortLayout].
 *
 * Quick Settings opens in [QuickSettingsSideSheet] when [SideRailPostureConfig.useSideSheet] is
 * `true` (tablet and foldable layouts) or in [QuickSettingsBottomSheetScaffold] otherwise
 * ([CompactShortLayout]). A back press or a tap on the scrim dismisses it and invokes
 * [onDismissQuickSettings]. Dynamically measures [StartSideRail] and [EndSideRail] widths via
 * [onSizeChanged] so the top HUD overlays clear both rails.
 */
@Composable
internal fun SideRailCaptureScaffold(
    slots: CaptureSlots,
    viewfinder: @Composable (Modifier) -> Unit,
    modifier: Modifier,
    state: CaptureScaffoldState,
    config: SideRailPostureConfig,
    controlsVisible: Boolean,
    isErrorDialogShowing: Boolean,
    onDismissQuickSettings: () -> Unit,
    onRequiresImmersiveChange: (Boolean) -> Unit,
    overlay: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit,
    screenFlashOverlay: @Composable (Modifier) -> Unit
) {
    val overlapTargetBounds = remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    val controlSize = config.resolvedControlSize
    val localeDirection = LocalLayoutDirection.current
    val rtlPolicy = LocalCaptureRtlPolicy.current
    val railContainerDirection = if (rtlPolicy.mirrorSideRails) {
        localeDirection
    } else {
        LayoutDirection.Ltr
    }

    val seededRailWidthPx = remember(config.railWidth, config.railHorizontalPadding, density) {
        with(density) {
            if (config.railWidth.isSpecified) {
                config.railWidth.roundToPx()
            } else {
                (MinSideRailWidth + config.railHorizontalPadding * 2).roundToPx()
            }
        }
    }
    var startRailWidthPx by remember(seededRailWidthPx) { mutableIntStateOf(seededRailWidthPx) }
    var endRailWidthPx by remember(seededRailWidthPx) { mutableIntStateOf(seededRailWidthPx) }
    val startRailWidthDp = with(density) { startRailWidthPx.toDp() }
    val endRailWidthDp = with(density) { endRailWidthPx.toDp() }

    var isErrorDialogPlaced by remember { mutableStateOf(false) }
    val isErrorModalActive = isErrorDialogShowing || isErrorDialogPlaced
    val isQuickSettingsVisible = state.isQuickSettingsOpen && !isErrorModalActive

    val handleDismissQuickSettings = {
        state.dismissQuickSettings()
        onDismissQuickSettings()
    }
    BackHandler(
        enabled = !config.useSideSheet && isQuickSettingsVisible,
        onBack = handleDismissQuickSettings
    )

    // Side-rail layouts always ask the host to hide the navigation bar, so a persistent taskbar
    // doesn't cover the viewfinder or sit beside the camera controls. Requesting it for the layout,
    // rather than for the current preview size, keeps the bar and the controls from moving when the
    // lens or aspect ratio changes.
    val currentOnRequiresImmersiveChange by rememberUpdatedState(onRequiresImmersiveChange)
    DisposableEffect(Unit) {
        currentOnRequiresImmersiveChange(true)
        onDispose { currentOnRequiresImmersiveChange(false) }
    }

    // With the navigation bar hidden, the rails don't reserve space for it, which keeps them
    // centred on the viewfinder and stops them from shifting when the bar is revealed with a swipe.
    // In multi-window mode the bars stay visible, so the rails clear them.
    val isInMultiWindowMode = rememberIsInMultiWindowMode()
    val liveSafeInsets = if (isInMultiWindowMode) {
        WindowInsets.systemBars.union(WindowInsets.displayCutout)
    } else {
        WindowInsets.statusBars
            .union(WindowInsets.captionBar)
            .union(WindowInsets.displayCutout)
    }

    CompositionLocalProvider(
        LocalOverlapTargetBounds provides overlapTargetBounds,
        LocalActiveCaptureLayout provides config.activeLayoutName
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Full-bleed centered viewfinder flush from top to bottom of the window, kept outside
            // BottomSheetScaffold's SubcomposeLayout so SurfaceView hardware overlays stay active.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .suppressBackgroundForModal(isQuickSettingsVisible || isErrorModalActive),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                    viewfinder(Modifier.fillMaxSize())
                }
            }

            val foregroundChrome: @Composable (Boolean) -> Unit = { isSheetVisible ->
                val isModalOpen = isSheetVisible || isErrorModalActive
                // Overlaid foreground chrome padded by live visible systemBars and
                // displayCutout.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(liveSafeInsets)
                ) {
                    if (controlsVisible) {
                        CompositionLocalProvider(
                            LocalLayoutDirection provides railContainerDirection
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                CompositionLocalProvider(
                                    LocalLayoutDirection provides localeDirection
                                ) {
                                    StartSideRail(
                                        slots = slots,
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .fillMaxHeight()
                                            .onSizeChanged { startRailWidthPx = it.width }
                                            .suppressBackgroundForModal(isModalOpen),
                                        isCompactHeight = config.isCompactHeight,
                                        controlSize = controlSize,
                                        railWidth = config.railWidth,
                                        topPadding = config.railTopPadding,
                                        bottomPadding = config.railBottomPadding,
                                        horizontalPadding = config.railHorizontalPadding,
                                        minSectionGap = config.railMinSectionGap,
                                        centerVerticalFraction = config.centerVerticalFraction
                                    )

                                    EndSideRail(
                                        slots = slots,
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight()
                                            .onSizeChanged { endRailWidthPx = it.width }
                                            .suppressBackgroundForModal(isModalOpen),
                                        isCompactHeight = config.isCompactHeight,
                                        controlSize = controlSize,
                                        railWidth = config.railWidth,
                                        showElapsedTimeInRail = false,
                                        topPadding = config.railTopPadding,
                                        bottomPadding = config.railBottomPadding,
                                        horizontalPadding = config.railHorizontalPadding,
                                        minSectionGap = config.railMinSectionGap,
                                        centerVerticalFraction = config.centerVerticalFraction
                                    )
                                }
                            }
                        }
                    }

                    val topOverlayClearance = max(startRailWidthDp, endRailWidthDp) +
                        SideRailTokens.TopOverlayRailClearanceGap

                    CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                        SideRailTopHud(
                            gap = SideRailTokens.TopHudSlotGap,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = topOverlayClearance)
                                .padding(top = config.hudVerticalPadding),
                            start = {
                                OverlapAwareCaptureSlot(
                                    modifier = Modifier.suppressBackgroundForModal(isModalOpen),
                                    orientation = Orientation.Horizontal,
                                    controlSize = controlSize,
                                    content = slots.topStartContent
                                )
                            },
                            end = {
                                OverlapAwareCaptureSlot(
                                    modifier = Modifier.suppressBackgroundForModal(isModalOpen),
                                    orientation = Orientation.Horizontal,
                                    controlSize = controlSize,
                                    content = slots.viewfinderTopEndContent
                                )
                            },
                            center = {
                                Column(
                                    modifier = Modifier.suppressBackgroundForModal(isModalOpen),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    PillSnackbarHost(snackbarHostState = state.snackbarHostState)
                                    if (controlsVisible) {
                                        OverlapAwareCaptureSlot(
                                            orientation = Orientation.Horizontal,
                                            controlSize = controlSize,
                                            content = slots.elapsedTimeDisplay
                                        )
                                    }
                                }
                            }
                        )

                        if (controlsVisible) {
                            OverlapAwareCaptureSlot(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(
                                        start = topOverlayClearance,
                                        end = topOverlayClearance,
                                        bottom = config.hudVerticalPadding
                                    )
                                    .suppressBackgroundForModal(isModalOpen),
                                orientation = Orientation.Horizontal,
                                controlSize = controlSize,
                                content = slots.captureModeCarousel
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .suppressBackgroundForModal(isModalOpen)
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                            slots.snackBar(Modifier, state.snackbarHostState)
                        }
                    }
                }

                CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                    OverlapAwareCaptureSlot(
                        modifier = Modifier.suppressBackgroundForModal(isModalOpen),
                        orientation = Orientation.Horizontal,
                        controlSize = controlSize,
                        content = overlay
                    )
                }

                if (config.useSideSheet) {
                    val sideSheetAlignment = BiasAlignment(
                        horizontalBias = -1f,
                        verticalBias = 2f * config.centerVerticalFraction - 1f
                    )
                    QuickSettingsSideSheet(
                        isOpen = isQuickSettingsVisible,
                        onDismissRequest = handleDismissQuickSettings,
                        contentAlignment = sideSheetAlignment,
                        sheetContainerDirection = railContainerDirection,
                        windowInsets = liveSafeInsets
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                            OverlapAwareCaptureSlot(
                                orientation = Orientation.Vertical,
                                controlSize = ControlSize.Standard,
                                content = slots.quickSettingsOverlay
                            )
                        }
                    }
                } else if (isSheetVisible && !isErrorModalActive) {
                    QuickSettingsSheetScrim(onDismiss = handleDismissQuickSettings)
                }

                CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                    Box(
                        modifier = Modifier.onSizeChanged { size ->
                            isErrorDialogPlaced = size.width > 0 && size.height > 0
                        }
                    ) {
                        slots.errorDialog(Modifier)
                    }
                }

                // Drawn last and outside all inset-padded containers so screen flash covers the
                // entire window, including system bar and display cutout areas.
                screenFlashOverlay(Modifier)
            }

            if (config.useSideSheet) {
                Box(modifier = Modifier.fillMaxSize()) {
                    foregroundChrome(isQuickSettingsVisible)
                }
            } else {
                QuickSettingsBottomSheetScaffold(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    onDismissQuickSettings = onDismissQuickSettings,
                    sheetContent = {
                        CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                            OverlapAwareCaptureSlot(
                                orientation = Orientation.Vertical,
                                controlSize = ControlSize.Standard,
                                content = slots.quickSettingsOverlay
                            )
                        }
                    },
                    content = foregroundChrome
                )
            }
        }
    }
}


/** Returns whether the host activity is in multi-window mode, updating on configuration changes. */
@Composable
private fun rememberIsInMultiWindowMode(): Boolean {
    val configuration = LocalConfiguration.current
    val activity = LocalActivity.current
    return remember(configuration, activity) { activity?.isInMultiWindowMode == true }
}

/**
 * Lays out the top HUD between the side rails so that its slots never overlap.
 *
 * Places [start] at the top-start and [end] at the top-end, limiting [end] to the width left beside
 * [start]. Centers [center] between them when it clears both by [gap], and otherwise moves it below
 * them.
 */
@Composable
private fun SideRailTopHud(
    gap: Dp,
    start: @Composable () -> Unit,
    end: @Composable () -> Unit,
    center: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Layout(contents = listOf(start, end, center), modifier = modifier) {
            (startMeasurables, endMeasurables, centerMeasurables), constraints ->
        val gapPx = gap.roundToPx()
        val width = constraints.maxWidth
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val startPlaceables = startMeasurables.map { it.measure(looseConstraints) }
        val startWidth = startPlaceables.maxOfOrNull { it.width } ?: 0
        val endMaxWidth = if (startWidth > 0) {
            (width - startWidth - gapPx).coerceAtLeast(0)
        } else {
            width
        }
        val endPlaceables = endMeasurables.map {
            it.measure(looseConstraints.copy(maxWidth = endMaxWidth))
        }
        val endWidth = endPlaceables.maxOfOrNull { it.width } ?: 0
        val edgeHeight = maxOf(
            startPlaceables.maxOfOrNull { it.height } ?: 0,
            endPlaceables.maxOfOrNull { it.height } ?: 0
        )

        val centerPlaceables = centerMeasurables.map { it.measure(looseConstraints) }
        val centerWidth = centerPlaceables.maxOfOrNull { it.width } ?: 0
        val centerHeight = centerPlaceables.maxOfOrNull { it.height } ?: 0
        val edgeWidth = maxOf(startWidth, endWidth)
        val centerFitsBetweenEdges = edgeWidth == 0 || centerWidth == 0 ||
            centerWidth + 2 * (edgeWidth + gapPx) <= width
        val centerY = if (centerFitsBetweenEdges || edgeHeight == 0) 0 else edgeHeight + gapPx

        val height = if (constraints.hasBoundedHeight) {
            constraints.maxHeight
        } else {
            maxOf(edgeHeight, centerY + centerHeight)
        }
        layout(width, height) {
            startPlaceables.forEach { it.placeRelative(0, 0) }
            endPlaceables.forEach { it.placeRelative(width - it.width, 0) }
            centerPlaceables.forEach { it.placeRelative((width - it.width) / 2, centerY) }
        }
    }
}

@Preview(
    name = "Foldable - Unfolded (852x883dp)",
    device = "spec:width=852dp,height=883dp",
    showBackground = true
)
@Composable
private fun FoldableUnfoldedPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        FoldableLayout(
            slots = previewSideRailSlots(includeElapsedTime = true),
            viewfinder = { mod -> Box(modifier = mod.fillMaxSize()) }
        )
    }
}

@Preview(
    name = "Foldable - Quick Settings Open (852x883dp)",
    device = "spec:width=852dp,height=883dp",
    showBackground = true
)
@Composable
private fun FoldableQuickSettingsOpenPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        FoldableLayout(
            slots = previewSideRailSlots(includeElapsedTime = true),
            viewfinder = { mod -> Box(modifier = mod.fillMaxSize()) },
            state = rememberCaptureScaffoldState(isInitiallyExpanded = true)
        )
    }
}

@Preview(
    name = "TabletLandscape - Recording (1280x800dp)",
    device = "spec:width=1280dp,height=800dp",
    showBackground = true
)
@Composable
private fun TabletLandscapePreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        TabletLandscapeLayout(
            slots = previewSideRailSlots(includeElapsedTime = true),
            viewfinder = { mod -> Box(modifier = mod.fillMaxSize()) }
        )
    }
}

@Preview(
    name = "TabletPortrait - Lower Thumb Zone (800x1280dp)",
    device = "spec:width=800dp,height=1280dp",
    showBackground = true
)
@Composable
private fun TabletPortraitPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        TabletPortraitLayout(
            slots = previewSideRailSlots(includeElapsedTime = true),
            viewfinder = { mod -> Box(modifier = mod.fillMaxSize()) }
        )
    }
}

@Preview(
    name = "TabletLandscape - RTL Default Policy (Unmirrored Rails)",
    device = "spec:width=1280dp,height=800dp",
    locale = "ar",
    showBackground = true
)
@Composable
private fun TabletLandscapeRtlDefaultPolicyPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CompositionLocalProvider(
            LocalCaptureRtlPolicy provides CaptureRtlPolicy(mirrorSideRails = false)
        ) {
            TabletLandscapeLayout(
                slots = previewSideRailSlots(includeElapsedTime = true),
                viewfinder = { mod -> Box(modifier = mod.fillMaxSize()) },
                state = rememberCaptureScaffoldState(isInitiallyExpanded = true)
            )
        }
    }
}

@Preview(
    name = "TabletLandscape - RTL Mirrored Policy",
    device = "spec:width=1280dp,height=800dp",
    locale = "ar",
    showBackground = true
)
@Composable
private fun TabletLandscapeRtlMirroredPolicyPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CompositionLocalProvider(
            LocalCaptureRtlPolicy provides CaptureRtlPolicy(mirrorSideRails = true)
        ) {
            TabletLandscapeLayout(
                slots = previewSideRailSlots(includeElapsedTime = true),
                viewfinder = { mod -> Box(modifier = mod.fillMaxSize()) },
                state = rememberCaptureScaffoldState(isInitiallyExpanded = true)
            )
        }
    }
}
