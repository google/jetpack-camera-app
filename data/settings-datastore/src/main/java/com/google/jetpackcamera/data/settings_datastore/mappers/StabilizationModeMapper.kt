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

package com.google.jetpackcamera.data.settings_datastore.mappers

import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.StabilizationMode.AUTO
import com.google.jetpackcamera.model.StabilizationMode.HIGH_QUALITY
import com.google.jetpackcamera.model.StabilizationMode.OFF
import com.google.jetpackcamera.model.StabilizationMode.ON
import com.google.jetpackcamera.model.StabilizationMode.OPTICAL
import com.google.jetpackcamera.model.proto.StabilizationMode as StabilizationModeProto


/** returns the AspectRatio enum equivalent of a provided AspectRatioProto */
fun StabilizationModeProto.fromProto(): StabilizationMode =
    when (this) {
        StabilizationModeProto.STABILIZATION_MODE_OFF -> OFF
        StabilizationModeProto.STABILIZATION_MODE_ON -> ON
        StabilizationModeProto.STABILIZATION_MODE_HIGH_QUALITY -> HIGH_QUALITY
        StabilizationModeProto.STABILIZATION_MODE_OPTICAL -> OPTICAL

        // Default to AUTO
        StabilizationModeProto.STABILIZATION_MODE_UNDEFINED,
        StabilizationModeProto.UNRECOGNIZED,
        StabilizationModeProto.STABILIZATION_MODE_AUTO
            -> AUTO
    }
