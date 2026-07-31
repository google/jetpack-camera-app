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

import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.ui.controller.CameraController

/**
 * A fake implementation of [CameraController] that allows for configuring actions for its methods.
 *
 * @param startCameraAction The action to perform when [startCamera] is called.
 * @param stopCameraAction The action to perform when [stopCamera] is called.
 * @param tapToFocusAction The action to perform when [tapToFocus] is called.
 * @param setDisplayRotationAction The action to perform when [setDisplayRotation] is called.
 */
class FakeCameraController(
    var startCameraAction: () -> Unit = {},
    var stopCameraAction: () -> Unit = {},
    var tapToFocusAction: (x: Float, y: Float) -> Unit = { _, _ -> },
    var setDisplayRotationAction: (DeviceRotation) -> Unit = {}
) : CameraController {
    override fun startCamera() {
        startCameraAction()
    }

    override fun stopCamera() {
        stopCameraAction()
    }

    override fun tapToFocus(x: Float, y: Float) {
        tapToFocusAction(x, y)
    }

    override fun setDisplayRotation(deviceRotation: DeviceRotation) {
        setDisplayRotationAction(deviceRotation)
    }
}
