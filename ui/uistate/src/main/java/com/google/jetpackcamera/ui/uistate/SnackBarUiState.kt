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
import androidx.compose.material3.SnackbarVisuals
import java.util.LinkedList
import java.util.Queue

/**
 * A custom implementation of [SnackbarVisuals] used to pass resolved snack-bar data to the UI layer.
 *
 * This class is necessary because [SnackbarVisuals] requires fully resolved [String] values for
 * messages and labels. By contrast, the app's internal [SnackbarData] uses [Int] resource IDs
 * because it appears in the view model (app layer).
 *
 * @property message The resolved text message to display.
 * @property actionLabel The resolved text for the action button, if any.
 * @property withDismissAction Whether to show a dismiss (close) button.
 * @property duration How long the snack-bar should be displayed.
 * @property isError Whether the snack-bar represents an error state, used for custom styling.
 */
data class CustomSnackbarVisuals(
    override val message: String,
    override val actionLabel: String?,
    override val withDismissAction: Boolean,
    override val duration: SnackbarDuration,
    val isError: Boolean
) : SnackbarVisuals

/**
 * A sealed interface representing the UI state for the SnackBar component.
 *
 * This interface defines the possible states for a snack-bar display, which can either be
 * completely disabled or enabled with a queue of messages to show.
 */
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
 * This class uses resource IDs ([Int]) rather than resolved [String] values. This ensures that
 * ViewModels do not need access to the Android `Context` (maintaining architectural separation)
 * and ensures that messages are resolved using the current locale at the moment they are
 * displayed to the user.
 *
 * @param cookie A unique identifier for the snack-bar message.
 * @param stringResource The resource ID of the string to be displayed.
 * @param duration The duration for which the snack-bar should be displayed.
 * @param actionLabelRes The resource ID of the action label, if any.
 * @param withDismissAction Whether to show a dismiss action.
 * @param testTag A test tag for UI testing.
 * @param isError Whether the snack-bar represents an error state.
 */
data class SnackbarData(
    val cookie: String,
    val stringResource: Int,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val actionLabelRes: Int? = null,
    val withDismissAction: Boolean = false,
    val testTag: String = "",
    val isError: Boolean = false
)
