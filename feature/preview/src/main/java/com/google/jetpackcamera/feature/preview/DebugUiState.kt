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
package com.google.jetpackcamera.feature.preview

import android.util.Size
import com.google.jetpackcamera.settings.model.TestPattern

sealed interface DebugUiState {
    data object Disabled : DebugUiState

    sealed interface Enabled : DebugUiState

    data object Closed : Enabled

    data class Open(
        val cameraPropertiesJSON: String = "",
        val videoResolution: Size? = null,
        val selectedTestPattern: TestPattern = TestPattern.Off,
        val availableTestPatterns: Set<TestPattern> = setOf(TestPattern.Off)
    ) : Enabled
}
