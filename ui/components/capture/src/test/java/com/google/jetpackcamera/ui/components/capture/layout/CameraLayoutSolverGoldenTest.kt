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
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

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
@RunWith(JUnit4::class)
class CameraLayoutSolverGoldenTest {

    private val cases = GoldenFixture.load()

    /** Positions are quantized to 0.1 dp, so parity is exact at that resolution. */
    private val tolerance = 0.05

    /**
     * Navigation bar heights a device reports, each paired with the bottom gesture zone reported
     * alongside it: gesture navigation with a 24 dp bar, or 16 dp before Android 13, over a 32 dp
     * zone; a window that reports no bar, with the same zone; and 3-button navigation, whose 48 dp
     * bar covers a zone of the same height.
     */
    private val reportedBottomInsets = listOf(24f to 32f, 16f to 32f, 0f to 32f, 48f to 48f)

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
     * Every bottom inset the device can report shares one layout. The full-height toolbar stays
     * where gesture navigation puts it, the primary frame shifts down to clear the top-start
     * control and the controls, the square frame centres in the primary frame's area before
     * shifting down, and the tall frame passes behind the top-start control with its bottom edge
     * below the bottom row.
     */
    @Test
    fun window360x740MatchesTheDesignReference() {
        val spec = CameraLayoutDefaults.spec()
        val topInset = 48f
        reportedBottomInsets.forEach { (navInset, gestureInset) ->
            val window = CameraWindow(
                width = 360.dp,
                height = 740.dp,
                topInset = topInset.dp,
                navInset = navInset.dp,
                gestureInset = gestureInset.dp
            )
            val solution = CameraLayoutSolver.solve(spec, window)
            val label = "nav $navInset gesture $gestureInset"

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

    /**
     * The bottom inset of a single device changes with the navigation mode and the platform
     * release, so the layout must not move with it.
     *
     * Each window in the set is solved with every bottom inset a device reports and compared with
     * its gesture navigation layout. A comparison is skipped only when the tall frame fits between
     * the top bar and the navigation bar in one configuration and not in the other: the frame then
     * changes placement, and the controls move with it. In this set that applies to pixel3 with
     * 3-button navigation and to pixel_fold with a 16 dp bar or none, and the skipped comparisons
     * are pinned so that the exemption cannot grow to hide a regression.
     */
    @Test
    fun layoutDoesNotMoveWithTheBottomInset() {
        val spec = CameraLayoutDefaults.spec()
        val skipped = mutableListOf<String>()
        cases.filter { it.navMode == "gesture" && !it.requiresImmersive }.forEach { case ->
            val reference = CameraLayoutSolver.solve(spec, case.window)
            reportedBottomInsets.forEach { (navInset, gestureInset) ->
                val window = case.window.copy(
                    navInset = navInset.dp,
                    gestureInset = gestureInset.dp
                )
                val label = "${case.device} nav $navInset gesture $gestureInset"
                if (tallFrameFitsBetweenBars(window) != tallFrameFitsBetweenBars(case.window)) {
                    skipped += label
                } else {
                    val solution = CameraLayoutSolver.solve(spec, window)
                    assertSameBands(label, reference.rowBands, solution.rowBands)
                    assertSameBands(label, reference.viewfinders, solution.viewfinders)
                }
            }
        }
        assertThat(skipped).containsExactly(
            "pixel3 nav 48.0 gesture 48.0",
            "pixel_fold nav 16.0 gesture 32.0",
            "pixel_fold nav 0.0 gesture 32.0"
        )
    }

    private fun tallFrameFitsBetweenBars(window: CameraWindow): Boolean {
        val tallest = window.width.value / (9f / 16f)
        return tallest <= window.height.value - window.topInset.value - window.navInset.value
    }

    private fun assertSameBands(
        label: String,
        expected: Map<String, DpRange>,
        actual: Map<String, DpRange>
    ) {
        expected.forEach { (id, band) ->
            assertWithMessage("%s %s top", label, id)
                .that(actual.getValue(id).top.value.toDouble())
                .isWithin(tolerance).of(band.top.value.toDouble())
            assertWithMessage("%s %s bottom", label, id)
                .that(actual.getValue(id).bottom.value.toDouble())
                .isWithin(tolerance).of(band.bottom.value.toDouble())
        }
    }

    private fun forEachCase(block: (GoldenCase, CameraLayoutSolution) -> Unit) {
        val spec = CameraLayoutDefaults.spec()
        cases.forEach { case -> block(case, CameraLayoutSolver.solve(spec, case.window)) }
    }
}
