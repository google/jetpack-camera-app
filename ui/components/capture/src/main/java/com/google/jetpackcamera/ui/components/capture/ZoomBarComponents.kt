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
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/** Test tag for the active zoom stop indicator pill inside [ZoomBar]. */
internal const val ZOOM_BAR_INDICATOR_TAG = "box_zoom_bar_indicator"

private object ZoomBarTokens {
    const val TENTHS_PER_UNIT_INT = 10
    const val TENTHS_PER_UNIT_FLOAT = 10f
    const val HALF_UP_OFFSET = 0.5f
    const val FINE_STEP_TENTHS = 1
    const val ROTATED_AXIS_SIN_THRESHOLD = 0.5
    const val MIN_LABEL_SCALE = 0.6f
    val LabelHorizontalPadding = 2.dp
    val RotatedLabelPillMargin = 6.dp
    val UnrotatedLabelPillMargin = 4.dp
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
internal fun toTenths(value: Float): Int =
    floor(value * ZoomBarTokens.TENTHS_PER_UNIT_FLOAT + ZoomBarTokens.HALF_UP_OFFSET).toInt()

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

private fun formatZoomDecimal(tenths: Int, keepLeadingZero: Boolean, locale: Locale): String {
    val hasFraction = tenths % ZoomBarTokens.TENTHS_PER_UNIT_INT != 0
    val hasIntegerPart = tenths == 0 || tenths >= ZoomBarTokens.TENTHS_PER_UNIT_INT
    val numberFormat = NumberFormat.getNumberInstance(locale).apply {
        isGroupingUsed = false
        minimumIntegerDigits = if (keepLeadingZero || hasIntegerPart) 1 else 0
        minimumFractionDigits = if (hasFraction) 1 else 0
        maximumFractionDigits = if (hasFraction) 1 else 0
    }
    return numberFormat.format(BigDecimal.valueOf(tenths.toLong(), 1))
}

/**
 * Formats a zoom value for speech / accessibility announcements (e.g. "0.5", "1", "2").
 *
 * Keeps the leading zero for speech clarity and uses [locale]-consistent digits and decimal
 * separator via [NumberFormat].
 */
internal fun formatAccessibleZoomLabel(value: Float, locale: Locale = Locale.getDefault()): String =
    formatZoomDecimal(tenths = toTenths(value), keepLeadingZero = true, locale = locale)

/**
 * Formats zoom ratios for camera controls:
 * - Unselected stops: ".5", "1", "2", "5" (no leading zero, no multiplication symbol).
 * - Selected stop: ".5×", "1×", "2×", "5×" (with Unicode multiplication symbol \u00D7).
 * - In-between ratios (e.g. while pinching): "1.4×".
 * Uses [locale]-consistent digits and decimal separator via [NumberFormat].
 */
internal fun formatZoomLabel(
    targetZoom: Float,
    currentRatio: Float,
    isSelected: Boolean,
    locale: Locale = Locale.getDefault()
): String {
    val valueToFormat = if (isSelected) currentRatio else targetZoom
    val formattedNumber = formatZoomDecimal(
        tenths = toTenths(valueToFormat),
        keepLeadingZero = false,
        locale = locale
    )
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
 * In [Orientation.Horizontal], stops follow the active layout direction (left-to-right in LTR and
 * mirrored right-to-left in RTL). In [Orientation.Vertical], stops remain ordered top-to-bottom
 * regardless of layout direction.
 *
 * TODO(UX): Confirm final [Orientation.Vertical] stop ordering (currently `0.5x` at top to `5x` at
 * bottom on `main`, non-inverted in RTL, vs. placing telephoto `5x` at the top so moving/dragging
 * up zooms in).
 *
 * @param zoomControlUiState The [ZoomControlUiState] providing available zoom levels and
 *   current ratio.
 * @param onChangeZoom Callback when a zoom level is tapped or adjusted via accessibility actions.
 * @param modifier Modifier for the component.
 * @param orientation Layout orientation ([Orientation.Horizontal] or [Orientation.Vertical]),
 *   allowing flexible placement across portrait phones, landscape mode, and open foldables/tablets.
 * @param colors Styling configuration for track and indicator colors.
 * @param labelRotationDegrees Rotation in degrees applied to each stop's label in the draw phase to
 *   keep labels upright across device orientations without recomposing items or reordering stops.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ZoomBar(
    zoomControlUiState: ZoomControlUiState,
    onChangeZoom: (Float) -> Unit,
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
    colors: ZoomBarColors = ZoomBarDefaults.colors(),
    labelRotationDegrees: Float = 0f
) {
    if (zoomControlUiState is ZoomControlUiState.Enabled) {
        val currentZoomState by rememberUpdatedState(zoomControlUiState)
        val currentLabelRotation by rememberUpdatedState(labelRotationDegrees)
        val labelRotationProvider = remember { { currentLabelRotation } }
        val selectedOptionIndex: Int by remember(zoomControlUiState.primaryLensFacing) {
            derivedStateOf {
                val checkValue = currentZoomState.animatingToValue
                    ?: currentZoomState.displayZoomRatio()
                selectedZoomStopIndex(currentZoomState.zoomLevels, checkValue)
            }
        }

        val zoomLevels = zoomControlUiState.zoomLevels
        if (zoomLevels.isEmpty()) return

        val minZoom = zoomLevels.minOrNull() ?: 1f
        val maxZoom = zoomLevels.maxOrNull() ?: 1f
        val validIndex = selectedOptionIndex.coerceIn(0, zoomLevels.size - 1)
        val disableAnimations = LocalDisableAnimations.current
        val indicatorPosition by animateFloatAsState(
            targetValue = validIndex.toFloat(),
            animationSpec = if (disableAnimations) {
                snap()
            } else {
                MaterialTheme.motionScheme.defaultSpatialSpec()
            },
            label = "zoomIndicatorPosition"
        )

        val currentRatioProvider = remember {
            { currentZoomState.animatingToValue ?: currentZoomState.displayZoomRatio() }
        }

        Box(
            modifier = modifier
                .wrapContentSize()
                .background(color = colors.trackColor, shape = ZoomBarDefaults.TrackShape)
                .semantics {
                    isTraversalGroup = true
                    testTag = ZOOM_BUTTON_ROW_TAG
                }
        ) {
            if (orientation == Orientation.Horizontal) {
                Box(
                    modifier = Modifier
                        .offset {
                            val insetPx = ZoomBarDefaults.Inset.roundToPx()
                            val slotSizePx = ZoomBarDefaults.SlotSize.roundToPx()
                            IntOffset(
                                x = insetPx + (indicatorPosition * slotSizePx).roundToInt(),
                                y = insetPx
                            )
                        }
                        .size(ZoomBarDefaults.SlotSize, ZoomBarDefaults.IndicatorHeight)
                        .background(colors.selectedIndicatorColor, MaterialTheme.shapes.medium)
                        .testTag(ZOOM_BAR_INDICATOR_TAG)
                )

                Row(
                    modifier = Modifier
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
                            minZoom = minZoom,
                            maxZoom = maxZoom,
                            colors = colors,
                            onChangeZoom = onChangeZoom,
                            labelRotationProvider = labelRotationProvider
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .offset {
                            val insetPx = ZoomBarDefaults.Inset.roundToPx()
                            val slotSizePx = ZoomBarDefaults.SlotSize.roundToPx()
                            val indicatorHeightPx = ZoomBarDefaults.IndicatorHeight.roundToPx()
                            IntOffset(
                                x = 0,
                                y = insetPx +
                                    (indicatorPosition * slotSizePx).roundToInt() +
                                    (slotSizePx - indicatorHeightPx) / 2
                            )
                        }
                        .size(ZoomBarDefaults.SlotSize, ZoomBarDefaults.IndicatorHeight)
                        .background(colors.selectedIndicatorColor, MaterialTheme.shapes.medium)
                        .testTag(ZOOM_BAR_INDICATOR_TAG)
                )

                Column(
                    modifier = Modifier
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
                            minZoom = minZoom,
                            maxZoom = maxZoom,
                            colors = colors,
                            onChangeZoom = onChangeZoom,
                            labelRotationProvider = labelRotationProvider
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
    minZoom: Float,
    maxZoom: Float,
    colors: ZoomBarColors,
    onChangeZoom: (Float) -> Unit,
    modifier: Modifier = Modifier,
    labelRotationProvider: () -> Float = { 0f }
) {
    val currentOnChangeZoom by rememberUpdatedState(onChangeZoom)
    val displayTenths by remember(value, isSelected) {
        derivedStateOf {
            toTenths(if (isSelected) currentRatioProvider() else value)
        }
    }
    val isExactStopSelected = isSelected && displayTenths == toTenths(value)
    val locale = LocalResources.current.configuration.locales.get(0) ?: Locale.getDefault()
    val displayText = remember(value, isSelected, displayTenths, locale) {
        formatZoomLabel(
            targetZoom = value,
            currentRatio = displayTenths / ZoomBarTokens.TENTHS_PER_UNIT_FLOAT,
            isSelected = isSelected,
            locale = locale
        )
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

    val accessibleZoomLabel = remember(value, locale) {
        formatAccessibleZoomLabel(value, locale)
    }
    val stopContentDescription = stringResource(
        R.string.zoom_stop_content_description,
        accessibleZoomLabel
    )
    val onClickLabel = stringResource(R.string.zoom_stop_click_label, accessibleZoomLabel)
    val pinchedStateDescription = if (isSelected && !isExactStopSelected) {
        val currentAccessibleRatio = remember(displayTenths, locale) {
            formatAccessibleZoomLabel(
                displayTenths / ZoomBarTokens.TENTHS_PER_UNIT_FLOAT,
                locale
            )
        }
        stringResource(
            R.string.zoom_stop_current_ratio_state_description,
            currentAccessibleRatio
        )
    } else {
        null
    }

    val stepInLabel = stringResource(R.string.zoom_action_step_in)
    val stepOutLabel = stringResource(R.string.zoom_action_step_out)
    val minZoomTenths = toTenths(minZoom)
    val maxZoomTenths = toTenths(maxZoom)
    val fineZoomActions = if (isSelected) {
        buildList {
            if (displayTenths + ZoomBarTokens.FINE_STEP_TENTHS <= maxZoomTenths) {
                add(
                    CustomAccessibilityAction(label = stepInLabel) {
                        val nextTenths =
                            toTenths(currentRatioProvider()) + ZoomBarTokens.FINE_STEP_TENTHS
                        val nextZoom = (nextTenths / ZoomBarTokens.TENTHS_PER_UNIT_FLOAT)
                            .coerceAtMost(maxZoom)
                        currentOnChangeZoom(nextZoom)
                        true
                    }
                )
            }
            if (displayTenths - ZoomBarTokens.FINE_STEP_TENTHS >= minZoomTenths) {
                add(
                    CustomAccessibilityAction(label = stepOutLabel) {
                        val nextTenths =
                            toTenths(currentRatioProvider()) - ZoomBarTokens.FINE_STEP_TENTHS
                        val nextZoom = (nextTenths / ZoomBarTokens.TENTHS_PER_UNIT_FLOAT)
                            .coerceAtLeast(minZoom)
                        currentOnChangeZoom(nextZoom)
                        true
                    }
                )
            }
        }
    } else {
        emptyList()
    }

    val density = LocalDensity.current
    val clampedDensity = remember(density) {
        Density(
            density = density.density,
            fontScale = density.fontScale.coerceAtMost(ZoomBarDefaults.MAX_LABEL_FONT_SCALE)
        )
    }

    val baseLabelStyle = MaterialTheme.typography.labelLarge
    val labelStyle = remember(baseLabelStyle) {
        baseLabelStyle.copy(
            textAlign = TextAlign.Center,
            textDirection = TextDirection.Ltr
        )
    }

    Box(
        modifier = modifier
            .size(ZoomBarDefaults.SlotSize)
            .selectable(
                selected = isExactStopSelected,
                role = Role.RadioButton,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { currentOnChangeZoom(value) }
            )
            .semantics {
                testTag = getZoomButtonTestTag(value)
                if (pinchedStateDescription != null) {
                    stateDescription = pinchedStateDescription
                }
                onClick(label = onClickLabel) {
                    currentOnChangeZoom(value)
                    true
                }
                if (fineZoomActions.isNotEmpty()) {
                    customActions = fineZoomActions
                }
            },
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalDensity provides clampedDensity) {
            BasicText(
                text = labelText,
                style = labelStyle,
                color = {
                    lerp(
                        colors.unselectedContentColor,
                        colors.selectedContentColor,
                        (1f - abs(indicatorPositionProvider() - itemIndex)).coerceIn(0f, 1f)
                    )
                },
                modifier = Modifier
                    .wrapContentSize(unbounded = true)
                    .semantics {
                        contentDescription = stopContentDescription
                    }
                    .graphicsLayer {
                        val rotation = labelRotationProvider()
                        rotationZ = rotation
                        if (size.width > 0f) {
                            val rotationRadians = Math.toRadians(rotation.toDouble())
                            val isRotatedCrossAxis = abs(sin(rotationRadians)) >
                                ZoomBarTokens.ROTATED_AXIS_SIN_THRESHOLD
                            val maxAllowedSpan = if (isRotatedCrossAxis) {
                                ZoomBarDefaults.IndicatorHeight -
                                    ZoomBarTokens.RotatedLabelPillMargin
                            } else {
                                ZoomBarDefaults.SlotSize -
                                    ZoomBarTokens.UnrotatedLabelPillMargin
                            }
                            val maxSpanPx = maxAllowedSpan.toPx()
                            val scale = if (size.width > maxSpanPx) {
                                (maxSpanPx / size.width)
                                    .coerceAtLeast(ZoomBarTokens.MIN_LABEL_SCALE)
                            } else {
                                1f
                            }
                            scaleX = scale
                            scaleY = scale
                        }
                    }
                    .padding(horizontal = ZoomBarTokens.LabelHorizontalPadding),
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

@Preview(name = "Horizontal RTL - 1x Selected", locale = "ar", showBackground = true)
@Composable
private fun ZoomBarHorizontalRtlPreview() {
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

@Preview(name = "Horizontal RTL - Fraction (.5x Selected)", locale = "ar", showBackground = true)
@Composable
private fun ZoomBarHorizontalRtlFractionPreview() {
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

@Preview(name = "Vertical RTL - Flush Sides", locale = "ar", showBackground = true)
@Composable
private fun ZoomBarVerticalRtlPreview() {
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

@Preview(name = "Horizontal - Rotated Labels (90 deg)", showBackground = true)
@Composable
private fun ZoomBarHorizontalRotatedLabelsPreview() {
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
            colors = ZoomBarDefaults.black60Colors(),
            labelRotationDegrees = 90f
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
