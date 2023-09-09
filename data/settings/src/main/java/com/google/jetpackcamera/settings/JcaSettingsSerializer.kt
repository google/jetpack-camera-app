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

package com.google.jetpackcamera.settings

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

// tells DataStore how to read/write our settings file
const val FILE_LOCATION = "app_settings.pb"

object JcaSettingsSerializer : Serializer<JcaSettings> {

    override val defaultValue: JcaSettings = JcaSettings.newBuilder()
        .setDarkModeStatus(DarkModeProto.DARK_MODE_SYSTEM)
        .setDefaultFrontCamera(false)
        .setBackCameraAvailable(true)
        .setFrontCameraAvailable(true)
        .setFlashModeStatus(FlashModeProto.FLASH_MODE_OFF)
        .setCaptureModeStatus(CaptureModeProto.CAMERA_MODE_DEFAULT)
        .build()

    override suspend fun readFrom(input: InputStream): JcaSettings {
        try {
            return JcaSettings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: JcaSettings,
        output: OutputStream
    ) = t.writeTo(output)
}

val Context.settingsDataStore: DataStore<JcaSettings> by dataStore(
    fileName = FILE_LOCATION,
    serializer = JcaSettingsSerializer
)