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
package com.google.jetpackcamera.ui.components.capture.quicksettings

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_OVERLAY_CARD_TAG
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_SCRIM
import com.google.jetpackcamera.ui.components.capture.R
import com.google.jetpackcamera.ui.controller.quicksettings.QuickSettingsController
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState

private object QuickSettingsOverlayCardTokens {
    val MaxCardWidth = 412.dp
    val FoldableMaxCardWidth = 380.dp
    val OuterPadding = 16.dp
    val ContentVerticalPadding = 20.dp
    val ScrimAlpha = 0.32f
}

/**
 * In-hierarchy floating card overlay for Quick Settings on adaptive medium and expanded layouts.
 *
 * Renders nothing when [isOpen] is `false` or [quickSettingsUiState] is not
 * [QuickSettingsUiState.Available]. While open, intercepts system back and unhandled hardware
 * [Key.Escape] presses, renders a full-screen scrim that dismisses on tap while blocking touch
 * pass-through to underlying camera controls, and displays [QuickSettingsContent] inside a
 * vertically scrollable Material 3 [Surface] tagged with [QUICK_SETTINGS_OVERLAY_CARD_TAG] (while
 * [QuickSettingsContent] retains `QUICK_SETTINGS_BOTTOM_SHEET` until a shared
 * `QUICK_SETTINGS_CONTENT_TAG` is split out).
 *
 * @param isOpen Whether the Quick Settings overlay card is currently open.
 * @param onDismissRequest Callback invoked when the user taps the scrim or presses back/escape.
 * @param quickSettingsUiState The current [QuickSettingsUiState].
 * @param quickSettingsController The [QuickSettingsController] to handle setting changes.
 * @param modifier The [Modifier] applied to the full-size overlay container.
 * @param onNavigateToSettings Callback invoked when the "More settings" button is clicked.
 * @param showMoreSettingsButton Whether to display the "More settings" navigation button inside
 *   the card. Defaults to `false` for adaptive layouts that surface settings in persistent chrome.
 * @param contentAlignment Alignment of the floating card within the overlay container.
 *   Defaults to [AbsoluteAlignment.BottomLeft] so the card's container placement remains static
 *   across LTR and RTL layouts while interior controls mirror for RTL.
 * @param cardPadding Padding around the floating card surface (used by side-rail layouts to clear
 *   measured start and end rail widths without shifting the full-screen scrim).
 * @param maxCardWidth Maximum width cap applied to the floating card surface.
 * @param maxCardHeight Optional maximum height cap applied to the floating card surface.
 * @param cardContainerDirection Layout direction used to resolve [contentAlignment] and
 *   [cardPadding] outside the card surface while restoring the caller's `LocalLayoutDirection`
 *   inside the card content.
 */
@Composable
fun QuickSettingsOverlayCard(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    quickSettingsUiState: QuickSettingsUiState,
    quickSettingsController: QuickSettingsController,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    showMoreSettingsButton: Boolean = false,
    contentAlignment: Alignment = AbsoluteAlignment.BottomLeft,
    cardPadding: PaddingValues = PaddingValues(QuickSettingsOverlayCardTokens.OuterPadding),
    maxCardWidth: Dp = QuickSettingsOverlayCardTokens.MaxCardWidth,
    maxCardHeight: Dp = Dp.Unspecified,
    cardContainerDirection: LayoutDirection = LocalLayoutDirection.current
) {
    if (!isOpen || quickSettingsUiState !is QuickSettingsUiState.Available) {
        return
    }

    QuickSettingsOverlayCard(
        isOpen = true,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        paneTitle = stringResource(quickSettingsTitleRes(quickSettingsUiState)),
        contentAlignment = contentAlignment,
        cardPadding = cardPadding,
        maxCardWidth = maxCardWidth,
        maxCardHeight = maxCardHeight,
        cardContainerDirection = cardContainerDirection
    ) {
        QuickSettingsContent(
            quickSettingsUiState = quickSettingsUiState,
            quickSettingsController = quickSettingsController,
            onNavigateToSettings = onNavigateToSettings,
            showMoreSettingsButton = showMoreSettingsButton
        )
    }
}

