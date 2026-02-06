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

package com.google.jetpackcamera.ui.uistateadapter

import com.google.jetpackcamera.ui.uistate.SnackBarUiState
import com.google.jetpackcamera.ui.uistate.SnackbarData
import java.util.Queue

/**
 * Creates a [SnackBarUiState] from a queue of [SnackbarData].
 *
 * @param snackBarQueue The queue of snackbar messages to be displayed.
 * @return A [SnackBarUiState] instance containing the provided queue.
 */
fun SnackBarUiState.Companion.from(snackBarQueue: Queue<SnackbarData>): SnackBarUiState {
    return SnackBarUiState.Enabled(snackBarQueue)
}
