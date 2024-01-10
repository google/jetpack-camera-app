/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.google.jetpackcamera.feature.preview

import android.content.ContentResolver
import androidx.camera.core.Preview
import com.google.jetpackcamera.domain.camera.CameraUseCase
import com.google.jetpackcamera.domain.camera.test.FakeCameraUseCase
import com.google.jetpackcamera.feature.preview.rules.MainDispatcherRule
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.FlashMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenFlashTest {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val cameraUseCase = FakeCameraUseCase(testScope)
    private lateinit var screenFlash: ScreenFlash

    @Before
    fun setup() = runTest(testDispatcher) {
        screenFlash = ScreenFlash(cameraUseCase, testScope)

        val surfaceProvider: Preview.SurfaceProvider = Mockito.mock()
        cameraUseCase.initialize(DEFAULT_CAMERA_APP_SETTINGS)
        cameraUseCase.runCamera(surfaceProvider, DEFAULT_CAMERA_APP_SETTINGS)
    }

    @Test
    fun initialScreenFlashUiState_disabledByDefault() {
        assertEquals(false, screenFlash.screenFlashUiState.value.enabled)
    }

    @Test
    fun captureScreenFlashImage_screenFlashUiStateChangedInCorrectSequence() =
        runTest(testDispatcher) {
            val states = mutableListOf<ScreenFlash.ScreenFlashUiState>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                screenFlash.screenFlashUiState.toList(states)
            }

            // FlashMode.ON in front facing camera automatically enables screen flash
            cameraUseCase.setFlashMode(FlashMode.ON, true)
            val contentResolver: ContentResolver = Mockito.mock()
            cameraUseCase.takePicture(contentResolver, null)

            advanceUntilIdle()
            assertEquals(
                listOf(
                    false,
                    true,
                    false
                ),
                states.map { it.enabled }
            )
        }

    @Test
    fun emitClearUiEvent_screenFlashUiStateContainsClearUiScreenBrightness() =
        runTest(testDispatcher) {
            screenFlash.setClearUiScreenBrightness(5.0f)
            cameraUseCase.emitScreenFlashEvent(
                CameraUseCase.ScreenFlashEvent(CameraUseCase.ScreenFlashEvent.Type.CLEAR_UI) { }
            )

            advanceUntilIdle()
            assertEquals(
                5.0f,
                screenFlash.screenFlashUiState.value.screenBrightnessToRestore
            )
        }

    @Test
    fun invokeOnChangeCompleteAfterClearUiEvent_screenFlashUiStateReset() =
        runTest(testDispatcher) {
            screenFlash.setClearUiScreenBrightness(5.0f)
            cameraUseCase.emitScreenFlashEvent(
                CameraUseCase.ScreenFlashEvent(CameraUseCase.ScreenFlashEvent.Type.CLEAR_UI) { }
            )

            advanceUntilIdle()
            screenFlash.screenFlashUiState.value.onChangeComplete()

            advanceUntilIdle()
            assertEquals(
                ScreenFlash.ScreenFlashUiState(),
                screenFlash.screenFlashUiState.value
            )
        }
}
