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
package com.google.jetpackcamera.ui.components.capture.controller

import com.google.jetpackcamera.model.CameraZoomRatio

/**
 * Interface for controlling camera zoom.
 */
interface ZoomController {
    /**
     * Sets the camera's zoom ratio.
     *
     * @param zoomRatio The [CameraZoomRatio] to set.
     */
    fun setZoomRatio(zoomRatio: CameraZoomRatio)

    /**
     * Sets the target value for the zoom animation.
     *
     * @param targetValue The target zoom ratio for the animation, or null to clear it.
     */
    fun setZoomAnimationState(targetValue: Float?)
}
