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


/** Enum class representing the device's supported stabilization configurations. */
enum class StabilizationMode {
    /** Stabilization off */
    OFF,

    /**
     * Device-chosen stabilization mode
     *
     * This will choose [ON] if the device and settings support it, otherwise it will be [OFF].
     */
    AUTO,

    /** Preview stabilization. */
    ON,

    /** Video stabilization.*/
    HIGH_QUALITY,

    /** Optical Stabilization (OIS) */
    OPTICAL;

}
