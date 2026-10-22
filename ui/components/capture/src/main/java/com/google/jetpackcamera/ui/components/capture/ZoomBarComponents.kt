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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val US_SYMBOLS = DecimalFormatSymbols(Locale.US)

private val SUB_ONE_FORMATTER = ThreadLocal.withInitial {
    DecimalFormat(".#", US_SYMBOLS).apply {
        minimumIntegerDigits = 0
        roundingMode = RoundingMode.HALF_UP
    }
}

private val DECIMAL_FORMATTER = ThreadLocal.withInitial {
    DecimalFormat("#.#", US_SYMBOLS).apply {
        roundingMode = RoundingMode.HALF_UP
    }
}

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
    val HorizontalItemWidth = 40.dp
    val HorizontalItemHeight = 36.dp
    val VerticalItemWidth = 40.dp
    val VerticalItemHeight = 40.dp
    val HorizontalEndPadding = 6.dp
    val VerticalEndPadding = 4.dp
    val EndPadding = 4.dp
    val HorizontalClearance = 4.dp

    val TrackShape = RoundedCornerShape(56.dp)
    val IndicatorShape = RoundedCornerShape(12.dp)

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
        trackColor: Color = Color.Black.copy(alpha = 0.60f),
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
        trackColor: Color = Color.White.copy(alpha = 0.20f),
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
 * Formats zoom ratios for camera controls:
 * - Unselected stops: ".5", "1", "2", "5" (no leading zero, no multiplication symbol).
 * - Selected stop: ".5×", "1×", "2×", "5×" (with Unicode multiplication symbol \u00D7).
 * - In-between ratios (e.g. while pinching): "1.4×".
 */
fun formatZoomLabel(targetZoom: Float, currentRatio: Float, isSelected: Boolean): String {
    val valueToFormat = if (isSelected) currentRatio else targetZoom
    val formattedNumber = if (valueToFormat < 1f && valueToFormat > 0f) {
        val formatted = SUB_ONE_FORMATTER.get()!!.format(valueToFormat)
        if (formatted == "1" || formatted == "1.0") "1" else formatted
    } else if (valueToFormat % 1.0f == 0f) {
        valueToFormat.toInt().toString()
    } else {
        DECIMAL_FORMATTER.get()!!.format(valueToFormat)
    }
    return if (isSelected) {
        "$formattedNumber\u00D7"
    } else {
        formattedNumber
    }
}

