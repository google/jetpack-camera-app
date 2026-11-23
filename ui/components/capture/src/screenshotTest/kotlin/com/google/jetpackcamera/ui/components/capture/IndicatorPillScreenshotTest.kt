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

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.android.tools.screenshot.PreviewTest
import com.google.jetpackcamera.model.DEFAULT_HDR_DYNAMIC_RANGE
import com.google.jetpackcamera.model.DEFAULT_HDR_IMAGE_OUTPUT
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.FlashModeIndicator
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.HdrIndicator
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.StabilizationUiState

private object IndicatorPillPreviewTokens {
    val ContainerPadding = 8.dp
    val CutoutBounds = DpRect(
        left = 4.dp,
        top = 72.dp,
        right = 44.dp,
        bottom = 112.dp
    )
}

@Composable
private fun VerticalIndicatorPillPreviewContent(
    cutoutRectDp: DpRect? = null,
    stabilizationUiState: StabilizationUiState = StabilizationUiState.Specific(
        stabilizationMode = StabilizationMode.ON,
        active = true
    )
) {
    val density = LocalDensity.current
    val cutoutRectPx = cutoutRectDp?.let { rect ->
        with(density) {
            IntRect(
                left = rect.left.roundToPx(),
                top = rect.top.roundToPx(),
                right = rect.right.roundToPx(),
                bottom = rect.bottom.roundToPx()
            )
        }
    }
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.DarkGray, Color.Black)
                    )
                )
                .padding(IndicatorPillPreviewTokens.ContainerPadding)
        ) {
            if (cutoutRectDp != null) {
                Box(
                    modifier = Modifier
                        .absoluteOffset(x = cutoutRectDp.left, y = cutoutRectDp.top)
                        .size(width = cutoutRectDp.width, height = cutoutRectDp.height)
                        .background(Color.Black, CircleShape)
                )
            }
            IndicatorPill(
                cutoutRectsOverride = listOfNotNull(cutoutRectPx),
                orientation = Orientation.Vertical
            ) {
                FlashModeIndicator(
                    flashModeUiState = FlashModeUiState.Available(
                        selectedFlashMode = FlashMode.ON,
                        availableFlashModes = listOf(
                            SingleSelectableUiState.SelectableUi(FlashMode.ON)
                        ),
                        isLowLightBoostActive = false
                    )
                )
                HdrIndicator(
                    hdrUiState = HdrUiState.Available(
                        selectedDynamicRange = DEFAULT_HDR_DYNAMIC_RANGE,
                        selectedImageFormat = DEFAULT_HDR_IMAGE_OUTPUT,
                        isSupported = true
                    )
                )
                VideoQualityIcon(videoQuality = VideoQuality.FHD)
                StabilizationIcon(stabilizationUiState = stabilizationUiState)
            }
        }
    }
}

/** Single unsplit vertical indicator pill. */
@PreviewTest
@Preview(
    name = "Single Vertical Pill",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
internal fun VerticalIndicatorPillSingleScreenshotPreview() {
    VerticalIndicatorPillPreviewContent()
}

/** Single vertical indicator pill with the stabilization Off indicator. */
@PreviewTest
@Preview(
    name = "Single Vertical Pill - Stabilization Off",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
internal fun VerticalIndicatorPillStabilizationOffScreenshotPreview() {
    VerticalIndicatorPillPreviewContent(stabilizationUiState = StabilizationUiState.Off)
}

/** Vertical indicator pill split around a display cutout. */
@PreviewTest
@Preview(
    name = "Cutout Split Vertical Pill",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
internal fun VerticalIndicatorPillCutoutSplitScreenshotPreview() {
    VerticalIndicatorPillPreviewContent(
        cutoutRectDp = IndicatorPillPreviewTokens.CutoutBounds
    )
}

/** RTL vertical indicator pill split around a display cutout. */
@PreviewTest
@Preview(
    name = "Cutout Split Vertical Pill - RTL",
    locale = "ar",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
internal fun VerticalIndicatorPillRtlScreenshotPreview() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        VerticalIndicatorPillPreviewContent(
            cutoutRectDp = IndicatorPillPreviewTokens.CutoutBounds
        )
    }
}
