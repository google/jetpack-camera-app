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
package com.google.jetpackcamera.ui.controller

import com.google.jetpackcamera.ui.uistate.DisableRationale
import com.google.jetpackcamera.ui.uistate.SnackbarData

/**
 * Interface for managing and displaying snackbar messages.
 */
interface SnackBarController {
    /**
     * Enqueues a snackbar to inform the user why a feature (like HDR) is disabled.
     */
    fun enqueueDisabledHdrToggleSnackBar(disabledReason: DisableRationale)

    /**
     * Handles the result of a snackbar being dismissed or its action being performed.
     */
    fun onSnackBarResult(cookie: String)

    /**
     * Atomically increments and returns the snackbar count, used for generating unique cookies.
     */
    fun incrementAndGetSnackBarCount(): Int

    /**
     * Adds a new [SnackbarData] to the queue to be displayed.
     */
    fun addSnackBarData(snackBarData: SnackbarData)
}
