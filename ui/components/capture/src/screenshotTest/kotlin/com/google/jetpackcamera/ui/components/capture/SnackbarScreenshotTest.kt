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
package com.google.jetpackcamera.ui.components.capture

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview
@Composable
fun PillSnackbarErrorMessageOnlyScreenshotPreview() {
    PillSnackbarErrorMessageOnlyPreview()
}

@PreviewTest
@Preview
@Composable
fun PillSnackbarErrorWithDismissScreenshotPreview() {
    PillSnackbarErrorWithDismissPreview()
}

@PreviewTest
@Preview
@Composable
fun PillSnackbarErrorWithActionScreenshotPreview() {
    PillSnackbarErrorWithActionPreview()
}

@PreviewTest
@Preview
@Composable
fun PillSnackbarInfoWithDismissScreenshotPreview() {
    PillSnackbarInfoWithDismissPreview()
}

@PreviewTest
@Preview(widthDp = 412)
@Composable
fun PillSnackbarLongMessageScreenshotPreview() {
    PillSnackbarLongMessagePreview()
}

@PreviewTest
@Preview(locale = "ar")
@Composable
fun PillSnackbarRtlScreenshotPreview() {
    PillSnackbarRtlPreview()
}

@PreviewTest
@Preview(fontScale = 2f, widthDp = 412)
@Composable
fun PillSnackbarLargeFontScreenshotPreview() {
    PillSnackbarLargeFontPreview()
}
