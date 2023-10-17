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
package com.google.jetpackcamera.settings.test

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.jetpackcamera.settings.DarkMode
import com.google.jetpackcamera.settings.JcaSettings
import com.google.protobuf.InvalidProtocolBufferException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class FakeJcaSettingsSerializer(
    var failReadWithCorruptionException: Boolean = false
) : Serializer<JcaSettings> {

    override val defaultValue: JcaSettings = JcaSettings.newBuilder()
        .setDarkModeStatus(DarkMode.DARK_MODE_SYSTEM)
        .setDefaultFrontCamera(false)
        .build()

    override suspend fun readFrom(input: InputStream): JcaSettings {
        if (failReadWithCorruptionException) {
            throw CorruptionException(
                "Corruption Exception",
                IOException()
            )
        }
        try {
            return JcaSettings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: JcaSettings, output: OutputStream) = t.writeTo(output)
}
