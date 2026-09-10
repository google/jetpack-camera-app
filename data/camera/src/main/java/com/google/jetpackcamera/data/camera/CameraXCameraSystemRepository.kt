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

import androidx.camera.core.SurfaceRequest
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.core.camera.CameraXCameraSystem
import com.google.jetpackcamera.settings.SettingsRepository
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.applyExternalCaptureMode
import com.google.jetpackcamera.settings.model.getSupportedMimeTypes
import javax.inject.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

/**
 * Implementation of [CameraSystemRepository] that manages [CameraXCameraSystem] initialization
 * and exposes camera streams.
 */
class CameraXCameraSystemRepository(
    private val cameraXCameraSystemProvider: Provider<out CameraSystem>,
    private val settingsRepository: SettingsRepository,
    private val launchConfigProvider: CameraLaunchConfigProvider,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : CameraSystemRepository {

    constructor(
        cameraXCameraSystemProvider: Provider<out CameraSystem>,
        settingsRepository: SettingsRepository,
        launchConfig: CameraLaunchConfig = CameraLaunchConfig(),
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    ) : this(
        cameraXCameraSystemProvider = cameraXCameraSystemProvider,
        settingsRepository = settingsRepository,
        launchConfigProvider = CameraLaunchConfigProvider().apply { setConfig(launchConfig) },
        scope = scope
    )

    private val cameraSystem: CameraSystem by lazy {
        cameraXCameraSystemProvider.get()
    }

    override val surfaceRequest: StateFlow<SurfaceRequest?> by lazy {
        cameraSystem.getSurfaceRequest()
    }

    override val systemConstraints: StateFlow<CameraSystemConstraints?> by lazy {
        cameraSystem.getSystemConstraints()
    }

    override val currentSettings: StateFlow<CameraAppSettings?> by lazy {
        cameraSystem.getCurrentSettings()
    }

    override val currentCameraState: StateFlow<CameraState> by lazy {
        cameraSystem.getCurrentCameraState()
    }

    private val _cameraPropertiesJSON = MutableStateFlow<String?>(null)
    override val cameraPropertiesJSON: StateFlow<String?> = _cameraPropertiesJSON.asStateFlow()

    private val initializationDeferred: Deferred<Unit> =
        scope.async(start = CoroutineStart.LAZY) {
            val launchConfig = launchConfigProvider.config.value
            val initialSettings = settingsRepository.getCurrentDefaultCameraAppSettings()
                .applyExternalCaptureMode(launchConfig.externalCaptureMode)
                .copy(debugSettings = launchConfig.debugSettings)
            cameraSystem.initialize(initialSettings) { properties ->
                _cameraPropertiesJSON.value = properties
            }
        }

    override suspend fun getCameraSystem(): CameraSystem {
        initializationDeferred.await()
        return cameraSystem
    }

    override suspend fun getSupportedMimeTypes(): List<String> {
        initializationDeferred.await()
        val constraints = systemConstraints.filterNotNull().first()
        return constraints.getSupportedMimeTypes().values.flatten().distinct()
    }
}
