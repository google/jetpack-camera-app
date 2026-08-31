/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import com.google.jetpackcamera.core.common.FilePathGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class FocusMeteringTest {
    private val standardDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(standardDispatcher)

    private lateinit var cameraSessionContext: CameraSessionContext
    private lateinit var mockCameraControl: CameraControl
    private lateinit var mockCameraInfo: CameraInfo
    private lateinit var captureResultsFlow: MutableStateFlow<TotalCaptureResult?>

    private lateinit var focusMeteringEvents: Channel<CameraEvent.FocusMeteringEvent>
    private lateinit var currentCameraState: MutableStateFlow<CameraState>
    private lateinit var surfaceRequests: MutableStateFlow<SurfaceRequest?>

    private lateinit var cameraExtMock: org.mockito.MockedStatic<*>

    @Before
    fun setup() {
        mockCameraControl = mock(CameraControl::class.java)
        mockCameraInfo = mock(CameraInfo::class.java)

        cameraExtMock = FocusMeteringTestHelper.mockSensorRect(
            mockCameraInfo,
            Rect(0, 0, 1920, 1080)
        )

        whenever(
            mockCameraInfo.isFocusMeteringSupported(any(FocusMeteringAction::class.java))
        ).thenReturn(true)

        val mockFocusResult = mock(FocusMeteringResult::class.java)
        whenever(mockFocusResult.isFocusSuccessful).thenReturn(true)
        whenever(mockCameraControl.startFocusAndMetering(any(FocusMeteringAction::class.java)))
            .thenReturn(Futures.immediateFuture(mockFocusResult))

        whenever(mockCameraControl.cancelFocusAndMetering())
            .thenReturn(Futures.immediateFuture(null))

        captureResultsFlow = MutableStateFlow(null)
        focusMeteringEvents = Channel()
        currentCameraState = MutableStateFlow(CameraState())

        val mockSurfaceRequest = mock(SurfaceRequest::class.java)
        whenever(mockSurfaceRequest.resolution).thenReturn(android.util.Size(1920, 1080))
        doAnswer { invocation ->
            val listener = invocation.getArgument<SurfaceRequest.TransformationInfoListener>(1)
            val mockTransformationInfo = mock(SurfaceRequest.TransformationInfo::class.java)
            org.mockito.Mockito.`when`(
                mockTransformationInfo.sensorToBufferTransform
            ).thenReturn(Matrix())
            listener.onTransformationInfoUpdate(mockTransformationInfo)
            null
        }.`when`(mockSurfaceRequest).setTransformationInfoListener(
            any(java.util.concurrent.Executor::class.java),
            any(SurfaceRequest.TransformationInfoListener::class.java)
        )

        surfaceRequests = MutableStateFlow(mockSurfaceRequest)

        cameraSessionContext = CameraSessionContext(
            context = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>(),
            cameraProvider = mock(ProcessCameraProvider::class.java),
            backgroundDispatcher = standardDispatcher,
            screenFlashEvents = Channel(),
            filePathGenerator = mock(FilePathGenerator::class.java),
            focusMeteringEvents = focusMeteringEvents,
            videoCaptureControlEvents = Channel(),
            currentCameraState = currentCameraState,
            surfaceRequests = surfaceRequests,
            transientSettings = MutableStateFlow(null)
        )
    }

    private fun mockCaptureResult(sceneChangeDetected: Boolean): TotalCaptureResult {
        val result = mock(TotalCaptureResult::class.java)
        val value = if (sceneChangeDetected) {
            CameraMetadata.CONTROL_AF_SCENE_CHANGE_DETECTED
        } else {
            CameraMetadata.CONTROL_AF_SCENE_CHANGE_NOT_DETECTED
        }
        whenever(result.get(CaptureResult.CONTROL_AF_SCENE_CHANGE)).thenReturn(value)
        return result
    }

    @org.junit.After
    fun teardown() {
        cameraExtMock.close()
    }

    @Test
    fun focusHeld_whenCaptureResultsAreNull_during3000msDelay() = testScope.runTest {
        val job = launch {
            with(cameraSessionContext) {
                processFocusMeteringEvents(mockCameraInfo, mockCameraControl, captureResultsFlow)
            }
        }

        // Send a tap to focus event
        focusMeteringEvents.send(CameraEvent.FocusMeteringEvent(0.5f, 0.5f))

        advanceTimeBy(3000) // Delay is 3000ms

        // capture results are null (not sent), so it should just wait for first not-null.
        // It shouldn't cancel.
        verify(mockCameraControl, never()).cancelFocusAndMetering()

        job.cancel()
    }

    @Test
    fun focusNotCancelled_whenNonConsecutiveSceneChangeDetects() = testScope.runTest {
        val job = launch {
            with(cameraSessionContext) {
                processFocusMeteringEvents(mockCameraInfo, mockCameraControl, captureResultsFlow)
            }
        }

        focusMeteringEvents.send(CameraEvent.FocusMeteringEvent(0.5f, 0.5f))
        advanceTimeBy(3000)

        // Generate non-consecutive results
        captureResultsFlow.value = mockCaptureResult(sceneChangeDetected = true)
        advanceTimeBy(100)
        captureResultsFlow.value = mockCaptureResult(sceneChangeDetected = true)
        advanceTimeBy(100)
        captureResultsFlow.value = mockCaptureResult(sceneChangeDetected = false)
        advanceTimeBy(100)
        captureResultsFlow.value = mockCaptureResult(sceneChangeDetected = true)
        advanceTimeBy(100)
        captureResultsFlow.value = mockCaptureResult(sceneChangeDetected = true)
        advanceTimeBy(100)

        verify(mockCameraControl, never()).cancelFocusAndMetering()

        job.cancel()
    }

    @Test
    fun focusCancelled_whenConsecutiveSceneChangeDetects_afterDelay() = testScope.runTest {
        val job = launch {
            with(cameraSessionContext) {
                processFocusMeteringEvents(mockCameraInfo, mockCameraControl, captureResultsFlow)
            }
        }

        focusMeteringEvents.send(CameraEvent.FocusMeteringEvent(0.5f, 0.5f))

        var currentState = currentCameraState.value.focusState
        assertThat(currentState).isInstanceOf(FocusState.Specified::class.java)

        advanceTimeBy(3000)

        // 3 consecutive scene change detected frames
        captureResultsFlow.value = mockCaptureResult(sceneChangeDetected = true)
        advanceTimeBy(33) // ~30 fps
        captureResultsFlow.value = mockCaptureResult(sceneChangeDetected = true)
        advanceTimeBy(33)
        captureResultsFlow.value = mockCaptureResult(sceneChangeDetected = true)
        advanceTimeBy(33)

        verify(mockCameraControl).cancelFocusAndMetering()

        currentState = currentCameraState.value.focusState
        assertThat(currentState).isInstanceOf(FocusState.Specified::class.java)
        assertThat(
            (currentState as FocusState.Specified).status
        ).isEqualTo(FocusState.Status.CANCELLED)

        job.cancel()
    }

    @Test
    fun newFocusEvent_preemptsPreviousWaitingJob() = testScope.runTest {
        val job = launch {
            with(cameraSessionContext) {
                processFocusMeteringEvents(mockCameraInfo, mockCameraControl, captureResultsFlow)
            }
        }

        focusMeteringEvents.send(CameraEvent.FocusMeteringEvent(0.5f, 0.5f))

        advanceTimeBy(1500)

        // At 1.5 seconds, send another FocusMeteringEvent
        focusMeteringEvents.send(CameraEvent.FocusMeteringEvent(0.5f, 0.5f))

        advanceTimeBy(1500)

        captureResultsFlow.value = mockCaptureResult(sceneChangeDetected = true)
        captureResultsFlow.value = mockCaptureResult(sceneChangeDetected = true)
        captureResultsFlow.value = mockCaptureResult(sceneChangeDetected = true)

        verify(mockCameraControl, never()).cancelFocusAndMetering()

        // Wait the remaining 1500ms for the second event
        advanceTimeBy(1500)

        // Now send the 3 frames for the second event
        captureResultsFlow.value = mockCaptureResult(sceneChangeDetected = true)
        advanceTimeBy(33)
        captureResultsFlow.value = mockCaptureResult(sceneChangeDetected = true)
        advanceTimeBy(33)
        captureResultsFlow.value = mockCaptureResult(sceneChangeDetected = true)
        advanceTimeBy(33)

        verify(mockCameraControl).cancelFocusAndMetering()

        job.cancel()
    }
}