/**
 * Slot-based overload of [QuickSettingsOverlayCard] hosting arbitrary Quick Settings [content]
 * (such as `CaptureScaffoldDefaults.QuickSettings`) inside the in-hierarchy floating card, scrim,
 * focus group, and escape/back handlers.
 *
 * Renders nothing when [isOpen] is `false`.
 *
 * @param isOpen Whether the Quick Settings overlay card is currently open.
 * @param onDismissRequest Callback invoked when the user taps the scrim or presses back/escape.
 * @param modifier The [Modifier] applied to the full-size overlay container.
 * @param paneTitle Accessibility pane title announced for the floating card surface.
 * @param contentAlignment Alignment of the floating card within the overlay container.
 *   Defaults to [AbsoluteAlignment.BottomLeft] so the card's container placement remains static
 *   across LTR and RTL layouts while interior controls mirror for RTL.
 * @param cardPadding Padding around the floating card surface (used by side-rail layouts to clear
 *   measured start and end rail widths without shifting the full-screen scrim).
 * @param maxCardWidth Maximum width cap applied to the floating card surface.
 * @param maxCardHeight Optional maximum height cap applied to the floating card surface.
 * @param cardContainerDirection Layout direction used to resolve [contentAlignment] and
 *   [cardPadding] outside the card surface while restoring the caller's `LocalLayoutDirection`
 *   inside the card content.
 * @param content Composable Quick Settings content rendered inside the scrollable card surface.
 */
