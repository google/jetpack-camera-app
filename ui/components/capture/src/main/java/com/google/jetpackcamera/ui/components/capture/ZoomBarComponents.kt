/*
 * Copyright (C) 2025 The Android Open Source Project
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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

/**
 * Styling configuration for [ZoomBar].
 *
 * @param trackColor Background color of the stadium track capsule (e.g. 60% black over
 *   viewfinder, 20% white off viewfinder).
 * @param selectedIndicatorColor Background color of the indicator pill behind the active stop (e.g.
 *   primaryFixed).
 * @param selectedContentColor Text color for the currently selected stop (e.g. onPrimaryFixed).
 * @param unselectedContentColor Text color for unselected stops (e.g. Color.White).
 */
@Immutable
data class ZoomBarColors(
    val trackColor: Color,
    val selectedIndicatorColor: Color,
    val selectedContentColor: Color,
    val unselectedContentColor: Color
)

/**
 * Default color palettes and dimensions for [ZoomBar].
 */
object ZoomBarDefaults {
    val SlotSize = 48.dp
    val IndicatorHeight = ButtonDefaults.MinHeight // 40.dp
    val Inset = (SlotSize - IndicatorHeight) / 2 // 4.dp
    val TrackShape = CircleShape
    const val MAX_LABEL_FONT_SCALE = 1.3f

    /**
     * Default dynamic colors adapting to viewfinder overlap via [LocalCameraControlBackgroundStyle]
     * and Material 3 theme color tokens.
     */
    @Composable
    fun colors(
        trackColor: Color = LocalCameraControlBackgroundStyle.current.containerColor,
        selectedIndicatorColor: Color = MaterialTheme.colorScheme.primaryFixed,
        selectedContentColor: Color = MaterialTheme.colorScheme.onPrimaryFixed,
        unselectedContentColor: Color = Color.White
    ) = ZoomBarColors(
        trackColor = trackColor,
        selectedIndicatorColor = selectedIndicatorColor,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor
    )

    /**
     * Colors for overlapping the viewfinder (60% black background).
     */
    @Composable
    fun black60Colors(
        trackColor: Color = CameraControlBackgroundStyle.BLACK_60.containerColor,
        selectedIndicatorColor: Color = MaterialTheme.colorScheme.primaryFixed,
        selectedContentColor: Color = MaterialTheme.colorScheme.onPrimaryFixed,
        unselectedContentColor: Color = Color.White
    ) = ZoomBarColors(
        trackColor = trackColor,
        selectedIndicatorColor = selectedIndicatorColor,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor
    )

    /**
     * Colors when not overlapping the viewfinder (lighter translucent background).
     */
    @Composable
    fun white20Colors(
        trackColor: Color = CameraControlBackgroundStyle.WHITE_20.containerColor,
        selectedIndicatorColor: Color = MaterialTheme.colorScheme.primaryFixed,
        selectedContentColor: Color = MaterialTheme.colorScheme.onPrimaryFixed,
        unselectedContentColor: Color = Color.White
    ) = ZoomBarColors(
        trackColor = trackColor,
        selectedIndicatorColor = selectedIndicatorColor,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor
    )
}

/**
 * Rounds a zoom ratio half-up to the nearest tenth.
 */
internal fun toTenths(value: Float): Int = floor(value * 10f + 0.5f).toInt()

/**
 * Determines the index of the selected zoom stop for a given [zoomRatio].
 *
 * Stops are matched against the half-up rounded tenth of [zoomRatio]. If adjacent stops round
 * to the same tenth (e.g. 0.96x and 1.0x both round to 1.0x), the stop closest in absolute
 * difference to [zoomRatio] is selected.
 */
internal fun selectedZoomStopIndex(zoomLevels: List<Float>, zoomRatio: Float): Int {
    if (zoomLevels.isEmpty()) return 0
    val checkTenths = toTenths(zoomRatio)
    val index = zoomLevels.indexOfLast { zoomLevelOption ->
        toTenths(zoomLevelOption) <= checkTenths
    }
    return if (index >= 0) {
        if (index > 0 &&
            toTenths(zoomLevels[index]) == toTenths(zoomLevels[index - 1])
        ) {
            val distCurrent = abs(zoomLevels[index] - zoomRatio)
            val distPrev = abs(zoomLevels[index - 1] - zoomRatio)
            if (distPrev < distCurrent) index - 1 else index
        } else {
            index
        }
    } else {
        0
    }
}

/**
 * Formats a zoom value for speech / accessibility announcements (e.g. "0.5", "1", "2").
 *
 * Keeps the leading zero for speech clarity and respects the given [locale]'s decimal separator.
 */
