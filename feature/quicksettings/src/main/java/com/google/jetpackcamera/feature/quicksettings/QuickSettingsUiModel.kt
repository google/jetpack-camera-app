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

/**
 * An instance of this class represents the UI of a quick setting feature. A list of instances of
 * this class should be supplied to [QuickSettingsScreen] to reflect the UI in the quick settings.
 *
 * @param currentQuickSettingEnum  the current option of this feature
 * @param onClick                  the onClick callback for the options of this feature
 */
internal data class QuickSettingsUiModel(
    val availableQuickSettingsEnums: List<QuickSettingsEnum>,
    val currentQuickSettingEnum: QuickSettingsEnum,
    val onClick: (QuickSettingsEnum) -> Unit
)

internal fun getQuickSettingsUiModelList(
    lensFace: CameraLensFace,
    flashMode: CameraFlashMode,
    aspectRatio: CameraAspectRatio,
    timer: CameraTimer,
    availableLensFace: List<CameraLensFace>,
    availableFlashMode: List<CameraFlashMode>,
    availableAspectRatio: List<CameraAspectRatio>,
    availableTimer: List<CameraTimer>,
    onLensFaceClick: (lensFace: CameraLensFace) -> Unit,
    onFlashModeClick: (flashMode: CameraFlashMode) -> Unit,
    onAspectRatioClick: (aspectRatio: CameraAspectRatio) -> Unit,
    onTimerClick: (timer: CameraTimer) -> Unit
): List<QuickSettingsUiModel> {
    val result = mutableListOf<QuickSettingsUiModel>()
    if (availableLensFace.size > 1) {
        result.add(QuickSettingsUiModel(
            availableQuickSettingsEnums = availableLensFace,
            currentQuickSettingEnum = lensFace,
            onClick = onLensFaceClick as (QuickSettingsEnum) -> Unit
        ))
    }
    if (availableFlashMode.size > 1) {
        result.add(QuickSettingsUiModel(
            availableQuickSettingsEnums = availableFlashMode,
            currentQuickSettingEnum = flashMode,
            onClick = onFlashModeClick as (QuickSettingsEnum) -> Unit
        ))
    }
    if (availableAspectRatio.size > 1) {
        result.add(QuickSettingsUiModel(
            availableQuickSettingsEnums = availableAspectRatio,
            currentQuickSettingEnum = aspectRatio,
            onClick = onAspectRatioClick as (QuickSettingsEnum) -> Unit
        ))
    }
    if (availableTimer.size > 1) {
        result.add(QuickSettingsUiModel(
            availableQuickSettingsEnums = availableTimer,
            currentQuickSettingEnum = timer,
            onClick = onTimerClick as (QuickSettingsEnum) -> Unit
        ))
    }
    return result.toList()
}
