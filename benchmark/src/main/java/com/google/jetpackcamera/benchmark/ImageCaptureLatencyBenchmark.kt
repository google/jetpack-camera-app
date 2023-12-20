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
package com.google.jetpackcamera.benchmark

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageCaptureLatencyBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun rearCameraNoFlashLatency() {
        imageCaptureLatency(shouldFaceFront = false, flashMode = FlashMode.OFF)
    }

    @Test
    fun frontCameraNoFlashLatency() {
        imageCaptureLatency(shouldFaceFront = true, flashMode = FlashMode.OFF)
    }

    @Test
    fun rearCameraWithFlashLatency() {
        imageCaptureLatency(shouldFaceFront = false, flashMode = FlashMode.ON)
    }

    // todo(kimblebee): front flash latency test

    /**
     * Measures the time between an onClick event on the Capture Button and onImageCapture
     * callback being fired from
     * [takePicture][com.google.jetpackcamera.domain.camera.CameraXCameraUseCase.takePicture].
     *
     *  @param shouldFaceFront the direction the camera should be facing.
     *  @param flashMode the designated [FlashMode] for the camera.
     *  @param timeout option to change the default timeout length after clicking the Image Capture
     *  button.
     *
     */
    @OptIn(ExperimentalMetricApi::class)
    private fun imageCaptureLatency(
        shouldFaceFront: Boolean,
        flashMode: FlashMode,
        timeout: Long = 15000
    ) {
        benchmarkRule.measureRepeated(
            packageName = JCA_PACKAGE_NAME,
            metrics = listOf(
                TraceSectionMetric(sectionName = IMAGE_CAPTURE_TRACE, targetPackageOnly = false)
            ),
            iterations = DEFAULT_TEST_ITERATIONS,
            setupBlock = {
                allowCamera()
                pressHome()
                startActivityAndWait()
                toggleQuickSettings(device)
                setQuickFrontFacingCamera(shouldFaceFront = shouldFaceFront, device = device)
                setQuickSetFlash(flashMode = flashMode, device = device)
                toggleQuickSettings(device)
                device.waitForIdle()
            }

        ) {
            device.waitForIdle()

            clickCaptureButton(device)

            val str = "ImageCaptureSuccessToast"
            // ensure trace is closed
            findObjectByRes(
                device = device,
                testTag = str,
                timeout = timeout,
                shouldFailIfNotFound = true
            )
        }
    }
}
