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
package com.google.jetpackcamera.benchmark

import android.content.Intent
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.jetpackcamera.benchmark.utils.APP_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.benchmark.utils.DEFAULT_TEST_ITERATIONS
import com.google.jetpackcamera.benchmark.utils.JCA_PACKAGE_NAME
import com.google.jetpackcamera.benchmark.utils.allowAllRequiredPerms
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeatureGroupQueryBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun featureGroupQueryCold() = benchmarkFeatureQuery(StartupMode.COLD)

    @Test
    fun featureGroupQueryWarm() = benchmarkFeatureQuery(StartupMode.WARM)

    @OptIn(ExperimentalMetricApi::class)
    private fun benchmarkFeatureQuery(startupMode: StartupMode) {
        benchmarkRule.measureRepeated(
            packageName = JCA_PACKAGE_NAME,
            metrics = listOf(
                TraceSectionMetric("JCA:UpdateSystemConstraints"),
                TraceSectionMetric("JCA:IsGroupingSupported", TraceSectionMetric.Mode.Sum),
                TraceSectionMetric("JCA:BuildPipeline", TraceSectionMetric.Mode.Sum),
                TraceSectionMetric("JCA:OrchestrateQuery", TraceSectionMetric.Mode.Sum),
                TraceSectionMetric("JCA:GetCameraInfo", TraceSectionMetric.Mode.Sum),
                TraceSectionMetric("JCA:CreateVideoUseCase", TraceSectionMetric.Mode.Sum),
                TraceSectionMetric("JCA:CreateSessionConfig", TraceSectionMetric.Mode.Sum),
                TraceSectionMetric("JCA:CreatePreviewUseCase", TraceSectionMetric.Mode.Sum),
                TraceSectionMetric("JCA:CreateImageUseCase", TraceSectionMetric.Mode.Sum),
                TraceSectionMetric("JCA:CreateCameraEffects", TraceSectionMetric.Mode.Sum),
                TraceSectionMetric("JCA:FCQValidation", TraceSectionMetric.Mode.Sum)
            ),
            iterations = DEFAULT_TEST_ITERATIONS,
            startupMode = startupMode,
            setupBlock = {
                allowAllRequiredPerms(perms = APP_REQUIRED_PERMISSIONS.toTypedArray())
            }
        ) {
            pressHome()
            val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context
            val intent = context.packageManager.getLaunchIntentForPackage(JCA_PACKAGE_NAME)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
            device.wait(androidx.test.uiautomator.Until.hasObject(androidx.test.uiautomator.By.res("CaptureButton").enabled(true)), 20000)
        }
    }
}
