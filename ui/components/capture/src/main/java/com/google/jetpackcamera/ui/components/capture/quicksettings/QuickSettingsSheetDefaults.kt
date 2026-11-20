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
 * Container styling shared by the Quick Settings bottom and side sheets. The sheet is always dark
 * (per the mocks), so rows use theme-independent `*Fixed` roles and text inherits [ContentColor].
 */
internal object QuickSettingsSheetDefaults {
    val CornerRadius: Dp = 28.dp

    val BottomSheetShape: Shape = RoundedCornerShape(topStart = CornerRadius, topEnd = CornerRadius)

    /** Side sheet shape: flat on the attached side, rounded on the free edge. */
    fun sideSheetShape(attachedToLeft: Boolean): Shape = if (attachedToLeft) {
        AbsoluteRoundedCornerShape(topRight = CornerRadius, bottomRight = CornerRadius)
    } else {
        AbsoluteRoundedCornerShape(topLeft = CornerRadius, bottomLeft = CornerRadius)
    }

    val ContainerColor: Color = Color.Black.copy(alpha = 0.92f)

    val ContentColor: Color = Color.White

    val DragHandleColor: Color = ContentColor.copy(alpha = 0.4f)

    /** Material 3 level-3 elevation, matching the mocks' drop shadow. */
    val ShadowElevation: Dp = 6.dp

    /** Below the bottom sheet's content, above the navigation bar inset. */
    val BottomSheetContentBottomPadding: Dp = 24.dp
}
