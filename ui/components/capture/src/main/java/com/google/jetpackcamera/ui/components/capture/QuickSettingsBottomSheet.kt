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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.ui.components.capture.quicksettings.QuickSettingsSheetDefaults

/**
 * Dimension constants for the in-hierarchy Quick Settings bottom sheet.
 */
private object QuickSettingsBottomSheetTokens {
    val SheetPeekHeight: Dp = 0.dp
}

/**
 * Hosts [content] inside an in-hierarchy [BottomSheetScaffold] whose sheet shows the Quick
 * Settings [sheetContent], keeping the sheet's expanded/hidden state synchronized with
 * [CaptureScaffoldState.isQuickSettingsOpen] in both directions without spawning a secondary
 * window.
 *
 * Used by the compact and side-rail capture layouts.
 *
 * @param state Mode-agnostic [CaptureScaffoldState] owning Quick Settings visibility and the
 *   snackbar host.
 * @param onDismissQuickSettings Callback invoked when Quick Settings is dismissed through the drag
 *   handle.
 * @param sheetContent Quick Settings content rendered inside the sheet.
 * @param modifier [Modifier] applied to the bottom sheet scaffold.
 * @param content Layout drawn behind the sheet; receives whether the sheet is visible or animating
 *   toward expanded so callers can suppress background chrome and draw a dismiss scrim.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun QuickSettingsBottomSheetScaffold(
    state: CaptureScaffoldState,
    onDismissQuickSettings: () -> Unit,
    sheetContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (isSheetVisible: Boolean) -> Unit
) {
    val currentOnDismissQuickSettings by rememberUpdatedState(onDismissQuickSettings)
    val bottomSheetState = rememberBottomSheetState(
        initialValue = if (state.isQuickSettingsOpen) SheetValue.Expanded else SheetValue.Hidden,
        confirmValueChange = { targetValue ->
            if (targetValue == SheetValue.Hidden) {
                if (state.isQuickSettingsOpen) {
                    state.dismissQuickSettings()
                    currentOnDismissQuickSettings()
                }
            } else if (targetValue == SheetValue.Expanded) {
                state.showQuickSettings()
            }
            true
        }
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState,
        snackbarHostState = state.snackbarHostState
    )
    LaunchedEffect(state.isQuickSettingsOpen) {
        if (state.isQuickSettingsOpen) {
            if (bottomSheetState.targetValue != SheetValue.Expanded) {
                bottomSheetState.expand()
            }
        } else {
            if (bottomSheetState.isVisible || bottomSheetState.targetValue != SheetValue.Hidden) {
                bottomSheetState.hide()
            }
        }
    }

    val isSheetVisible = state.isQuickSettingsOpen ||
        bottomSheetState.isVisible ||
        bottomSheetState.targetValue == SheetValue.Expanded
    val focusManager = LocalFocusManager.current
    val sheetFocusRequester = remember { FocusRequester() }
    var sheetHasFocus by remember { mutableStateOf(false) }
    // Suppression only blocks focus from entering the background, so move focus already there
    // into the sheet on open, and release it on hide so it isn't left on an off-screen sheet.
    LaunchedEffect(isSheetVisible) {
        if (isSheetVisible) {
            if (!sheetFocusRequester.requestFocus()) {
                focusManager.clearFocus(force = true)
            }
        } else if (sheetHasFocus) {
            focusManager.clearFocus(force = true)
        }
    }

    val dismissQuickSettingsLabel =
        stringResource(R.string.quick_settings_btn_close_expanded_settings_description)

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetPeekHeight = QuickSettingsBottomSheetTokens.SheetPeekHeight,
        sheetDragHandle = null,
        sheetShape = QuickSettingsSheetDefaults.BottomSheetShape,
        containerColor = Color.Transparent,
        sheetContainerColor = QuickSettingsSheetDefaults.ContainerColor,
        sheetContentColor = QuickSettingsSheetDefaults.ContentColor,
        sheetShadowElevation = QuickSettingsSheetDefaults.ShadowElevation,
        sheetContent = {
            // Rendered inside sheetContent rather than sheetDragHandle because
            // StandardBottomSheet wraps non-null sheetDragHandle in its own clickable
            // DragHandleWithTooltip, creating duplicate clickable bounds and overriding
            // onDismissQuickSettings.
            BottomSheetDefaults.DragHandle(
                color = QuickSettingsSheetDefaults.DragHandleColor,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag(QUICK_SETTINGS_DRAG_HANDLE)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = dismissQuickSettingsLabel,
                        onClick = {
                            state.dismissQuickSettings()
                            onDismissQuickSettings()
                        }
                    )
                    .semantics {
                        dismiss(dismissQuickSettingsLabel) {
                            state.dismissQuickSettings()
                            onDismissQuickSettings()
                            true
                        }
                    }
                    .minimumInteractiveComponentSize()
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { sheetHasFocus = it.hasFocus }
                    .focusRequester(sheetFocusRequester)
                    .focusGroup()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(
                        WindowInsets.navigationBarsIgnoringVisibility
                            .union(WindowInsets.tappableElement)
                            .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                    )
                    .padding(bottom = QuickSettingsSheetDefaults.BottomSheetContentBottomPadding)
            ) {
                sheetContent()
            }
        },
        snackbarHost = {}
    ) {
        content(isSheetVisible)
    }
}

/**
 * Transparent full-size scrim drawn above the capture chrome while the Quick Settings bottom sheet
 * is open; tapping it invokes [onDismiss].
 */
@Composable
internal fun QuickSettingsSheetScrim(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val dismissQuickSettingsLabel =
        stringResource(R.string.quick_settings_btn_close_expanded_settings_description)
    val quickSettingsScrimDescription =
        stringResource(R.string.quick_settings_scrim_description)
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(QUICK_SETTINGS_SCRIM)
            .semantics {
                contentDescription = quickSettingsScrimDescription
                traversalIndex = 1f
            }
            .clickable(
                onClickLabel = dismissQuickSettingsLabel,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    )
}
