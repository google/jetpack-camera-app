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

sealed interface CameraZoomState {
    val changeType: ZoomChange
    data class Ratio(override val changeType: ZoomChange) : CameraZoomState
    data class Linear(override val changeType: ZoomChange) : CameraZoomState
}
enum class LensToZoom {
    PRIMARY,
    SECONDARY
}
sealed interface ZoomChange {
    val value: Float
    val lensToZoom: LensToZoom

    /**
     * Use Set to directly set a specific zoom ratio or linear state to the value
     */
    data class Set(
        override val value: Float,
        override val lensToZoom: LensToZoom = LensToZoom.PRIMARY
    ) : ZoomChange

    /**
     * Use Scale to multiply current zoom ratio or linear state by the value
     */
    data class Scale(
        override val value: Float,
        override val lensToZoom: LensToZoom = LensToZoom.PRIMARY
    ) : ZoomChange

    /**
     * Use Increment to add the value to the current zoom ratio or linear state
     */
    data class Increment(
        override val value: Float,
        override val lensToZoom: LensToZoom = LensToZoom.PRIMARY
    ) : ZoomChange
}
