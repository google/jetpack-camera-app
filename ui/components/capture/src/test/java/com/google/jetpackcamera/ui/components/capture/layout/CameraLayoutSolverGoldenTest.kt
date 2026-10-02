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
package com.google.jetpackcamera.ui.components.capture.layout

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Pins [CameraLayoutSolver] to the layouts the design was reviewed against.
 *
 * The design was validated interactively in a browser prototype before any of this code existed,
 * and the fixture in `test/resources/layout/solver_golden.txt` began as a verbatim capture of that
 * prototype's solutions for every device window in the evaluation set. Cases whose layout later
 * changed on purpose were regenerated from this solver, and the fixture header lists each change.
 * The fixture travels with the repository, so these tests need nothing external to run. A refactor
 * that quietly changes where the shutter button lands fails here rather than in someone's hands.
 *
 * The solver deliberately differs from the prototype in two respects, neither of which is visible
 * in this fixture: it refuses to enter the compact-toolbar band when the compacted toolbar would
 * not fit there, and it bounds the repair search. Both were corrections to real defects, and both
 * were verified to leave every case below byte-identical before being adopted.
 *
 * If a change to the solver is intentional, regenerate the fixture and review the diff: every line
 * that moves is a device whose layout you have changed.
 *
 * These are plain JVM tests. The solver has no Compose or Android dependency, so the whole
 * evaluation set runs in milliseconds and belongs in ordinary presubmit.
 */
class CameraLayoutSolverGoldenTest {

    private val cases = GoldenFixture.load()

    /** Positions are quantized to 0.1 dp, so parity is exact at that resolution. */
    private val tolerance = 0.05

    @Test
    fun fixtureCoversTheFullEvaluationSet() {
        // 11 devices x 2 navigation modes. Guards against a truncated fixture silently
        // weakening every other test in this class.
        assertThat(cases).hasSize(22)
        assertThat(cases.map { it.label }.toSet()).hasSize(22)
    }

    @Test
    fun controlRowsMatchTheReference() {
        forEachCase { case, solution ->
            case.rows.forEach { (rowId, expected) ->
                val actual = solution.rowBands.getValue(rowId)
                assertWithMessage("%s %s top", case.label, rowId)
                    .that(actual.top.value.toDouble())
                    .isWithin(tolerance).of(expected.first.toDouble())
                assertWithMessage("%s %s bottom", case.label, rowId)
                    .that(actual.bottom.value.toDouble())
                    .isWithin(tolerance).of(expected.second.toDouble())
            }
        }
    }

    @Test
    fun viewfinderPlacementsMatchTheReference() {
        forEachCase { case, solution ->
            case.viewfinders.forEach { (id, expected) ->
                val actual = solution.viewfinders.getValue(id)
                assertWithMessage("%s %s top", case.label, id)
                    .that(actual.top.value.toDouble())
                    .isWithin(tolerance).of(expected.first.toDouble())
                assertWithMessage("%s %s bottom", case.label, id)
                    .that(actual.bottom.value.toDouble())
                    .isWithin(tolerance).of(expected.second.toDouble())
            }
        }
    }

    @Test
    fun immersiveDecisionMatchesTheReference() {
        forEachCase { case, solution ->
            assertWithMessage("%s requiresImmersive", case.label)
                .that(solution.requiresImmersive)
                .isEqualTo(case.requiresImmersive)
        }
    }

    @Test
    fun resolvedSpacingMatchesTheReference() {
        forEachCase { case, solution ->
            assertWithMessage("%s toolbar height", case.label)
                .that(solution.toolbarHeight.value.toDouble())
                .isWithin(tolerance).of(case.toolbarHeightDp.toDouble())
            assertWithMessage("%s bottom padding", case.label)
                .that(solution.resolvedBottomPadding.value.toDouble())
                .isWithin(tolerance).of(case.bottomPaddingDp.toDouble())

            // resolvedGaps is bottom-up, parallel to CameraLayoutDefaults.rows().
            val expected = listOf(
                case.gapAboveToolbarDp,
                case.gapAboveModeDp,
                case.gapAboveCaptureDp
            )
            expected.forEachIndexed { index, value ->
                assertWithMessage("%s gap[%s]", case.label, index)
                    .that(solution.resolvedGaps[index].value.toDouble())
                    .isWithin(tolerance).of(value.toDouble())
            }
        }
    }

