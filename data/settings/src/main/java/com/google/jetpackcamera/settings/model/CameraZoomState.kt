/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.google.jetpackcamera.settings.model/*
 * Copyright (C) 2024 The Android Open Source Project
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

/**
 * Data classes representing modifications to the camera zoom state.
 */
interface CameraZoomState {
    /**
     * Scale the current camera's active zoom ratio by the [scalingFactor]
     */
    data class ScaleRatio(val scalingFactor: Float) : CameraZoomState

    /**
     * Set the current camera's active zoom based on a linear value between 0.0 and 1.0
     */
    data class Linear(val linearValue: Float) : CameraZoomState {
        init {
            require(linearValue in 0f..1f) { "Linear zoom value must be between 0 and 1" }
        }
    }

    /**
     * Set the current camera's active zoom based on the provided [ratioValue]
     *
     * The range of the supported ratios varies by device and lens. If the ratio value falls outside of these bounds, it must be clamped appropriately within CameraSession
     */
    data class Ratio(val ratioValue: Float) : CameraZoomState
}
