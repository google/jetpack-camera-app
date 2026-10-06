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
 * Configuration for the Jetpack Camera App that allows developers and host applications
 * to customize the default camera experience and constrain user-facing controls.
 *
 * Each configured setting can specify:
 * 1. An initial [SettingConfig.defaultValue] applied on startup (overriding stored preferences).
 * 2. A [SettingConfig.visibility] policy ([OptionVisibility]) restricting or hiding the control.
 *
 * ### Invariants & Safety Rules:
 * - **Flash Mode:** Cannot be [OptionVisibility.Hidden] unless its default value is [FlashMode.OFF].
 *   If [OptionVisibility.Only] is used, [FlashMode.OFF] must always be included.
 * - **HDR Image Format & Video Dynamic Range:** When [OptionVisibility.Hidden] is used, developers may
 *   default or lock capture to Ultra HDR ([ImageOutputFormat.JPEG_ULTRA_HDR]) or HDR video ([DynamicRange.HLG10]).
 *   Hardware fallbacks to SDR will be handled at runtime if the active camera does not support HDR.
 *
 * ### Example:
 * ```kotlin
 * val featurePolicy = CameraFeaturePolicy(
 *     flashMode = SettingConfig(
 *         defaultValue = FlashMode.OFF,
 *         visibility = OptionVisibility.Hidden
 *     ),
 *     captureMode = SettingConfig(
 *         defaultValue = CaptureMode.IMAGE_ONLY,
 *         visibility = OptionVisibility.from(CaptureMode.IMAGE_ONLY, CaptureMode.VIDEO_ONLY)
 *     )
 * )
 * ```
 *
 * @param captureMode Configuration for camera capture mode (e.g. Standard, Image-only, Video-only).
 * @param aspectRatio Configuration for preview and capture aspect ratio.
 * @param flashMode Configuration for camera flash mode. Must have defaultValue of [FlashMode.OFF]
 *   if [OptionVisibility.Hidden].
 * @param imageFormat Configuration for captured photo format (e.g. JPEG, Ultra HDR).
 * @param dynamicRange Configuration for captured video dynamic range (e.g. SDR, HLG10).
 *
 * TODO (kc): Defer audioEnabled configuration to a follow-up PR, pending design for visual UX.
 */
data class CameraFeaturePolicy(
    val captureMode: SettingConfig<CaptureMode>? = null,
    val aspectRatio: SettingConfig<AspectRatio>? = null,
    val flashMode: SettingConfig<FlashMode>? = null,
    val imageFormat: SettingConfig<ImageOutputFormat>? = null,
    val dynamicRange: SettingConfig<DynamicRange>? = null
) {
    // Ensures that all individual setting configurations are valid.
    init {
        flashMode?.let { config ->
            when (val visibility = config.visibility) {
                is OptionVisibility.Only -> require(
                    FlashMode.OFF in visibility.enabledOptions
                ) {
                    "FlashMode.OFF must always be included in enabledOptions for flashMode."
                }

                is OptionVisibility.Hidden -> require(
                    config.defaultValue == FlashMode.OFF
                ) {
                    "When flashMode is Hidden, defaultValue must be FlashMode.OFF."
                }

                is OptionVisibility.Visible -> Unit
            }
        }
    }

    /**
     * Generates a baseline [CameraAppSettings] with developer-defined default values applied.
     *
     * Used by the settings storage layer to establish baseline defaults when preferences are unconfigured,
     * and in testing environments.
     *
     * @param defaultSettings The baseline settings to apply overrides onto. Defaults to [DEFAULT_CAMERA_APP_SETTINGS].
     * @return A merged [CameraAppSettings] with developer-specified default values applied.
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

    /**
     * Clamps [settings] to satisfy all [SettingConfig] restrictions in this policy.
     */
    fun enforceRestrictions(settings: CameraAppSettings): CameraAppSettings = settings.copy(
        aspectRatio = enforceSetting(settings.aspectRatio, aspectRatio),
        flashMode = enforceSetting(settings.flashMode, flashMode),
        imageFormat = enforceSetting(settings.imageFormat, imageFormat),
        dynamicRange = enforceSetting(settings.dynamicRange, dynamicRange),
        captureMode = enforceSetting(settings.captureMode, captureMode)
    )

    /**
     * Returns true if every setting restriction in [other] has at least one mutually permitted
     * value with this policy. Used to hide sub-modes whose required [CameraFeaturePolicy]
     * conflicts with a host application's locked [CameraFeaturePolicy].
     */
    fun isCompatibleWith(other: CameraFeaturePolicy): Boolean =
        isSettingCompatible(captureMode, other.captureMode) &&
            isSettingCompatible(aspectRatio, other.aspectRatio) &&
            isSettingCompatible(flashMode, other.flashMode) &&
            isSettingCompatible(imageFormat, other.imageFormat) &&
            isSettingCompatible(dynamicRange, other.dynamicRange)

    /**
     * Returns the intersection of this policy (typically the host application's policy) and
     * [other] (typically the active sub-mode's policy), narrowing visibility and selecting a
     * mutually permitted default value for each setting.
     */
    fun intersect(other: CameraFeaturePolicy): CameraFeaturePolicy = CameraFeaturePolicy(
        captureMode = intersectSetting(captureMode, other.captureMode),
        aspectRatio = intersectSetting(aspectRatio, other.aspectRatio),
        flashMode = intersectSetting(flashMode, other.flashMode),
        imageFormat = intersectSetting(imageFormat, other.imageFormat),
        dynamicRange = intersectSetting(dynamicRange, other.dynamicRange)
    )
}