internal fun formatAccessibleZoomLabel(value: Float, locale: Locale = Locale.getDefault()): String {
    val tenths = toTenths(value)
    val whole = tenths / 10
    val fraction = tenths % 10
    val decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    return if (fraction == 0) {
        whole.toString()
    } else {
        "$whole$decimalSeparator$fraction"
    }
}

/**
 * Formats zoom ratios for camera controls:
 * - Unselected stops: ".5", "1", "2", "5" (no leading zero, no multiplication symbol).
 * - Selected stop: ".5×", "1×", "2×", "5×" (with Unicode multiplication symbol \u00D7).
 * - In-between ratios (e.g. while pinching): "1.4×".
 * Respects the given [locale]'s decimal separator.
 */
internal fun formatZoomLabel(
    targetZoom: Float,
    currentRatio: Float,
    isSelected: Boolean,
    locale: Locale = Locale.getDefault()
): String {
    val valueToFormat = if (isSelected) currentRatio else targetZoom
    val tenths = toTenths(valueToFormat)
    val whole = tenths / 10
    val fraction = tenths % 10
    val decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    val formattedNumber = if (whole == 0) {
        "$decimalSeparator$fraction"
    } else if (fraction == 0) {
        whole.toString()
    } else {
        "$whole$decimalSeparator$fraction"
    }
    return if (isSelected) {
        "$formattedNumber\u00D7"
    } else {
        formattedNumber
    }
}

private fun ZoomControlUiState.Enabled.displayZoomRatio(): Float =
    primarySettingZoomRatio ?: primaryZoomRatio ?: initialZoomRatio ?: 1f

