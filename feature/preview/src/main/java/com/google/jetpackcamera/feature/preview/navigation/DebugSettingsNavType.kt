/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.google.jetpackcamera.feature.preview.navigation

import android.os.Bundle
import androidx.navigation.NavType
import com.google.jetpackcamera.settings.model.DebugSettings
import com.google.jetpackcamera.settings.model.DebugSettings.Companion.encodeAsByteArray
import com.google.jetpackcamera.settings.model.DebugSettings.Companion.encodeAsString

/**
 * Custom NavType to handle DebugSettings data class.
 * It converts the data class to/from its Proto representation for serialization
 * in Bundles and navigation routes.
 */
internal object DebugSettingsNavType : NavType<DebugSettings>(isNullableAllowed = false) {
    /**
     * Puts the [DebugSettings] value into the Bundle by converting to Proto and serializing.
     */
    override fun put(bundle: Bundle, key: String, value: DebugSettings) {
        bundle.putByteArray(key, value.encodeAsByteArray())
    }

    /**
     * Gets the [DebugSettings] value from the Bundle by deserializing the Proto.
     */
    override fun get(bundle: Bundle, key: String): DebugSettings? {
        return bundle.getByteArray(key)?.let { bytes ->
            DebugSettings.parseFromByteArray(bytes)
        }
    }

    /**
     * Parses the Base64 encoded Proto string from the navigation route.
     */
    override fun parseValue(value: String): DebugSettings = DebugSettings.parseFromString(value)

    /**
     * Encodes the [DebugSettings] data class to a Base64 string for navigation routes.
     */
    override fun serializeAsValue(value: DebugSettings): String = value.encodeAsString()

    override val name: String = "DebugSettingsNavType"
}
