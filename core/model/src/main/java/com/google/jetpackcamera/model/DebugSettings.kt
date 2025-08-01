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
package com.google.jetpackcamera.model

import android.util.Base64
import com.google.jetpackcamera.settings.DebugSettings as DebugSettingsProto
import com.google.jetpackcamera.settings.debugSettings as debugSettingsProto
import com.google.jetpackcamera.settings.model.LensFacing.Companion.toProto
import com.google.jetpackcamera.settings.model.TestPattern.Companion.toProto

/**
 * Data class for defining settings used in debug flows within the app.
 *
 * @param isDebugModeEnabled Controls whether the debug mode UI is shown.
 *                         Acceptable values are `true` or `false`.
 * @param singleLensMode Configures the camera to only use a single lens on the device,
 *                       making it appear as if no other lenses are present.
 *                       The provided [LensFacing] determines which lens will be used.
 *                       If `null`, single lens mode is disabled.
 */
data class DebugSettings(
    val isDebugModeEnabled: Boolean = false,
    val singleLensMode: LensFacing? = null,
    val testPattern: TestPattern = TestPattern.Off
) {
    companion object {
        /**
         * Creates a [DebugSettings] domain model from its protobuf representation.
         *
         * @param proto The [DebugSettingsProto] instance.
         * @return The corresponding [DebugSettings] instance.
         */
        fun fromProto(proto: DebugSettingsProto): DebugSettings {
            return DebugSettings(
                isDebugModeEnabled = proto.isDebugModeEnabled,
                singleLensMode = if (proto.hasSingleLensMode()) {
                    LensFacing.fromProto(proto.singleLensMode)
                } else {
                    null
                },
                testPattern = TestPattern.fromProto(proto.testPattern)
            )
        }

        /**
         * Converts a [DebugSettings] domain model to its protobuf representation.
         *
         * @receiver The [DebugSettings] instance to convert.
         * @return The corresponding [DebugSettingsProto] instance.
         */
        fun DebugSettings.toProto(): DebugSettingsProto = debugSettingsProto {
            isDebugModeEnabled = this@toProto.isDebugModeEnabled
            if (this@toProto.singleLensMode != null) {
                singleLensMode = this@toProto.singleLensMode.toProto()
            }
            testPattern = this@toProto.testPattern.toProto()
        }

        /**
         * Parses the encoded byte array into a [DebugSettings] instance.
         */
        fun parseFromByteArray(value: ByteArray): DebugSettings {
            val protoValue = DebugSettingsProto.parseFrom(value)
            return fromProto(protoValue)
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
}
