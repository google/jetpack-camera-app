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
package com.google.jetpackcamera.ui.components.capture.quicksettings.controller

import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.ui.uistate.capture.compound.FocusedQuickSetting

interface QuickSettingsController {
    fun toggleQuickSettings()
    fun setFocusedSetting(focusedQuickSetting: FocusedQuickSetting)
    fun setLensFacing(lensFace: LensFacing)
    fun setFlash(flashMode: FlashMode)
    fun setAspectRatio(aspectRatio: AspectRatio)
    fun setStreamConfig(streamConfig: StreamConfig)
    fun setDynamicRange(dynamicRange: DynamicRange)
    fun setImageFormat(imageOutputFormat: ImageOutputFormat)
    fun setConcurrentCameraMode(concurrentCameraMode: ConcurrentCameraMode)
    fun setCaptureMode(captureMode: CaptureMode)
}
