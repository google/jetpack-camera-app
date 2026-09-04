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

/**
 * Defines a configuration for the Jetpack Camera App that can be used by developers
 * to override the default app settings.
 */
data class DeveloperAppConfig(
    val captureMode: SettingConfig<CaptureMode> = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.captureMode),
    val aspectRatio: SettingConfig<AspectRatio> = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.aspectRatio),
    val flashMode: SettingConfig<FlashMode> = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.flashMode),
    val imageOutputFormat: SettingConfig<ImageOutputFormat> = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.imageFormat),
    val videoDynamicRange: SettingConfig<DynamicRange> = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.dynamicRange)
) {
    // Ensures that all individual setting configurations are valid.
    init {
        when (val restriction = flashMode.uiRestriction) {
            is OptionRestrictionConfig.OptionsEnabled -> require(FlashMode.OFF in restriction.enabledOptions) {
                "FlashMode.OFF must always be included in enabledOptions for flashMode."
            }
            is OptionRestrictionConfig.FullyRestricted -> require(flashMode.defaultValue == FlashMode.OFF) {
                "When flashMode is FullyRestricted, defaultValue must be FlashMode.OFF to prevent hardware flash lockup."
            }
            is OptionRestrictionConfig.NotRestricted -> Unit
        }
    }

    /**
     * Converts this [DeveloperAppConfig] into a [CameraAppSettings] object.
     *
     * This function maps the developer-defined settings to the internal camera app settings model.
     */
    fun toCameraAppSettings(
        defaultSettings: CameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS
    ): CameraAppSettings {
        return defaultSettings.copy(
            aspectRatio = this.aspectRatio.defaultValue,
            flashMode = this.flashMode.defaultValue,
            captureMode = this.captureMode.defaultValue,
            imageFormat = this.imageOutputFormat.defaultValue,
            dynamicRange = this.videoDynamicRange.defaultValue
        )
    }

    /**
     * Returns a copy of this [DeveloperAppConfig] with all UI restrictions removed,
     * preserving each setting's default value.
     */
    fun withoutRestrictions(): DeveloperAppConfig = DeveloperAppConfig(
        aspectRatio = SettingConfig(this.aspectRatio.defaultValue),
        flashMode = SettingConfig(this.flashMode.defaultValue),
        captureMode = SettingConfig(this.captureMode.defaultValue),
        imageOutputFormat = SettingConfig(this.imageOutputFormat.defaultValue),
        videoDynamicRange = SettingConfig(this.videoDynamicRange.defaultValue)
    )
}

/**
 * Represents a single configurable setting in the application, including its
 * default value and any UI restrictions that apply to it.
 *
 * @param defaultValue The initial value for this setting.
 * @param uiRestriction The restrictions applied to this setting in the UI.
 */
data class SettingConfig<T>(
    val defaultValue: T,
    val uiRestriction: OptionRestrictionConfig<T> = OptionRestrictionConfig.NotRestricted
) {
    init {
        // Validate that if options are enabled for this setting, the default value
        // is always included in the set of enabled options.
        if (uiRestriction is OptionRestrictionConfig.OptionsEnabled) {
            require(uiRestriction.enabledOptions.size >= 2) {
                "enabledOptions must contain at least 2 options. Use FullyRestricted to hide the control."
            }
            require(defaultValue in uiRestriction.enabledOptions) {
                "The defaultValue ('$defaultValue') must be one of the enabledOptions: ${uiRestriction.enabledOptions}"
            }
        }
    }
}

/**
 * Represents UI option restrictions applied to a setting.
 */
sealed interface OptionRestrictionConfig<out T> {
    /** All device-supported options are available. */
    data object NotRestricted : OptionRestrictionConfig<Nothing> {
        operator fun invoke(): OptionRestrictionConfig<Nothing> = this
    }

    /** The entire setting is unavailable and hidden from the UI. */
    data object FullyRestricted : OptionRestrictionConfig<Nothing> {
        operator fun invoke(): OptionRestrictionConfig<Nothing> = this
    }

    /** ONLY the options in this set are allowed, if supported by the device. */
    data class OptionsEnabled<T>(val enabledOptions: Set<T>) : OptionRestrictionConfig<T> {
        init {
            require(enabledOptions.isNotEmpty()) {
                "enabledOptions must not be empty. " +
                    "Use FullyRestricted to disable the feature."
            }
        }
    }
}
