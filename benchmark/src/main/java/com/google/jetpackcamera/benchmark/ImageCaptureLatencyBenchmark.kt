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
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.fail
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
        // Flash test needs extra time at the end to ensure the trace is closed
        imageCaptureLatency(shouldFaceFront = false, flashMode = FlashMode.ON, sleepInterval = 5000)
    }


    //todo(kimblebee): front flash latency test

    /**
     * Measures the time between an onClick event on the Capture Button and onImageCapture
     * callback being fired from
     * [takePicture][com.google.jetpackcamera.domain.camera.CameraXCameraUseCase.takePicture].
     *
     *  @param shouldFaceFront the direction the camera should be facing.
     *  @param flashMode the designated [FlashMode] for the camera.
     *  @param sleepInterval option to change the default sleep interval after performing clicking
     *  the Image Capture button.
     *
     */
    @OptIn(ExperimentalMetricApi::class)
    private fun imageCaptureLatency(
        shouldFaceFront: Boolean,
        flashMode: FlashMode,
        sleepInterval: Long = 500
    ) {
        benchmarkRule.measureRepeated(
            packageName = JCA_PACKAGE_NAME,
            metrics = listOf(
                TraceSectionMetric(sectionName = IMAGE_CAPTURE_TRACE, targetPackageOnly = false)
            ),
            iterations = DEFAULT_TEST_ITERATIONS,
            startupMode = StartupMode.WARM,
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
            val selector = By.res("CaptureButton")

            if (!device.wait(Until.hasObject(selector), 2_500)) {
                fail("Did not find object with id CaptureButton")
            }
            device
                .findObject(selector)
                .click()

            // ensure trace is closed
            Thread.sleep(sleepInterval)
        }
    }
}