/**
 * A composable that displays zoom controls formatted as a unified capsule track
 * with a sliding indicator pill that smoothly tracks the active zoom stop.
 *
 * @param zoomControlUiState The [ZoomControlUiState] providing available zoom levels and
 *   current ratio.
 * @param onChangeZoom Callback when a zoom level is tapped.
 * @param modifier Modifier for the component.
 * @param orientation Layout orientation ([Orientation.Horizontal] or [Orientation.Vertical]),
 *   allowing flexible placement across portrait phones, landscape mode, and open foldables/tablets.
 * @param colors Styling configuration for track and indicator colors.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ZoomBar(
    zoomControlUiState: ZoomControlUiState,
    onChangeZoom: (Float) -> Unit,
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
    colors: ZoomBarColors = ZoomBarDefaults.colors()
) {
    if (zoomControlUiState is ZoomControlUiState.Enabled) {
        val currentZoomState by rememberUpdatedState(zoomControlUiState)
        val selectedOptionIndex: Int by remember(zoomControlUiState.primaryLensFacing) {
            derivedStateOf {
                val checkValue = currentZoomState.animatingToValue
                    ?: currentZoomState.displayZoomRatio()
                selectedZoomStopIndex(currentZoomState.zoomLevels, checkValue)
            }
        }

        val zoomLevels = zoomControlUiState.zoomLevels
        if (zoomLevels.isEmpty()) return

        val validIndex = selectedOptionIndex.coerceIn(0, zoomLevels.size - 1)
        val indicatorPosition by animateFloatAsState(
            targetValue = validIndex.toFloat(),
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            label = "zoomIndicatorPosition"
        )

        val currentRatioProvider = remember {
            { currentZoomState.animatingToValue ?: currentZoomState.displayZoomRatio() }
        }

        val totalAlongTrack = ZoomBarDefaults.SlotSize * zoomLevels.size + ZoomBarDefaults.Inset * 2
        val totalAcrossTrack = ZoomBarDefaults.SlotSize

        val totalWidth =
            if (orientation == Orientation.Horizontal) totalAlongTrack else totalAcrossTrack
        val totalHeight =
            if (orientation == Orientation.Horizontal) totalAcrossTrack else totalAlongTrack

        Box(
            modifier = modifier
                .size(totalWidth, totalHeight)
                .background(color = colors.trackColor, shape = ZoomBarDefaults.TrackShape)
                .semantics {
                    testTag = ZOOM_BUTTON_ROW_TAG
                }
        ) {
            if (orientation == Orientation.Horizontal) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (
                                    ZoomBarDefaults.Inset +
                                        indicatorPosition * ZoomBarDefaults.SlotSize
                                    ).roundToPx(),
                                y = ZoomBarDefaults.Inset.roundToPx()
                            )
                        }
                        .size(ZoomBarDefaults.SlotSize, ZoomBarDefaults.IndicatorHeight)
                        .background(colors.selectedIndicatorColor, MaterialTheme.shapes.medium)
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .selectableGroup()
                        .padding(horizontal = ZoomBarDefaults.Inset),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    zoomLevels.forEachIndexed { index, value ->
                        ZoomBarItem(
                            value = value,
                            currentRatioProvider = currentRatioProvider,
                            isSelected = index == validIndex,
                            indicatorPositionProvider = { indicatorPosition },
                            itemIndex = index,
                            colors = colors,
                            onClick = { onChangeZoom(value) }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = 0,
                                y = (
                                    ZoomBarDefaults.Inset +
                                        indicatorPosition * ZoomBarDefaults.SlotSize +
                                        ZoomBarDefaults.Inset
                                    ).roundToPx()
                            )
                        }
                        .size(ZoomBarDefaults.SlotSize, ZoomBarDefaults.IndicatorHeight)
                        .background(colors.selectedIndicatorColor, MaterialTheme.shapes.medium)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .selectableGroup()
                        .padding(vertical = ZoomBarDefaults.Inset),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    zoomLevels.forEachIndexed { index, value ->
                        ZoomBarItem(
                            value = value,
                            currentRatioProvider = currentRatioProvider,
                            isSelected = index == validIndex,
                            indicatorPositionProvider = { indicatorPosition },
                            itemIndex = index,
                            colors = colors,
                            onClick = { onChangeZoom(value) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomBarItem(
    value: Float,
    currentRatioProvider: () -> Float,
    isSelected: Boolean,
    indicatorPositionProvider: () -> Float,
    itemIndex: Int,
    colors: ZoomBarColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val displayText by remember(value, isSelected) {
        derivedStateOf {
            formatZoomLabel(
                targetZoom = value,
                currentRatio = if (isSelected) currentRatioProvider() else value,
                isSelected = isSelected
            )
        }
    }

    val labelText = remember(displayText) {
        if (displayText.endsWith('×')) {
            buildAnnotatedString {
                append(displayText.dropLast(1))
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append('×')
                pop()
            }
        } else {
            AnnotatedString(displayText)
        }
    }

    val accessibleZoomLabel = formatAccessibleZoomLabel(value)

    val density = LocalDensity.current

    val onClickLabel = stringResource(R.string.zoom_stop_click_label, accessibleZoomLabel)

    Box(
        modifier = modifier
            .size(ZoomBarDefaults.SlotSize)
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .semantics {
                testTag = getZoomButtonTestTag(value)
                onClick(label = onClickLabel) {
                    onClick()
                    true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = density.density,
                fontScale = density.fontScale.coerceAtMost(ZoomBarDefaults.MAX_LABEL_FONT_SCALE)
            )
        ) {
            // TODO(#619): Apply Modifier.animateToUpright() to keep labels upright on rotation
            //  (https://github.com/google/jetpack-camera-app/pull/619).
            BasicText(
                text = labelText,
                style = MaterialTheme.typography.labelLarge.copy(
                    textAlign = TextAlign.Center
                ),
                color = {
                    lerp(
                        colors.unselectedContentColor,
                        colors.selectedContentColor,
                        (1f - abs(indicatorPositionProvider() - itemIndex)).coerceIn(0f, 1f)
                    )
                },
                modifier = Modifier
                    .wrapContentSize(unbounded = true)
                    .padding(horizontal = 2.dp),
                softWrap = false
            )
        }
    }
}

/**
 * A composable that displays a row of zoom buttons.
 *
 * @deprecated Use [ZoomBar] directly.
 */
@Deprecated(
    message = "Use ZoomBar directly.",
    replaceWith = ReplaceWith(
        "ZoomBar(" +
            "zoomControlUiState = zoomControlUiState, " +
            "onChangeZoom = onChangeZoom, " +
            "modifier = modifier, " +
            "orientation = orientation, " +
            "colors = colors" +
            ")",
        "com.google.jetpackcamera.ui.components.capture.ZoomBar"
    )
)
@Composable
fun ZoomButtonRow(
    onChangeZoom: (Float) -> Unit,
    modifier: Modifier = Modifier,
    zoomControlUiState: ZoomControlUiState,
    buttonSize: Dp = ButtonDefaults.ExtraSmallContainerHeight,
    spacing: Dp = 16.dp,
    orientation: Orientation = Orientation.Horizontal,
    colors: ZoomBarColors = ZoomBarDefaults.colors()
) {
    ZoomBar(
        zoomControlUiState = zoomControlUiState,
        onChangeZoom = onChangeZoom,
        modifier = modifier,
        orientation = orientation,
        colors = colors
    )
}

