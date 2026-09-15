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
package com.google.jetpackcamera.settings.model

import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat

/**
 * Defines a configuration for the Jetpack Camera App that can be used by developers
 * to override the default app settings.
 */
data class CameraAppConfig(
    val captureMode: SettingConfig<CaptureMode>? = null,
    val aspectRatio: SettingConfig<AspectRatio>? = null,
    val flashMode: SettingConfig<FlashMode>? = null,
    val imageFormat: SettingConfig<ImageOutputFormat>? = null,
    val dynamicRange: SettingConfig<DynamicRange>? = null
) {
    // Ensures that all individual setting configurations are valid.
    init {
        flashMode?.let { config ->
            when (val visibility = config.uiVisibility) {
                is OptionAvailabilityConfig.OptionsEnabled -> require(
                    FlashMode.OFF in visibility.enabledOptions
                ) {
                    "FlashMode.OFF must always be included in enabledOptions for flashMode."
                }
                is OptionAvailabilityConfig.Hidden -> require(config.defaultValue == FlashMode.OFF) {
                    "When flashMode is Hidden, defaultValue must be FlashMode.OFF."
                }
                is OptionAvailabilityConfig.NotRestricted -> Unit
            }
        }
    }

    /**
     * Converts this [CameraAppConfig] into a [CameraAppSettings] object.
     *
     * This function maps the developer-defined settings to the internal camera app settings model.
     */
    fun toCameraAppSettings(
        defaultSettings: CameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS
    ): CameraAppSettings {
        return defaultSettings.copy(
            aspectRatio = this.aspectRatio?.defaultValue ?: defaultSettings.aspectRatio,
            flashMode = this.flashMode?.defaultValue ?: defaultSettings.flashMode,
            captureMode = this.captureMode?.defaultValue ?: defaultSettings.captureMode,
            imageFormat = this.imageFormat?.defaultValue ?: defaultSettings.imageFormat,
            dynamicRange = this.dynamicRange?.defaultValue ?: defaultSettings.dynamicRange
        )
    }
}

/**
 * Represents a single configurable setting in the application, including its
 * default value and UI visibility / option availability.
 *
 * @param defaultValue The initial value for this setting.
 * @param uiVisibility The UI visibility and option availability configuration for this setting.
 */
data class SettingConfig<T>(
    val defaultValue: T,
    val uiVisibility: OptionAvailabilityConfig<T> = OptionAvailabilityConfig.NotRestricted
) {
    init {
        // Validate that if options are enabled for this setting, the default value
        // is always included in the set of enabled options.
        if (uiVisibility is OptionAvailabilityConfig.OptionsEnabled) {
            require(defaultValue in uiVisibility.enabledOptions) {
                "The defaultValue ('$defaultValue') must be one of the enabledOptions: " +
                        "${uiVisibility.enabledOptions}"
            }
        }
    }
}

/**
 * Represents UI option availability applied to a setting.
 */
sealed interface OptionAvailabilityConfig<out T> {
    /** All device-supported options are available. */
    data object NotRestricted : OptionAvailabilityConfig<Nothing>

    /** The entire setting is unavailable and hidden from the UI. */
    data object Hidden : OptionAvailabilityConfig<Nothing>

    /** ONLY the options in this set are allowed, if supported by the device. */
    data class OptionsEnabled<T>(val enabledOptions: Set<T>) : OptionAvailabilityConfig<T> {
        init {
            require(enabledOptions.size >= 2) {
                "enabledOptions must contain at least 2 options. Use Hidden to lock a single option and hide the control."
            }
        }
    }
}
