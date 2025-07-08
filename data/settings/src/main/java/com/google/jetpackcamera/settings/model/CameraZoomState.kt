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

/**
 * Represents an action to modify the current zoom Ratio
 *  @param changeType the [ZoomStrategy] to be performed on the current Zoom Ratio
 */

data class CameraZoomRatio(val changeType: ZoomStrategy)

/**
 * Abstract placeholders
 */
enum class LensToZoom {

    /**
     * An abstract placeholder for the "Current" [LensFacing] in a single camera session,
     * or the Primary `LensFacing` in a concurrent session.
     */
    PRIMARY,

    /**
     * An abstract placeholder for the "Inactive" [LensFacing] in a single camera session,
     * or the `Secondary LensFacing` in a concurrent session.
     *
     * An "Inactive `LensFacing`" is not guaranteed in a single camera session.
     * @see[SystemConstraints.availableLenses]
     */
    SECONDARY
}

/**
 * Represents the different types of actions to modify the current zoom state
 */
sealed interface ZoomStrategy {
    val value: Float
    val lensToZoom: LensToZoom

    /**
     * Use Absolute to set the current zoom ratio or linear state to the value
     */
    data class Absolute(
        override val value: Float,
        override val lensToZoom: LensToZoom = LensToZoom.PRIMARY
    ) : ZoomStrategy

    /**
     * Use Scale to multiply current zoom ratio or linear state by the value
     */
    data class Scale(
        override val value: Float,
        override val lensToZoom: LensToZoom = LensToZoom.PRIMARY
    ) : ZoomStrategy

    /**
     * Use Increment to add the value to the current zoom ratio or linear state
     */
    data class Increment(
        override val value: Float,
        override val lensToZoom: LensToZoom = LensToZoom.PRIMARY
    ) : ZoomStrategy
}
