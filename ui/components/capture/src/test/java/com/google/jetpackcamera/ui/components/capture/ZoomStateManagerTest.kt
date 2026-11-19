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
package com.google.jetpackcamera.ui.components.capture

import android.util.Range
import androidx.compose.animation.core.snap
import androidx.compose.runtime.MonotonicFrameClock
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CameraZoomRatio
import com.google.jetpackcamera.model.LensToZoom
import com.google.jetpackcamera.model.ZoomStrategy
import com.google.jetpackcamera.ui.controller.testing.FakeZoomController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ZoomStateManagerTest {

    private val appliedRatios = mutableListOf<CameraZoomRatio>()
    private val animationStates = mutableListOf<Float?>()
    private lateinit var zoomController: FakeZoomController
    private lateinit var zoomStateManager: ZoomStateManager

    private val testFrameClock = object : MonotonicFrameClock {
        private var frameTimeNanos = 0L
        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            frameTimeNanos += 16_000_000L
            return onFrame(frameTimeNanos)
        }
    }

    @Before
    fun setUp() {
        appliedRatios.clear()
        animationStates.clear()
        zoomController = FakeZoomController(
            setZoomRatioAction = { appliedRatios.add(it) },
            setZoomAnimationStateAction = { animationStates.add(it) }
        )
        zoomStateManager = ZoomStateManager(
            initialZoomLevel = 1.0f,
            zoomRange = Range(0.5f, 10.0f),
            zoomController = zoomController
        )
    }

    @Test
    fun animatedZoom_setsAndClearsZoomAnimationStateOnCompletion() = runTest {
        animationStates.clear()

        withContext(testFrameClock) {
            zoomStateManager.animatedZoom(
                targetZoomLevel = 5.0f,
                animationSpec = snap(),
                lensToZoom = LensToZoom.PRIMARY
            )
        }

        assertThat(animationStates).containsExactly(5.0f, null).inOrder()
        assertThat(appliedRatios.last()).isEqualTo(
            CameraZoomRatio(ZoomStrategy.Absolute(5.0f, LensToZoom.PRIMARY))
        )
    }

    @Test
    fun animatedZoom_whenAlreadyAtTargetZoom_shortCircuits() = runTest {
        animationStates.clear()

        withContext(testFrameClock) {
            zoomStateManager.animatedZoom(
                targetZoomLevel = 1.0f,
                animationSpec = snap(),
                lensToZoom = LensToZoom.PRIMARY
            )
        }

        assertThat(appliedRatios).isEmpty()
        assertThat(animationStates).containsExactly(null)
    }

    @Test
    fun onChangeLens_resetsZoomAnimationStateAndUpdatesRange() = runTest {
        animationStates.clear()

        zoomStateManager.onChangeLens(
            newInitialZoomLevel = 2.0f,
            newZoomRange = Range(1.0f, 4.0f)
        )
        zoomStateManager.scaleZoom(scalingFactor = 3.0f, lensToZoom = LensToZoom.PRIMARY)

        assertThat(animationStates).contains(null)
        assertThat(appliedRatios.last()).isEqualTo(
            CameraZoomRatio(ZoomStrategy.Absolute(4.0f, LensToZoom.PRIMARY))
        )
    }
}
