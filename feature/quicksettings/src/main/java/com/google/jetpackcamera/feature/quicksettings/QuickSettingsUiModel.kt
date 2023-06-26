/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.google.jetpackcamera.feature.quicksettings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.jetpackcamera.quicksettings.R

/**
 * An instance of this class represents the UI of a quick setting feature. A list of instances of
 * this class should be supplied to [QuickSettingsScreen] to reflect the UI in the quick settings.
 *
 * @param drawableResIds           resource ids for the icons corresponding to the options of this
 *                                 feature
 * @param textResIds               resource ids for the text corresponding to the options of this
 *                                 feature
 * @param descriptionResIds        resource ids for the description corresponding to the options of
 *                                 this feature
 * @param currentQuickSettingEnum  the current option of this feature
 * @param onClick                  the onClick callback for the options of this feature
 */
internal data class QuickSettingsUiModel(
    @DrawableRes val drawableResIds: List<Int>,
    @StringRes val textResIds: List<Int>,
    @StringRes val descriptionResIds: List<Int>,
    val availableQuickSettingsEnums: List<QuickSettingsEnum>,
    val currentQuickSettingEnum: QuickSettingsEnum,
    val onClick: (QuickSettingsEnum) -> Unit
)

internal fun getQuickSettingsUiModelList(
    lensFace: CameraLensFace,
    flashMode: CameraFlashMode,
    aspectRatio: CameraAspectRatio,
    timer: CameraTimer,
    onLensFaceClick: (lensFace: CameraLensFace) -> Unit,
    onFlashModeClick: (flashMode: CameraFlashMode) -> Unit,
    onAspectRatioClick: (aspectRatio: CameraAspectRatio) -> Unit,
    onTimerClick: (timer: CameraTimer) -> Unit
): List<QuickSettingsUiModel> {
    return listOf(
        QuickSettingsUiModel(
            drawableResIds = listOf(
                R.drawable.baseline_cameraswitch_72,
                R.drawable.baseline_cameraswitch_72
            ),
            textResIds = listOf(
                R.string.quick_settings_front_camera_text,
                R.string.quick_settings_back_camera_text
            ),
            descriptionResIds = listOf(
                R.string.quick_settings_front_camera_description,
                R.string.quick_settings_back_camera_description
            ),
            availableQuickSettingsEnums = CameraLensFace.values().asList(),
            currentQuickSettingEnum = lensFace,
            onClick = onLensFaceClick as (QuickSettingsEnum) -> Unit
        ),
        QuickSettingsUiModel(
            drawableResIds = listOf(
                R.drawable.baseline_flash_off_72,
                R.drawable.baseline_flash_auto_72,
                R.drawable.baseline_flash_on_72,
            ),
            textResIds = listOf(
                R.string.quick_settings_flash_off,
                R.string.quick_settings_flash_auto,
                R.string.quick_settings_flash_on,
            ),
            descriptionResIds = listOf(
                R.string.quick_settings_flash_off_description,
                R.string.quick_settings_flash_auto_description,
                R.string.quick_settings_flash_on_description,
            ),
            availableQuickSettingsEnums = CameraFlashMode.values().asList(),
            currentQuickSettingEnum = flashMode,
            onClick = onFlashModeClick as (QuickSettingsEnum) -> Unit
        ),
        QuickSettingsUiModel(
            drawableResIds = listOf(
                R.drawable.baseline_aspect_ratio_72,
                R.drawable.baseline_aspect_ratio_72,
                R.drawable.baseline_aspect_ratio_72,
            ),
            textResIds = listOf(
                R.string.quick_settings_aspect_ratio_3_4,
                R.string.quick_settings_aspect_ratio_9_16,
                R.string.quick_settings_aspect_ratio_1_1,
            ),
            descriptionResIds = listOf(
                R.string.quick_settings_aspect_ratio_3_4_description,
                R.string.quick_settings_aspect_ratio_9_16_description,
                R.string.quick_settings_aspect_ratio_1_1_description,
            ),
            availableQuickSettingsEnums = CameraAspectRatio.values().asList(),
            currentQuickSettingEnum = aspectRatio,
            onClick = onAspectRatioClick as (QuickSettingsEnum) -> Unit
        ),
        QuickSettingsUiModel(
            drawableResIds = listOf(
                R.drawable.baseline_timer_off_72,
                R.drawable.baseline_timer_3_72,
                R.drawable.baseline_timer_10_72,
            ),
            textResIds = listOf(
                R.string.quick_settings_timer_off,
                R.string.quick_settings_timer_3,
                R.string.quick_settings_timer_10,
            ),
            descriptionResIds = listOf(
                R.string.quick_settings_timer_off_description,
                R.string.quick_settings_timer_3_description,
                R.string.quick_settings_timer_10_description,
            ),
            availableQuickSettingsEnums = CameraTimer.values().asList(),
            currentQuickSettingEnum = timer,
            onClick = onTimerClick as (QuickSettingsEnum) -> Unit
        )
    )
}
