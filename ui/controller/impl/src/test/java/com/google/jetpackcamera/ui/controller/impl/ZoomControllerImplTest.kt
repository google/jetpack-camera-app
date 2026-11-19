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
package com.google.jetpackcamera.ui.controller.impl

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.core.camera.testing.FakeCameraSystem
import com.google.jetpackcamera.model.CameraZoomRatio
import com.google.jetpackcamera.model.LensToZoom
import com.google.jetpackcamera.model.ZoomStrategy
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
internal class ZoomControllerImplTest {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

    private class RecordingCameraSystem : CameraSystem by FakeCameraSystem() {
        val zoomChanges = mutableListOf<CameraZoomRatio>()

        override fun changeZoomRatio(newZoomState: CameraZoomRatio) {
            zoomChanges.add(newZoomState)
        }
    }

    private val cameraSystem = RecordingCameraSystem()
    private val trackedCaptureUiState = MutableStateFlow(TrackedCaptureUiState())
    private lateinit var controller: ZoomControllerImpl

    @Before
    fun setup() {
        controller = ZoomControllerImpl(
            cameraSystemProvider = { cameraSystem },
            trackedCaptureUiState = trackedCaptureUiState,
            coroutineContext = testDispatcher
        )
    }

    @Test
    fun setZoomRatio_conflatesPendingAbsoluteUpdatesForSameLens() = testScope.runTest {
        val deferredCameraSystem = CompletableDeferred<CameraSystem>()
        val delayedController = ZoomControllerImpl(
            cameraSystemProvider = { deferredCameraSystem.await() },
            trackedCaptureUiState = trackedCaptureUiState,
            coroutineContext = testDispatcher
        )

        delayedController.setZoomRatio(
            CameraZoomRatio(ZoomStrategy.Absolute(0.5f, LensToZoom.PRIMARY))
        )
        delayedController.setZoomRatio(
            CameraZoomRatio(ZoomStrategy.Absolute(2.0f, LensToZoom.PRIMARY))
        )
        delayedController.setZoomRatio(
            CameraZoomRatio(ZoomStrategy.Absolute(5.0f, LensToZoom.PRIMARY))
        )

        deferredCameraSystem.complete(cameraSystem)
        advanceUntilIdle()

        assertThat(cameraSystem.zoomChanges).containsExactly(
            CameraZoomRatio(ZoomStrategy.Absolute(5.0f, LensToZoom.PRIMARY))
        )
    }

    @Test
    fun setZoomRatio_primaryAndSecondaryDoNotCancelEachOther() = testScope.runTest {
        controller.setZoomRatio(
            CameraZoomRatio(ZoomStrategy.Absolute(2.0f, LensToZoom.PRIMARY))
        )
        controller.setZoomRatio(
            CameraZoomRatio(ZoomStrategy.Absolute(1.0f, LensToZoom.SECONDARY))
        )
        advanceUntilIdle()

        assertThat(cameraSystem.zoomChanges).containsExactly(
            CameraZoomRatio(ZoomStrategy.Absolute(2.0f, LensToZoom.PRIMARY)),
            CameraZoomRatio(ZoomStrategy.Absolute(1.0f, LensToZoom.SECONDARY))
        ).inOrder()
    }

    @Test
    fun setZoomAnimationState_updatesTrackedCaptureUiState() {
        controller.setZoomAnimationState(5.0f)
        assertThat(trackedCaptureUiState.value.zoomAnimationTarget).isEqualTo(5.0f)

        controller.setZoomAnimationState(null)
        assertThat(trackedCaptureUiState.value.zoomAnimationTarget).isNull()
    }
}
