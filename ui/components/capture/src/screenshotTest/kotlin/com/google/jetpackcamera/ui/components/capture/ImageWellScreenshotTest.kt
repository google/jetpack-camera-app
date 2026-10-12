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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview
@Composable
fun ImageWellScreenshotPreview() {
    ImageWellPreview()
}

@PreviewTest
@Preview
@Composable
fun ImageWellBlack60ScreenshotPreview() {
    CompositionLocalProvider(
        LocalCameraControlBackgroundStyle provides CameraControlBackgroundStyle.BLACK_60
    ) {
        ImageWellPreview()
    }
}

@PreviewTest
@Preview
@Composable
fun DisabledImageWellScreenshotPreview() {
    ImageWellDisabledPreview()
}

@PreviewTest
@Preview
@Composable
fun DisabledImageWellBlack60ScreenshotPreview() {
    CompositionLocalProvider(
        LocalCameraControlBackgroundStyle provides CameraControlBackgroundStyle.BLACK_60
    ) {
        ImageWellDisabledPreview()
    }
}
