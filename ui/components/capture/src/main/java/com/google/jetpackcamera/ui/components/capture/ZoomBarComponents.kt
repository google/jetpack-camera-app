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
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

private object ZoomBarTokens {
    const val TENTHS_PER_UNIT_INT = 10
    const val TENTHS_PER_UNIT_FLOAT = 10f
    const val HALF_UP_OFFSET = 0.5f
    const val FINE_STEP_TENTHS = 1
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
 * mirrored right-to-left in RTL). In [Orientation.Vertical], stops are ordered top-to-bottom from
 * lowest to highest magnification (e.g. `0.5x` at top to `5x` at bottom, non-inverted in RTL).
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
// TODO: Confirm final vertical stop ordering with UX (e.g. telephoto at top so dragging up
//  zooms in).
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

        val indicatorShape = MaterialTheme.shapes.medium

        @Composable
        fun ZoomStopItems() {
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

        Box(
            modifier = modifier
                .wrapContentSize()
                .background(color = colors.trackColor, shape = ZoomBarDefaults.TrackShape)
                .testTag(ZOOM_BUTTON_ROW_TAG)
                .semantics {
                    isTraversalGroup = true
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
                        .drawZoomIndicator(
                            color = colors.selectedIndicatorColor,
                            shape = indicatorShape,
                            orientation = orientation,
                            lastIndex = zoomLevels.lastIndex,
                            // The indicator is inset equally from the track's ends and sides, so
                            // its rounded ends are already concentric with the track's end caps.
                            extendToTrackEnds = false,
                            positionProvider = { indicatorPosition }
                        )
                        .testTag(ZOOM_BAR_INDICATOR_TAG)
                )

                Row(
                    modifier = Modifier
                        .selectableGroup()
                        .padding(horizontal = ZoomBarDefaults.Inset),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ZoomStopItems()
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
                        .drawZoomIndicator(
                            color = colors.selectedIndicatorColor,
                            shape = indicatorShape,
                            orientation = orientation,
                            lastIndex = zoomLevels.lastIndex,
                            // The indicator is flush with the track's sides, so on the first and
                            // last stops it extends to the ends of the track to fill the end caps.
                            extendToTrackEnds = true,
                            positionProvider = { indicatorPosition }
                        )
                        .testTag(ZOOM_BAR_INDICATOR_TAG)
                )

                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .padding(vertical = ZoomBarDefaults.Inset),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ZoomStopItems()
                }
            }
        }
    }
}

/**
 * How far the indicator has moved onto an end stop that is [distance] stops away, from 0 (one or
 * more stops away) to 1 (on or past the end stop).
 */
internal fun endCapFraction(distance: Float): Float = (1f - distance).coerceIn(0f, 1f)

/**
 * Draws the indicator pill behind the selected stop.
 *
 * As the indicator moves onto the first or last stop, the side facing the track's end cap morphs
 * from the corner radius of [shape] to a fully rounded end so that it follows the curve of the end
 * cap. When [extendToTrackEnds] is `true`, that side also extends to the exact pixel edge of the
 * track's end cap, keeping the end cap completely filled across fractional screen densities when
 * the indicator is flush with the track's sides. With a single stop, both sides are rounded.
 */
