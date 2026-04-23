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

import com.google.jetpackcamera.model.TestPattern
import com.google.jetpackcamera.ui.controller.debug.DebugController

/**
 * A fake implementation of [DebugController] that allows for configuring actions for its methods.
 *
 * @param toggleDebugHidingComponentsAction The action to perform when [toggleDebugHidingComponents] is called.
 * @param toggleDebugOverlayAction The action to perform when [toggleDebugOverlay] is called.
 * @param setTestPatternAction The action to perform when [setTestPattern] is called.
 */
class FakeDebugController(
    var toggleDebugHidingComponentsAction: () -> Unit = {},
    var toggleDebugOverlayAction: () -> Unit = {},
    var setTestPatternAction: (TestPattern) -> Unit = {}
) : DebugController {
    override fun toggleDebugHidingComponents() {
        toggleDebugHidingComponentsAction()
    }

    override fun toggleDebugOverlay() {
        toggleDebugOverlayAction()
    }

    override fun setTestPattern(testPattern: TestPattern) {
        setTestPatternAction(testPattern)
    }
}
