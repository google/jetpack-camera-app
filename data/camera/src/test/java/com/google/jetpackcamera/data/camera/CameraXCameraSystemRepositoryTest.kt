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
package com.google.jetpackcamera.data.camera

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.core.camera.testing.FakeCameraSystem
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DebugSettings
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.testing.FakeSettingsRepository
import javax.inject.Provider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class CameraXCameraSystemRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private class TestCameraSystem(
        private val delegate: FakeCameraSystem = FakeCameraSystem()
    ) : CameraSystem by delegate {
        var initializedSettings: CameraAppSettings? = null
        var mockPropertiesJSON: String? = null

        override suspend fun initialize(
            cameraAppSettings: CameraAppSettings,
            cameraPropertiesJSONCallback: (result: String) -> Unit
        ) {
            delegate.initialize(cameraAppSettings, cameraPropertiesJSONCallback)
            delegate.setSystemConstraints(
                CameraSystemConstraints(
                    availableLenses = listOf(),
                    concurrentCamerasSupported = false
                )
            )
            initializedSettings = cameraAppSettings
            mockPropertiesJSON?.let(cameraPropertiesJSONCallback)
        }
    }

    @Test
    fun getCameraSystem_lazilyInitializesCameraSystem() = testScope.runTest {
        val testCamera = TestCameraSystem().apply {
            mockPropertiesJSON = "{\"test\": true}"
        }

        val launchConfig = CameraLaunchConfig(
            externalCaptureMode = ExternalCaptureMode.ImageCapture,
            debugSettings = DebugSettings(isDebugModeEnabled = true)
        )
        val settingsRepository = FakeSettingsRepository()

        val repository = CameraXCameraSystemRepository(
            cameraXCameraSystemProvider = Provider { testCamera },
            settingsRepository = settingsRepository,
            launchConfig = launchConfig,
            scope = testScope
        )

        // Before getCameraSystem is called, camera is not initialized
        assertThat(testCamera.initializedSettings).isNull()
        assertThat(repository.cameraPropertiesJSON.value).isNull()

        // Calling getCameraSystem triggers lazy initialization
        val cameraSystem = repository.getCameraSystem()
        assertThat(cameraSystem).isEqualTo(testCamera)
        assertThat(testCamera.initializedSettings).isNotNull()
        assertThat(testCamera.initializedSettings?.captureMode).isEqualTo(CaptureMode.IMAGE_ONLY)
        assertThat(testCamera.initializedSettings?.debugSettings?.isDebugModeEnabled).isTrue()
        assertThat(repository.cameraPropertiesJSON.value).isEqualTo("{\"test\": true}")
    }

    @Test
    fun getSupportedMimeTypes_initializesAndReturnsMimeTypes() = testScope.runTest {
        val testCamera = TestCameraSystem()

        val repository = CameraXCameraSystemRepository(
            cameraXCameraSystemProvider = Provider { testCamera },
            settingsRepository = FakeSettingsRepository(),
            launchConfig = CameraLaunchConfig(),
            scope = testScope
        )

        val mimeTypes = repository.getSupportedMimeTypes()
        assertThat(mimeTypes).isNotNull()
        assertThat(testCamera.initializedSettings).isNotNull()
    }
}