private fun Modifier.drawZoomIndicator(
    color: Color,
    shape: CornerBasedShape,
    orientation: Orientation,
    lastIndex: Int,
    extendToTrackEnds: Boolean,
    positionProvider: () -> Float
): Modifier = drawBehind {
    val position = positionProvider()
    val startFraction = endCapFraction(distance = position)
    val endFraction = endCapFraction(distance = lastIndex - position)
    val isHorizontal = orientation == Orientation.Horizontal
    val cornerRadius = shape.topStart.toPx(size, this)
    val capRadius = (if (isHorizontal) size.height else size.width) / 2f
    val startRadius = lerp(cornerRadius, capRadius, startFraction)
    val endRadius = lerp(cornerRadius, capRadius, endFraction)
    val startMaxExtensionPx: Float
    val endMaxExtensionPx: Float
    if (extendToTrackEnds) {
        val insetPx = ZoomBarDefaults.Inset.roundToPx()
        val slotSizePx = ZoomBarDefaults.SlotSize.roundToPx()
        val indicatorHeightPx = ZoomBarDefaults.IndicatorHeight.roundToPx()
        val leadingCenteringPx = (slotSizePx - indicatorHeightPx) / 2
        val trailingCenteringPx = (slotSizePx - indicatorHeightPx) - leadingCenteringPx
        startMaxExtensionPx = (insetPx + leadingCenteringPx).toFloat()
        endMaxExtensionPx = (insetPx + trailingCenteringPx).toFloat()
    } else {
        startMaxExtensionPx = 0f
        endMaxExtensionPx = 0f
    }
    val startExtension = startMaxExtensionPx * startFraction
    val endExtension = endMaxExtensionPx * endFraction
    // In a right-to-left row, the first stop is on the right.
    val leadingExtension =
        if (isHorizontal && layoutDirection == LayoutDirection.Rtl) endExtension else startExtension
    // Corner radii are in the order topStart, topEnd, bottomEnd, bottomStart.
    val pillShape = if (isHorizontal) {
        RoundedCornerShape(startRadius, endRadius, endRadius, startRadius)
    } else {
        RoundedCornerShape(startRadius, startRadius, endRadius, endRadius)
    }
    val extension = startExtension + endExtension
    val pillSize = if (isHorizontal) {
        size.copy(width = size.width + extension)
    } else {
        size.copy(height = size.height + extension)
    }
    translate(
        left = if (isHorizontal) -leadingExtension else 0f,
        top = if (isHorizontal) 0f else -leadingExtension
    ) {
        drawOutline(pillShape.createOutline(pillSize, layoutDirection, this), color)
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

    val labelText = remember(displayText) { buildZoomLabelAnnotatedString(displayText) }

    val accessibleZoomLabel = remember(value, locale) {
        formatAccessibleZoomLabel(value, locale)
    }
    val stopContentDescription = stringResource(
        R.string.zoom_stop_content_description,
        accessibleZoomLabel
    )
    val onClickLabel = stringResource(R.string.zoom_stop_click_label, accessibleZoomLabel)
    val pinchedStateDescription = if (isSelected && !isExactStopSelected) {
        stringResource(
            R.string.zoom_stop_current_ratio_state_description,
            formatZoomDecimal(tenths = displayTenths, keepLeadingZero = true, locale = locale)
        )
    } else {
        null
    }
    val fineZoomActions = if (isSelected) {
        buildFineZoomActions(
            displayTenths = displayTenths,
            minZoom = minZoom,
            maxZoom = maxZoom,
            stepInLabel = stringResource(R.string.zoom_action_step_in),
            stepOutLabel = stringResource(R.string.zoom_action_step_out),
            currentRatioProvider = currentRatioProvider,
            onChangeZoom = { currentOnChangeZoom(it) }
        )
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
            .testTag(getZoomButtonTestTag(value))
            .semantics {
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
                    .padding(horizontal = ZoomBarTokens.LabelHorizontalPadding)
                    .semantics {
                        contentDescription = stopContentDescription
                    }
                    .uprightScaledZoomLabel(labelRotationProvider),
                softWrap = false
            )
        }
    }
}

private fun buildZoomLabelAnnotatedString(displayText: String): AnnotatedString =
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

private fun buildFineZoomActions(
    displayTenths: Int,
    minZoom: Float,
    maxZoom: Float,
    stepInLabel: String,
    stepOutLabel: String,
    currentRatioProvider: () -> Float,
    onChangeZoom: (Float) -> Unit
): List<CustomAccessibilityAction> = buildList {
    val minZoomTenths = toTenths(minZoom)
    val maxZoomTenths = toTenths(maxZoom)
    if (displayTenths + ZoomBarTokens.FINE_STEP_TENTHS <= maxZoomTenths) {
        add(
            CustomAccessibilityAction(label = stepInLabel) {
                val nextTenths = toTenths(currentRatioProvider()) + ZoomBarTokens.FINE_STEP_TENTHS
                val nextZoom = (nextTenths / ZoomBarTokens.TENTHS_PER_UNIT_FLOAT)
                    .coerceAtMost(maxZoom)
                onChangeZoom(nextZoom)
                true
            }
        )
    }
    if (displayTenths - ZoomBarTokens.FINE_STEP_TENTHS >= minZoomTenths) {
        add(
            CustomAccessibilityAction(label = stepOutLabel) {
                val nextTenths = toTenths(currentRatioProvider()) - ZoomBarTokens.FINE_STEP_TENTHS
                val nextZoom = (nextTenths / ZoomBarTokens.TENTHS_PER_UNIT_FLOAT)
                    .coerceAtLeast(minZoom)
                onChangeZoom(nextZoom)
                true
            }
        )
    }
}

