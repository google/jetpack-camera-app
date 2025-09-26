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

import android.hardware.camera2.TotalCaptureResult

/**
 * Interface describing the state of a Google Low Light Boost Session.
 */
sealed interface LowLightBoostSessionState {
    /**
     * Low Light Boost Session hasn't been created yet.
     */
    data object Uninitialized : LowLightBoostSessionState

    /**
     * Low Light Boost Session has been created but hasn't processed any frames yet.
     */
    data object Ready : LowLightBoostSessionState

    /**
     * Low Light Boost Session is running and processing frames.
     */
    data class Processing(val result: TotalCaptureResult) : LowLightBoostSessionState

    /**
     * Low Light Boost Session has been released and should no longer be used.
     */
    data object Released : LowLightBoostSessionState
}