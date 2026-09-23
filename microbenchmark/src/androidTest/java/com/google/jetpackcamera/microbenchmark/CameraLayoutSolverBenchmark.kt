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
 * makes the three cases below meaningful next to each other.
 *
 * It is *not* the cost the user pays. The solve runs once, during the first composition of the
 * camera UI, when the code is cold and on the startup critical path. On a desktop JVM the first
 * call cost 25 ms against 1.7 us warm, a factor of roughly 15,000. Whatever this benchmark reports,
 * the startup cost is a different and larger number, and measuring it needs a trace section and the
 * macrobenchmark module rather than this.
 *
 * ## Why these three windows
 *
 * The solve tries the designed layout first and only searches if that collides. How much searching
 * follows depends entirely on the window, so a single geometry would measure one arbitrary point on
 * a very wide range. These three bracket it, and the evaluation counts are exact, not estimated:
 *
 * | window                  | candidate layouts evaluated |
 * |-------------------------|-----------------------------|
 * | Pixel 9, gesture nav    | 1                           |
 * | Pixel 9, three-button   | 96                          |
 * | 460x920 at 2.00 aspect  | 1,055                       |
 *
 * The first is the common case: 10 of the 22 device configurations in the golden fixture solve on
 * the first attempt. The second is the worst any real device in that fixture costs, and it is not
 * exotic, 9 of the 22 land there. The third is the worst supported window anywhere in the property
 * sweep; it is wider than any shipping phone and is included as a ceiling, not as a case to
 * optimise for.
 *
 * Comparing the first two isolates the cost of the search from the fixed cost of a solve, which is
 * the number that matters when deciding whether the search is worth optimising at all.
 */
@RunWith(AndroidJUnit4::class)
class CameraLayoutSolverBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val spec = CameraLayoutDefaults.spec()

    private fun window(w: Float, h: Float, top: Float, nav: Float, gesture: Float = 24f) =
        CameraWindow(w.dp, h.dp, top.dp, nav.dp, gesture.dp)

    /** Designed layout fits as-is. No search. */
    @Test
    fun solveTypicalPhone() {
        val window = window(412f, 925f, 60f, 24f)
        benchmarkRule.measureRepeated {
            CameraLayoutSolver.solve(spec, window)
        }
    }

    /** Worst real device geometry in the golden fixture: a full padding sweep. */
    @Test
    fun solveWorstRealDevice() {
        val window = window(412f, 925f, 60f, 48f)
        benchmarkRule.measureRepeated {
            CameraLayoutSolver.solve(spec, window)
        }
    }

    /** Worst supported window anywhere in the sweep: a full gap sweep. Wider than any phone. */
    @Test
    fun solveWorstSupportedWindow() {
        val window = window(460f, 920f, 42f, 48f)
        benchmarkRule.measureRepeated {
            CameraLayoutSolver.solve(spec, window)
        }
    }

    /**
     * Unsupported geometry, which exhausts the evaluation budget by design.
     *
     * A split-screen pane can be any shape, so this is reachable in production even though it is
     * not a shape the layout targets. What it must not be is unbounded, and this is where that
     * guarantee costs the most.
     */
    @Test
    fun solveUnsupportedGeometry() {
        val window = window(340f, 600f, 60f, 0f)
        benchmarkRule.measureRepeated {
            CameraLayoutSolver.solve(spec, window)
        }
    }
}
