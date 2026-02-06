/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.google.jetpackcamera.ui.uistate

import androidx.compose.material3.SnackbarDuration
import java.util.LinkedList
import java.util.Queue

sealed interface SnackBarUiState {
    data object Disabled : SnackBarUiState
    data class Enabled(
        val snackBarQueue: Queue<SnackbarData> = LinkedList()
    ) : SnackBarUiState

    companion object
}

/**
 * Represents the data for a single snack-bar message.
 *
 * @param cookie A unique identifier for the snack-bar message.
 * @param stringResource The resource ID of the string to be displayed.
 * @param duration The duration for which the snack-bar should be displayed.
 * @param actionLabelRes The resource ID of the action label, if any.
 * @param withDismissAction Whether to show a dismiss action.
 * @param testTag A test tag for UI testing.
 */
data class SnackbarData(
    val cookie: String,
    val stringResource: Int,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val actionLabelRes: Int? = null,
    val withDismissAction: Boolean = false,
    val testTag: String = ""
)
