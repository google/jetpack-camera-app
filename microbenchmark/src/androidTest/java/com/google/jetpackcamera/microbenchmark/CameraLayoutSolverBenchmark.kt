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
package com.google.jetpackcamera.microbenchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutDefaults
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutSolver
import com.google.jetpackcamera.ui.components.capture.layout.CameraWindow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Per-solve cost of [CameraLayoutSolver] on real hardware.
 *
 * This exists because the cost was previously only ever measured on a desktop JVM, which is both
 * much faster per core and running a completely different runtime. Those numbers were used to argue
 * the solve was cheap. This is the measurement that can actually support that claim, or refute it.
 *
 * ## What this does and does not tell you
 *
 * A microbenchmark reports warm, steady-state cost: the harness warms up and then takes the median
 * of many iterations. That is the right way to compare one window against another, and it is what
 * makes the cases below meaningful next to each other.
 *
 * It is *not* the cost the user pays. The solve runs once, during the first composition of the
 * camera UI, when the code is cold and on the startup critical path. On a desktop JVM the first
 * call cost 25 ms against 1.7 us warm, a factor of roughly 15,000. Whatever this benchmark reports,
 * the startup cost is a different and larger number, and measuring it needs a trace section and the
 * macrobenchmark module rather than this.
 *
 * ## Why these windows
 *
 * The solve tries the designed layout first and only searches if that collides. How much searching
 * follows depends entirely on the window, so a single geometry would measure one arbitrary point on
 * a very wide range. These cases bracket it, and the evaluation counts are exact, not estimated:
 *
 * | case                       | window                            | candidate layouts evaluated |
 * |----------------------------|-----------------------------------|-----------------------------|
 * | [solveTypicalPhone]        | Pixel 9, gesture navigation       | 24                          |
 * | [solveWorstRealDevice]     | Pixel 9 XL, gesture navigation    | 89                          |
 * | [solveTallWindowZeroDrop]  | 443.1 x 986.3                     | 32                          |
 * | [solveWorstInScopeWindow]  | 340 x 720, 3-button navigation    | 587                         |
 * | [solveWorstRoutedWindow]   | 332 x 616, immersive              | 2,850                       |
 *
 * The first two are real devices from the golden fixture: the common case, and the most expensive
 * device in it. The third is a tall window where the primary frame stays flush only by tightening
 * gaps. The last two are ceilings rather than cases to optimise for: the most expensive window in
 * the property-test grid, and the most expensive one found by sweeping every window shape the
 * capture layout sends to the solver in 4 dp width and 8 dp height steps.
 */
@RunWith(AndroidJUnit4::class)
class CameraLayoutSolverBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val spec = CameraLayoutDefaults.spec()

    private fun window(w: Float, h: Float, top: Float, nav: Float, gesture: Float) =
        CameraWindow(w.dp, h.dp, top.dp, nav.dp, gesture.dp)

    /** Pixel 9 with gesture navigation, the common case. */
    @Test
    fun solveTypicalPhone() {
        val window = window(412f, 925f, 60f, 24f, 32f)
        benchmarkRule.measureRepeated {
            CameraLayoutSolver.solve(spec, window)
        }
    }

    /** Pixel 9 XL with gesture navigation, the most expensive device in the golden fixture. */
    @Test
    fun solveWorstRealDevice() {
        val window = window(448f, 997f, 54f, 24f, 32f)
        benchmarkRule.measureRepeated {
            CameraLayoutSolver.solve(spec, window)
        }
    }

    /**
     * Tall window where 9:16 fits below the top bar and 3:4 requires gap tightening to stay flush.
     */
    @Test
    fun solveTallWindowZeroDrop() {
        val window = window(443.1f, 986.3f, 66f, 24f, 32f)
        benchmarkRule.measureRepeated {
            CameraLayoutSolver.solve(spec, window)
        }
    }

    /** Most expensive window in the property-test grid. */
    @Test
    fun solveWorstInScopeWindow() {
        val window = window(340f, 720f, 48f, 48f, 48f)
        benchmarkRule.measureRepeated {
            CameraLayoutSolver.solve(spec, window)
        }
    }

    /** Most expensive window found in a fine sweep of the shapes the capture layout solves. */
    @Test
    fun solveWorstRoutedWindow() {
        val window = window(332f, 616f, 30f, 48f, 24f)
        benchmarkRule.measureRepeated {
            CameraLayoutSolver.solve(spec, window)
        }
    }
}
