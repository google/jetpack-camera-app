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
package com.google.jetpackcamera.data.settingsdatastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object PreferenceKeys {
    val KEY_LENS_FACING = stringPreferencesKey("lens_facing")
    val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
    val KEY_FLASH_MODE = stringPreferencesKey("flash_mode")
    val KEY_ASPECT_RATIO = stringPreferencesKey("aspect_ratio")
    val KEY_STREAM_CONFIG = stringPreferencesKey("stream_config")
    val KEY_STABILIZATION_MODE = stringPreferencesKey("stabilization_mode")
    val KEY_DYNAMIC_RANGE = stringPreferencesKey("dynamic_range")
    val KEY_VIDEO_QUALITY = stringPreferencesKey("video_quality")
    val KEY_IMAGE_FORMAT = stringPreferencesKey("image_format")
    val KEY_MAX_VIDEO_DURATION = longPreferencesKey("max_video_duration")
    val KEY_AUDIO_ENABLED = booleanPreferencesKey("audio_enabled")
    val KEY_LOW_LIGHT_BOOST_PRIORITY = stringPreferencesKey("low_light_boost_priority")
    val KEY_TARGET_FRAME_RATE = intPreferencesKey("target_frame_rate")
}
