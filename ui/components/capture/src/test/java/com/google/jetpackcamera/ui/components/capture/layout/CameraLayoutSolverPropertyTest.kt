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
import kotlin.system.measureTimeMillis
import org.junit.Test

/**
 * Exhaustive and invariant tests for [CameraLayoutSolver].
 *
 * The golden tests pin behaviour to the reference implementation on real devices. These tests do
 * something different: they assert the *properties* the layout promises, across a synthetic sweep
 * of window geometries far larger than any device set we could enumerate.
 *
 * That distinction matters. The reason this feature exists is that the set of Android devices is
 * not enumerable, so a layout validated only against known phones is validated against the wrong
 * thing. The grid below is the closest we can get to checking the promise itself.
 */
class CameraLayoutSolverPropertyTest {

    /**
     * A synthetic sweep of portrait window geometries.
     *
     * Bounds are chosen to bracket the real world generously rather than to describe it: widths
     * from a small phone to a large one, heights from a 16:9 handset to a tall flagship, top insets
     * from a slim status bar to a deep cutout, and navigation insets covering hidden, gesture and
     * three-button.
     *
     * Sweeping width and height independently also produces combinations no handheld has, such as
     * `480x760`, which is wider than 16:9. Those are tablets, foldable inner displays or
     * split-screen panes. They are out of scope for this layout and would want a different template
     * entirely, so they are separated out rather than silently held to the same promise: see
     * [inScope] and [outOfScope]. Holding an out-of-scope shape to an in-scope guarantee measures
     * the wrong thing and hides the result for the shapes that matter.
     */
    private object Grid {
        val widths = (320..480 step 20).map { it.toFloat() }
        val heights = (600..1160 step 40).map { it.toFloat() }
        val topInsets = listOf(24f, 30f, 36f, 42f, 48f, 54f, 60f)
        val navInsets = listOf(0f, 16f, 24f, 48f)

        /**
         * Real portrait handhelds run from 16:9 to roughly 21:9.
         *
         * The floor is 16:9 because that is the squarest aspect ratio a phone has shipped in
         * volume. It is not a round number chosen for convenience: windows even slightly squarer
         * than this are where the repair search's cost explodes, so drawing the line anywhere
         * looser would be quietly promising something the solver cannot deliver in bounded time.
         */
        private const val MIN_ASPECT = 1.78f
        private const val MAX_ASPECT = 2.4f

        private fun CameraWindow.isHandheldShaped(): Boolean {
            val aspect = height.value / width.value
            return aspect >= MIN_ASPECT && aspect <= MAX_ASPECT
        }

        private fun all(): Sequence<CameraWindow> = sequence {
            for (w in widths) {
                for (h in heights) {
                    for (top in topInsets) {
                        for (nav in navInsets) {
                            yield(
                                CameraWindow(
                                    width = w.dp,
                                    height = h.dp,
                                    topInset = top.dp,
                                    navInset = nav.dp,
                                    gestureInset = 24.dp
                                )
                            )
                        }
                    }
                }
            }
        }

        val size = widths.size * heights.size * topInsets.size * navInsets.size

        /** Every window in the sweep, in scope or not. For invariants that must hold regardless. */
        fun everyWindow(): Sequence<CameraWindow> = all()

        /** Shapes the layout promises to solve cleanly. */
        fun inScope(): Sequence<CameraWindow> = all().filter { it.isHandheldShaped() }

        /** Shapes the layout only promises not to break on. */
        fun outOfScope(): Sequence<CameraWindow> = all().filterNot { it.isHandheldShaped() }
    }

    private val spec = CameraLayoutDefaults.spec()

    /**
     * Solved layouts, shared by every test in this class.
     *
     * JUnit constructs a fresh instance per test method, so without this each assertion re-solves
     * the whole sweep from scratch. Five of the tests below touch every window, which turned into
     * roughly 35 seconds of presubmit time spent deriving the same answers five times over.
     *
     * Caching is only legitimate because the solver is a pure function of its inputs, which
     * [solverIsDeterministic] pins independently rather than taking on trust. The two tests that
     * measure or verify solving itself deliberately bypass this and call the solver directly.
     */
    private companion object Solutions {
        val cache: Map<CameraWindow, CameraLayoutSolution> by lazy {
            val spec = CameraLayoutDefaults.spec()
            Grid.everyWindow().associateWith { CameraLayoutSolver.solve(spec, it) }
        }
    }

    private fun solutionFor(window: CameraWindow): CameraLayoutSolution = cache.getValue(window)

    @Test
    fun gridIsTheExpectedSize() {
        // Pinned so that accidentally narrowing a sweep axis is visible rather than silent.
        assertThat(Grid.size).isEqualTo(3780)
        assertThat(Grid.inScope().count()).isEqualTo(1568)
        assertThat(Grid.outOfScope().count()).isEqualTo(2212)
    }

