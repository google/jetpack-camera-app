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

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Pins [CameraLayoutSolver] to the reference implementation the layout design was reviewed against.
 *
 * The design was validated interactively in a browser prototype before any of this code existed,
 * and the layouts that prototype produced are what reviewers actually signed off on. The fixture in
 * `test/resources/layout/solver_golden.txt` is a verbatim capture of those solutions for every
 * device window in the evaluation set, so it travels with the repository and these tests need
 * nothing external to run. A refactor that quietly changes where the shutter button lands fails
 * here rather than in someone's hands.
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

    private fun forEachCase(block: (GoldenCase, CameraLayoutSolution) -> Unit) {
        val spec = CameraLayoutDefaults.spec()
        cases.forEach { case -> block(case, CameraLayoutSolver.solve(spec, case.window)) }
    }
}