private fun <T : Any> enforceSetting(currentValue: T, config: SettingConfig<T>?): T =
    when (val visibility = config?.visibility) {
        null, is OptionVisibility.Visible -> currentValue
        is OptionVisibility.Hidden -> config.defaultValue
        is OptionVisibility.Only ->
            if (currentValue in visibility.enabledOptions) currentValue else config.defaultValue
    }

/**
 * Returns `true` if [value] is permitted by this [SettingConfig]'s [visibility] restriction.
 */
fun <T : Any> SettingConfig<T>.permits(value: T): Boolean = when (visibility) {
    is OptionVisibility.Visible -> true
    is OptionVisibility.Hidden -> value == defaultValue
    is OptionVisibility.Only -> value in visibility.enabledOptions
}

private fun <T : Any> isSettingCompatible(
    base: SettingConfig<T>?,
    overlay: SettingConfig<T>?
): Boolean {
    if (base == null || overlay == null) return true
    return when (val baseVis = base.visibility) {
        is OptionVisibility.Visible -> true
        is OptionVisibility.Hidden -> overlay.permits(base.defaultValue)
        is OptionVisibility.Only -> when (val overlayVis = overlay.visibility) {
            is OptionVisibility.Visible -> true
            is OptionVisibility.Hidden -> overlay.defaultValue in baseVis.enabledOptions
            is OptionVisibility.Only ->
                (baseVis.enabledOptions intersect overlayVis.enabledOptions).isNotEmpty()
        }
    }
}

private fun <T : Any> intersectSetting(
    base: SettingConfig<T>?,
    overlay: SettingConfig<T>?
): SettingConfig<T>? {
    if (base == null) return overlay
    if (overlay == null) return base
    return when (val overlayVis = overlay.visibility) {
        is OptionVisibility.Hidden -> SettingConfig(
            defaultValue = if (overlay.permits(base.defaultValue)) {
                base.defaultValue
            } else {
                overlay.defaultValue
            },
            visibility = OptionVisibility.Hidden
        )
        is OptionVisibility.Visible -> when (base.visibility) {
            is OptionVisibility.Hidden, is OptionVisibility.Only -> base
            is OptionVisibility.Visible -> SettingConfig(
                defaultValue = overlay.defaultValue,
                visibility = OptionVisibility.Visible
            )
        }
        is OptionVisibility.Only -> when (val baseVis = base.visibility) {
            is OptionVisibility.Hidden -> SettingConfig(
                defaultValue = if (base.defaultValue in overlayVis.enabledOptions) {
                    base.defaultValue
                } else {
                    overlay.defaultValue
                },
                visibility = OptionVisibility.Hidden
            )
            is OptionVisibility.Visible -> overlay
            is OptionVisibility.Only -> {
                val common = baseVis.enabledOptions intersect overlayVis.enabledOptions
                val resolvedDefault = when {
                    overlay.defaultValue in common -> overlay.defaultValue
                    base.defaultValue in common -> base.defaultValue
                    else -> common.firstOrNull() ?: overlay.defaultValue
                }
                SettingConfig(
                    defaultValue = resolvedDefault,
                    visibility = OptionVisibility.from(common)
                )
            }
        }
    }
}

