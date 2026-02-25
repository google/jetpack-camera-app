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
package com.google.jetpackcamera.ui.components.capture.debug.controller

import com.google.jetpackcamera.model.TestPattern

/**
 * This file contains the [DebugController] interface for managing debug actions.
 */

/**
 * Interface for controlling debug features.
 */
interface DebugController {
    /**
     * Toggles the visibility of debug UI components.
     */
    fun toggleDebugHidingComponents()

    /**
     * Toggles the debug overlay.
     */
    fun toggleDebugOverlay()

    /**
     * Sets the test pattern for the camera.
     *
     * @param testPattern The test pattern to set.
     */
    fun setTestPattern(testPattern: TestPattern)
}
