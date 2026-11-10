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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private object Tokens {
    val ContainerWidth = ButtonDefaults.MinHeight
    val MinSlotWidth = 48.dp
    val ContainerVerticalPadding = 6.dp
    val PreviewContainerPadding = 8.dp
    val PreviewCutoutBounds = DpRect(
        left = 4.dp,
        top = 60.dp,
        right = 44.dp,
        bottom = 100.dp
    )
}

/**
 * Shared item-padding tokens for vertically oriented [IndicatorPillIcon] instances inside
 * [VerticalIndicatorPill].
 */
internal object VerticalIndicatorPillTokens {
    /** Horizontal start/end padding applied around each status icon in a vertical pill (`8.dp`). */
    val IconHorizontalPadding = 8.dp

    /** Vertical top/bottom padding applied around each status icon in a vertical pill (`6.dp`). */
    val IconVerticalPadding = 6.dp
}

/**
 * CompositionLocal specifying the axis along which [IndicatorPillIcon] expands, shrinks, and pads
 * its icon content ([Orientation.Horizontal] in [HorizontalIndicatorPill], [Orientation.Vertical]
 * in [VerticalIndicatorPill]).
 */
internal val LocalIndicatorPillOrientation = staticCompositionLocalOf { Orientation.Horizontal }

/**
 * A cutout-aware vertical pill container that groups active side-rail status indicators into
 * connected rounded pills, splitting into separate pills above and below any intersecting display
 * cutout.
 *
 * Enforces a minimum horizontal slot width of `48.dp` while centering a `40.dp`-wide pill
 * background with `6.dp` top and bottom segment padding within the slot. Provides
 * [LocalIndicatorPillOrientation] set to [Orientation.Vertical] and `Color.White` content color so
 * child [IndicatorPillIcon]s animate vertically with `8x6.dp` item padding, and inherits
 * `isTraversalGroup = true` from [CutoutAwareColumn] so screen readers traverse all active
 * indicators in the rail together. Draws no pill segments and collapses to `0.dp` height when no
 * indicators are active.
 *
 * Status icons stack top-to-bottom in declaration order (`Flash -> HDR -> VideoQuality ->
 * Stabilization`, non-inverted in RTL).
 *
 * @param modifier the [Modifier] to be applied to this vertical pill container.
 * @param content the status indicator children, scoped to [ColumnScope].
 */
// TODO: Confirm final vertical status indicator stacking order with UX.
@Composable
fun VerticalIndicatorPill(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    VerticalIndicatorPill(
        cutoutRectsOverride = null,
        modifier = modifier,
        onPillSegmentsPlaced = null,
        content = content
    )
}

/**
 * Internal overload of [VerticalIndicatorPill] exposing [cutoutRectsOverride] and
 * [onPillSegmentsPlaced] hooks for deterministic cutout testing and previews.
 *
 * @param cutoutRectsOverride optional list of cutout bounding rectangles in the column's local
 *   coordinate space, used for deterministic testing and previews.
 * @param modifier the [Modifier] to be applied to this vertical pill container.
 * @param onPillSegmentsPlaced optional callback invoked with the physical local [IntRect] bounds of
 *   each active pill segment after layout, used for testing.
 * @param content the status indicator children, scoped to [ColumnScope].
 */
@Composable
internal fun VerticalIndicatorPill(
    cutoutRectsOverride: List<IntRect>?,
    modifier: Modifier = Modifier,
    onPillSegmentsPlaced: ((List<IntRect>) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    CompositionLocalProvider(
        LocalContentColor provides Color.White,
        LocalIndicatorPillOrientation provides Orientation.Vertical
    ) {
        CutoutAwareColumn(
            modifier = modifier.defaultMinSize(minWidth = Tokens.MinSlotWidth),
            verticalSpacing = 0.dp,
            pillVerticalPadding = Tokens.ContainerVerticalPadding,
            pillMinWidth = Tokens.ContainerWidth,
            pillBackgroundColor = CameraControlBackgroundStyle.BLACK_60.containerColor,
            horizontalAlignment = Alignment.CenterHorizontally,
            cutoutRectsOverride = cutoutRectsOverride,
            onPillSegmentsPlaced = onPillSegmentsPlaced,
            content = content
        )
    }
}

@Composable
private fun VerticalIndicatorPillPreviewContent(cutoutRectDp: DpRect? = null) {
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
                .padding(Tokens.PreviewContainerPadding)
        ) {
            if (cutoutRectDp != null) {
                Box(
                    modifier = Modifier
                        .offset(x = cutoutRectDp.left, y = cutoutRectDp.top)
                        .size(width = cutoutRectDp.width, height = cutoutRectDp.height)
                        .background(Color.Black, CircleShape)
                )
            }
            IndicatorPill(
                cutoutRectsOverride = listOfNotNull(cutoutRectPx),
                isVertical = true
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

@Preview(
    name = "Single Vertical Pill",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
private fun VerticalIndicatorPillSinglePreview() {
    VerticalIndicatorPillPreviewContent()
}

@Preview(
    name = "Cutout Split Vertical Pill",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
private fun VerticalIndicatorPillCutoutSplitPreview() {
    VerticalIndicatorPillPreviewContent(
        cutoutRectDp = Tokens.PreviewCutoutBounds
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
        cutoutRectDp = Tokens.PreviewCutoutBounds
    )
}
