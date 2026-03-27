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
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS


@ConsistentCopyVisibility
data class DeveloperAppConfig private constructor(
    val captureMode: SettingConfig<CaptureMode>,
    val aspectRatio: SettingConfig<AspectRatio>,
    val flashMode: SettingConfig<FlashMode>,
    val audio: SettingConfig<Boolean>
) {
    // checks validity of all individual setting configs
    init {
        fun <T: Any> SettingConfig<T>.containsIfOptionsEnabled(options: Set<T>): Boolean {
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
            audio = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.audioEnabled)
        )

        /**
         * Creates a [DeveloperAppConfig] instance, applying necessary corrections to ensure validity.
         *
         * This factory method ensures that for any [SettingConfig] using [OptionRestrictionConfig.OptionsEnabled]:
         * - The `defaultValue` is always included in the `enabledOptions`.
         * - Specific mandatory options are included (e.g., [FlashMode.OFF] for `flashMode`).
         *
         * Any adjustments are made silently by adding the required options to the `enabledOptions` set.
         **/
        fun create(
            captureMode: SettingConfig<CaptureMode> = LibraryDefaults.captureMode,
            aspectRatio: SettingConfig<AspectRatio> = LibraryDefaults.aspectRatio,
            flashMode: SettingConfig<FlashMode> = LibraryDefaults.flashMode,
            audio: SettingConfig<Boolean> = LibraryDefaults.audio
        ): DeveloperAppConfig {
            fun <T : Any> SettingConfig<T>.merge(settingConfig: SettingConfig<T>): SettingConfig<T> {
                return this.copy(
                    defaultValue = this.defaultValue ?: settingConfig.defaultValue,
                    uiRestriction = this.uiRestriction ?: settingConfig.uiRestriction
                )
            }
            return DeveloperAppConfig(
                captureMode = captureMode.merge(LibraryDefaults.captureMode).ensureDefaultOption(),
                aspectRatio = aspectRatio.merge(LibraryDefaults.aspectRatio).ensureDefaultOption(),
                flashMode = flashMode.merge(LibraryDefaults.flashMode).ensureDefaultOption().ensureOption(FlashMode.OFF),
                audio = audio.merge(LibraryDefaults.audio).ensureDefaultOption()
            )
        }
    }


    fun toCameraAppSettings(
        defaultSettings: CameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS
    ): CameraAppSettings {
        return defaultSettings.copy(
            aspectRatio = this.aspectRatio.defaultValue ?: defaultSettings.aspectRatio,
            flashMode = this.flashMode.defaultValue ?: defaultSettings.flashMode,
            captureMode = this.captureMode.defaultValue ?: defaultSettings.captureMode,
            audioEnabled = this.audio.defaultValue ?: defaultSettings.audioEnabled
            // ... copy other defaultValues from this config
        )
    }
}


data class SettingConfig<T:Any>(
    val defaultValue: T?,
    val uiRestriction: OptionRestrictionConfig<T>? = OptionRestrictionConfig.NotRestricted()
) {
    /**
     * Returns a new [SettingConfig] ensuring the [defaultValue] is included
     * in the [uiRestriction] if it's [OptionRestrictionConfig.OptionsEnabled].
     */
    fun ensureOption(option: T): SettingConfig<T> {
        return when (val currentRestriction = this.uiRestriction) {
            is OptionRestrictionConfig.OptionsEnabled<T> -> {
                if (!currentRestriction.enabledOptions.contains(option)) {
                    this.copy(
                        uiRestriction = currentRestriction.copy(
                            enabledOptions = currentRestriction.enabledOptions + option
                        )
                    )
                } else this
            }

            else -> this // No change for NotRestricted or FullyRestricted
        }
    }

    fun ensureDefaultOption(): SettingConfig<T> =
        if (this.defaultValue != null && this.uiRestriction != null) this.ensureOption(this.defaultValue) else this

}


sealed interface OptionRestrictionConfig<T:Any> {
    /** All device-supported options are available. */
    class NotRestricted<T : Any> : OptionRestrictionConfig<T>

    /** The entire setting is unavailable and hidden from the UI. */
    class FullyRestricted<T : Any> : OptionRestrictionConfig<T>

    /** ONLY the options in this set are allowed, if supported by the device. */
    data class OptionsEnabled<T : Any>(val enabledOptions: Set<T>) : OptionRestrictionConfig<T> {
        init {
            require(enabledOptions.isNotEmpty()) {
                "enabledOptions must have at least one option. " +
                        "Use FullyRestricted to disable the feature."
            }
        }
    }
}