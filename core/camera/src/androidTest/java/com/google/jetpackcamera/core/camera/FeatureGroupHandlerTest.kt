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
package com.google.jetpackcamera.core.camera

import android.app.Application
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.common.FakeFilePathGenerator
import com.google.jetpackcamera.settings.SettableConstraintsRepositoryImpl
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeatureGroupHandlerTest {

    private lateinit var application: Application
    private lateinit var cameraSystem: CameraXCameraSystem
    private lateinit var featureGroupHandler: FeatureGroupHandler
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var constraintsRepository: SettableConstraintsRepositoryImpl

    @Before
    fun setup() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        application = instrumentation.targetContext.applicationContext as Application

        cameraProvider = ProcessCameraProvider.awaitInstance(application)

        constraintsRepository = SettableConstraintsRepositoryImpl()
        cameraSystem = CameraXCameraSystem(
            application = application,
            defaultDispatcher = Dispatchers.Main,
            iODispatcher = Dispatchers.IO,
            constraintsRepository = constraintsRepository,
            filePathGenerator = FakeFilePathGenerator(),
            availabilityCheckers = emptyMap(),
            effectProviders = emptyMap(),
            imagePostProcessors = emptyMap()
        )
        cameraSystem.initialize(DEFAULT_CAMERA_APP_SETTINGS) {}

        featureGroupHandler = FeatureGroupHandler(
            cameraSystem = cameraSystem,
            cameraProvider = cameraProvider,
            defaultCameraSessionContext = cameraSystem.defaultCameraSessionContext,
            defaultDispatcher = Dispatchers.Main
        )
    }

    @Test
    fun isGroupingSupported_returnsTrue_forDefaultSettings() = runBlocking {
        val currentSettings = DEFAULT_CAMERA_APP_SETTINGS
        val cameraSelector = currentSettings.cameraLensFacing.toCameraSelector()
        val cameraInfo = cameraProvider.getCameraInfo(cameraSelector)

        val result = featureGroupHandler.isGroupingSupported(
            cameraAppSettings = currentSettings,
            cameraInfo = cameraInfo,
            initialSystemConstraints = constraintsRepository.systemConstraints.value!!
        )

        assertThat(result).isTrue()
    }

    @Test
    fun filterSystemConstraints_returnsValidConstraints() = runBlocking {
        val currentSettings = DEFAULT_CAMERA_APP_SETTINGS
        val initialConstraints = constraintsRepository.systemConstraints.value!!

        val result = featureGroupHandler.filterSystemConstraints(
            currentSettings = currentSettings,
            initialSystemConstraints = initialConstraints,
            currentSystemConstraints = initialConstraints
        )

        assertThat(result).isNotNull()
        assertThat(
            result.availableLenses
        ).containsAtLeastElementsIn(initialConstraints.availableLenses)

        // Ensure per-lens constraints for current lens are present
        val lensFacing = currentSettings.cameraLensFacing
        assertThat(result.perLensConstraints).containsKey(lensFacing)
    }

    @Test
    fun filterSystemConstraints_forAllAvailableLenses() = runBlocking {
        val initialConstraints = constraintsRepository.systemConstraints.value!!
        for (lensFacing in initialConstraints.availableLenses) {
            val currentSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(cameraLensFacing = lensFacing)
            val result = featureGroupHandler.filterSystemConstraints(
                currentSettings = currentSettings,
                initialSystemConstraints = initialConstraints,
                currentSystemConstraints = initialConstraints
            )
            assertThat(result.perLensConstraints).containsKey(lensFacing)
        }
    }

    @Test
    fun isHdrSupportedWithJpegR_initiallyNull() {
        // Based on atomic<Boolean?>(null) initialization
        assertThat(featureGroupHandler.isHdrSupportedWithJpegR()).isNull()
    }

    @Test
    fun isHdrSupportedWithJpegR_updatesAfterConstraintsUpdate() = runBlocking {
        val currentSettings = DEFAULT_CAMERA_APP_SETTINGS
        val initialConstraints = constraintsRepository.systemConstraints.value!!

        featureGroupHandler.filterSystemConstraints(
            currentSettings = currentSettings,
            initialSystemConstraints = initialConstraints,
            currentSystemConstraints = initialConstraints
        )

        // After update, it should return a boolean value (true or false), confirming the cache
        // logic executed.
        assertThat(featureGroupHandler.isHdrSupportedWithJpegR()).isNotNull()
    }
}
