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

import com.google.jetpackcamera.ui.controller.SnackBarController
import com.google.jetpackcamera.ui.uistate.DisableRationale
import com.google.jetpackcamera.ui.uistate.SnackbarData

/**
 * A fake implementation of [SnackBarController] that allows for configuring actions for its methods.
 *
 * @param enqueueDisabledHdrToggleSnackBarAction The action to perform when [enqueueDisabledHdrToggleSnackBar] is called.
 * @param onSnackBarResultAction The action to perform when [onSnackBarResult] is called.
 * @param incrementAndGetSnackBarCountAction The action to perform when [incrementAndGetSnackBarCount] is called.
 * @param addSnackBarDataAction The action to perform when [addSnackBarData] is called.
 */
class FakeSnackBarController(
    var enqueueDisabledHdrToggleSnackBarAction: (DisableRationale) -> Unit = {},
    var onSnackBarResultAction: (String) -> Unit = {},
    var incrementAndGetSnackBarCountAction: () -> Int = { 0 },
    var addSnackBarDataAction: (SnackbarData) -> Unit = {}
) : SnackBarController {
    override fun enqueueDisabledHdrToggleSnackBar(disabledReason: DisableRationale) {
        enqueueDisabledHdrToggleSnackBarAction(disabledReason)
    }

    override fun onSnackBarResult(cookie: String) {
        onSnackBarResultAction(cookie)
    }

    override fun incrementAndGetSnackBarCount(): Int {
        return incrementAndGetSnackBarCountAction()
    }

    override fun addSnackBarData(snackBarData: SnackbarData) {
        addSnackBarDataAction(snackBarData)
    }
}
