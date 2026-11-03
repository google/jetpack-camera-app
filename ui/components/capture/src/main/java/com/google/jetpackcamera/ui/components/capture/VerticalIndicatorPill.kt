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

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
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

/**
 * Layout dimension tokens for [VerticalIndicatorPill] and vertically oriented [IndicatorPillIcon]
 * items.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal object VerticalIndicatorPillTokens {
    val ContainerWidth = ButtonDefaults.MinHeight
    val MinSlotWidth = 48.dp
    val ContainerVerticalPadding = 6.dp
    val IconHorizontalPadding = 8.dp
    val IconVerticalPadding = 6.dp
}

/**
 * CompositionLocal specifying the axis along which [IndicatorPillIcon] expands, shrinks, and pads
 * its icon content (`Orientation.Horizontal` in [IndicatorPill], `Orientation.Vertical` in
 * [VerticalIndicatorPill]).
 */
internal val LocalIndicatorPillOrientation = staticCompositionLocalOf { Orientation.Horizontal }

/**
 * A cutout-aware vertical pill container that groups active side-rail status indicators into
 * connected rounded pills, splitting into separate pills above and below any intersecting display
 * cutout.
 *
 * Enforces a minimum horizontal slot width of [VerticalIndicatorPillTokens.MinSlotWidth] (`48.dp`)
 * while centering a `40.dp`-wide pill background ([VerticalIndicatorPillTokens.ContainerWidth])
 * with `6.dp` top and bottom segment padding within the slot. Provides
 * [LocalIndicatorPillOrientation] set to [Orientation.Vertical] and `Color.White` content color so
 * child [IndicatorPillIcon]s animate vertically with `8x6.dp` item padding, and inherits
 * `isTraversalGroup = true` from [CutoutAwareColumn] so screen readers traverse all active
 * indicators in the rail together. Draws no pill segments and collapses to `0.dp` height when no
 * indicators are active.
 *
 * TODO(UX): `Top -> Bottom` status icon stacking (`Flash -> HDR -> VideoQuality -> Stabilization`,
 * non-inverted in RTL) is pending final UX confirmation.
 *
 * @param modifier the [Modifier] to be applied to this vertical pill container.
 * @param cutoutRectsOverride optional list of cutout bounding rectangles in the column's local
 *   coordinate space, used for deterministic testing and previews.
 * @param onPillSegmentsPlaced optional callback invoked with the physical local [IntRect] bounds of
 *   each active pill segment after layout, used for testing.
 * @param content the status indicator children, scoped to [ColumnScope].
 */
@SuppressLint("AvoidNullableCollections", "NullableCollection")
@Composable
fun VerticalIndicatorPill(
    modifier: Modifier = Modifier,
    cutoutRectsOverride: List<IntRect>? = null,
    onPillSegmentsPlaced: ((List<IntRect>) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    CompositionLocalProvider(
        LocalContentColor provides Color.White,
        LocalIndicatorPillOrientation provides Orientation.Vertical
    ) {
        CutoutAwareColumn(
            modifier = modifier.defaultMinSize(minWidth = VerticalIndicatorPillTokens.MinSlotWidth),
            verticalSpacing = 0.dp,
            pillVerticalPadding = VerticalIndicatorPillTokens.ContainerVerticalPadding,
            pillMinWidth = VerticalIndicatorPillTokens.ContainerWidth,
            pillBackgroundColor = CameraControlBackgroundStyle.BLACK_60.containerColor,
            horizontalAlignment = Alignment.CenterHorizontally,
            cutoutRectsOverride = cutoutRectsOverride,
            onPillSegmentsPlaced = onPillSegmentsPlaced,
            content = content
        )
    }
}

@Composable
private fun VerticalIndicatorPillPreviewContent(cutoutRect: IntRect? = null) {
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colorScheme) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = if (isSystemInDarkTheme()) {
                            listOf(Color.DarkGray, Color.Black)
                        } else {
                            listOf(Color.LightGray, Color.Gray)
                        }
                    )
                )
                .padding(8.dp)
        ) {
            if (cutoutRect != null) {
                Box(
                    modifier = Modifier
                        .offset(x = cutoutRect.left.dp, y = cutoutRect.top.dp)
                        .size(width = cutoutRect.width.dp, height = cutoutRect.height.dp)
                        .background(Color.Black, CircleShape)
                )
            }
            VerticalIndicatorPill(
                cutoutRectsOverride = listOfNotNull(cutoutRect)
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
                StabilizationIcon(
                    stabilizationUiState = StabilizationUiState.Specific(
                        stabilizationMode = StabilizationMode.ON,
                        active = true
                    )
                )
            }
        }
    }
}

@Preview(name = "Single Vertical Pill - Light", showBackground = true)
@Preview(
    name = "Single Vertical Pill - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
private fun VerticalIndicatorPillSinglePreview() {
    VerticalIndicatorPillPreviewContent()
}

@Preview(name = "Cutout Split Vertical Pill - Light", showBackground = true)
@Preview(
    name = "Cutout Split Vertical Pill - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
private fun VerticalIndicatorPillCutoutSplitPreview() {
    VerticalIndicatorPillPreviewContent(
        cutoutRect = IntRect(left = 4, top = 56, right = 44, bottom = 96)
    )
}

@Preview(
    name = "Cutout Split Vertical Pill - RTL",
    locale = "ar",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
private fun VerticalIndicatorPillRtlPreview() {
    VerticalIndicatorPillPreviewContent(
        cutoutRect = IntRect(left = 4, top = 56, right = 44, bottom = 96)
    )
}