/**
 * Represents a single configurable setting in the application, including its
 * default value and UI visibility policy.
 *
 * @param T The enum or model type representing the setting's values (e.g. [FlashMode], [CaptureMode]).
 * @property defaultValue The initial value for this setting applied on launch.
 * @property visibility The UI visibility and option restriction policy for this setting.
 *   Defaults to [OptionVisibility.Visible].
 * @throws IllegalArgumentException if [visibility] is [OptionVisibility.Only] and [defaultValue]
 *   is not present in [OptionVisibility.Only.enabledOptions].
 */
data class SettingConfig<T : Any> @JvmOverloads constructor(
    val defaultValue: T,
    val visibility: OptionVisibility<T> = OptionVisibility.Visible
) {
    init {
        // Validate that if options are enabled for this setting, the default value
        // is always included in the set of enabled options.
        if (visibility is OptionVisibility.Only) {
            require(defaultValue in visibility.enabledOptions) {
                "The defaultValue ('$defaultValue') must be one of the enabledOptions: " +
                    "${visibility.enabledOptions}"
            }
        }
    }
}

/**
 * Defines the UI visibility and option availability policy applied to a camera setting.
 *
 * Use [Visible] to permit all device-supported options, [Hidden] to lock a setting to its default
 * value and hide the control, or [Only] to restrict the UI to a subset of options.
 *
 * When creating option restrictions, prefer using [OptionVisibility.from] for safe instantiation
 * that automatically falls back to [Hidden] if fewer than 2 options are available.
 *
 * @param T The type of setting options governed by this policy.
 * @see OptionVisibility.from
 */
sealed interface OptionVisibility<out T : Any> {
    /** All device-supported options are visible and selectable in the UI. */
    data object Visible : OptionVisibility<Nothing>

    /**
     * The setting is completely hidden and inaccessible in the user interface.
     *
     * Note: The setting remains active and locked to its configured [SettingConfig.defaultValue].
     *
     * @see OptionVisibility.Only
     */
    data object Hidden : OptionVisibility<Nothing>

    /**
     * Restricts the user interface to display only the specified subset of [enabledOptions],
     * provided they are supported by the current device hardware.
     *
     * @property enabledOptions The permitted options. Must contain at least 2 options and must
     *   include the setting's [SettingConfig.defaultValue]. If only a single option is desired,
     *   use [Hidden] with that default value instead, or use [OptionVisibility.from] to safely
     *   fall back to [Hidden] when options are computed dynamically.
     * @throws IllegalArgumentException if [enabledOptions] contains fewer than 2 items.
     * @see OptionVisibility.from
     * @see OptionVisibility.Hidden
     */
    data class Only<T : Any>(val enabledOptions: Set<T>) : OptionVisibility<T> {
        /**
         * Creates an [Only] policy from vararg [options].
         *
         * Note: Prefer [OptionVisibility.from] if [options] may contain fewer than 2 items at runtime.
         *
         * @param options The permitted options. Must contain at least 2 unique items.
         * @throws IllegalArgumentException if fewer than 2 unique options are provided.
         * @see OptionVisibility.from
         */
        constructor(vararg options: T) : this(options.toSet())

        init {
            require(enabledOptions.size >= 2) {
                "enabledOptions must contain at least 2 options. Use Hidden to lock a single option and hide the control."
            }
        }
    }

    companion object {
        /**
         * Safely creates an [OptionVisibility] policy from the specified [options].
         *
         * - **2 or more options (`size >= 2`):** Returns [OptionVisibility.Only] containing the specified options.
         * - **Single option or empty set (`size < 2`):** Always resolves to [OptionVisibility.Hidden] rather than
         *   throwing an [IllegalArgumentException]. When resolved to [Hidden], the control is hidden from
         *   the user interface and locked to the setting's configured [SettingConfig.defaultValue].
         *
         * This factory function is recommended when options are filtered or resolved dynamically at runtime
         * (e.g. against remote flags or device capabilities) to avoid runtime crash traps.
         *
         * @see OptionVisibility.Only
         * @see OptionVisibility.Hidden
         */
        fun <T : Any> from(options: Set<T>): OptionVisibility<T> =
            if (options.size >= 2) Only(options) else Hidden

        /**
         * Safely creates an [OptionVisibility] policy from vararg [options].
         *
         * Resolves to [OptionVisibility.Only] if 2 or more options are provided, or [OptionVisibility.Hidden]
         * if 0 or 1 option is provided.
         *
         * @see OptionVisibility.Only
         * @see OptionVisibility.Hidden
         */
        fun <T : Any> from(vararg options: T): OptionVisibility<T> = from(options.toSet())
    }
}
