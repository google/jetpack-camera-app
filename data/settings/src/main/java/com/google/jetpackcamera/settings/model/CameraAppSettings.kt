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

package com.google.jetpackcamera.settings.model

/**
 * Data layer representation for settings.
 */
data class CameraAppSettings(
    val default_front_camera : Boolean,
    val dark_mode_status : DarkModeStatus,
    val flash_mode_status : FlashModeStatus
)


fun getDefaultSettings(): CameraAppSettings{
    return CameraAppSettings(
        default_front_camera = false,
        dark_mode_status = DarkModeStatus.SYSTEM,
        flash_mode_status = FlashModeStatus.OFF
    )
}