private fun Modifier.uprightScaledZoomLabel(labelRotationProvider: () -> Float): Modifier =
    graphicsLayer {
        val rotation = labelRotationProvider()
        rotationZ = rotation
        if (size.width > 0f) {
            val rotationRadians = Math.toRadians(rotation.toDouble())
            val unrotatedMaxSpanPx =
                (ZoomBarDefaults.SlotSize - ZoomBarTokens.UnrotatedLabelPillMargin).toPx()
            val rotatedMaxSpanPx =
                (ZoomBarDefaults.IndicatorHeight - ZoomBarTokens.RotatedLabelPillMargin).toPx()
            val maxSpanPx = lerp(
                unrotatedMaxSpanPx,
                rotatedMaxSpanPx,
                abs(sin(rotationRadians)).toFloat()
            )
            val scale = if (size.width > maxSpanPx) {
                (maxSpanPx / size.width).coerceAtLeast(ZoomBarTokens.MIN_LABEL_SCALE)
            } else {
                1f
            }
            scaleX = scale
            scaleY = scale
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

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

/**
 * Renders a [ZoomBar] on a dark background, at [zoomRatio] with the given [zoomLevels], for
 * previews and screenshot tests.
 */
@Composable
internal fun ZoomBarPreview(
    zoomRatio: Float,
    orientation: Orientation = Orientation.Horizontal,
    zoomLevels: List<Float> = listOf(0.5f, 1.0f, 2.0f, 5.0f),
    colors: ZoomBarColors = ZoomBarDefaults.black60Colors(),
    labelRotationDegrees: Float = 0f
) {
    Box(
        modifier = Modifier
            .background(Color(0xFF2B2B2B))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        ZoomBar(
            zoomControlUiState = ZoomControlUiState.Enabled(
                zoomLevels = zoomLevels,
                primaryLensFacing = LensFacing.BACK,
                primaryZoomRatio = zoomRatio
            ),
            onChangeZoom = {},
            orientation = orientation,
            colors = colors,
            labelRotationDegrees = labelRotationDegrees
        )
    }
}

@Preview(name = "Horizontal 60% Black (Overlapping Viewfinder)", showBackground = true)
@Composable
private fun ZoomBarHorizontalDarkPreview() {
    ZoomBarPreview(zoomRatio = 1.0f)
}

@Preview(name = "Horizontal 20% White (Not Overlapping Viewfinder)", showBackground = true)
@Composable
private fun ZoomBarHorizontalTranslucentPreview() {
    ZoomBarPreview(zoomRatio = 1.0f, colors = ZoomBarDefaults.white20Colors())
}

@Preview(name = "Horizontal - Fraction (.5x Selected)", showBackground = true)
@Composable
private fun ZoomBarHorizontalFractionPreview() {
    ZoomBarPreview(zoomRatio = 0.5f)
}

@Preview(name = "Horizontal - Last Stop (5x Selected)", showBackground = true)
@Composable
private fun ZoomBarHorizontalLastStopPreview() {
    ZoomBarPreview(zoomRatio = 5.0f)
}

@Preview(name = "Vertical 60% Black - Flush Sides", showBackground = true)
@Composable
private fun ZoomBarVerticalDarkPreview() {
    ZoomBarPreview(zoomRatio = 1.0f, orientation = Orientation.Vertical)
}

@Preview(name = "Vertical - First Stop (.5x Selected)", showBackground = true)
@Composable
private fun ZoomBarVerticalFirstStopPreview() {
    ZoomBarPreview(zoomRatio = 0.5f, orientation = Orientation.Vertical)
}

@Preview(name = "Vertical - Last Stop (5x Selected)", showBackground = true)
@Composable
private fun ZoomBarVerticalLastStopPreview() {
    ZoomBarPreview(zoomRatio = 5.0f, orientation = Orientation.Vertical)
}

@Preview(name = "Single Stop (1x Only)", showBackground = true)
@Composable
private fun ZoomBarSingleStopPreview() {
    ZoomBarPreview(zoomRatio = 1.0f, zoomLevels = listOf(1.0f), colors = ZoomBarDefaults.colors())
}

@Preview(name = "Extended Telephoto Range (2x Selected)", showBackground = true)
@Composable
private fun ZoomBarExtendedRangePreview() {
    ZoomBarPreview(
        zoomRatio = 2.0f,
        zoomLevels = listOf(0.5f, 1.0f, 2.0f, 5.0f, 10.0f),
        colors = ZoomBarDefaults.colors()
    )
}

@Preview(name = "Horizontal RTL - 1x Selected", locale = "ar", showBackground = true)
@Composable
private fun ZoomBarHorizontalRtlPreview() {
    ZoomBarPreview(zoomRatio = 1.0f)
}

@Preview(name = "Horizontal RTL - Fraction (.5x Selected)", locale = "ar", showBackground = true)
@Composable
private fun ZoomBarHorizontalRtlFractionPreview() {
    ZoomBarPreview(zoomRatio = 0.5f)
}

@Preview(name = "Vertical RTL - Flush Sides", locale = "ar", showBackground = true)
@Composable
private fun ZoomBarVerticalRtlPreview() {
    ZoomBarPreview(zoomRatio = 1.0f, orientation = Orientation.Vertical)
}

@Preview(name = "Horizontal - Rotated Labels (90 deg)", showBackground = true)
@Composable
private fun ZoomBarHorizontalRotatedLabelsPreview() {
    ZoomBarPreview(zoomRatio = 1.0f, labelRotationDegrees = 90f)
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
