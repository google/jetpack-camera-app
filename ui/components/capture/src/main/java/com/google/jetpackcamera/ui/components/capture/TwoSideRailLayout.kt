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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private object TwoSideRailTokens {
    val StandardChromePadding = 16.dp
    val StandardTopOverlayHorizontalClearance = 120.dp
    val QuickSettingsCardMaxWidth = 360.dp
    val QuickSettingsCardCornerRadius = 28.dp
    val QuickSettingsCardTonalElevation = 6.dp
    val QuickSettingsScrimColor = Color.Black.copy(alpha = 0.45f)
}

/**
 * Large-screen and foldable capture layout anchoring vertical control rails to the start and end
 * edges over a full-bleed camera viewfinder.
 *
 * Places [viewfinder] in a full-screen centered background box without portrait solver offsets so
 * previews sit flush from top to bottom. Overlays [StartSideRail], [EndSideRail] (with
 * `showElapsedTimeInRail = false`), and center HUD slots inside live
 * `WindowInsets.systemBars.union(WindowInsets.displayCutout)` safe bounds so visible multi-window
 * status bars are cleared while hidden immersive bars introduce zero dead padding.
 *
 * Scopes [LocalCaptureRtlPolicy] (`mirrorSideRails`) solely to the spatial rail container and Quick
 * Settings card anchor while immediately restoring the active locale's `LayoutDirection` inside
 * both rails and every child slot. Hosts Quick Settings as an in-hierarchy floating [Surface] card
 * (optionally tagged with [QUICK_SETTINGS_BOTTOM_SHEET]) without spawning secondary windows.
 *
 * @param slots [CaptureSlots] supplying the side-rail controls, HUD overlays, Quick Settings
 *   content, snackbar, and error dialog.
 * @param viewfinder Composable rendering the camera preview frame.
 * @param modifier [Modifier] applied to the root layout box.
 * @param snackbarHostState [SnackbarHostState] driving the top-center [PillSnackbarHost] and passed
 *   to [CaptureSlots.snackBar].
 * @param isQuickSettingsOpen Whether the in-hierarchy Quick Settings scrim and floating card are
 *   displayed.
 * @param isRecording Whether an active video recording session is in progress.
 * @param isErrorDialogShowing Whether a modal camera error dialog is currently visible above the
 *   capture chrome.
 * @param tagQuickSettingsCard Whether the outer floating Quick Settings [Surface] card carries
 *   [QUICK_SETTINGS_BOTTOM_SHEET]; defaults to `true` so callers whose inner overlay is untagged
 *   preserve test-tag parity with compact sheets, and can be set to `false` when the inner overlay
 *   already attaches that tag.
 * @param onDismissQuickSettings Callback invoked on scrim tap, system back press while Quick
 *   Settings is open, or [CaptureSlotScope.dismissQuickSettings].
 * @param debugOverlay Untouched pass-through composable for debug UI overlays.
 * @param debugVisibilityWrapper Wrapper conditionally hiding camera chrome when debug component
 *   hiding is enabled.
 * @param screenFlashOverlay Full-bleed screen flash composable drawn above all chrome and system
 *   bar areas.
 */
