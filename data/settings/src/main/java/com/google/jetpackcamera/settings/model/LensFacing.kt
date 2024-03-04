/*
 * Copyright (C) 2024 The Android Open Source Project
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

import com.google.jetpackcamera.settings.LensFacing as LensFacingProto

enum class LensFacing {
    BACK,
    FRONT;

    fun flip(): LensFacing {
        return when (this) {
            FRONT -> BACK
            BACK -> FRONT
        }
    }

    companion object {

        /** returns the LensFacing enum equivalent of a provided LensFacingProto */
        fun fromProto(lensFacingProto: LensFacingProto): LensFacing {
            return when (lensFacingProto) {
                LensFacingProto.LENS_FACING_BACK -> BACK

                // Treat unrecognized as front as a fallback
                LensFacingProto.LENS_FACING_FRONT,
                LensFacingProto.UNRECOGNIZED -> FRONT
            }
        }

        fun LensFacing.toProto(): LensFacingProto {
            return when (this) {
                BACK -> LensFacingProto.LENS_FACING_BACK
                FRONT -> LensFacingProto.LENS_FACING_FRONT
            }
        }
    }
}
