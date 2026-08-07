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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Applies [LocalCameraControlBackgroundStyle] based on whether this
 * component's spatial bounds overlap with the [LocalOverlapTargetBounds].
 *
 * @param modifier The [Modifier] to be applied to the outer layout.
 * @param overlapThreshold The ratio of overlap (between 0.0 and 1.0) required to switch the style.
 * @param content The composable content that will receive the computed [LocalCameraControlBackgroundStyle].
 */
@Composable
internal fun OverlapAwareStyleProvider(
    modifier: Modifier = Modifier,
    overlapThreshold: Float = 0.5f,
    content: @Composable () -> Unit
) {
    val targetBounds = LocalOverlapTargetBounds.current.value
    var myBounds by remember { mutableStateOf(Rect.Zero) }

    val intersect = targetBounds.intersect(myBounds)
    val intersectArea = if (intersect.isEmpty) 0f else intersect.width * intersect.height
    val myArea = myBounds.width * myBounds.height
    val isOverlapping = myArea > 0f && (intersectArea / myArea) >= overlapThreshold

    val style = if (isOverlapping) {
        CameraControlBackgroundStyle.BLACK_60
    } else {
        CameraControlBackgroundStyle.WHITE_20
    }

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            val b = coordinates.boundsInWindow()
            if (myBounds != b) {
                myBounds = b
            }
        }
    ) {
        CompositionLocalProvider(LocalCameraControlBackgroundStyle provides style) {
            content()
        }
    }
}
