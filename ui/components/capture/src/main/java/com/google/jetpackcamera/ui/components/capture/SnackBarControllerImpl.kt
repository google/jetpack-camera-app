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
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "SnackBarControllerImpl"

class SnackBarControllerImpl(
    private val viewModelScope: CoroutineScope,
    private val snackBarUiState: MutableStateFlow<SnackBarUiState.Enabled>
) : SnackBarController {
    val snackBarCount = atomic(0)
    override fun enqueueDisabledHdrToggleSnackBar(disabledReason: DisableRationale) {
        val cookieInt = incrementAndGetSnackBarCount()
        val cookie = "DisabledHdrToggle-$cookieInt"
        addSnackBarData(
            SnackbarData(
                cookie = cookie,
                stringResource = disabledReason.reasonTextResId,
                withDismissAction = true,
                testTag = disabledReason.testTag
            )
        )
    }

    override fun onSnackBarResult(cookie: String) {
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

    override fun incrementAndGetSnackBarCount(): Int {
        return snackBarCount.incrementAndGet()
    }

    override fun addSnackBarData(snackBarData: SnackbarData) {
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
}
