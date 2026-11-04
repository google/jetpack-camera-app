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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview
@Composable
private fun ZoomBarHorizontalFirstStopScreenshotPreview() {
    ZoomBarPreview(zoomRatio = 0.5f)
}

@PreviewTest
@Preview
@Composable
private fun ZoomBarHorizontalMiddleStopScreenshotPreview() {
    ZoomBarPreview(zoomRatio = 1.0f)
}

@PreviewTest
@Preview
@Composable
private fun ZoomBarHorizontalLastStopScreenshotPreview() {
    ZoomBarPreview(zoomRatio = 5.0f)
}

@PreviewTest
@Preview
@Composable
private fun ZoomBarVerticalFirstStopScreenshotPreview() {
    ZoomBarPreview(zoomRatio = 0.5f, orientation = Orientation.Vertical)
}

@PreviewTest
@Preview
@Composable
private fun ZoomBarVerticalMiddleStopScreenshotPreview() {
    ZoomBarPreview(zoomRatio = 1.0f, orientation = Orientation.Vertical)
}

@PreviewTest
@Preview
@Composable
private fun ZoomBarVerticalLastStopScreenshotPreview() {
    ZoomBarPreview(zoomRatio = 5.0f, orientation = Orientation.Vertical)
}

@PreviewTest
@Preview
@Composable
private fun ZoomBarSingleStopScreenshotPreview() {
    ZoomBarPreview(zoomRatio = 1.0f, zoomLevels = listOf(1.0f), colors = ZoomBarDefaults.colors())
}
