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

import androidx.compose.ui.tooling.preview.Preview

/**
 * MultiPreview annotation that generates Compose previews across a diverse set of portrait device
 * screen sizes and form factors:
 *
 * - **Standard Phone** (412 × 915 dp): Typical modern tall smartphone (e.g. Pixel 8 / 20:9).
 * - **Compact Phone / Short Screen** (360 × 640 dp): Smaller screen or multi-window split screen
 *   where available height is compressed below 600dp, exercising the compact control stack layout.
 * - **Foldable (Unfolded Portrait)** (673 × 841 dp): Foldable device inner display in portrait.
 * - **Tablet (Portrait)** (800 × 1280 dp): 10" tablet in portrait orientation.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Preview(
    name = "1. Phone - Standard",
    device = "spec:width=412dp,height=915dp",
    showSystemUi = true
)
@Preview(
    name = "2. Phone - Compact (Short)",
    device = "spec:width=360dp,height=640dp",
    showSystemUi = true
)
@Preview(
    name = "3. Foldable - Unfolded Portrait",
    device = "spec:width=673dp,height=841dp",
    showSystemUi = true
)
@Preview(
    name = "4. Tablet - Portrait",
    device = "spec:width=800dp,height=1280dp,dpi=240",
    showSystemUi = true
)
annotation class PreviewPortraitDevices