/**
 * A composable that displays zoom controls formatted as a unified capsule track
 * with a selected indicator pill appearing on the active stop.
 *
 * @param zoomControlUiState The [ZoomControlUiState] providing available zoom levels and
 *   current ratio.
 * @param onChangeZoom Callback when a zoom level is tapped.
 * @param modifier Modifier for the component.
 * @param orientation Layout orientation ([Orientation.Horizontal] or [Orientation.Vertical]),
 *   allowing flexible placement across portrait phones, landscape mode, and open foldables/tablets.
 * @param colors Styling configuration for track and indicator colors.
 */
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
                // If animating towards a value, select that target; otherwise find closest
                // matching stop
                val checkValue =
                    currentZoomState.animatingToValue ?: currentZoomState.primaryZoomRatio ?: 1f
                val index = currentZoomState.zoomLevels.indexOfLast { zoomLevelOption ->
                    checkValue >= zoomLevelOption
                }
                if (index >= 0) index else 0
            }
        }

        val zoomLevels = zoomControlUiState.zoomLevels
        if (zoomLevels.isEmpty()) return

        val validIndex = selectedOptionIndex.coerceIn(0, zoomLevels.size - 1)
        val currentRatio = currentZoomState.primaryZoomRatio ?: 1f

        val itemWidth = if (orientation == Orientation.Horizontal) {
            ZoomBarDefaults.HorizontalItemWidth
        } else {
            ZoomBarDefaults.VerticalItemWidth
        }
        val itemHeight = if (orientation == Orientation.Horizontal) {
            ZoomBarDefaults.HorizontalItemHeight
        } else {
            ZoomBarDefaults.VerticalItemHeight
        }

        val endPadding = if (orientation == Orientation.Horizontal) {
            ZoomBarDefaults.HorizontalEndPadding
        } else {
            ZoomBarDefaults.VerticalEndPadding
        }

        // In horizontal, clearance exists on all sides; in vertical, buttons are flush with
        // the vertical sides.
        val totalWidth = if (orientation == Orientation.Horizontal) {
            itemWidth * zoomLevels.size + endPadding * 2
        } else {
            itemWidth
        }
        val totalHeight = if (orientation == Orientation.Horizontal) {
            itemHeight + ZoomBarDefaults.HorizontalClearance * 2
        } else {
            itemHeight * zoomLevels.size + endPadding * 2
        }

        Box(
            modifier = modifier
                .size(totalWidth, totalHeight)
                .background(color = colors.trackColor, shape = ZoomBarDefaults.TrackShape)
                .semantics {
                    testTag = ZOOM_BUTTON_ROW_TAG
                    stateDescription = zoomControlUiState.primaryZoomRatio.toString()
                }
        ) {
            // Stops
            if (orientation == Orientation.Horizontal) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = endPadding,
                            vertical = ZoomBarDefaults.HorizontalClearance
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    zoomLevels.forEachIndexed { index, value ->
                        val isSelected = index == validIndex
                        ZoomBarItem(
                            value = value,
                            currentRatio = if (isSelected) currentRatio else null,
                            isSelected = isSelected,
                            colors = colors,
                            modifier = Modifier.size(itemWidth, itemHeight),
                            onClick = { onChangeZoom(value) }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = endPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    zoomLevels.forEachIndexed { index, value ->
                        val isSelected = index == validIndex
                        ZoomBarItem(
                            value = value,
                            currentRatio = if (isSelected) currentRatio else null,
                            isSelected = isSelected,
                            colors = colors,
                            modifier = Modifier.size(itemWidth, itemHeight),
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
    currentRatio: Float?,
    isSelected: Boolean,
    colors: ZoomBarColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val displayText = remember(value, isSelected, currentRatio) {
        formatZoomLabel(
            targetZoom = value,
            currentRatio = currentRatio ?: value,
            isSelected = isSelected
        )
    }

    val containerColor by animateColorAsState(
        targetValue =
        if (isSelected) {
            colors.selectedIndicatorColor
        } else {
            Color.Transparent
        },
        label = "zoomItemContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue =
        if (isSelected) {
            colors.selectedContentColor
        } else {
            colors.unselectedContentColor
        },
        label = "zoomItemContentColor"
    )

    Box(
        modifier = modifier
            .background(
                color = containerColor,
                shape = ZoomBarDefaults.IndicatorShape
            )
            .clip(ZoomBarDefaults.IndicatorShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .semantics {
                testTag = getZoomButtonTestTag(value)
                contentDescription = if (isSelected) "selected" else "not selected"
                selected = isSelected
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.animateContentSize(),
            text = displayText,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * A composable that displays a row of zoom buttons.
 *
 * Maintained as a delegate to [ZoomBar] for backward compatibility with existing callers.
 *
 * @param onChangeZoom the callback for when the zoom changes
 * @param modifier the modifier for this component
 * @param zoomControlUiState the [ZoomControlUiState] for this component
 * @param buttonSize the size of the buttons (retained for backward compatibility)
 * @param spacing the spacing between the buttons (retained for backward compatibility)
 * @param orientation the layout orientation ([Orientation.Horizontal] or [Orientation.Vertical])
 * @param colors the [ZoomBarColors] styling configuration for track and indicator colors
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
fun ZoomBarHorizontalDarkPreview() {
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
fun ZoomBarHorizontalTranslucentPreview() {
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
fun ZoomBarHorizontalFractionPreview() {
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
fun ZoomBarVerticalDarkPreview() {
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

@Preview(name = "Vertical 20% White - Flush Sides", showBackground = true)
@Composable
fun ZoomBarVerticalTranslucentPreview() {
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
            colors = ZoomBarDefaults.white20Colors()
        )
    }
}

@Preview(name = "Single Value Range (1.0x only)", showBackground = true)
@Composable
fun ZoomBarSingleValuePreview() {
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
fun ZoomBarExtendedRangePreview() {
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
fun ZoomBarPreviewOneSelected() {
    ZoomBarHorizontalDarkPreview()
}

@Preview(name = "Legacy Delegate - Fraction Selected", showBackground = true)
@Composable
fun ZoomBarPreviewFractionSelected() {
    ZoomBarHorizontalFractionPreview()
}
