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
package com.google.jetpackcamera.ui.components.capture

import android.util.Range
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.core.util.toClosedRange
import com.google.jetpackcamera.model.CameraZoomRatio
import com.google.jetpackcamera.model.LensToZoom
import com.google.jetpackcamera.model.ZoomStrategy

class ZoomState(
    initialZoomLevel: Float,
    zoomRange: Range<Float>,
    val onChangeZoomLevel: (CameraZoomRatio) -> Unit,
    val onAnimateStateChanged: (Float?) -> Unit
) {
    init {
        onAnimateStateChanged(null)
    }

    private var functionalZoom = initialZoomLevel

    private var functionalZoomRange = zoomRange

    private val mutatorMutex = MutatorMutex()

    private suspend fun mutateZoom(block: suspend () -> Unit) {
        mutatorMutex.mutate {
            onAnimateStateChanged(null)
            block()
        }
    }

    /**
     * Immediately set the current zoom level to [targetZoomLevel].
     */
    suspend fun absoluteZoom(targetZoomLevel: Float, lensToZoom: LensToZoom) {
        mutateZoom {
            if (lensToZoom == LensToZoom.PRIMARY) {
                functionalZoom = targetZoomLevel.coerceIn(functionalZoomRange.toClosedRange())
            }
            onChangeZoomLevel(
                CameraZoomRatio(
                    ZoomStrategy.Absolute(
                        targetZoomLevel.coerceIn(functionalZoomRange.toClosedRange()),
                        lensToZoom = lensToZoom
                    )
                )
            )
        }
    }

    /**
     * Scales the current zoom level by the given [scalingFactor].
     *
     * @param scalingFactor The factor by which to scale the current zoom level.
     * @param lensToZoom Specifies which lens's zoom to modify (primary or secondary).
     */
    suspend fun scaleZoom(scalingFactor: Float, lensToZoom: LensToZoom) {
        absoluteZoom(scalingFactor * functionalZoom, lensToZoom)
    }

    /**
     * Increments the current zoom level by the given [increment].
     *
     * @param increment The amount to increment the current zoom level by.
     * @param lensToZoom Specifies which lens's zoom to modify (primary or secondary).
     */
    suspend fun incrementZoom(increment: Float, lensToZoom: LensToZoom) {
        absoluteZoom(increment + functionalZoom, lensToZoom)
    }

    /**
     * Animates the zoom level to the specified [targetZoomLevel].
     *
     * @param targetZoomLevel The target zoom level to animate to.
     * @param animationSpec The [AnimationSpec] to use for the animation.
     * @param lensToZoom Specifies which lens's zoom to modify (primary or secondary).
     */
    suspend fun animatedZoom(
        targetZoomLevel: Float,
        animationSpec: AnimationSpec<Float> = tween(durationMillis = 500),
        lensToZoom: LensToZoom
    ) {
        mutatorMutex.mutate {
            onAnimateStateChanged(targetZoomLevel)

            Animatable(initialValue = functionalZoom).animateTo(
                targetValue = targetZoomLevel,
                animationSpec = animationSpec
            ) {
                // this is called every animation frame
                functionalZoom = value.coerceIn(functionalZoomRange.toClosedRange())
                onChangeZoomLevel(
                    CameraZoomRatio(
                        ZoomStrategy.Absolute(
                            functionalZoom,
                            lensToZoom
                        )
                    )
                )
            }
        }
    }

    /**
     * Updates the zoom state when the camera lens changes.
     *
     * @param newInitialZoomLevel The new initial zoom level.
     * @param newZoomRange The new zoom range.
     */
    suspend fun onChangeLens(newInitialZoomLevel: Float, newZoomRange: Range<Float>) {
        mutatorMutex.mutate(MutatePriority.PreventUserInput) {
            functionalZoom = newInitialZoomLevel
            functionalZoomRange = newZoomRange
        }
    }
}