@Composable
fun TwoSideRailLayout(
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
    SideRailCaptureScaffold(
        slots = slots,
        viewfinder = viewfinder,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        isCompactHeight = false,
        chromePadding = TwoSideRailTokens.StandardChromePadding,
        topOverlayHorizontalClearance = TwoSideRailTokens.StandardTopOverlayHorizontalClearance,
        quickSettingsCardMaxWidth = TwoSideRailTokens.QuickSettingsCardMaxWidth,
        quickSettingsCardMaxHeight = Dp.Unspecified,
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

/**
 * Shared full-bleed viewfinder and dual-rail capture scaffold backing [TwoSideRailLayout] and
 * [WideShortLayout].
 */
@Composable
internal fun SideRailCaptureScaffold(
    slots: CaptureSlots,
    viewfinder: @Composable (Modifier) -> Unit,
    modifier: Modifier,
    snackbarHostState: SnackbarHostState,
    isCompactHeight: Boolean,
    chromePadding: Dp,
    topOverlayHorizontalClearance: Dp,
    quickSettingsCardMaxWidth: Dp,
    quickSettingsCardMaxHeight: Dp,
    isQuickSettingsOpen: Boolean,
    isRecording: Boolean,
    isErrorDialogShowing: Boolean,
    tagQuickSettingsCard: Boolean,
    onDismissQuickSettings: () -> Unit,
    debugOverlay: @Composable (Modifier) -> Unit,
    debugVisibilityWrapper: @Composable (@Composable () -> Unit) -> Unit,
    screenFlashOverlay: @Composable (Modifier) -> Unit
) {
    val overlapTargetBounds = remember { mutableStateOf(Rect.Zero) }
    val localeDirection = LocalLayoutDirection.current
    val rtlPolicy = LocalCaptureRtlPolicy.current
    val railContainerDirection = if (rtlPolicy.mirrorSideRails) {
        localeDirection
    } else {
        LayoutDirection.Ltr
    }
    val isModalOpen = isQuickSettingsOpen || isErrorDialogShowing
    val liveSafeInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    val dismissQuickSettingsLabel =
        stringResource(R.string.quick_settings_btn_close_expanded_settings_description)

    BackHandler(enabled = isQuickSettingsOpen, onBack = onDismissQuickSettings)

    CompositionLocalProvider(
        LocalOverlapTargetBounds provides overlapTargetBounds,
        LocalGravityRotationState provides DefaultGravityRotationState
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Full-bleed centered viewfinder flush from top to bottom of the window.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .suppressBackgroundForModal(isModalOpen),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                    viewfinder(Modifier.fillMaxSize())
                }
            }

            // Overlaid foreground chrome padded by live visible systemBars and displayCutout.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(liveSafeInsets)
            ) {
                debugVisibilityWrapper {
                    CompositionLocalProvider(LocalLayoutDirection provides railContainerDirection) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .fillMaxHeight()
                                    .suppressBackgroundForModal(isModalOpen)
                            ) {
                                CompositionLocalProvider(
                                    LocalLayoutDirection provides localeDirection
                                ) {
                                    StartSideRail(
                                        slots = slots,
                                        modifier = Modifier.fillMaxHeight(),
                                        isCompactHeight = isCompactHeight,
                                        isRecording = isRecording,
                                        onDismissQuickSettings = onDismissQuickSettings
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .suppressBackgroundForModal(isModalOpen)
                            ) {
                                CompositionLocalProvider(
                                    LocalLayoutDirection provides localeDirection
                                ) {
                                    EndSideRail(
                                        slots = slots,
                                        modifier = Modifier.fillMaxHeight(),
                                        isCompactHeight = isCompactHeight,
                                        isRecording = isRecording,
                                        showElapsedTimeInRail = false,
                                        onDismissQuickSettings = onDismissQuickSettings
                                    )
                                }
                            }
                        }
                    }

                    CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = topOverlayHorizontalClearance)
                        ) {
                            OverlapAwareCaptureSlot(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(top = chromePadding)
                                    .suppressBackgroundForModal(isQuickSettingsOpen),
                                orientation = Orientation.Horizontal,
                                isCompact = isCompactHeight,
                                isRecording = isRecording,
                                onDismissQuickSettings = onDismissQuickSettings,
                                content = slots.viewfinderTopStart
                            )

                            OverlapAwareCaptureSlot(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = chromePadding)
                                    .suppressBackgroundForModal(isModalOpen),
                                orientation = Orientation.Horizontal,
                                isCompact = isCompactHeight,
                                isRecording = isRecording,
                                onDismissQuickSettings = onDismissQuickSettings,
                                content = slots.viewfinderTopEnd
                            )
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = chromePadding)
                                .suppressBackgroundForModal(isModalOpen),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            PillSnackbarHost(snackbarHostState = snackbarHostState)
                            OverlapAwareCaptureSlot(
                                orientation = Orientation.Horizontal,
                                isCompact = isCompactHeight,
                                isRecording = isRecording,
                                onDismissQuickSettings = onDismissQuickSettings,
                                content = slots.elapsedTimeDisplay
                            )
                        }

                        OverlapAwareCaptureSlot(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = chromePadding)
                                .suppressBackgroundForModal(isModalOpen),
                            orientation = Orientation.Horizontal,
                            isCompact = isCompactHeight,
                            isRecording = isRecording,
                            onDismissQuickSettings = onDismissQuickSettings,
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
                        slots.snackBar(Modifier, snackbarHostState)
                    }
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                debugOverlay(Modifier)
            }

            if (isQuickSettingsOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TwoSideRailTokens.QuickSettingsScrimColor)
                        .testTag(QUICK_SETTINGS_SCRIM)
                        .clickable(
                            onClickLabel = dismissQuickSettingsLabel,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismissQuickSettings
                        )
                )

                val quickSettingsAlignment = if (rtlPolicy.mirrorSideRails) {
                    Alignment.BottomStart
                } else {
                    AbsoluteAlignment.BottomLeft
                }
                val cardTagModifier = if (tagQuickSettingsCard) {
                    Modifier.testTag(QUICK_SETTINGS_BOTTOM_SHEET)
                } else {
                    Modifier
                }
                val cardHeightModifier = if (quickSettingsCardMaxHeight != Dp.Unspecified) {
                    Modifier.heightIn(max = quickSettingsCardMaxHeight)
                } else {
                    Modifier
                }

                CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(liveSafeInsets)
                            .padding(chromePadding)
                    ) {
                        Surface(
                            modifier = Modifier
                                .align(quickSettingsAlignment)
                                .then(cardTagModifier)
                                .widthIn(max = quickSettingsCardMaxWidth)
                                .then(cardHeightModifier)
                                .pointerInput(Unit) {
                                    detectTapGestures { /* Consume card taps */ }
                                },
                            shape = RoundedCornerShape(
                                TwoSideRailTokens.QuickSettingsCardCornerRadius
                            ),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = TwoSideRailTokens.QuickSettingsCardTonalElevation
                        ) {
                            CompositionLocalProvider(
                                LocalLayoutDirection provides localeDirection
                            ) {
                                OverlapAwareCaptureSlot(
                                    orientation = Orientation.Vertical,
                                    isCompact = isCompactHeight,
                                    isRecording = isRecording,
                                    onDismissQuickSettings = onDismissQuickSettings,
                                    content = slots.quickSettingsOverlay
                                )
                            }
                        }
                    }
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides localeDirection) {
                slots.errorDialog(Modifier)
            }

            // Drawn last and outside all inset-padded containers so screen flash covers the
            // entire window, including system bar and display cutout areas.
            screenFlashOverlay(Modifier)
        }
    }
}

