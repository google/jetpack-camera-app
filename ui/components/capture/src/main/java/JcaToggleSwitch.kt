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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.ui.components.capture.theme.PreviewPreviewTheme

/**
 * A custom Switch component built with Canvas, supporting drag gestures
 * and custom icons for on/off states, styled for the JCA project.
 *
 * @param checked Whether the switch is currently checked (on).
 * @param onCheckedChange Lambda called when the switch state changes.
 * @param modifier Modifier for this component.
 * @param toggleMode whether to treat the appearance like on/off or simple toggle
 * @param enabled Whether the switch is interactive.
 * @param trackColor The color of the track when unchecked.
 * @param thumbColor The color of the thumb and the checked track.
 * @param leftIcon The [ImageVector] (e.g., Google Symbol) for the 'off' state.
 * @param rightIcon The [ImageVector] (e.g., Google Symbol) for the 'on' state.
 * @param offIconColor The tint color for the 'off' icon.
 * @param onIconColor The tint color for the 'on' icon.
 */
@Composable
fun JcaSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    toggleMode: Boolean = true,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trackColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    leftIcon: ImageVector? = null,
    rightIcon: ImageVector? = null,
    offIconColor: Color = Color.White.copy(alpha = 0.8f), // Color for the 'Off' icon
    onIconColor: Color = Color.Black.copy(alpha = 0.8f),  // Color for the 'On' icon
) {
    // --- 1. Dimensions ---
    // Encapsulate all Dp and Px calculations
    val dims = rememberSwitchDimensions(
        switchWidth = 60.dp,
        switchHeight = 32.dp,
        thumbDiameter = 24.dp,
        iconSize = 16.dp
    )
    val thumbY = dims.switchHeightPx / 2
    val trackCornerRadius = CornerRadius(dims.switchHeightPx / 2, dims.switchHeightPx / 2)

    // --- 2. Drag and Animation State ---
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val targetPosition = if (checked) 1f else 0f

    // Animate the thumb position.
    // This snaps (0ms) during drag and animates (300ms) on tap or drag release.
    val animatedPosition by animateFloatAsState(
        targetValue = (targetPosition + (dragOffset / dims.dragRange)).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = if (dragOffset == 0f) 300 else 0),
        label = "thumbPosition"
    )
    val initialThumbX = dims.startX + dims.dragRange * animatedPosition

    // --- 3. Color Animations ---
    //color changes if togglemode is off (to indicate on/off state)
    val trackAnimatedColor by animateColorAsState(
        targetValue = if (checked || toggleMode) thumbColor.copy(alpha = 0.5f) else trackColor.copy(alpha = 0.38f),
        animationSpec = tween(durationMillis = 300),
        label = "trackColor"
    )
    val thumbAnimatedColor by animateColorAsState(
        targetValue = if (checked || toggleMode) thumbColor else trackColor,
        animationSpec = tween(durationMillis = 300),
        label = "thumbColor"
    )

    // --- 4. Icon Painters ---
    val offIconPainter = leftIcon?.let { rememberVectorPainter(image = leftIcon) }
    val onIconPainter = rightIcon?.let { rememberVectorPainter(image = rightIcon) }

    // --- 5. Gesture Handlers ---
    val draggableState = rememberDraggableState { delta ->
        val newOffset = dragOffset + delta
        // Constrain drag within the allowed range based on the *current* state
        val dragMin = if (checked) -dims.dragRange else 0f
        val dragMax = if (checked) 0f else dims.dragRange
        dragOffset = newOffset.coerceIn(dragMin, dragMax)
    }

    val gestureModifier = if (enabled) {
        modifier
            .pointerInput(checked) { // Re-key gesture input when 'checked' changes
                detectTapGestures(
                    onTap = {
                        onCheckedChange(!checked)
                        dragOffset = 0f
                    }
                )
            }
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    // Determine new state based on final drag position
                    val finalThumbPos = initialThumbX - dims.startX
                    val newCheckedState = finalThumbPos > (dims.dragRange / 2)
                    if (newCheckedState != checked) {
                        onCheckedChange(newCheckedState)
                    }
                    // Reset drag offset after drag stops
                    dragOffset = 0f
                }
            )
    } else {
        modifier
    }

    // --- 6. Canvas Drawing ---
    Canvas(
        modifier = modifier
            .size(dims.switchWidthDp, dims.switchHeightDp)
            .then(gestureModifier)
    ) {
        // 1. Draw the Track
        drawSwitchTrack(
            color = trackAnimatedColor,
            cornerRadius = trackCornerRadius
        )

        // 2. Calculate Thumb Rect and Icon Padding
        val thumbRect = Rect(
            left = initialThumbX - dims.thumbRadiusPx,
            top = thumbY - dims.thumbRadiusPx,
            right = initialThumbX + dims.thumbRadiusPx,
            bottom = thumbY + dims.thumbRadiusPx
        )
        val iconPadding = (dims.switchHeightPx - dims.iconSizePx) / 2

        // 4. Draw the Thumb (under Icons)
        drawSwitchThumb(
            center = Offset(initialThumbX, thumbY),
            radius = dims.thumbRadiusPx,
            color = thumbAnimatedColor.copy(alpha = .5f)
        )

        // 3. Draw the Clipped Icons
        if (offIconPainter != null || onIconPainter != null)
        drawClippedSwitchIcons(
            thumbRect = thumbRect,
            iconSizePx = dims.iconSizePx,
            iconPadding = iconPadding,
            leftIconPainter = offIconPainter,
            rightIconPainter = onIconPainter,
            offIconColor = offIconColor,
            onIconColor = onIconColor
        )

        // 5. Draw Disabled state overlay
        if (!enabled) {
            drawDisabledOverlay(
                color = Color.Black.copy(alpha = 0.12f),
                cornerRadius = trackCornerRadius
            )
        }
    }
}

