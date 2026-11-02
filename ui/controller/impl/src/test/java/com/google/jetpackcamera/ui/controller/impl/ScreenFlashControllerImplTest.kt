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
package com.google.jetpackcamera.ui.controller.impl

import android.content.ContentResolver
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.core.camera.testing.FakeCameraSystem
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.SaveLocation
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.ui.uistate.capture.ScreenFlashUiState
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ScreenFlashControllerImplTest {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val cameraSystem = FakeCameraSystem()
    private val trackedCaptureUiState = MutableStateFlow(TrackedCaptureUiState())
    private lateinit var screenFlash: ScreenFlashControllerImpl

    @Before
    fun setup() = runTest(testDispatcher) {
        screenFlash = ScreenFlashControllerImpl(
            cameraSystemProvider = { cameraSystem },
            trackedCaptureUiState = trackedCaptureUiState,
            coroutineContext = testScope.coroutineContext,
            minApplyUiDurationMillis = MIN_APPLY_UI_DURATION_MILLIS
        )
    }

    @Test
    fun initialScreenFlashUiState_disabledByDefault() {
        assertThat(trackedCaptureUiState.value.screenFlashUiState.enabled).isFalse()
    }

    @Test
    fun captureScreenFlashImage_screenFlashUiStateChangedInCorrectSequence() = runCameraTest {
        val states = mutableListOf<ScreenFlashUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            trackedCaptureUiState.map { it.screenFlashUiState }.toList(states)
        }

        // FlashMode.ON in front facing camera automatically enables screen flash
        cameraSystem.setLensFacing(lensFacing = LensFacing.FRONT)
        cameraSystem.setFlashMode(FlashMode.ON)
        val contentResolver: ContentResolver =
            ApplicationProvider.getApplicationContext<Context>().contentResolver
        cameraSystem.takePicture(contentResolver, SaveLocation.Default).let { unused -> }

        advanceUntilIdle()
        assertThat(states.map { it.enabled }).containsExactlyElementsIn(
            listOf(
                false,
                true,
                false
            )
        ).inOrder()
    }

    @Test
    fun emitClearUiEvent_screenFlashUiStateContainsClearUiScreenBrightness() = runCameraTest {
        screenFlash.setClearUiScreenBrightness(5.0f)
        cameraSystem.emitScreenFlashEvent(
            CameraSystem.ScreenFlashEvent(CameraSystem.ScreenFlashEvent.Type.CLEAR_UI) { }
        )

        advanceUntilIdle()
        assertThat(trackedCaptureUiState.value.screenFlashUiState.screenBrightnessToRestore)
            .isWithin(FLOAT_TOLERANCE)
            .of(5.0f)
    }

    @Test
    fun invokeOnChangeCompleteAfterClearUiEvent_screenFlashUiStateReset() = runCameraTest {
        screenFlash.setClearUiScreenBrightness(5.0f)
        cameraSystem.emitScreenFlashEvent(
            CameraSystem.ScreenFlashEvent(CameraSystem.ScreenFlashEvent.Type.CLEAR_UI) { }
        )

        advanceUntilIdle()
        trackedCaptureUiState.value.screenFlashUiState.onChangeComplete()

        advanceUntilIdle()
        assertThat(ScreenFlashUiState())
            .isEqualTo(trackedCaptureUiState.value.screenFlashUiState)
    }

    @Test
    fun applyUiEvent_uiChangeCompletesImmediately_onCompleteInvokedAfterMinDuration() =
        runCameraTest {
            var isApplyUiComplete = false
            cameraSystem.emitScreenFlashEvent(
                CameraSystem.ScreenFlashEvent(CameraSystem.ScreenFlashEvent.Type.APPLY_UI) {
                    isApplyUiComplete = true
                }
            )
            runCurrent()

            trackedCaptureUiState.value.screenFlashUiState.onChangeComplete()
            advanceTimeBy(MIN_APPLY_UI_DURATION_MILLIS - 1)
            runCurrent()
            assertThat(isApplyUiComplete).isFalse()

            advanceTimeBy(1)
            runCurrent()
            assertThat(isApplyUiComplete).isTrue()
        }

    @Test
    fun applyUiEvent_uiChangeCompletesAfterMinDuration_onCompleteInvokedAfterUiChange() =
        runCameraTest {
            var isApplyUiComplete = false
            cameraSystem.emitScreenFlashEvent(
                CameraSystem.ScreenFlashEvent(CameraSystem.ScreenFlashEvent.Type.APPLY_UI) {
                    isApplyUiComplete = true
                }
            )
            runCurrent()

            advanceTimeBy(MIN_APPLY_UI_DURATION_MILLIS * 2)
            runCurrent()
            assertThat(isApplyUiComplete).isFalse()

            trackedCaptureUiState.value.screenFlashUiState.onChangeComplete()
            runCurrent()
            assertThat(isApplyUiComplete).isTrue()
        }

    @Test
    fun clearUiEventBeforeApplyUiCompletes_applyUiOnCompleteNotInvoked() = runCameraTest {
        var isApplyUiComplete = false
        cameraSystem.emitScreenFlashEvent(
            CameraSystem.ScreenFlashEvent(CameraSystem.ScreenFlashEvent.Type.APPLY_UI) {
                isApplyUiComplete = true
            }
        )
        runCurrent()
        trackedCaptureUiState.value.screenFlashUiState.onChangeComplete()

        cameraSystem.emitScreenFlashEvent(
            CameraSystem.ScreenFlashEvent(CameraSystem.ScreenFlashEvent.Type.CLEAR_UI) { }
        )
        advanceUntilIdle()

        assertThat(isApplyUiComplete).isFalse()
    }

    private fun runCameraTest(testBody: suspend TestScope.() -> Unit) = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cameraSystem.initialize(
                DEFAULT_CAMERA_APP_SETTINGS
            ) {}
            cameraSystem.runCamera()
        }

        testBody()
    }

    companion object {
        const val FLOAT_TOLERANCE = 0.001f
        const val MIN_APPLY_UI_DURATION_MILLIS = 1000L
    }
}
