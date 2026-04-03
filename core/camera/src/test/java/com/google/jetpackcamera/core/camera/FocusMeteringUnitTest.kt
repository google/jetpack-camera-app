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

import android.content.Context
import android.graphics.Matrix
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.CameraInternal
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import com.google.jetpackcamera.core.common.FakeFilePathGenerator
import com.google.jetpackcamera.core.camera.CameraEvent
import java.lang.reflect.Proxy
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager
import org.robolectric.shadows.ShadowLooper

class FakeLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this).apply {
        currentState = Lifecycle.State.RESUMED
    }
    override val lifecycle: Lifecycle get() = registry
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class FocusMeteringUnitTest {

    private lateinit var context: Context
    private lateinit var cameraSessionContext: CameraSessionContext
    private lateinit var currentCameraState: MutableStateFlow<CameraState>
    private lateinit var surfaceRequests: MutableStateFlow<SurfaceRequest?>
    private lateinit var focusMeteringEvents: Channel<CameraEvent.FocusMeteringEvent>
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var cameraProvider: ProcessCameraProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        currentCameraState = MutableStateFlow(CameraState())
        surfaceRequests = MutableStateFlow(null)
        focusMeteringEvents = Channel(Channel.UNLIMITED)

        // Setup simulated hardware camera for Robolectric
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics = Shadows.shadowOf(characteristics)
        shadowCharacteristics.set(CameraCharacteristics.LENS_FACING, CameraCharacteristics.LENS_FACING_BACK)
        shadowCharacteristics.set(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE, Rect(0, 0, 640, 480))
        
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        Shadows.shadowOf(cameraManager).addCamera("0", characteristics)

        ProcessCameraProvider.configureInstance(Camera2Config.defaultConfig())
        cameraProvider = ProcessCameraProvider.getInstance(context).get()

        cameraSessionContext = CameraSessionContext(
            context = context,
            cameraProvider = cameraProvider,
            backgroundDispatcher = testDispatcher,
            screenFlashEvents = Channel(),
            filePathGenerator = FakeFilePathGenerator(),
            focusMeteringEvents = focusMeteringEvents,
            videoCaptureControlEvents = Channel(),
            currentCameraState = currentCameraState,
            surfaceRequests = surfaceRequests,
            transientSettings = MutableStateFlow(null)
        )
    }

    @After
    fun tearDown() {
        cameraProvider.unbindAll()
    }

    @Test
    fun processFocusMeteringEvents_handlesIllegalArgumentException() = runTest(testDispatcher) {
        // Arrange
        val camera = cameraProvider.bindToLifecycle(FakeLifecycleOwner(), CameraSelector.DEFAULT_BACK_CAMERA)
        val realCameraInfo = camera.cameraInfo
        
        // We need a real SurfaceRequest to trigger the transformation info flow
        val surfaceRequest = SurfaceRequest(Size(640, 480), camera as CameraInternal) { }
        surfaceRequests.value = surfaceRequest

        val cameraControl = Proxy.newProxyInstance(
            CameraControl::class.java.classLoader,
            arrayOf(CameraControl::class.java)
        ) { _, method, _ ->
            if (method.name == "startFocusAndMetering") {
                throw IllegalArgumentException("Test Exception")
            }
            null
        } as CameraControl

        // Act
        val job = launch {
            with(cameraSessionContext) {
                processFocusMeteringEvents(realCameraInfo, cameraControl)
            }
        }

        // Trigger the transformation info flow
        surfaceRequest.updateTransformationInfo(
            SurfaceRequest.TransformationInfo.of(
                Rect(0, 0, 640, 480),
                0,
                0,
                false,
                Matrix(),
                false
            )
        )
        
        ShadowLooper.idleMainLooper()
        advanceUntilIdle()

        // Send a focus event
        focusMeteringEvents.trySend(CameraEvent.FocusMeteringEvent(0.5f, 0.5f))
        
        ShadowLooper.idleMainLooper()
        advanceUntilIdle()

        // Assert
        val focusState = currentCameraState.value.focusState
        assertThat(focusState).isInstanceOf(FocusState.Specified::class.java)
        assertThat((focusState as FocusState.Specified).status).isEqualTo(FocusState.Status.FAILURE)

        job.cancel()
    }
}