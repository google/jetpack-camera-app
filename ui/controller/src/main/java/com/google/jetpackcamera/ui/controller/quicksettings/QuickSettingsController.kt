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
package com.google.jetpackcamera.ui.controller.quicksettings

import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.GridType
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.ui.uistate.capture.compound.FocusedQuickSetting

/**
 * Interface for controlling quick settings.
 */
interface QuickSettingsController {
    /**
     * Toggles the visibility of the quick settings menu.
     */
    fun toggleQuickSettings()

    /**
     * Sets the currently focused quick setting, used for expanding a setting's options.
     *
     * @param focusedQuickSetting The quick setting to focus.
     */
    fun setFocusedSetting(focusedQuickSetting: FocusedQuickSetting)

    /**
     * Sets the lens facing (e.g., front or back camera).
     *
     * @param lensFace The lens facing to set.
     */
    fun setLensFacing(lensFace: LensFacing)

    /**
     * Sets the flash mode.
     *
     * @param flashMode The flash mode to set.
     */
    fun setFlash(flashMode: FlashMode)

    /**
     * Sets the aspect ratio for capture.
     *
     * @param aspectRatio The aspect ratio to set.
     */
    fun setAspectRatio(aspectRatio: AspectRatio)

    /**
     * Sets the stream configuration (e.g., single stream for performance, multi-stream for flexibility).
     *
     * @param streamConfig The stream configuration to set.
     */
    fun setStreamConfig(streamConfig: StreamConfig)

    /**
     * Sets the dynamic range (e.g., SDR, HDR).
     *
     * @param dynamicRange The dynamic range to set.
     */
    fun setDynamicRange(dynamicRange: DynamicRange)

    /**
     * Sets the image output format (e.g., JPEG, RAW, Ultra HDR).
     *
     * @param imageOutputFormat The image format to set.
     */
    fun setImageFormat(imageOutputFormat: ImageOutputFormat)

    /**
     * Sets the concurrent camera mode.
     *
     * @param concurrentCameraMode The concurrent camera mode to set.
     */
    fun setConcurrentCameraMode(concurrentCameraMode: ConcurrentCameraMode)

    /**
     * Sets the capture mode (e.g., single image, single video, or hybrid).
     *
     * @param captureMode The capture mode to set.
     */
    fun setCaptureMode(captureMode: CaptureMode)

    /**
     * Sets the grid type.
     *
     * @param gridType The grid type to set.
     */
    fun setGridType(gridType: GridType)
}