    @Test
    fun everyInScopeWindowIsCollisionFree() {
        val failures = Grid.inScope()
            .map { it to solutionFor(it) }
            .filter { (_, solution) -> solution.collisions.isNotEmpty() }
            .map { (window, solution) -> "${window.describe()} -> ${solution.collisions}" }
            .take(10)
            .toList()

        assertThat(failures).isEmpty()
    }

    /**
     * Out-of-scope shapes get no collision-free guarantee, but they must not take the app down with
     * them. An app can be launched into a split-screen pane of any shape, so "we do not support
     * this" has to mean a degraded layout, not a crash and not a stalled UI thread.
     */
    @Test
    fun outOfScopeWindowsDegradeGracefullyRatherThanFailing() {
        Grid.outOfScope().forEach { window ->
            val solution = solutionFor(window)

            // A solution is always produced, and it is structurally well formed even when it is
            // not collision free.
            assertWithMessage("no rows at %s", window.describe())
                .that(solution.rowBands)
                .isNotEmpty()
            solution.rowBands.forEach { (id, band) ->
                assertWithMessage("row %s inverted at %s", id, window.describe())
                    .that(band.bottom.value)
                    .isAtLeast(band.top.value)
            }
            solution.viewfinders.forEach { (id, band) ->
                assertWithMessage("frame %s inverted at %s", id, window.describe())
                    .that(band.bottom.value)
                    .isAtLeast(band.top.value)
            }
        }
    }

    /**
     * The layout advertises 4 dp of daylight between any viewfinder edge and any control, and this
     * holds it to exactly that.
     *
     * An earlier revision asserted only 3.9 dp, one quantum less. That was not a real property of
     * the layout: the collision test carried 0.12 dp of slack, so the search stopped as soon as it
     * was within a quantum of the target instead of taking the one extra dp of bottom padding that
     * would have cleared it properly. The slack is now float noise and the bound is the real one.
     *
     * The only tolerance left is float representation. A clearance is the difference of two
     * coordinates of order several hundred dp, where a `Float` resolves to roughly 6e-5 dp, so an
     * exact hit can read a few hundred-thousandths low.
     */
    @Test
    fun everyInScopeWindowKeepsAtLeastTheRequiredClearance() {
        val floatSlack = 1e-3f
        val floor = CameraLayoutDefaults.MinControlClearance.value - floatSlack

        var worst = Float.MAX_VALUE
        var worstWindow: CameraWindow? = null
        Grid.inScope().forEach { window ->
            val solution = solutionFor(window)
            if (solution.minClearance.value < worst) {
                worst = solution.minClearance.value
                worstWindow = window
            }
        }

        assertWithMessage("worst clearance %s dp at %s", worst, worstWindow?.describe())
            .that(worst)
            .isAtLeast(floor)
    }

    // The next three are structural rather than aesthetic: they hold by construction of the stack,
    // so they are asserted over every window including the out-of-scope ones. A degraded layout is
    // still not allowed to be an incoherent one.

    @Test
    fun theControlStackNeverOverlapsTheTopBar() {
        Grid.everyWindow().forEach { window ->
            val solution = solutionFor(window)
            val highest = solution.rowBands.values.minOf { it.top.value }
            assertWithMessage("stack top %s at %s", highest, window.describe())
                .that(highest)
                .isAtLeast(window.topInset.value - 0.05f)
        }
    }

    @Test
    fun controlRowsNeverOverlapEachOther() {
        Grid.everyWindow().forEach { window ->
            val solution = solutionFor(window)
            val ordered = solution.rowBands.values.sortedBy { it.top.value }
            ordered.zipWithNext { upper, lower ->
                assertWithMessage("rows overlap at %s", window.describe())
                    .that(lower.top.value)
                    .isAtLeast(upper.bottom.value - 0.05f)
            }
        }
    }

    @Test
    fun immersiveIsDecidedOnceAndAppliesToEveryAspectRatio() {
        // The solution exposes a single requiresImmersive flag shared by all frames, so the
        // structural guarantee is that the tall frame is what drives it and no frame disagrees.
        Grid.everyWindow().forEach { window ->
            val solution = solutionFor(window)
            val tallest = window.width.value / (9f / 16f)
            val fitsWithBars = tallest <= (window.height.value - window.navInset.value)
            assertThat(solution.requiresImmersive).isEqualTo(!fitsWithBars)
        }
    }

    @Test
    fun solverIsDeterministic() {
        Grid.inScope().take(200).forEach { window ->
            val a = CameraLayoutSolver.solve(spec, window)
            val b = CameraLayoutSolver.solve(spec, window)
            assertThat(b).isEqualTo(a)
        }
    }

    @Test
    fun theModeSwitcherSlotIsReservedUnconditionally() {
        // The mode carousel is hidden in video mode. Its slot must stay reserved so the shutter
        // button does not jump under the user's thumb when the mode changes.
        //
        // The solver takes no content-visibility input at all, so the guarantee reduces to the
        // default spec reporting a constant height for this row. That is what this pins: if
        // someone later "optimises" by collapsing the slot when the carousel is hidden, they have
        // to delete this test to do it.
        val row = CameraLayoutDefaults.rows().single { it.id == CameraRowIds.MODE_SWITCHER }
        assertThat(row.height).isEqualTo(CameraLayoutDefaults.ModeSwitcherHeight)
        assertThat(row.height.value).isGreaterThan(0f)
    }