// --- Helper class and functions ---

/**
 * A holder for switch dimensions in both Dp and Px.
 */
private class SwitchDimensions(
    val switchWidthDp: Dp,
    val switchHeightDp: Dp,
    val switchWidthPx: Float,
    val switchHeightPx: Float,
    val thumbRadiusPx: Float,
    val iconSizePx: Float,
    val startX: Float,
    val dragRange: Float
)

/**
 * Calculates and remembers all necessary pixel dimensions for the switch.
 */
@Composable
private fun rememberSwitchDimensions(
    switchWidth: Dp,
    switchHeight: Dp,
    thumbDiameter: Dp,
    iconSize: Dp
): SwitchDimensions {
    val density = LocalDensity.current
    return remember(density, switchWidth, switchHeight, thumbDiameter, iconSize) {
        val switchWidthPx = with(density) { switchWidth.toPx() }
        val switchHeightPx = with(density) { switchHeight.toPx() }
        val thumbRadiusPx = with(density) { thumbDiameter.toPx() / 2 }
        val paddingPx = with(density) { (switchHeight - thumbDiameter).toPx() / 2 }
        val iconSizePx = with(density) { iconSize.toPx() }

        val startX = paddingPx + thumbRadiusPx
        val endX = switchWidthPx - paddingPx - thumbRadiusPx
        val dragRange = endX - startX

        SwitchDimensions(
            switchWidthDp = switchWidth,
            switchHeightDp = switchHeight,
            switchWidthPx = switchWidthPx,
            switchHeightPx = switchHeightPx,
            thumbRadiusPx = thumbRadiusPx,
            iconSizePx = iconSizePx,
            startX = startX,
            dragRange = dragRange
        )
    }
}

/**
 * Draws the switch track (the background).
 */
private fun DrawScope.drawSwitchTrack(
    color: Color,
    cornerRadius: CornerRadius
) {
    drawRoundRect(
        color = color,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = cornerRadius
    )
}


/**
 * Draws the 'Off' and 'On' icons, clipping them based on the thumb's position.
 */
private fun DrawScope.drawSwitchIcons(
    iconSizePx: Float,
    iconPadding: Float,
    leftIconPainter: Painter?,
    rightIconPainter: Painter?,
    leftIconColor: Color,
    rightIconColor: Color,
) {
    val iconY = iconPadding
    val iconDrawSize = Size(iconSizePx, iconSizePx)

    // --- Draw the OFF Icon (Left Side) ---
    if(leftIconPainter != null) {
        val offIconX = iconPadding

        translate(
            left = offIconX,
            top = iconY
        ) {
            with(leftIconPainter) {
                draw(
                    size = iconDrawSize,
                    colorFilter = ColorFilter.tint(leftIconColor)
                )
            }
        }

    }
    // --- Draw the ON Icon (Right Side) ---
    if(rightIconPainter != null) {
        val onIconX = size.width - iconPadding - iconSizePx

        translate(
            left = onIconX,
            top = iconY
        ) {
            with(rightIconPainter) {
                draw(
                    size = iconDrawSize,
                    colorFilter = ColorFilter.tint(rightIconColor)
                )
            }
        }
    }
}

/**
 * Draws the 'Off' and 'On' icons, clipping them based on the thumb's position.
 */