@Preview(
    name = "TwoSideRail - Foldable Unfolded (852x883dp)",
    device = "spec:width=852dp,height=883dp",
    showBackground = true
)
@Composable
private fun TwoSideRailFoldablePreview() {
    MaterialTheme {
        TwoSideRailLayout(
            slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true),
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) }
        )
    }
}

@Preview(
    name = "TwoSideRail - Tablet Landscape (1280x800dp)",
    device = "spec:width=1280dp,height=800dp",
    showBackground = true
)
@Composable
private fun TwoSideRailTabletLandscapePreview() {
    MaterialTheme {
        TwoSideRailLayout(
            slots = PlaceholderCaptureSlots.sideRail(
                includePauseSlot = true,
                includeElapsedTime = true
            ),
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
            isRecording = true
        )
    }
}

@Preview(
    name = "TwoSideRail - Tablet Portrait (800x1280dp)",
    device = "spec:width=800dp,height=1280dp",
    showBackground = true
)
@Composable
private fun TwoSideRailTabletPortraitPreview() {
    MaterialTheme {
        TwoSideRailLayout(
            slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true),
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) }
        )
    }
}

@Preview(
    name = "TwoSideRail - RTL Default Policy (Unmirrored Rails)",
    device = "spec:width=1280dp,height=800dp",
    locale = "ar",
    showBackground = true
)
@Composable
private fun TwoSideRailRtlDefaultPolicyPreview() {
    MaterialTheme {
        CompositionLocalProvider(
            LocalCaptureRtlPolicy provides CaptureRtlPolicy(mirrorSideRails = false)
        ) {
            TwoSideRailLayout(
                slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true),
                viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
                isQuickSettingsOpen = true
            )
        }
    }
}

@Preview(
    name = "TwoSideRail - RTL Mirrored Policy",
    device = "spec:width=1280dp,height=800dp",
    locale = "ar",
    showBackground = true
)
@Composable
private fun TwoSideRailRtlMirroredPolicyPreview() {
    MaterialTheme {
        CompositionLocalProvider(
            LocalCaptureRtlPolicy provides CaptureRtlPolicy(mirrorSideRails = true)
        ) {
            TwoSideRailLayout(
                slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true),
                viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
                isQuickSettingsOpen = true
            )
        }
    }
}
