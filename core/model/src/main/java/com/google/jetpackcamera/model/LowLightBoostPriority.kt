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

import com.google.jetpackcamera.model.proto.LowLightBoostPriority as LowLightBoostPriorityProto

enum class LowLightBoostPriority {
    PRIORITIZE_AE_MODE,
    PRIORITIZE_GOOGLE_PLAY_SERVICES;

    companion object {
        /**
         * Returns the LowLightBoostPriority enum equivalent of a provided
         * LowLightBoostPriorityProto
         */
        fun fromProto(
            lowLightBoostPriorityProto: LowLightBoostPriorityProto
        ): LowLightBoostPriority {
            return when (lowLightBoostPriorityProto) {
                LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_AE_MODE -> PRIORITIZE_AE_MODE
                LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_GOOGLE_PLAY_SERVICES ->
                    PRIORITIZE_GOOGLE_PLAY_SERVICES
                LowLightBoostPriorityProto.UNRECOGNIZED -> PRIORITIZE_AE_MODE // Default to AE mode
            }
        }

        fun LowLightBoostPriority.toProto(): LowLightBoostPriorityProto {
            return when (this) {
                PRIORITIZE_AE_MODE -> LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_AE_MODE
                PRIORITIZE_GOOGLE_PLAY_SERVICES ->
                    LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_GOOGLE_PLAY_SERVICES
            }
        }
    }
}
