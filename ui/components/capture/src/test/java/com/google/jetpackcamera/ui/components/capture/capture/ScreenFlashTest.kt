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
package com.google.jetpackcamera.ui.components.capture.capture

import android.content.ContentResolver
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.CameraUseCase
import com.google.jetpackcamera.core.camera.test.FakeCameraUseCase
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.SaveLocation
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.ui.components.capture.ScreenFlash
import com.google.jetpackcamera.ui.components.capture.capture.rules.MainDispatcherRule
import com.google.jetpackcamera.ui.uistate.capture.ScreenFlashUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ScreenFlashTest {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val cameraUseCase = FakeCameraUseCase()
    private lateinit var screenFlash: ScreenFlash

    @Before
    fun setup() = runTest(testDispatcher) {
        screenFlash = ScreenFlash(cameraUseCase, testScope)
    }

    @Test
    fun initialScreenFlashUiState_disabledByDefault() {
        assertThat(screenFlash.screenFlashUiState.value.enabled).isFalse()
    }

    @Test
    fun captureScreenFlashImage_screenFlashUiStateChangedInCorrectSequence() = runCameraTest {
        val states = mutableListOf<ScreenFlashUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            screenFlash.screenFlashUiState.toList(states)
        }

        // FlashMode.ON in front facing camera automatically enables screen flash
        cameraUseCase.setLensFacing(lensFacing = LensFacing.FRONT)
        cameraUseCase.setFlashMode(FlashMode.ON)
        val contentResolver: ContentResolver = Mockito.mock()
        cameraUseCase.takePicture(contentResolver, SaveLocation.Default)

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
        cameraUseCase.emitScreenFlashEvent(
            CameraUseCase.ScreenFlashEvent(CameraUseCase.ScreenFlashEvent.Type.CLEAR_UI) { }
        )

        advanceUntilIdle()
        assertThat(screenFlash.screenFlashUiState.value.screenBrightnessToRestore)
            .isWithin(FLOAT_TOLERANCE)
            .of(5.0f)
    }

    @Test
    fun invokeOnChangeCompleteAfterClearUiEvent_screenFlashUiStateReset() = runCameraTest {
        screenFlash.setClearUiScreenBrightness(5.0f)
        cameraUseCase.emitScreenFlashEvent(
            CameraUseCase.ScreenFlashEvent(CameraUseCase.ScreenFlashEvent.Type.CLEAR_UI) { }
        )

        advanceUntilIdle()
        screenFlash.screenFlashUiState.value.onChangeComplete()

        advanceUntilIdle()
        assertThat(ScreenFlashUiState())
            .isEqualTo(screenFlash.screenFlashUiState.value)
    }

    private fun runCameraTest(testBody: suspend TestScope.() -> Unit) = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cameraUseCase.initialize(
                DEFAULT_CAMERA_APP_SETTINGS
            ) {}
            cameraUseCase.runCamera()
        }

        testBody()
    }

    companion object {
        const val FLOAT_TOLERANCE = 0.001f
    }
}