private fun getZoomButtonTestTag(buttonValue: Float): String {
    return if (buttonValue < 1 && buttonValue > 0) {
        ZOOM_BUTTON_MIN_TAG
    } else if (buttonValue == 1f) {
        ZOOM_BUTTON_1_TAG
    } else if (buttonValue == 2f) {
        ZOOM_BUTTON_2_TAG
    } else if (buttonValue == 5f) {
        ZOOM_BUTTON_5_TAG
    } else {
        "ZoomButton_${buttonValue}Tag"
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Horizontal 60% Black (Overlapping Viewfinder)", showBackground = true)
@Composable
private fun ZoomBarHorizontalDarkPreview() {
    Box(
        modifier = Modifier
            .background(Color(0xFF2B2B2B))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        ZoomBar(
            zoomControlUiState = ZoomControlUiState.Enabled(
                zoomLevels = listOf(0.5f, 1.0f, 2.0f, 5.0f),
                primaryLensFacing = LensFacing.BACK,
                primaryZoomRatio = 1.0f
            ),
            onChangeZoom = {},
            orientation = Orientation.Horizontal,
            colors = ZoomBarDefaults.black60Colors()
        )
    }
}

@Preview(name = "Horizontal 20% White (Not Overlapping Viewfinder)", showBackground = true)
@Composable
private fun ZoomBarHorizontalTranslucentPreview() {
    Box(
        modifier = Modifier
            .background(Color(0xFF2B2B2B))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        ZoomBar(
            zoomControlUiState = ZoomControlUiState.Enabled(
                zoomLevels = listOf(0.5f, 1.0f, 2.0f, 5.0f),
                primaryLensFacing = LensFacing.BACK,
                primaryZoomRatio = 1.0f
            ),
            onChangeZoom = {},
            orientation = Orientation.Horizontal,
            colors = ZoomBarDefaults.white20Colors()
        )
    }
}

@Preview(name = "Horizontal - Fraction (.5x Selected)", showBackground = true)
@Composable
private fun ZoomBarHorizontalFractionPreview() {
    Box(
        modifier = Modifier
            .background(Color(0xFF2B2B2B))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        ZoomBar(
            zoomControlUiState = ZoomControlUiState.Enabled(
                zoomLevels = listOf(0.5f, 1.0f, 2.0f, 5.0f),
                primaryLensFacing = LensFacing.BACK,
                primaryZoomRatio = 0.5f
            ),
            onChangeZoom = {},
            orientation = Orientation.Horizontal,
            colors = ZoomBarDefaults.black60Colors()
        )
    }
}

@Preview(name = "Vertical 60% Black - Flush Sides", showBackground = true)
@Composable
private fun ZoomBarVerticalDarkPreview() {
    Box(
        modifier = Modifier
            .background(Color(0xFF2B2B2B))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        ZoomBar(
            zoomControlUiState = ZoomControlUiState.Enabled(
                zoomLevels = listOf(0.5f, 1.0f, 2.0f, 5.0f),
                primaryLensFacing = LensFacing.BACK,
                primaryZoomRatio = 1.0f
            ),
            onChangeZoom = {},
            orientation = Orientation.Vertical,
            colors = ZoomBarDefaults.black60Colors()
        )
    }
}

@Preview(name = "Single Stop (1x Only)", showBackground = true)
@Composable
private fun ZoomBarSingleStopPreview() {
    Box(
        modifier = Modifier
            .background(Color(0xFF2B2B2B))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        ZoomBar(
            zoomControlUiState = ZoomControlUiState.Enabled(
                zoomLevels = listOf(1.0f),
                primaryLensFacing = LensFacing.BACK,
                primaryZoomRatio = 1.0f
            ),
            onChangeZoom = {},
            orientation = Orientation.Horizontal
        )
    }
}

@Preview(name = "Extended Telephoto Range (2x Selected)", showBackground = true)
@Composable
private fun ZoomBarExtendedRangePreview() {
    Box(
        modifier = Modifier
            .background(Color(0xFF2B2B2B))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        ZoomBar(
            zoomControlUiState = ZoomControlUiState.Enabled(
                zoomLevels = listOf(0.5f, 1.0f, 2.0f, 5.0f, 10.0f),
                primaryLensFacing = LensFacing.BACK,
                primaryZoomRatio = 2.0f
            ),
            onChangeZoom = {},
            orientation = Orientation.Horizontal
        )
    }
}

@Preview(name = "Legacy Delegate - One Selected", showBackground = true)
@Composable
private fun ZoomBarPreviewOneSelected() {
    ZoomBarHorizontalDarkPreview()
}

@Preview(name = "Legacy Delegate - Fraction Selected", showBackground = true)
@Composable
private fun ZoomBarPreviewFractionSelected() {
    ZoomBarHorizontalFractionPreview()
}
