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
package com.google.jetpackcamera.ui.uistate.capture.compound

import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState

/**
 * The UI state for the preview display.
 *
 * @param lastBlinkTimeStamp The timestamp of the most recent capture blink animation.
 * @param aspectRatioUiState The UI state for the aspect ratio of the preview.
 */
data class PreviewDisplayUiState(
    val lastBlinkTimeStamp: Long = 0,
    val aspectRatioUiState: AspectRatioUiState
)
