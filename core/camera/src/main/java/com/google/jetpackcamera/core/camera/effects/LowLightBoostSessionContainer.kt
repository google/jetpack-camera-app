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
package com.google.jetpackcamera.core.camera.effects

import android.os.Build
import com.google.android.gms.cameralowlight.LowLightBoostSession

class LowLightBoostSessionContainer {
    var lowLightBoostSession: LowLightBoostSession? = null

    fun releaseSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            lowLightBoostSession?.release()
            lowLightBoostSession = null
        }
    }

    companion object {
        private var instance: LowLightBoostSessionContainer? = null

        fun getInstance(): LowLightBoostSessionContainer = instance ?: synchronized(this) {
            instance ?: LowLightBoostSessionContainer().also { instance = it }
        }
    }
}