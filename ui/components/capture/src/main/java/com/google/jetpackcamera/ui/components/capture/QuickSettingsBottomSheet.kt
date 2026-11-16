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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dimension constants for the in-hierarchy Quick Settings bottom sheet.
 */
private object QuickSettingsBottomSheetTokens {
    val SheetPeekHeight: Dp = 0.dp
    val SheetTopCornerRadius: Dp = 28.dp
}

/**
 * Hosts [content] inside an in-hierarchy [BottomSheetScaffold] whose sheet shows the Quick
 * Settings [sheetContent], keeping the sheet's expanded/hidden state synchronized with
 * [CaptureScaffoldState.isQuickSettingsOpen] in both directions without spawning a secondary
 * window.
 *
 * Used by compact layouts; tablet and foldable side-rail layouts present Quick Settings as a
 * floating card instead.
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickSettingsBottomSheetScaffold(
    state: CaptureScaffoldState,
    onDismissQuickSettings: () -> Unit,
    sheetContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (isSheetVisible: Boolean) -> Unit
) {
    val bottomSheetState = rememberBottomSheetState(
        initialValue = if (state.isQuickSettingsOpen) SheetValue.Expanded else SheetValue.Hidden,
        confirmValueChange = { targetValue ->
            if (targetValue == SheetValue.Hidden) {
                state.dismissQuickSettings()
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

    val dismissQuickSettingsLabel =
        stringResource(R.string.quick_settings_btn_close_expanded_settings_description)

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetPeekHeight = QuickSettingsBottomSheetTokens.SheetPeekHeight,
        sheetDragHandle = {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier
                    .testTag(QUICK_SETTINGS_DRAG_HANDLE)
                    .minimumInteractiveComponentSize()
                    .clickable(
                        role = Role.Button,
                        onClickLabel = dismissQuickSettingsLabel,
                        onClick = {
                            state.dismissQuickSettings()
                            onDismissQuickSettings()
                        }
                    )
            )
        },
        sheetShape = RoundedCornerShape(
            topStart = QuickSettingsBottomSheetTokens.SheetTopCornerRadius,
            topEnd = QuickSettingsBottomSheetTokens.SheetTopCornerRadius
        ),
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        sheetContent = { sheetContent() },
        snackbarHost = {}
    ) {
        val isSheetVisible = state.isQuickSettingsOpen ||
            bottomSheetState.isVisible ||
            bottomSheetState.targetValue == SheetValue.Expanded
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(QUICK_SETTINGS_SCRIM)
            .semantics {
                contentDescription = dismissQuickSettingsLabel
                traversalIndex = 1f
            }
            .clickable(
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    )
}