private fun DrawScope.drawClippedSwitchIcons(
    thumbRect: Rect,
    iconSizePx: Float,
    iconPadding: Float,
    leftIconPainter: Painter?,
    rightIconPainter: Painter?,
    offIconColor: Color,
    onIconColor: Color,
) {
    val iconY = iconPadding
    val iconDrawSize = Size(iconSizePx, iconSizePx)

    // --- Draw the OFF Icon (Left Side) ---
    if(leftIconPainter != null) {
        val offIconX = iconPadding
        val offIconRect = Rect(offIconX, iconY, offIconX + iconSizePx, iconY + iconSizePx)

        // Clip area: The part *not* covered by the thumb
        val offClipRight =
            if (thumbRect.overlaps(offIconRect)) thumbRect.left else offIconRect.right

        translate(
            left = offIconX,
            top = iconY
        ) {
            with(leftIconPainter) {
                draw(
                    size = iconDrawSize,
                    //  topLeft = Offset(offIconX, iconY),
                    colorFilter = ColorFilter.tint(onIconColor)
                )
            }
        }
        clipRect(
            left = offIconRect.left,
            top = offIconRect.top,
            right = offClipRight.coerceIn(offIconRect.left, offIconRect.right),
            bottom = offIconRect.bottom
        ) {
            translate(
                left = offIconX,
                top = iconY
            ) {
                with(leftIconPainter) {
                    draw(
                        size = iconDrawSize,
                        //  topLeft = Offset(offIconX, iconY),
                        colorFilter = ColorFilter.tint(offIconColor)
                    )

                }
            }
        }
    }
    // --- Draw the ON Icon (Right Side) ---
    if(rightIconPainter != null) {
        val onIconX = size.width - iconPadding - iconSizePx
        val onIconRect = Rect(onIconX, iconY, onIconX + iconSizePx, iconY + iconSizePx)

        // Clip area: The part *not* covered by the thumb
        val onClipLeft = if (thumbRect.overlaps(onIconRect)) thumbRect.right else onIconRect.left

        translate(
            left = onIconX,
            top = iconY
        ) {
            with(rightIconPainter) {
                draw(
                    size = iconDrawSize,
                    colorFilter = ColorFilter.tint(onIconColor)
                )
            }
        }

        clipRect(
            left = onClipLeft.coerceIn(onIconRect.left, onIconRect.right),
            top = onIconRect.top,
            right = onIconRect.right,
            bottom = onIconRect.bottom
        ) {
            translate(
                left = onIconX,
                top = iconY
            ) {
                with(rightIconPainter) {
                    draw(
                        size = iconDrawSize,
                        colorFilter = ColorFilter.tint(offIconColor)
                    )
                }
            }
        }
    }
}

/**
 * Draws the thumb (the circular handle).
 */
private fun DrawScope.drawSwitchThumb(
    center: Offset,
    radius: Float,
    color: Color
) {
    drawCircle(
        color = color,
        radius = radius,
        center = center
    )
}

/**
 * Draws the semi-transparent overlay when the switch is disabled.
 */
private fun DrawScope.drawDisabledOverlay(
    color: Color,
    cornerRadius: CornerRadius
) {
    drawRoundRect(
        color = color,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = cornerRadius
    )
}

@Preview
@Composable
private fun Capture_ToggleSwitch_On_Disabled() {
    PreviewPreviewTheme(dynamicColor = false) {
        JcaSwitch(
            leftIcon = Icons.Outlined.CameraAlt,
            rightIcon = Icons.Filled.Videocam,
            checked = true,
            onCheckedChange = {},
            enabled = false
        )
    }
}

@Preview
@Composable
private fun Capture_ToggleSwitch_Off_Disabled() {
    PreviewPreviewTheme(dynamicColor = false) {
        JcaSwitch(
            leftIcon = Icons.Outlined.CameraAlt,
            rightIcon = Icons.Filled.Videocam,
            checked = false,
            onCheckedChange = {},
            enabled = true
        )
    }
}


@Preview
@Composable
private fun Capture_ToggleSwitch_On_Enabled() {
    PreviewPreviewTheme(dynamicColor = false) {
        JcaSwitch(
            leftIcon = Icons.Outlined.CameraAlt,
            rightIcon = Icons.Filled.Videocam,
            checked = true,
            onCheckedChange = {},
            enabled = true
        )
    }
}


@Preview
@Composable
private fun Capture_ToggleSwitch_Off_Enabled() {
    PreviewPreviewTheme(dynamicColor = false) {
        JcaSwitch(
            leftIcon = Icons.Outlined.CameraAlt,
            rightIcon = Icons.Filled.Videocam,
            checked = false,
            onCheckedChange = {},
            enabled = true
        )
    }
}


