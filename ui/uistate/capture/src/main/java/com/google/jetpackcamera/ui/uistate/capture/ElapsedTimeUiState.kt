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
<<<<<<<< HEAD:ui/uistate/capture/src/main/java/com/google/jetpackcamera/ui/uistate/capture/ElapsedTimeUiState.kt
package com.google.jetpackcamera.ui.uistate.viewfinder
========
package com.google.jetpackcamera.ui.uistateadapter.capture
>>>>>>>> david/quickSettingsUiState:ui/uistateadapter/capture/src/main/java/com/google/jetpackcamera/ui/uistateadapter/capture/PreviewMode.kt

sealed interface ElapsedTimeUiState {
    data object Unavailable : ElapsedTimeUiState
    data class Enabled(val elapsedTimeNanos: Long) : ElapsedTimeUiState
}
