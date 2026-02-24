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

/**
 * Manages the camera's zoom level and handles interactions related to zooming.
 *
 * This class provides functions to control the camera's zoom, including setting an absolute
 * zoom level, scaling the current zoom, incrementing the zoom, and animating zoom transitions.
 * It also handles updating the zoom state when the camera lens changes.
 *
 * @param initialZoomLevel The starting zoom level when the camera is initialized.
 * @param zoomRange The supported zoom range of the current camera lens.
 * @param onChangeZoomLevel A callback function that is invoked when the zoom level changes.
 * @param onAnimateStateChanged A callback function to indicate whether a zoom animation is in progress.
 */
class ZoomStateManager(
    initialZoomLevel: Float,
    zoomRange: Range<Float>,
    val onChangeZoomLevel: (CameraZoomRatio) -> Unit,
    val onAnimateStateChanged: (Float?) -> Unit
) {
    init {
        onAnimateStateChanged(null)
    }

    private var functionalZoom = initialZoomLevel

    // The valid zoom range for the current camera lens.
    private var functionalZoomRange = zoomRange

    // A mutex to ensure that zoom operations are atomic and thread-safe. This is crucial for
    // preventing race conditions, as multiple zoom actions (e.g., pinch-to-zoom and button
    // clicks) can be triggered concurrently. By using a MutatorMutex, we can ensure that
    // only one zoom mutation is active at a time, and that new mutations can cancel
    // existing ones.
    private val mutatorMutex = MutatorMutex()

    private suspend fun mutateZoom(block: suspend () -> Unit) {
        mutatorMutex.mutate {
            onAnimateStateChanged(null)
            block()
        }
    }

    /**
     * Immediately sets the zoom level to a specified value.
     *
     * This function will ignore any ongoing animations and instantly update the zoom.
     * The provided [targetZoomLevel] will be coerced to stay within the valid [functionalZoomRange].
     *
     * @param targetZoomLevel The absolute zoom level to set.
     * @param lensToZoom Specifies which lens's zoom to modify (primary or secondary).
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
     * Scales the current zoom level by a given factor.
     *
     * This is useful for implementing pinch-to-zoom gestures, where the zoom level is adjusted
     * multiplicatively.
     *
     * @param scalingFactor The factor by which to multiply the current zoom level.
     * @param lensToZoom Specifies which lens's zoom to modify (primary or secondary).
     */
    suspend fun scaleZoom(scalingFactor: Float, lensToZoom: LensToZoom) {
        absoluteZoom(scalingFactor * functionalZoom, lensToZoom)
    }

    /**
     * Increments the current zoom level by a given amount.
     *
     * This can be used for implementing zoom buttons that increase or decrease the zoom level
     * by a fixed step.
     *
     * @param increment The amount to add to the current zoom level.
     * @param lensToZoom Specifies which lens's zoom to modify (primary or secondary).
     */
    suspend fun incrementZoom(increment: Float, lensToZoom: LensToZoom) {
        absoluteZoom(increment + functionalZoom, lensToZoom)
    }

    /**
     * Smoothly animates the zoom level to a target value.
     *
     * If there is an ongoing zoom animation, it will be canceled, and a new one will start.
     *
     * @param targetZoomLevel The target zoom level to animate to.
     * @param animationSpec The [AnimationSpec] that defines the animation's timing and curve.
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
     * Updates the internal state when the camera lens is changed.
     *
     * This function should be called when the active camera lens (e.g., front or back) is switched,
     * as different lenses may have different zoom capabilities.
     *
     * @param newInitialZoomLevel The initial zoom level for the new lens.
     * @param newZoomRange The valid zoom range for the new lens.
     */
    suspend fun onChangeLens(newInitialZoomLevel: Float, newZoomRange: Range<Float>) {
        mutatorMutex.mutate(MutatePriority.PreventUserInput) {
            functionalZoom = newInitialZoomLevel
            functionalZoomRange = newZoomRange
        }
    }
}