@Composable
fun QuickSettingsOverlayCard(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    paneTitle: String = stringResource(R.string.quick_settings_title_photo_and_video_settings),
    contentAlignment: Alignment = AbsoluteAlignment.BottomLeft,
    cardPadding: PaddingValues = PaddingValues(QuickSettingsOverlayCardTokens.OuterPadding),
    maxCardWidth: Dp = QuickSettingsOverlayCardTokens.MaxCardWidth,
    maxCardHeight: Dp = Dp.Unspecified,
    cardContainerDirection: LayoutDirection = LocalLayoutDirection.current,
    content: @Composable () -> Unit
) {
    if (!isOpen) {
        return
    }

    BackHandler(enabled = true, onBack = onDismissRequest)

    val contentLayoutDirection = LocalLayoutDirection.current
    val dismissDescription =
        stringResource(R.string.quick_settings_btn_close_expanded_settings_description)
    val inputModeManager = LocalInputModeManager.current
    val overlayFocusRequester = remember { FocusRequester() }
    val cardFocusRequester = remember { FocusRequester() }
    val scrimFocusRequester = remember { FocusRequester() }
    var hasCardFocus by remember { mutableStateOf(false) }
    var hasScrimFocus by remember { mutableStateOf(false) }

    LaunchedEffect(inputModeManager.inputMode) {
        if (!cardFocusRequester.requestFocus() && !scrimFocusRequester.requestFocus()) {
            overlayFocusRequester.requestFocus()
        }
    }

    val heightCapModifier = if (maxCardHeight != Dp.Unspecified) {
        Modifier.heightIn(max = maxCardHeight)
    } else {
        Modifier
    }

    CompositionLocalProvider(LocalLayoutDirection provides cardContainerDirection) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .onKeyEvent { event ->
                    if (event.key == Key.Escape) {
                        if (event.type == KeyEventType.KeyUp) {
                            onDismissRequest()
                        }
                        event.type == KeyEventType.KeyDown || event.type == KeyEventType.KeyUp
                    } else {
                        false
                    }
                }
                .focusRequester(overlayFocusRequester)
                .focusProperties { canFocus = !hasCardFocus && !hasScrimFocus }
                .focusTarget()
                .semantics { isTraversalGroup = true },
            contentAlignment = contentAlignment
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        MaterialTheme.colorScheme.scrim.copy(
                            alpha = QuickSettingsOverlayCardTokens.ScrimAlpha
                        )
                    )
                    .testTag(QUICK_SETTINGS_SCRIM)
                    .semantics {
                        contentDescription = dismissDescription
                        traversalIndex = 1f
                    }
                    .onFocusChanged { hasScrimFocus = it.hasFocus }
                    .focusRequester(scrimFocusRequester)
                    .clickable(
                        role = Role.Button,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest
                    )
            )

            // Container tag reserved for the floating card surface (see TestTags.kt note on
            // QUICK_SETTINGS_BOTTOM_SHEET vs. QUICK_SETTINGS_OVERLAY_CARD_TAG vs. future
            // QUICK_SETTINGS_CONTENT_TAG).
            Surface(
                modifier = Modifier
                    .padding(cardPadding)
                    .widthIn(max = maxCardWidth)
                    .then(heightCapModifier)
                    .onFocusChanged { hasCardFocus = it.hasFocus }
                    .focusRequester(cardFocusRequester)
                    .focusGroup()
                    .semantics {
                        this.paneTitle = paneTitle
                        traversalIndex = 0f
                    }
                    .testTag(QUICK_SETTINGS_OVERLAY_CARD_TAG),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides contentLayoutDirection) {
                    Box(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(
                                vertical = QuickSettingsOverlayCardTokens.ContentVerticalPadding
                            )
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSettingsOverlayCardPreviewContent(
    showMoreSettingsButton: Boolean = false,
    cardPadding: PaddingValues = PaddingValues(QuickSettingsOverlayCardTokens.OuterPadding),
    maxCardWidth: Dp = QuickSettingsOverlayCardTokens.MaxCardWidth
) {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        QuickSettingsOverlayCard(
            isOpen = true,
            onDismissRequest = {},
            quickSettingsUiState = QuickSettingsUiState.Available(
                aspectRatioUiState = AspectRatioUiState.Available(
                    selectedAspectRatio = AspectRatio.NINE_SIXTEEN,
                    availableAspectRatios = listOf(
                        SingleSelectableUiState.SelectableUi(AspectRatio.NINE_SIXTEEN),
                        SingleSelectableUiState.SelectableUi(AspectRatio.THREE_FOUR),
                        SingleSelectableUiState.SelectableUi(AspectRatio.ONE_ONE)
                    )
                ),
                captureModeUiState = CaptureModeUiState.Available(
                    selectedCaptureMode = CaptureMode.STANDARD,
                    availableCaptureModes = listOf(
                        SingleSelectableUiState.SelectableUi(CaptureMode.STANDARD),
                        SingleSelectableUiState.SelectableUi(CaptureMode.VIDEO_ONLY),
                        SingleSelectableUiState.SelectableUi(CaptureMode.IMAGE_ONLY)
                    )
                ),
                flashModeUiState = FlashModeUiState.Available(
                    selectedFlashMode = FlashMode.OFF,
                    availableFlashModes = listOf(
                        SingleSelectableUiState.SelectableUi(FlashMode.OFF),
                        SingleSelectableUiState.SelectableUi(FlashMode.ON),
                        SingleSelectableUiState.SelectableUi(FlashMode.AUTO)
                    ),
                    isLowLightBoostActive = false
                ),
                flipLensUiState = FlipLensUiState.Available(
                    selectedLensFacing = LensFacing.BACK,
                    availableLensFacings = listOf(
                        SingleSelectableUiState.SelectableUi(LensFacing.BACK),
                        SingleSelectableUiState.SelectableUi(LensFacing.FRONT)
                    )
                ),
                hdrUiState = HdrUiState.Available(
                    selectedImageFormat = ImageOutputFormat.JPEG,
                    selectedDynamicRange = DynamicRange.SDR,
                    isSupported = true
                )
            ),
            quickSettingsController = NoOpQuickSettingsController(),
            onNavigateToSettings = {},
            showMoreSettingsButton = showMoreSettingsButton,
            cardPadding = cardPadding,
            maxCardWidth = maxCardWidth
        )
    }
}

@Preview(
    name = "LTR - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "RTL - Dark",
    locale = "ar",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "CompactShort (360dp height)",
    heightDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun QuickSettingsOverlayCardPreview() {
    QuickSettingsOverlayCardPreviewContent(showMoreSettingsButton = false)
}

@Preview(
    name = "Foldable Rail-Cleared (852x883dp)",
    widthDp = 852,
    heightDp = 883,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun QuickSettingsOverlayCardFoldablePreview() {
    QuickSettingsOverlayCardPreviewContent(
        showMoreSettingsButton = true,
        cardPadding = PaddingValues(start = 92.dp, end = 92.dp, top = 24.dp, bottom = 24.dp),
        maxCardWidth = QuickSettingsOverlayCardTokens.FoldableMaxCardWidth
    )
}

@Preview(
    name = "Tablet Rail-Cleared (1280x800dp)",
    widthDp = 1280,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun QuickSettingsOverlayCardTabletPreview() {
    QuickSettingsOverlayCardPreviewContent(
        showMoreSettingsButton = true,
        cardPadding = PaddingValues(start = 100.dp, end = 100.dp, top = 24.dp, bottom = 28.dp),
        maxCardWidth = QuickSettingsOverlayCardTokens.MaxCardWidth
    )
}

@Preview(
    name = "With More Settings - LTR - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "With More Settings - RTL - Dark",
    locale = "ar",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun QuickSettingsOverlayCardWithMoreSettingsPreview() {
    QuickSettingsOverlayCardPreviewContent(showMoreSettingsButton = true)
}
