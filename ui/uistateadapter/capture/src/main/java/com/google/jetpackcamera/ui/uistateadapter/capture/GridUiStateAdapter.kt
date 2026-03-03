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
package com.google.jetpackcamera.ui.uistateadapter.capture

import com.google.jetpackcamera.model.GridType
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState

/**
 * Creates a [GridType] from the given [TrackedCaptureUiState].
 *
 * This function acts as an adapter to extract the grid-related UI state.
 *
 * @param trackedCaptureUiState The UI state holder.
 * @return The current [GridType].
 */
fun GridType.Companion.from(trackedCaptureUiState: TrackedCaptureUiState): GridType {
    return trackedCaptureUiState.gridType
}
