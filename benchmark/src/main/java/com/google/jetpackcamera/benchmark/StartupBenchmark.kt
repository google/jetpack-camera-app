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

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Run this benchmark from Studio to see startup measurements, and captured system traces
 * for investigating your app's performance.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupColdWithoutCameraPermission() {
        benchmarkStartup()
    }

    @Test
    fun startupCold() {
        benchmarkStartup(
            setupBlock =
            { allowCamera() }
        )
    }

    @Test
    fun startupWarm() {
        benchmarkStartup(
            startupMode = StartupMode.WARM,
            setupBlock =
            { allowCamera() }
        )
    }

    @Test
    fun startupHot() {
        benchmarkStartup(
            startupMode = StartupMode.HOT,
            setupBlock =
            { allowCamera() }
        )
    }

    private fun benchmarkStartup(
        setupBlock: MacrobenchmarkScope.() -> Unit = {},
        startupMode: StartupMode? = StartupMode.COLD
    ) {
        benchmarkRule.measureRepeated(
            packageName = JCA_PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric()),
            iterations = DEFAULT_TEST_ITERATIONS,
            startupMode = startupMode,
            setupBlock = setupBlock
        ) {
            pressHome()
            startActivityAndWait()
        }
    }
}
