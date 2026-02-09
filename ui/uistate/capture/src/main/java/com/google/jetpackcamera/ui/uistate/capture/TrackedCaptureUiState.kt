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
package com.google.jetpackcamera.ui.uistate.capture

import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.ui.uistate.capture.compound.FocusedQuickSetting

/**
 * Data class to track UI-specific states within the PreviewViewModel.
 *
 * This state is managed by the ViewModel and can be thought of as UI configuration
 * or interaction states that might otherwise have been handled by Compose's
 * `remember` if not hoisted to the ViewModel for broader logic integration
 * or persistence. It is then transformed into the `PreviewUiState` that the UI
 * directly observes.
 */
data class TrackedCaptureUiState(
    val isQuickSettingsOpen: Boolean = false,
    val focusedQuickSetting: FocusedQuickSetting = FocusedQuickSetting.NONE,
    val isDebugOverlayOpen: Boolean = false,
    val isRecordingLocked: Boolean = false,
    val zoomAnimationTarget: Float? = null,
    val debugHidingComponents: Boolean = false,
    val recentCapturedMedia: MediaDescriptor = MediaDescriptor.None,
    val lastBlinkTimeStamp: Long = 0
)
