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

import android.content.Intent
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.jetpackcamera.benchmark.utils.APP_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.benchmark.utils.DEFAULT_TEST_ITERATIONS
import com.google.jetpackcamera.benchmark.utils.FIRST_FRAME_TRACE_MAIN_ACTIVITY
import com.google.jetpackcamera.benchmark.utils.FIRST_FRAME_TRACE_PREVIEW
import com.google.jetpackcamera.benchmark.utils.IMAGE_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.benchmark.utils.JCA_PACKAGE_NAME
import com.google.jetpackcamera.benchmark.utils.allowAllRequiredPerms
import com.google.jetpackcamera.benchmark.utils.clickCaptureButton
import com.google.jetpackcamera.benchmark.utils.findObjectByRes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirstFrameBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun timeToFirstFrameDefaultSettingsColdStartup() {
        benchmarkFirstFrame(setupBlock = {
            allowAllRequiredPerms(perms = APP_REQUIRED_PERMISSIONS.toTypedArray())
        })
    }

    @Test
    fun timeToFirstFrameDefaultSettingsHotStartup() {
        benchmarkFirstFrame(startupMode = StartupMode.HOT, setupBlock = {
            allowAllRequiredPerms(perms = APP_REQUIRED_PERMISSIONS.toTypedArray())
        })
    }

    /**
     * The benchmark for first frame tracks the amount of time it takes from preview loading on the
     * screen to when the use case is able to start capturing frames.
     *
     * Note that the trace this benchmark tracks is the earliest point in which a frame is captured
     * and sent to a surface. This does not necessarily mean the frame is visible on screen.
     *
     * @param startupMode the designated startup mode, either [StartupMode.COLD] or [StartupMode.HOT]
     * @param timeout option to change the default timeout length after clicking the Image Capture
     *  button.
     *
     */
    @OptIn(ExperimentalMetricApi::class)
    private fun benchmarkFirstFrame(
        startupMode: StartupMode? = StartupMode.COLD,
        iterations: Int = DEFAULT_TEST_ITERATIONS,
        timeout: Long = 15000,
        intent: Intent? = null,
        setupBlock: MacrobenchmarkScope.() -> Unit = {}
    ) {
        benchmarkRule.measureRepeated(
            packageName = JCA_PACKAGE_NAME,
            metrics = buildList {
                add(StartupTimingMetric())
                if (startupMode == StartupMode.COLD) {
                    add(
                        TraceSectionMetric(
                            sectionName = FIRST_FRAME_TRACE_MAIN_ACTIVITY,
                            targetPackageOnly = false,
                            mode = TraceSectionMetric.Mode.First
                        )
                    )
                }
                add(
                    TraceSectionMetric(
                        sectionName = FIRST_FRAME_TRACE_PREVIEW,
                        targetPackageOnly = false,
                        mode = TraceSectionMetric.Mode.First
                    )
                )
            },
            iterations = iterations,
            startupMode = startupMode,
            setupBlock = setupBlock
        ) {
            pressHome()
            if (intent == null) startActivityAndWait() else startActivityAndWait(intent)
            device.waitForIdle()

            clickCaptureButton(device)

            // ensure trace is closed
            findObjectByRes(
                device = device,
                testTag = IMAGE_CAPTURE_SUCCESS_TAG,
                timeout = timeout,
                shouldFailIfNotFound = true
            )
        }
    }
}
