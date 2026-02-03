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
package com.google.jetpackcamera.ui.uistate.capture

import android.util.Size
import com.google.jetpackcamera.model.TestPattern

/**
 * Defines the UI state for the debug information overlay.
 *
 * This sealed interface represents all possible states of the debug UI. It can be disabled,
 * enabled but closed, or enabled and open with detailed camera information.
 */
sealed interface DebugUiState {
    /**
     * The debug overlay is not available and should not be rendered.
     * This is the default state when debug mode is turned off in the settings.
     */
    data object Disabled : DebugUiState

    /**
     * The debug overlay is available but may be open or closed.
     * This state is active when debug mode is enabled in the settings.
     *
     * @property currentPhysicalCameraId The ID of the currently active physical camera sensor. This
     *   can be different from the logical camera ID in a multi-camera system.
     * @property currentLogicalCameraId The ID of the currently active logical camera.
     * @property currentPrimaryZoomRatio The current zoom ratio of the primary camera.
     * @property debugHidingComponents Indicates whether UI components are being hidden for debugging
     *   purposes, allowing an unobstructed view of the camera feed.
     */
    sealed interface Enabled : DebugUiState {
        val currentPhysicalCameraId: String?
        val currentLogicalCameraId: String?
        val currentPrimaryZoomRatio: Float?
        val debugHidingComponents: Boolean

        /**
         * The debug overlay is enabled but currently closed.
         * The user can open it to see detailed information.
         */
        data class Closed(
            override val currentPhysicalCameraId: String? = null,
            override val currentLogicalCameraId: String? = null,
            override val currentPrimaryZoomRatio: Float?,
            override val debugHidingComponents: Boolean = false
        ) : Enabled {
            companion object
        }

        /**
         * The debug overlay is open, displaying detailed camera and system information.
         *
         * @property cameraPropertiesJSON A JSON string containing detailed properties of the
         *   current camera device for inspection.
         * @property videoResolution The resolution of the current video stream, if applicable.
         * @property selectedTestPattern The currently active test pattern being displayed on the
         *   preview.
         * @property availableTestPatterns The set of test patterns supported by the current camera
         *   device.
         */
        data class Open(
            override val currentPhysicalCameraId: String? = null,
            override val currentLogicalCameraId: String? = null,
            override val currentPrimaryZoomRatio: Float?,
            override val debugHidingComponents: Boolean = false,
            val cameraPropertiesJSON: String = "",
            val videoResolution: Size? = null,
            val selectedTestPattern: TestPattern = TestPattern.Off,
            val availableTestPatterns: Set<TestPattern> = setOf(TestPattern.Off)
        ) : Enabled {
            companion object
        }
    }
}
