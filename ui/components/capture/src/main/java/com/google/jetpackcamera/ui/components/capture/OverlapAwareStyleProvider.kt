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

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Receiver scope for [PreviewLayout] slots whose content can be drawn either over the viewfinder
 * or over the background around it.
 *
 * Content can use [isOverViewfinder] to choose its own colors, or use
 * [cameraControlContainerColor] to match the built-in camera controls.
 */
@Stable
interface PreviewOverlaySlotScope {
    /**
     * Whether the slot's content currently overlaps the viewfinder. Reading this value recomposes
     * the reader when the viewfinder moves under or away from the slot, for example after an
     * aspect ratio change.
     */
    val isOverViewfinder: Boolean

    /**
     * The container color that the built-in camera controls use at the slot's current position: a
     * translucent black over the viewfinder and a translucent white over the background around
     * it. Using this color is optional.
     */
    val cameraControlContainerColor: Color
}

private class PreviewOverlaySlotScopeImpl(
    private val isOverViewfinderState: State<Boolean>
) : PreviewOverlaySlotScope {
    override val isOverViewfinder: Boolean
        get() = isOverViewfinderState.value

    override val cameraControlContainerColor: Color
        get() = backgroundStyleFor(isOverViewfinder).containerColor
}

private fun backgroundStyleFor(isOverlapping: Boolean): CameraControlBackgroundStyle =
    if (isOverlapping) {
        CameraControlBackgroundStyle.BLACK_60
    } else {
        CameraControlBackgroundStyle.WHITE_20
    }

/**
 * Applies [LocalCameraControlBackgroundStyle] based on whether this
 * component's spatial bounds overlap with the [LocalOverlapTargetBounds].
 *
 * The [content] also receives the overlap result through [PreviewOverlaySlotScope], so that
 * content supplied by callers outside this module can adapt its own styling.
 *
 * @param modifier The [Modifier] to be applied to the outer layout.
 * @param overlapThreshold The ratio of overlap (between 0.0 and 1.0) required to switch the style.
 * @param content The composable content that will receive the computed [LocalCameraControlBackgroundStyle].
 */
@Composable
internal fun OverlapAwareStyleProvider(
    modifier: Modifier = Modifier,
    overlapThreshold: Float = 0.5f,
    content: @Composable PreviewOverlaySlotScope.() -> Unit
) {
    val targetBoundsState = LocalOverlapTargetBounds.current
    var myBounds by remember { mutableStateOf(Rect.Zero) }

    val isOverlappingState = remember(targetBoundsState, overlapThreshold) {
        derivedStateOf {
            val intersect = targetBoundsState.value.intersect(myBounds)
            val intersectArea = if (intersect.isEmpty) 0f else intersect.width * intersect.height
            val myArea = myBounds.width * myBounds.height
            myArea > 0f && (intersectArea / myArea) >= overlapThreshold
        }
    }
    val scope = remember(isOverlappingState) { PreviewOverlaySlotScopeImpl(isOverlappingState) }
    val style = backgroundStyleFor(isOverlappingState.value)

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            val b = coordinates.boundsInWindow()
            if (myBounds != b) {
                myBounds = b
            }
        }
    ) {
        CompositionLocalProvider(LocalCameraControlBackgroundStyle provides style) {
            scope.content()
        }
    }
}
