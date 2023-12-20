/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.google.jetpackcamera.feature.preview.ui

import android.widget.Toast

/**
 * Helper class containing information used to create a [Toast].
 *
 * @param stringResource the resource ID of to be displayed.
 * @param isLongToast determines if the display time is [Toast.LENGTH_LONG] or [Toast.LENGTH_SHORT].
 * @property testTag the identifiable resource ID of a [ShowToast] on screen.
 */
class ToastMessage(
    val stringResource: Int,
    isLongToast: Boolean = false,
    val testTag: String = ""
) {
    val toastLength: Int = when (isLongToast) {
        true -> Toast.LENGTH_LONG
        false -> Toast.LENGTH_SHORT
    }
}
