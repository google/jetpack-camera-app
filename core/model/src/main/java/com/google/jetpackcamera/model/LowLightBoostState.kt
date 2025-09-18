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

/**
 * Interface describing the state of Low Light Boost.
 */
sealed interface LowLightBoostState {
    /**
     * Low Light Boost is not active.
     */
    data object Inactive : LowLightBoostState

    /**
     * Low Light Boost is active.
     *
     * @param strength The strength of brightening being applied.
     */
    data class Active(val strength: Float) : LowLightBoostState

    /**
     * An error occurred with Low Light Boost.
     */
    data class Error(val error: Throwable?) : LowLightBoostState

    companion object {
        const val MINIMUM_STRENGTH = 0.0f
        const val MAXIMUM_STRENGTH = 1.0f
    }
}
