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
package com.google.jetpackcamera.ui.components.capture.quicksettings

import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared container styling for every Quick Settings sheet, so the compact bottom sheet and the
 * large-screen panel cannot drift apart.
 *
 * The sheet is always dark regardless of the app theme, matching the camera design mocks. Rows
 * inside the sheet use theme-independent `*Fixed` color roles, and text inherits [ContentColor],
 * so contrast holds in both light and dark themes.
 */
internal object QuickSettingsSheetDefaults {
    /** Corner radius of the sheet's free edges. */
    val CornerRadius: Dp = 28.dp

    /** Shape of the compact bottom sheet: rounded top corners, flush with the bottom edge. */
    val BottomSheetShape: Shape = RoundedCornerShape(topStart = CornerRadius, topEnd = CornerRadius)

    /**
     * Shape of a side sheet flush with its attached edge: only the free-edge corners are rounded.
     */
    fun sideSheetShape(attachedToLeft: Boolean): Shape = if (attachedToLeft) {
        AbsoluteRoundedCornerShape(topRight = CornerRadius, bottomRight = CornerRadius)
    } else {
        AbsoluteRoundedCornerShape(topLeft = CornerRadius, bottomLeft = CornerRadius)
    }

    /** Black at roughly 95% opacity, independent of the app color scheme. */
    val ContainerColor: Color = Color.Black.copy(alpha = 0.95f)

    /** Default color for text and icons drawn directly on [ContainerColor]. */
    val ContentColor: Color = Color.White

    /** Drag handle tint on [ContainerColor]. */
    val DragHandleColor: Color = ContentColor.copy(alpha = 0.4f)

    /** Material 3 level-3 shadow elevation. */
    val ShadowElevation: Dp = 6.dp

    /** Bottom padding below the compact bottom sheet's content, above the navigation bar inset. */
    val BottomSheetContentBottomPadding: Dp = 24.dp
}
