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

package com.google.jetpackcamera.settings.api

import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS


data class DeveloperAppConfig(
    val captureMode: SettingConfig<CaptureMode>,
    val aspectRatio: SettingConfig<AspectRatio>,
    val flashMode: SettingConfig<FlashMode>,
    val audio: SettingConfig<Boolean>,
    val hdrEnabled: SettingConfig<Boolean>,
) {
    // checks validity of all individual setting configs
    init {
        fun <T : Any> SettingConfig<T>.containsIfOptionsEnabled(options: Set<T>): Boolean {
            return when (val restriction = this.uiRestriction) {
                is OptionRestrictionConfig.OptionsEnabled -> {
                    restriction.enabledOptions.containsAll(options)
                }

                else -> true
            }
        }

        require(flashMode.containsIfOptionsEnabled(setOf(FlashMode.OFF)))
    }

    companion object {
        // Provides the baseline, fully non-restricted configuration
        val LibraryDefaults: DeveloperAppConfig = DeveloperAppConfig(
            aspectRatio = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.aspectRatio),
            flashMode = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.flashMode),
            captureMode = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.captureMode),
            audio = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.audioEnabled),
            hdrEnabled = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.dynamicRange != DynamicRange.SDR),
        )
    }

    fun toCameraAppSettings(
        defaultSettings: CameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS
    ): CameraAppSettings {
        val imageOutputFormat = if (this.hdrEnabled.defaultValue)
            ImageOutputFormat.JPEG_ULTRA_HDR
        else
            ImageOutputFormat.JPEG

        val dynamicRange =
            if (this.hdrEnabled.defaultValue) DynamicRange.HLG10 else DynamicRange.SDR


        return defaultSettings.copy(
            aspectRatio = this.aspectRatio.defaultValue,
            flashMode = this.flashMode.defaultValue,
            captureMode = this.captureMode.defaultValue,
            audioEnabled = this.audio.defaultValue,
            imageFormat = imageOutputFormat,
            dynamicRange = dynamicRange
        )
    }
}

data class SettingConfig<T>(
    val defaultValue: T,
    val uiRestriction: OptionRestrictionConfig<T> = OptionRestrictionConfig.NotRestricted()
) {
    init {
        (uiRestriction as? OptionRestrictionConfig.OptionsEnabled)?.let {
            require(
                uiRestriction.enabledOptions.size >= 2 &&
                        uiRestriction.enabledOptions.contains(
                            defaultValue
                        )
            ) {
                "OptionsRestrictionConfig.OptionsEnabled#enabledOptions must also contain the defaultValue"
            }
        }
    }
}


sealed interface OptionRestrictionConfig<T> {
    /** All device-supported options are available. */
    class NotRestricted<T> : OptionRestrictionConfig<T>

    /** The entire setting is unavailable and hidden from the UI. */
    class FullyRestricted<T> : OptionRestrictionConfig<T>

    /** ONLY the options in this set are allowed, if supported by the device. */
    data class OptionsEnabled<T>(val enabledOptions: Set<T>) : OptionRestrictionConfig<T> {
        init {
            require(enabledOptions.isNotEmpty()) {
                "enabledOptions must have at least one option. " +
                        "Use FullyRestricted to disable the feature."
            }
        }
    }
}