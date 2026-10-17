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
package com.google.jetpackcamera.model

/**
 * Metadata describing an injectable capture sub-mode under a [parentCaptureMode].
 *
 * @property id Unique [CaptureSubModeId] of the sub-mode.
 * @property parentCaptureMode The parent [CaptureMode] under which this sub-mode appears.
 * @property labelResId Android string resource ID for the user-visible pill label in the carousel.
 * @property sortOrder Signed relative ordering index within [parentCaptureMode].
 *   - `0` is reserved for [CaptureSubModeId.DEFAULT] (the default parent mode pill).
 *   - Negative values (`< 0`, e.g. `-10`) place the sub-mode to the **left** of the default pill.
 *   - Positive values (`> 0`, e.g. `100`) place the sub-mode to the **right** of the default pill.
 *   - Equal [sortOrder] values are broken deterministically by placing [CaptureSubModeId.DEFAULT]
 *     first, then ordering lexicographically by [CaptureSubModeId.value].
 * @property quickSettingsTitleResId Optional Android string resource ID for the Quick Settings
 *   sheet title while this sub-mode is active (e.g. "Night settings"). When `null`, the title
 *   falls back to the title of [parentCaptureMode] (e.g. "Photo settings").
 */
data class CaptureSubModeDescriptor(
    val id: CaptureSubModeId,
    val parentCaptureMode: CaptureMode,
    val labelResId: Int,
    val sortOrder: Int = 100,
    val quickSettingsTitleResId: Int? = null
)
