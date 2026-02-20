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

import android.util.Log
import com.google.jetpackcamera.ui.uistate.DisableRationale
import com.google.jetpackcamera.ui.uistate.capture.SnackBarUiState
import com.google.jetpackcamera.ui.uistate.capture.SnackbarData
import java.util.LinkedList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * This file contains the data class [SnackBarCallbacks] and helper functions to create it.
 * [SnackBarCallbacks] is used to handle UI events related to snack bars.
 */

private const val TAG = "SnackBarCallbacks"

/**
 * Data class holding callbacks for snack bar UI events.
 *
 * @param enqueueDisabledHdrToggleSnackBar Enqueues a snack bar to inform the user that HDR is
 *   disabled.
 * @param onSnackBarResult Handles the result of a snack bar action.
 */
data class SnackBarCallbacks(
    val enqueueDisabledHdrToggleSnackBar: (DisableRationale) -> Unit = {},
    val onSnackBarResult: (String) -> Unit = {}
)

/**
 * Creates a [SnackBarCallbacks] instance.
 *
 * @param incrementSnackBarCount A function to increment the snack bar count.
 * @param viewModelScope The [CoroutineScope] for launching coroutines.
 * @param snackBarUiState The mutable state flow for the snack bar UI state.
 * @return An instance of [SnackBarCallbacks].
 */
fun getSnackBarCallbacks(
    incrementSnackBarCount: () -> Int = { 0 },
    viewModelScope: CoroutineScope,
    snackBarUiState: MutableStateFlow<SnackBarUiState.Enabled>
): SnackBarCallbacks {
    return SnackBarCallbacks(
        enqueueDisabledHdrToggleSnackBar = { disabledReason ->
            val cookieInt = incrementSnackBarCount()
            val cookie = "DisabledHdrToggle-$cookieInt"
            addSnackBarData(
                viewModelScope,
                snackBarUiState,
                SnackbarData(
                    cookie = cookie,
                    stringResource = disabledReason.reasonTextResId,
                    withDismissAction = true,
                    testTag = disabledReason.testTag
                )
            )
        },
        onSnackBarResult = { cookie ->
            viewModelScope.launch {
                snackBarUiState.update { old ->
                    val newQueue = LinkedList(old.snackBarQueue)
                    val snackBarData = newQueue.poll()
                    if (snackBarData != null && snackBarData.cookie == cookie) {
                        // If the latest snackBar had a result, then clear snackBarToShow
                        Log.d(TAG, "SnackBar removed. Queue size: ${newQueue.size}")
                        old.copy(
                            snackBarQueue = newQueue
                        )
                    } else {
                        old
                    }
                }
            }
        }
    )
}

/**
 * Adds a [SnackbarData] to the snack bar queue.
 *
 * @param viewModelScope The [CoroutineScope] for launching coroutines.
 * @param snackBarUiState The mutable state flow for the snack bar UI state.
 * @param snackBarData The data for the snack bar to be added.
 */
fun addSnackBarData(
    viewModelScope: CoroutineScope,
    snackBarUiState: MutableStateFlow<SnackBarUiState.Enabled>,
    snackBarData: SnackbarData
) {
    viewModelScope.launch {
        snackBarUiState.update { old ->
            val newQueue = LinkedList(old.snackBarQueue)
            newQueue.add(snackBarData)
            Log.d(TAG, "SnackBar added. Queue size: ${newQueue.size}")
            old.copy(
                snackBarQueue = newQueue
            )
        }
    }
}
