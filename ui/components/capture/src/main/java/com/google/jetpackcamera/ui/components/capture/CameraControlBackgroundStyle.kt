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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect

/**
 * Defines the background style variants for camera controls to maintain visual
 * contrast against varying viewfinder brightness levels.
 */
internal enum class CameraControlBackgroundStyle {
    BLACK_60,
    WHITE_20
}

private val DefaultOverlapTargetBounds = mutableStateOf(Rect.Zero)

/**
 * Provides the global bounds of the target overlapping region.
 * Elements can read this to determine their overlap with the targeted background (e.g. ViewFinder).
 */
internal val LocalOverlapTargetBounds = compositionLocalOf<MutableState<Rect>> {
    DefaultOverlapTargetBounds
}

/**
 * Current visual style for a control computed from target overlap.
 */
internal val LocalCameraControlBackgroundStyle = compositionLocalOf {
    CameraControlBackgroundStyle.WHITE_20
}
