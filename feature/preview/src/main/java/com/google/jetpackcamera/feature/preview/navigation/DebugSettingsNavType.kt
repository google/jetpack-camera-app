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
import com.google.jetpackcamera.model.DebugSettings
import com.google.jetpackcamera.model.DebugSettings.Companion.encodeAsString
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
        bundle.putString(key, value.encodeAsString())
    }

    /**
     * Gets the [DebugSettings] value from the Bundle by deserializing the Proto.
     */
    override fun get(bundle: Bundle, key: String): DebugSettings? {
        return bundle.getString(key)?.let { str ->
            DebugSettings.parseFromString(str)
        }
    }

    /**
     * Parses the URL-encoded serialized string from the navigation route.
     */
    override fun parseValue(value: String): DebugSettings {
        val decoded = URLDecoder.decode(value, StandardCharsets.UTF_8.toString())
        return DebugSettings.parseFromString(decoded)
    }

    /**
     * Encodes the [DebugSettings] data class to a URL-encoded string for navigation routes.
     */
    override fun serializeAsValue(value: DebugSettings): String {
        return URLEncoder.encode(value.encodeAsString(), StandardCharsets.UTF_8.toString())
    }

    override val name: String = "DebugSettingsNavType"
}