    @Test
    fun minimumClearanceMatchesTheReference() {
        forEachCase { case, solution ->
            assertWithMessage("%s min clearance", case.label)
                .that(solution.minClearance.value.toDouble())
                .isWithin(tolerance).of(case.minClearanceDp.toDouble())
        }
    }

    @Test
    fun everyDeviceInTheEvaluationSetIsCollisionFree() {
        forEachCase { case, solution ->
            assertWithMessage("%s collisions", case.label)
                .that(solution.collisions)
                .isEmpty()
        }
    }

    @Test
    fun viewfinderTopStartModeIsCollisionFreeAcrossEvaluationSet() {
        val spec = CameraLayoutDefaults.spec(
            topStartPlacement = TopStartPlacement.VIEWFINDER_TOP_START
        )
        cases.forEach { case ->
            val solution = CameraLayoutSolver.solve(spec, case.window)
            assertWithMessage("%s collisions", case.label)
                .that(solution.collisions)
                .isEmpty()
        }
    }

    /**
     * The design reference for a 360 x 740 window, checked independently of the fixture so that
     * regenerating the fixture cannot quietly lose it.
     *
     * Both navigation modes share one layout. The full-height toolbar stays where gesture
     * navigation puts it, the primary frame shifts down to clear the top-start control and the
     * controls, the square frame centres in the primary frame's area before shifting down, and the
     * tall frame passes behind the top-start control with its bottom edge below the bottom row.
     */
    @Test
    fun window360x740MatchesTheDesignReference() {
        val spec = CameraLayoutDefaults.spec()
        val topInset = 48f
        listOf(24f, 48f).forEach { navInset ->
            val window = CameraWindow(
                width = 360.dp,
                height = 740.dp,
                topInset = topInset.dp,
                navInset = navInset.dp,
                gestureInset = 16.dp
            )
            val solution = CameraLayoutSolver.solve(spec, window)
            val label = "nav $navInset"

            assertWithMessage("%s collisions", label).that(solution.collisions).isEmpty()
            assertWithMessage("%s toolbar height", label)
                .that(solution.toolbarHeight)
                .isEqualTo(CameraLayoutDefaults.BottomToolbarHeight)

            fun assertBand(bands: Map<String, DpRange>, id: String, top: Float, bottom: Float) {
                val band = bands.getValue(id)
                assertWithMessage("%s %s top", label, id)
                    .that(band.top.value.toDouble())
                    .isWithin(tolerance).of(top.toDouble())
                assertWithMessage("%s %s bottom", label, id)
                    .that(band.bottom.value.toDouble())
                    .isWithin(tolerance).of(bottom.toDouble())
            }
            assertBand(solution.rowBands, CameraRowIds.ZOOM_BAR, 376f, 424f)
            assertBand(solution.rowBands, CameraRowIds.CAPTURE_ROW, 456f, 542f)
            assertBand(solution.rowBands, CameraRowIds.MODE_SWITCHER, 566f, 598f)
            assertBand(solution.rowBands, CameraRowIds.BOTTOM_TOOLBAR, 622f, 686f)
            assertBand(solution.viewfinders, CameraViewfinderIds.RATIO_3_4, 122f, 602f)
            assertBand(solution.viewfinders, CameraViewfinderIds.RATIO_1_1, 186f, 546f)

            // The reference draws the tall frame 1 dp above the top-start control's visible top.
            // Any top from the top bar down to that limit passes behind the control.
            val tall = solution.viewfinders.getValue(CameraViewfinderIds.RATIO_9_16)
            val behindTopStartLimit = topInset + CameraLayoutDefaults.TopStartVisibleInset.value -
                CameraLayoutDefaults.TopStartMinEdgeGap.value
            assertWithMessage("%s 9:16 top", label).that(tall.top.value).isAtLeast(topInset)
            assertWithMessage("%s 9:16 top", label)
                .that(tall.top.value)
                .isAtMost(behindTopStartLimit)
            assertWithMessage("%s 9:16 height", label)
                .that((tall.bottom - tall.top).value.toDouble())
                .isWithin(tolerance).of(640.0)
        }
    }

    private fun forEachCase(block: (GoldenCase, CameraLayoutSolution) -> Unit) {
        val spec = CameraLayoutDefaults.spec()
        cases.forEach { case -> block(case, CameraLayoutSolver.solve(spec, case.window)) }
    }
}
