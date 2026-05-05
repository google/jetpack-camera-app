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

import com.google.jetpackcamera.core.camera.CameraXCameraSystem
import com.google.jetpackcamera.core.common.DefaultCoroutineScope
import com.google.jetpackcamera.settings.SettableConstraintsRepository
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Implementation of [CameraSystemRepository] that lazily constructs [CameraXCameraSystem]
 * and handles writing camera constraints to the settings datastore.
 */
@Singleton
class CameraXCameraSystemRepository @Inject constructor(
    private val cameraXCameraSystemProvider: Provider<CameraXCameraSystem>,
    private val constraintsRepository: SettableConstraintsRepository,
    @DefaultCoroutineScope private val coroutineScope: CoroutineScope
) : CameraSystemRepository {

    override val cameraSystem: CameraXCameraSystem by lazy {
        val system = cameraXCameraSystemProvider.get()

        // Observe discovered hardware constraints and save them to settings datastore
        coroutineScope.launch {
            system.getSystemConstraints().filterNotNull().collect { constraints ->
                constraintsRepository.updateSystemConstraints(constraints)
            }
        }
        system
    }
}
