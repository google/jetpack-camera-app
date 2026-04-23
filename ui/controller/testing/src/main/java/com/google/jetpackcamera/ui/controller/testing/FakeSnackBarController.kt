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
import java.util.concurrent.atomic.AtomicInteger

/**
 * A fake implementation of [SnackBarController] that allows for configuring actions for its methods.
 *
 * @param onSnackBarResultAction The action to perform when [onSnackBarResult] is called.
 * @param incrementAndGetSnackBarCountAction The action to perform when [incrementAndGetSnackBarCount] is called.
 * @param addSnackBarDataAction The action to perform when [addSnackBarData] is called.
 */
class FakeSnackBarController(
    var onSnackBarResultAction: (String) -> Unit = {},
    var incrementAndGetSnackBarCountAction: (() -> Int)? = null,
    var addSnackBarDataAction: (SnackbarData) -> Unit = {}
) : SnackBarController {
    private val snackBarCount by lazy { AtomicInteger(0) }

    override fun enqueueDisabledHdrToggleSnackBar(disabledReason: DisableRationale) {
        val cookieInt = incrementAndGetSnackBarCount()
        val cookie = "DisabledHdrToggle-$cookieInt"
        addSnackBarData(
            SnackbarData(
                cookie = cookie,
                stringResource = disabledReason.reasonTextResId,
                withDismissAction = true,
                testTag = disabledReason.testTag
            )
        )
    }

    override fun onSnackBarResult(cookie: String) {
        onSnackBarResultAction(cookie)
    }

    override fun incrementAndGetSnackBarCount(): Int {
        return incrementAndGetSnackBarCountAction?.invoke() ?: snackBarCount.incrementAndGet()
    }

    override fun addSnackBarData(snackBarData: SnackbarData) {
        addSnackBarDataAction(snackBarData)
    }
}
