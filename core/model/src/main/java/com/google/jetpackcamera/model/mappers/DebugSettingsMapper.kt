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
package com.google.jetpackcamera.model.mappers

import android.util.Base64
import com.google.jetpackcamera.model.DebugSettings
import com.google.jetpackcamera.model.proto.DebugSettings as DebugSettingsProto

object DebugSettingsMapper {
    /**
     * Parses the encoded byte array into a [DebugSettings] instance.
     */
    fun parseFromByteArray(value: ByteArray): DebugSettings {
        val protoValue = DebugSettingsProto.parseFrom(value)
        return protoValue.toDomain()
    }

    /**
     * Parses the Base64 encoded string into a [DebugSettings] instance.
     */
    fun parseFromString(value: String): DebugSettings {
        val decodedBytes = Base64.decode(value, Base64.NO_WRAP)
        return parseFromByteArray(decodedBytes)
    }

    /**
     * Encodes the [DebugSettings] data class into a byte array.
     */
    fun DebugSettings.encodeAsByteArray(): ByteArray = this.toProto().toByteArray()

    /**
     * Encodes the [DebugSettings] data class to a Base64 string.
     */
    fun DebugSettings.encodeAsString(): String {
        val protoValue = this.toProto() // Data class -> Proto
        return Base64.encodeToString(protoValue.toByteArray(), Base64.NO_WRAP)
    }
}
