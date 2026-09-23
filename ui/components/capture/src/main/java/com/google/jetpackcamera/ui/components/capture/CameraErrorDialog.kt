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

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.google.jetpackcamera.ui.uistate.capture.CameraErrorUiState

/**
 * Stateless Material 3 alert dialog displaying camera errors.
 *
 * @param cameraErrorUiState The current [CameraErrorUiState].
 * @param onDismissError Callback invoked when a recoverable error dialog is dismissed.
 * @param onExitApp Callback invoked when an unrecoverable error dialog is confirmed/dismissed.
 * @param modifier The [Modifier] to be applied to the dialog.
 */
@Composable
fun CameraErrorDialog(
    cameraErrorUiState: CameraErrorUiState,
    onDismissError: () -> Unit,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (cameraErrorUiState is CameraErrorUiState.Showing) {
        val handleConfirmOrDismiss = {
            if (cameraErrorUiState.shouldExitAppOnConfirm) {
                onExitApp()
            } else {
                onDismissError()
            }
        }

        AlertDialog(
            modifier = modifier.testTag(CAMERA_ERROR_DIALOG_TAG),
            onDismissRequest = handleConfirmOrDismiss,
            properties = DialogProperties(
                dismissOnBackPress = !cameraErrorUiState.shouldExitAppOnConfirm,
                dismissOnClickOutside = !cameraErrorUiState.shouldExitAppOnConfirm
            ),
            title = {
                Text(
                    text = stringResource(cameraErrorUiState.titleResId),
                    modifier = Modifier.testTag(CAMERA_ERROR_DIALOG_TITLE_TAG)
                )
            },
            text = cameraErrorUiState.bodyResId?.let { bodyResId ->
                {
                    Text(
                        text = stringResource(bodyResId),
                        modifier = Modifier.testTag(CAMERA_ERROR_DIALOG_BODY_TAG)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = handleConfirmOrDismiss,
                    modifier = Modifier.testTag(CAMERA_ERROR_DIALOG_CONFIRM_BUTTON_TAG)
                ) {
                    Text(text = stringResource(cameraErrorUiState.confirmButtonTextResId))
                }
            }
        )
    }
}
