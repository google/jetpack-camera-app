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

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Design tokens for camera capture UI components.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal object CaptureTokens {
    /**
     * Standard red color indicating an active video recording state across capture components.
     */
    val RecordingRed = Color(0xFFED0000)

    /**
     * Material 3 Expressive fast spatial spring, used for size, corner radius, and border width
     * changes.
     */
    val FastSpatialSpec: FiniteAnimationSpec<Dp> = MotionScheme.expressive().fastSpatialSpec()

    /**
     * Material 3 Expressive fast effects spring, used for quick color transitions.
     */
    val FastEffectsSpec: FiniteAnimationSpec<Color> = MotionScheme.expressive().fastEffectsSpec()

    /**
     * Material 3 Expressive default effects spring, used for less prominent color transitions.
     */
    val DefaultEffectsSpec: FiniteAnimationSpec<Color> =
        MotionScheme.expressive().defaultEffectsSpec()

    /**
     * Stiff, slightly underdamped spring used for the press and release of a tap in standard
     * capture mode.
     */
    val SnappyStandardTapSpatialSpec: SpringSpec<Dp> =
        spring(dampingRatio = 0.65f, stiffness = 1800f)
}