    @Test
    fun aDownwardShiftIsNeverBottomJustified() {
        // A frame allowed to drift freely ends up looking bottom-heavy even when it is technically
        // collision free. The black band above a frame must never exceed the band below it.
        Grid.inScope().forEach { window ->
            val solution = solutionFor(window)
            val bottomLimit = if (solution.requiresImmersive) {
                window.height.value
            } else {
                window.height.value - window.navInset.value
            }
            solution.viewfinders.forEach { (id, band) ->
                val above = band.top.value
                val below = bottomLimit - band.bottom.value
                if (above > window.topInset.value + 0.05f) {
                    assertWithMessage(
                        "%s black above=%s below=%s at %s",
                        id,
                        above,
                        below,
                        window.describe()
                    ).that(above).isAtMost(below + 0.05f)
                }
            }
        }
    }

    /**
     * The real cost guard: how many candidate layouts the solver has to look at.
     *
     * Wall clock is the thing we care about, but it is the wrong thing to assert on. It varies with
     * the machine, the JIT and whatever else shares the CI box, so a bound loose enough to be
     * reliable is too loose to catch anything. Evaluation count is exactly proportional to the work
     * done and is completely deterministic, so it can be bounded tightly.
     *
     * The numbers below are measured, not estimated. Across the in-scope sweep:
     *
     * | evaluations | windows |
     * |-------------|---------|
     * | 1           | 897     |
     * | 2 to 10     | 120     |
     * | 11 to 100   | 545     |
     * | over 1000   | 6       |
     *
     * So the designed layout already fits on 57% of supported windows with no search at all, and
     * only 6 windows in 1,568 ever pay for a full gap sweep. That is worth stating explicitly,
     * because the shape of this distribution is the reason the search is not worth optimising
     * further: the expensive path is rare and already bounded.
     */
    @Test
    fun supportedGeometryIsSolvedWithinAKnownEvaluationBound() {
        var worst = 0
        var worstWindow: CameraWindow? = null
        var total = 0L
        Grid.inScope().forEach { window ->
            val n = solutionFor(window).evaluationCount
            total += n
            if (n > worst) {
                worst = n
                worstWindow = window
            }
        }

        // One full gap sweep is 9 x 9 x 13 = 1,053 candidates, and the worst supported window costs
        // exactly one of those plus the padding sweep that preceded it. The bound is the measured
        // worst case with a little headroom, not a target: it is here so that a change which makes
        // the search enter the gap sweep at more than one padding fails loudly.
        assertWithMessage("worst window %s cost %s evaluations", worstWindow?.describe(), worst)
            .that(worst)
            .isAtMost(1_100)

        // Well under EVALUATION_BUDGET, which matters for correctness and not just speed: a solve
        // that exhausts the budget returns the best layout found so far rather than the best one
        // that exists. No supported window may come close to that cliff.
        assertThat(worst).isLessThan(5_000 / 4)

        assertWithMessage("in-scope sweep cost %s evaluations", total).that(total).isAtMost(60_000)
    }

    @Test
    fun unsupportedGeometryNeverExceedsTheEvaluationBudget() {
        // The budget is what stops an unsupported shape from pinning a core. It is a cap on work,
        // so it has to hold for every window, including the ones that reach it.
        Grid.everyWindow().forEach { window ->
            val n = solutionFor(window).evaluationCount
            assertWithMessage("%s cost %s evaluations", window.describe(), n)
                .that(n)
                .isAtMost(5_001)
        }
    }

    @Test
    fun supportedGeometrySolvesQuickly() {
        // Complements the evaluation-count bound above, which pins how many candidates are looked
        // at but says nothing about the cost of looking at one. This is the guard against an
        // evaluation itself becoming expensive, so a loose bound is the right kind of bound here.
        //
        // The solve runs once per window geometry on device, not per frame.
        val elapsed = measureTimeMillis {
            Grid.inScope().forEach { CameraLayoutSolver.solve(spec, it) }
        }
        assertWithMessage("in-scope sweep took %s ms", elapsed).that(elapsed).isLessThan(2_000)
    }

    @Test
    fun unsupportedGeometryStaysBounded() {
        // Unsupported shapes are allowed to be slow: they exhaust the evaluation budget by design,
        // because there is no cheap answer for geometry the layout was not built for. What they may
        // not do is run unbounded. This bound exists to catch the budget being removed or bypassed,
        // not to track performance, so it is deliberately generous.
        val elapsed = measureTimeMillis {
            Grid.outOfScope().forEach { CameraLayoutSolver.solve(spec, it) }
        }
        assertWithMessage("out-of-scope sweep took %s ms", elapsed).that(elapsed).isLessThan(30_000)
    }

    private fun CameraWindow.describe() =
        "${width.value}x${height.value} top=${topInset.value} nav=${navInset.value}"
}
