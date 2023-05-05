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

import androidx.datastore.core.DataStore
import com.google.jetpackcamera.settings.model.Settings
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of [SettingsRepository] with locally stored settings.
 */
class LocalSettingsRepository @Inject constructor(
    private val jcaSettings: DataStore<JcaSettings>
) : SettingsRepository {

    override val settings = jcaSettings.data
        .map {
            Settings(
                default_front_camera = it.defaultFrontCamera
            )
        }

    override suspend fun updateDefaultFrontCamera() {
        jcaSettings.updateData {
            it.copy { this.defaultFrontCamera = !this.defaultFrontCamera }
        }
    }

    override fun getSettings(): Settings {
        TODO("Not yet implemented")
    }
}