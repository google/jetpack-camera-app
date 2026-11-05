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

/** Horizontal zoom bar with the first stop selected. */
@PreviewTest
@Preview
@Composable
internal fun ZoomBarHorizontalFirstStopScreenshotPreview() {
    ZoomBarPreview(zoomRatio = 0.5f)
}

/** Horizontal zoom bar with a middle stop selected. */
@PreviewTest
@Preview
@Composable
internal fun ZoomBarHorizontalMiddleStopScreenshotPreview() {
    ZoomBarPreview(zoomRatio = 1.0f)
}

/** Horizontal zoom bar with the last stop selected. */
@PreviewTest
@Preview
@Composable
internal fun ZoomBarHorizontalLastStopScreenshotPreview() {
    ZoomBarPreview(zoomRatio = 5.0f)
}

/** Vertical zoom bar with the first stop selected. */
@PreviewTest
@Preview
@Composable
internal fun ZoomBarVerticalFirstStopScreenshotPreview() {
    ZoomBarPreview(zoomRatio = 0.5f, orientation = Orientation.Vertical)
}

/** Vertical zoom bar with a middle stop selected. */
@PreviewTest
@Preview
@Composable
internal fun ZoomBarVerticalMiddleStopScreenshotPreview() {
    ZoomBarPreview(zoomRatio = 1.0f, orientation = Orientation.Vertical)
}

/** Vertical zoom bar with the last stop selected. */
@PreviewTest
@Preview
@Composable
internal fun ZoomBarVerticalLastStopScreenshotPreview() {
    ZoomBarPreview(zoomRatio = 5.0f, orientation = Orientation.Vertical)
}

/** Zoom bar with a single stop. */
@PreviewTest
@Preview
@Composable
internal fun ZoomBarSingleStopScreenshotPreview() {
    ZoomBarPreview(zoomRatio = 1.0f, zoomLevels = listOf(1.0f), colors = ZoomBarDefaults.colors())
}
