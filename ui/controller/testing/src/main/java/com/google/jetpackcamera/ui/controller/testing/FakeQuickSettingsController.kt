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
package com.google.jetpackcamera.ui.controller.testing

import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.ui.controller.quicksettings.QuickSettingsController
import com.google.jetpackcamera.ui.uistate.capture.compound.FocusedQuickSetting

/**
 * A fake implementation of [QuickSettingsController] that allows for configuring actions for its methods.
 *
 * @param toggleQuickSettingsAction The action to perform when [toggleQuickSettings] is called.
 * @param setFocusedSettingAction The action to perform when [setFocusedSetting] is called.
 * @param setLensFacingAction The action to perform when [setLensFacing] is called.
 * @param setFlashAction The action to perform when [setFlash] is called.
 * @param setAspectRatioAction The action to perform when [setAspectRatio] is called.
 * @param setStreamConfigAction The action to perform when [setStreamConfig] is called.
 * @param setDynamicRangeAction The action to perform when [setDynamicRange] is called.
 * @param setImageFormatAction The action to perform when [setImageFormat] is called.
 * @param setConcurrentCameraModeAction The action to perform when [setConcurrentCameraMode] is called.
 * @param setCaptureModeAction The action to perform when [setCaptureMode] is called.
 */
class FakeQuickSettingsController(
    var toggleQuickSettingsAction: () -> Unit = {},
    var setFocusedSettingAction: (FocusedQuickSetting) -> Unit = {},
    var setLensFacingAction: (LensFacing) -> Unit = {},
    var setFlashAction: (FlashMode) -> Unit = {},
    var setAspectRatioAction: (AspectRatio) -> Unit = {},
    var setStreamConfigAction: (StreamConfig) -> Unit = {},
    var setDynamicRangeAction: (DynamicRange) -> Unit = {},
    var setImageFormatAction: (ImageOutputFormat) -> Unit = {},
    var setConcurrentCameraModeAction: (ConcurrentCameraMode) -> Unit = {},
    var setCaptureModeAction: (CaptureMode) -> Unit = {}
) : QuickSettingsController {
    override fun toggleQuickSettings() {
        toggleQuickSettingsAction()
    }

    override fun setFocusedSetting(focusedQuickSetting: FocusedQuickSetting) {
        setFocusedSettingAction(focusedQuickSetting)
    }

    override fun setLensFacing(lensFace: LensFacing) {
        setLensFacingAction(lensFace)
    }

    override fun setFlash(flashMode: FlashMode) {
        setFlashAction(flashMode)
    }

    override fun setAspectRatio(aspectRatio: AspectRatio) {
        setAspectRatioAction(aspectRatio)
    }

    override fun setStreamConfig(streamConfig: StreamConfig) {
        setStreamConfigAction(streamConfig)
    }

    override fun setDynamicRange(dynamicRange: DynamicRange) {
        setDynamicRangeAction(dynamicRange)
    }

    override fun setImageFormat(imageOutputFormat: ImageOutputFormat) {
        setImageFormatAction(imageOutputFormat)
    }

    override fun setConcurrentCameraMode(concurrentCameraMode: ConcurrentCameraMode) {
        setConcurrentCameraModeAction(concurrentCameraMode)
    }

    override fun setCaptureMode(captureMode: CaptureMode) {
        setCaptureModeAction(captureMode)
    }
}
