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

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Solves viewfinder and control placement for a camera window.
 *
 * ## Why this exists
 *
 * A camera UI has to place several viewfinder aspect ratios against a control stack that must not
 * move when the user switches between them. Positioning the viewfinder first and hoping the
 * controls fit underneath produces edge collisions on a large fraction of real devices. This solver
 * works the other way round: it locks the control stack once per window, then places each frame in
 * the highest position where every edge clears every control.
 *
 * Because all frames share one stationary stack, moving a control row to clear one frame can push
 * another row into a different frame. So this is a bounded constraint solve with escalating repair
 * passes rather than a fixed sequence of adjustments, and it verifies every frame simultaneously
 * before accepting a candidate.
 *
 * ## Runtime cost
 *
 * The solve runs once per window geometry, not per frame. The escalating passes exit as soon as a
 * collision-free layout is found, which on the overwhelming majority of devices happens in the
 * first pass. Callers should memoize on [CameraWindow].
 *
 * ## Purity
 *
 * This file must not gain a Compose or Android dependency. [Dp] is a pure Kotlin value class and is
 * the only import from the Compose artifacts. Keeping this pure is what lets the exhaustive device
 * grid run as an ordinary JVM unit test.
 */
object CameraLayoutSolver {

    /**
     * Slack subtracted from the clearance when *testing* for a collision.
     *
     * This exists only to absorb float representation error. Clearances are differences of
     * coordinates of order several hundred dp, where a `Float` resolves to roughly 6e-5 dp, so an
     * exact hit can read a hair below the target and must not be rejected as a collision.
     *
     * It is deliberately far smaller than [QUANTUM]. The reference used 0.12 dp, a full quantum
     * plus change, which meant the search accepted any layout within 3.88 dp and stopped looking.
     * On devices where the tall frame's edge is immovable, that is why the tightest clearance came
     * out at 3.9 dp rather than the 4.0 dp the layout advertises: the stack could have dropped one
     * more dp to reach 4.9, but 3.92 already passed the test so nothing made it look. Tightening
     * this to noise costs 1 dp of bottom padding on two device families and regresses nothing.
     */
    private const val COLLISION_TEST_SLACK = 1e-4f

    /** Candidate positions are rounded to this many dp. */
    private const val QUANTUM = 0.1f

    /** Cost of intruding on a control row, dominating any positional preference. */
    private const val INTRUSION_COST = 1e6f

    /** Weight of the "stay near the preferred position" term. */
    private const val PREFERENCE_WEIGHT = 1e-3f

    /** Step between candidate gap values in the repair sweep. */
    private const val GAP_SWEEP_STEP = 2

    /** Extra lift allowed when the tall frame already clears everything. */
    private const val INCIDENTAL_LIFT_BUDGET = 16

    /**
     * Hard ceiling on candidate evaluations per solve.
     *
     * The repair search is a cartesian sweep, so its cost depends on the window it is handed, and
     * we do not control that: an app can be launched into a split-screen pane or a foldable posture
     * of any shape. Without a ceiling the worst window measured cost 677,000 evaluations, which is
     * roughly a second of a stalled UI thread.
     *
     * Measured on a desktop JVM, an evaluation costs about 1.5 us:
     *
     * | case                                      | evaluations | wall clock |
     * |-------------------------------------------|-------------|------------|
     * | typical phone, solves on the first attempt | 1           | 26 us      |
     * | worst supported window in the sweep        | 1,055       | 1.6 ms     |
     * | this budget                                | 5,000       | ~6 ms      |
     *
     * 5,000 leaves nearly five times the headroom over the worst supported window while capping a
     * pathological one below a single frame at 60 Hz. An earlier revision used 20,000, which
     * measured at 25 ms: still bounded, but a frame and a half of jank for geometry we do not
     * support anyway.
     *
     * Tripping the budget is not an error, and no supported geometry comes close to it. The search
     * keeps the best layout it has found, and [CameraLayoutSolution.collisions] reports whether
     * that layout is actually clean.
     */
    private const val EVALUATION_BUDGET = 5_000

    /** An obstacle the solver must keep viewfinder edges away from. */
    private data class Obstacle(
        val id: String,
        val top: Float,
        val bottom: Float,
        val weight: Float
    )

    /** A fully evaluated candidate: one bottom padding plus one set of inter-row gaps. */
    private class Candidate(
        val bottomPadding: Int,
        val gaps: IntArray,
        val obstacles: List<Obstacle>,
        val bands: Map<String, DpRange>,
        val frames: Map<String, Pair<Float, Float>>,
        val collisions: List<Collision>,
        val hardCollisions: List<Collision>,
        val minClearance: Float
    )

    /**
     * Solves [spec] against [window].
     *
     * Always returns a solution. If no collision-free layout exists for this geometry the best
     * available is returned with [CameraLayoutSolution.collisions] populated, rather than throwing,
     * because a slightly wrong camera UI is better than no camera UI.
     */
    fun solve(spec: CameraLayoutSpec, window: CameraWindow): CameraLayoutSolution {
        val w = window.width.value
        val h = window.height.value
        val topInset = window.topInset.value
        val clearance = spec.minControlClearance.value

        // --- Once-per-window decisions -------------------------------------------------------
        // These are deliberately settled before any per-frame work, and are applied identically to
        // every aspect ratio. If immersive mode were decided per ratio the window would resize
        // mid-session and every control would jump under the user's thumb.

        val tallest = spec.viewfinders.maxOf { w / it.aspectRatio }

        // A full-width tall frame is as tall as the window itself on a 16:9 display, so it cannot
        // fit above a visible navigation bar even when flush with the top of the screen.
        val requiresImmersive = tallest > (h - window.navInset.value)
        val navInset = if (requiresImmersive) 0f else window.navInset.value
        val navTop = if (requiresImmersive) h else h - navInset

        val blackBarBelowTallest = navTop - tallest
        val useCompaction = spec.toolbarCompaction == ToolbarCompaction.COMPACT_TOOLBAR

        val bottomRow = spec.rows.first()
        val nominalToolbarHeight = bottomRow.height.value
        // The smallest drawn height whose interactive touch target still stays inside the
        // clearance margin above the toolbar.
        val minToolbarHeight = max(0f, spec.minInteractiveTouchTarget.value - clearance)

        val effectiveBottomZone = max(navInset, window.gestureInset.value)
        val gestureDemand = effectiveBottomZone - navInset
        val normalMinPadding =
            max(spec.bottomPadding.min.value, gestureDemand.roundToJsFloat()).roundToJs()
        // When a visible navigation bar already extends above the bottom gesture zone, only the
        // control clearance margin is required between the toolbar and the navigation bar.
        val compactBandMinPadding =
            if (navInset > window.gestureInset.value) clearance.roundToJs() else normalMinPadding

        // Derived bounds for the toolbar-compaction band:
        // - compactBandMin: smallest black bar that can fit minToolbarHeight + clearance + padding.
        // - compactBandMax: black bar where the full nominalToolbarHeight already fits with normal
        //   bottom padding, so no compaction is needed.
        val compactBandMin = minToolbarHeight + clearance + compactBandMinPadding
        val compactBandMax = nominalToolbarHeight + clearance + normalMinPadding
        val isCompactBand = useCompaction && !requiresImmersive &&
            blackBarBelowTallest >= compactBandMin && blackBarBelowTallest < compactBandMax

        val minBottomPadding: Int = if (isCompactBand) compactBandMinPadding else normalMinPadding

        val toolbarHeight: Float = if (isCompactBand) {
            max(
                minToolbarHeight,
                min(
                    nominalToolbarHeight,
                    floor(blackBarBelowTallest - clearance - minBottomPadding)
                )
            )
        } else {
            nominalToolbarHeight
        }

        val preferredPadding = spec.bottomPadding.preferred.value.roundToJs()
        val preferredGaps = IntArray(spec.rows.size) {
            spec.rows[it].gapAbove.preferred.value.roundToJs()
        }

        // --- Pass 0: try the preferred spacing ------------------------------------------------

        val context = SolveContext(
            spec = spec,
            window = window,
            clearance = clearance,
            topInset = topInset,
            navTop = navTop,
            requiresImmersive = requiresImmersive,
            toolbarHeight = toolbarHeight,
            minBottomPadding = minBottomPadding,
            windowHeight = h,
            windowWidth = w
        )

        var best = context.evaluate(preferredPadding, preferredGaps)
        var lifted = false

        // --- Escalating repair, cheapest and least invasive first -----------------------------

        if (best == null || best.collisions.isNotEmpty()) {
            val maxCompactPad = max(
                minBottomPadding.toFloat(),
                floor(blackBarBelowTallest - toolbarHeight - clearance)
            ).toInt()

            val tallestFrameId = spec.viewfinders.maxByOrNull { w / it.aspectRatio }?.id
            val tallestAlreadyClear = best != null &&
                best.collisions.none { it.viewfinderId == tallestFrameId }

            val liftBudget = when {
                isCompactBand -> 0
                tallestAlreadyClear -> INCIDENTAL_LIFT_BUDGET
                else -> max(0, spec.maxStackLift.value.roundToJs())
            }
            val padCeiling = if (isCompactBand) {
                maxCompactPad
            } else {
                max(minBottomPadding, preferredPadding) + liftBudget
            }
            val padHardMax = if (isCompactBand) {
                maxCompactPad
            } else {
                max(minBottomPadding, (navTop - topInset).roundToJs())
            }

            val search = RepairSearch(context, preferredPadding, preferredGaps)

            search.sweepPadding(minBottomPadding, padCeiling, allowTightDaylight = false)
            if (search.best == null) {
                search.sweepPaddingAndGaps(minBottomPadding, padCeiling, allowTightDaylight = false)
            }
            if (search.best == null) {
                search.sweepPaddingAndGaps(minBottomPadding, padCeiling, allowTightDaylight = true)
            }
            if (search.best == null) {
                search.sweepPaddingAndGaps(minBottomPadding, padHardMax, allowTightDaylight = false)
                if (search.best == null) {
                    search.sweepPaddingAndGaps(
                        minBottomPadding,
                        padHardMax,
                        allowTightDaylight = true
                    )
                }
            }

            val repaired = search.best
            if (repaired != null) {
                lifted = repaired.bottomPadding > preferredPadding
                best = repaired
            }
        }

        // Genuinely infeasible geometry. Surface the best available rather than failing; callers
        // can inspect `collisions` and decide whether to degrade further.
        val solution = best ?: context.evaluate(minBottomPadding, preferredGaps)
            ?: error(
                "No layout could be constructed for ${window.width} x ${window.height}. " +
                    "The control stack is taller than the window."
            )

        return CameraLayoutSolution(
            rowBands = solution.bands,
            viewfinders = solution.frames.mapValues { (_, v) -> DpRange(v.first.dp, v.second.dp) },
            requiresImmersive = requiresImmersive,
            toolbarHeight = toolbarHeight.dp,
            resolvedBottomPadding = solution.bottomPadding.dp,
            resolvedGaps = solution.gaps.map { it.dp },
            minClearance = (if (solution.minClearance.isFinite()) solution.minClearance else 0f).dp,
            collisions = solution.collisions,
            liftedStack = lifted
        )
    }

    // -----------------------------------------------------------------------------------------
    // Candidate evaluation
    // -----------------------------------------------------------------------------------------

    private class SolveContext(
        val spec: CameraLayoutSpec,
        val window: CameraWindow,
        val clearance: Float,
        val topInset: Float,
        val navTop: Float,
        val requiresImmersive: Boolean,
        val toolbarHeight: Float,
        val minBottomPadding: Int,
        val windowHeight: Float,
        val windowWidth: Float
    ) {
        /**
         * Builds the control stack for one candidate spacing and places every frame against it.
         *
         * Returns null when the stack would overflow the top bar, which means this candidate is
         * unusable rather than merely imperfect.
         *
         * This is the single source of truth for where everything lands. The repair search calls
         * exactly this, so a candidate can never be scored against a position that differs from the
         * one actually rendered.
         */
        fun evaluate(bottomPadding: Int, gaps: IntArray): Candidate? {
            val bands = LinkedHashMap<String, DpRange>(spec.rows.size)
            val obstacles = ArrayList<Obstacle>(spec.rows.size)

            var cursor = navTop - bottomPadding
            spec.rows.forEachIndexed { index, row ->
                // The bottom-most row is the one compaction applies to.
                val height = if (index == 0) toolbarHeight else row.height.value
                val bottom = cursor
                val top = bottom - height
                bands[row.id] = DpRange(top.dp, bottom.dp)
                // A zero-height row still reserves its slot, but cannot be collided with.
                if (height > 0f) {
                    obstacles += Obstacle(row.id, top, bottom, row.collisionWeight)
                }
                cursor = top - gaps[index]
            }

            val stackTop = obstacles.minOfOrNull { it.top } ?: cursor
            if (stackTop < topInset) return null

            val frames = LinkedHashMap<String, Pair<Float, Float>>(spec.viewfinders.size)
            val collisions = ArrayList<Collision>()
            val hardCollisions = ArrayList<Collision>()
            var minClearance = Float.POSITIVE_INFINITY

            for (slot in spec.viewfinders) {
                val rawHeight = windowWidth / slot.aspectRatio
                val placed = place(slot, rawHeight, obstacles, frames)
                frames[slot.id] = placed
                collisions += intrusions(
                    slot.id,
                    placed,
                    obstacles,
                    clearance - COLLISION_TEST_SLACK
                )
                hardCollisions += intrusions(slot.id, placed, obstacles, 0f)
                minClearance = min(minClearance, edgeClearance(placed, obstacles))
            }

            return Candidate(
                bottomPadding = bottomPadding,
                gaps = gaps.copyOf(),
                obstacles = obstacles,
                bands = bands,
                frames = frames,
                collisions = collisions,
                hardCollisions = hardCollisions,
                minClearance = minClearance
            )
        }

        /** Places one frame according to its policy, then resolves any collision. */
        private fun place(
            slot: ViewfinderSlot,
            rawHeight: Float,
            obstacles: List<Obstacle>,
            placed: Map<String, Pair<Float, Float>>
        ): Pair<Float, Float> = when (slot.policy) {
            PlacementPolicy.TOP_ALIGNED_PREFER_OBSTACLE_EDGE -> {
                val rawBottom = topInset + rawHeight
                val alreadyClear = obstacles.all {
                    rawBottom <= it.top - clearance || rawBottom >= it.bottom + clearance
                }
                // When the frame does collide, prefer resting on the lower edge of the topmost
                // control rather than drifting to some arbitrary clean position. Visually this
                // groups the frame border with the control it is dodging.
                val topmost = obstacles.minByOrNull { it.top }
                val preferred =
                    if (!alreadyClear && topmost != null && rawBottom < topmost.bottom) {
                        topmost.bottom
                    } else {
                        rawBottom
                    }
                minimax(
                    rawHeight,
                    topInset,
                    cappedBottom(rawHeight, topInset, navTop),
                    preferred,
                    obstacles
                )
            }

            PlacementPolicy.CENTERED_IN_REFERENCE -> {
                val reference = placed[slot.referenceId]
                val referenceSlot = spec.viewfinders.firstOrNull { it.id == slot.referenceId }
                val preferred = if (reference == null || referenceSlot == null) {
                    topInset + rawHeight
                } else {
                    // Centre within the reference's *frame area*, which is its raw geometry. Using
                    // the reference's solved band instead would compound its quantization into this
                    // frame's position.
                    val referenceRawHeight = windowWidth / referenceSlot.aspectRatio
                    quantize(reference.second - (referenceRawHeight - rawHeight) / 2f)
                }
                minimax(
                    rawHeight,
                    topInset,
                    cappedBottom(rawHeight, topInset, navTop),
                    preferred,
                    obstacles
                )
            }

            PlacementPolicy.BINARY_TOP_EDGE -> {
                val maxBottom = if (requiresImmersive) windowHeight else navTop
                // Would the frame still clear the bottom-most row if it started below the top bar?
                // The 0.05 guard absorbs float noise at the exact boundary.
                val fitsBelowTopBar = (topInset + rawHeight) <=
                    (maxBottom - minBottomPadding - toolbarHeight - clearance + 0.05f)
                if (!fitsBelowTopBar) {
                    // Expand to the very top of the window. Partial overlap with the top bar is
                    // never used: it reads as a mistake rather than a decision.
                    0f to quantize(rawHeight)
                } else {
                    minimax(
                        rawHeight,
                        topInset,
                        cappedBottom(rawHeight, topInset, maxBottom),
                        topInset + rawHeight,
                        obstacles
                    )
                }
            }
        }

        /**
         * Caps how far a frame may shift downward so the black band above it never exceeds the band
         * below it.
         *
         * The eye compares dead space above the frame against dead space below it, and the top bar
         * band reads as black whether or not it holds icons. A frame allowed to drift freely ends
         * up looking bottom-justified even when it is technically collision-free.
         *
         * The top-justified position always stays reachable, even on a device already past the
         * balance point.
         */
        private fun cappedBottom(rawHeight: Float, minTop: Float, maxBottom: Float): Float {
            if (!spec.enforceTopWeighting) return maxBottom
            val balancedTop = max(minTop, (maxBottom - rawHeight) / 2f)
            return max(minTop + rawHeight, min(maxBottom, balancedTop + rawHeight))
        }

        /**
         * Finds the frame position minimizing weighted intrusion depth, tie-broken by distance from
         * [preferredBottom].
         *
         * Only a small candidate set is evaluated rather than sweeping continuously: an optimal
         * position always rests against an obstacle boundary, sits in a gap midpoint, or is one of
         * the interval endpoints. That keeps this exact and cheap at the same time.
         */
        private fun minimax(
            rawHeight: Float,
            minTop: Float,
            maxBottom: Float,
            preferredBottom: Float,
            obstacles: List<Obstacle>
        ): Pair<Float, Float> {
            val lowerBound = min(maxBottom, minTop + rawHeight)
            val upperBound = maxBottom

            val candidates = LinkedHashSet<Float>()
            candidates += preferredBottom.coerceIn(lowerBound, upperBound)
            candidates += lowerBound
            candidates += upperBound

            val sorted = obstacles.sortedBy { it.top }
            sorted.forEachIndexed { index, obstacle ->
                // Bottom edge resting just outside the keep-out band, then top edge doing the same
                // (in which case the bottom edge is rawHeight lower).
                listOf(
                    obstacle.top - clearance,
                    obstacle.bottom + clearance,
                    obstacle.top - clearance + rawHeight,
                    obstacle.bottom + clearance + rawHeight
                ).forEach { c ->
                    if (c >= lowerBound && c <= upperBound) candidates += quantize(c)
                }
                if (index + 1 < sorted.size) {
                    val midpoint = quantize((obstacle.bottom + sorted[index + 1].top) / 2f)
                    if (midpoint >= lowerBound && midpoint <= upperBound) candidates += midpoint
                }
            }

            var bestBottom = lowerBound
            var bestCost = Float.POSITIVE_INFINITY
            for (candidate in candidates) {
                val cost = penalty(candidate, rawHeight, preferredBottom, obstacles)
                if (cost < bestCost) {
                    bestCost = cost
                    bestBottom = candidate
                }
            }

            return quantize(bestBottom - rawHeight) to quantize(bestBottom)
        }

        private fun penalty(
            bottom: Float,
            rawHeight: Float,
            preferredBottom: Float,
            obstacles: List<Obstacle>
        ): Float {
            val top = bottom - rawHeight
            var total = 0f
            for (obstacle in obstacles) {
                val keepOutTop = obstacle.top - clearance
                val keepOutBottom = obstacle.bottom + clearance
                if (keepOutBottom <= keepOutTop) continue
                if (bottom > keepOutTop && bottom < keepOutBottom) {
                    val depth = min(bottom - keepOutTop, keepOutBottom - bottom)
                    total += INTRUSION_COST + obstacle.weight * depth * depth
                }
                if (top > keepOutTop && top < keepOutBottom) {
                    val depth = min(top - keepOutTop, keepOutBottom - top)
                    total += INTRUSION_COST + obstacle.weight * depth * depth
                }
            }
            val distance = bottom - preferredBottom
            return total + PREFERENCE_WEIGHT * distance * distance
        }

        private fun intrusions(
            viewfinderId: String,
            frame: Pair<Float, Float>,
            obstacles: List<Obstacle>,
            margin: Float
        ): List<Collision> = obstacles.filter { obstacle ->
            val lo = obstacle.top - margin
            val hi = obstacle.bottom + margin
            (frame.second > lo && frame.second < hi) || (frame.first > lo && frame.first < hi)
        }.map { Collision(viewfinderId, it.id) }

        private fun edgeClearance(frame: Pair<Float, Float>, obstacles: List<Obstacle>): Float {
            var smallest = Float.POSITIVE_INFINITY
            for (obstacle in obstacles) {
                for (edge in listOf(frame.first, frame.second)) {
                    if (edge < 0f) continue
                    if (edge >= obstacle.top && edge <= obstacle.bottom) return 0f
                    val distance =
                        if (edge < obstacle.top) obstacle.top - edge else edge - obstacle.bottom
                    if (distance < smallest) smallest = distance
                }
            }
            return smallest
        }
    }

    // -----------------------------------------------------------------------------------------
    // Repair search
    // -----------------------------------------------------------------------------------------

    /**
     * Searches padding and gap combinations for a collision-free layout.
     *
     * Candidates are ranked lexicographically rather than by a weighted sum, so the priorities are
     * strict: keep frames flush with the top bar first, then avoid moving the stack, then stay near
     * the designed spacing. A weighted sum would let a large regression in an important term be
     * bought off by small improvements in unimportant ones.
     */
    private class RepairSearch(
        private val context: SolveContext,
        private val preferredPadding: Int,
        private val preferredGaps: IntArray
    ) {
        var best: Candidate? = null
            private set
        private var bestScore: List<Float>? = null

        /** Evaluations spent so far, across every rung. */
        private var evaluations = 0

        /** True when the budget ran out before the search finished. */
        var exhaustedBudget: Boolean = false
            private set

        /** Set by [consider] when a candidate could be placed at all, as opposed to overflowing. */
        private var placedAny = false

        private val rows = context.spec.rows

        /** Index of the row the grouping rule applies to, or -1. */
        private val groupedRowIndex: Int =
            context.spec.groupingRule
                ?.let { rule -> rows.indexOfFirst { it.id == rule.rowId } } ?: -1

        fun sweepPadding(from: Int, to: Int, allowTightDaylight: Boolean) {
            for (padding in from..to) {
                placedAny = false
                consider(padding, preferredGaps, allowTightDaylight)
                if (!placedAny || evaluations >= EVALUATION_BUDGET) break
            }
        }

        fun sweepPaddingAndGaps(from: Int, to: Int, allowTightDaylight: Boolean) {
            val grids = preferredGaps.mapIndexed { index, preferred -> gapGrid(index, preferred) }
            val working = IntArray(preferredGaps.size)
            for (padding in from..to) {
                placedAny = false
                permute(grids, working, 0) { consider(padding, working, allowTightDaylight) }
                if (!placedAny || evaluations >= EVALUATION_BUDGET) break
            }
        }

        private fun permute(
            grids: List<List<Int>>,
            working: IntArray,
            index: Int,
            emit: () -> Unit
        ) {
            if (index == grids.size) {
                emit()
                return
            }
            for (value in grids[index]) {
                working[index] = value
                permute(grids, working, index + 1, emit)
            }
        }

        /**
         * Descending candidate gaps, preferring the designed value first.
         *
         * The top-most row has nothing above it, so varying its gap changes no position. Pinning it
         * to a single value keeps the search space honest rather than multiplying it by a dimension
         * that cannot affect the outcome.
         */
        private fun gapGrid(index: Int, preferred: Int): List<Int> {
            if (index == preferredGaps.lastIndex) return listOf(preferred)
            val minGap = rows[index].gapAbove.min.value.roundToJs()
            val values = linkedSetOf(preferred)
            var v = minGap
            val ceiling = max(preferred, minGap)
            while (v <= ceiling) {
                values += v
                v += GAP_SWEEP_STEP
            }
            return values.sortedDescending()
        }

        private fun consider(padding: Int, gaps: IntArray, allowTightDaylight: Boolean) {
            if (evaluations >= EVALUATION_BUDGET) {
                exhaustedBudget = true
                return
            }
            evaluations++
            // A null here means the stack overflowed the top bar. That is the signal the padding
            // sweep prunes on, so it has to be recorded separately from "placed but unacceptable".
            val candidate = context.evaluate(padding, gaps) ?: return
            placedAny = true
            // Without tight daylight we demand the full keep-out margin. With it we accept any
            // layout with no actual intersection, and then rank by how much daylight survives.
            if (!allowTightDaylight && candidate.collisions.isNotEmpty()) return
            if (allowTightDaylight && candidate.hardCollisions.isNotEmpty()) return

            val score = score(candidate, padding, gaps)
            val ranked = if (allowTightDaylight) {
                listOf(-(candidate.minClearance * 10f).roundToJsFloat()) + score
            } else {
                score
            }

            val current = bestScore
            if (current == null || isBetter(ranked, current)) {
                bestScore = ranked
                best = candidate
            }
        }

        /**
         * Ranks a candidate. Earlier terms dominate absolutely.
         *
         * 1. How far the primary frame dropped from the top bar.
         * 2. How far the remaining frames dropped.
         * 3. How far the stack moved from its designed bottom padding.
         * 4. How far the gaps deviated from their targets.
         */
        private fun score(candidate: Candidate, padding: Int, gaps: IntArray): List<Float> {
            val topInset = context.topInset
            val frames = context.spec.viewfinders.map { candidate.frames.getValue(it.id) }
            val primaryDrop = max(0f, frames.first().first - topInset)
            val secondaryDrop = frames.drop(1)
                .fold(0f) { acc, f -> acc + max(0f, f.first - topInset) }

            var gapDeviation = 0f
            for (i in gaps.indices) {
                if (i == preferredGaps.lastIndex) continue
                val target = targetGap(i, candidate, gaps)
                // The grouped gap is the one the eye notices most, so deviating from it costs more.
                val weight = if (i == groupedRowIndex) 3f else 1f
                gapDeviation += weight * abs(target - gaps[i]).toFloat()
            }

            return listOf(
                primaryDrop,
                secondaryDrop,
                abs(padding - preferredPadding).toFloat(),
                gapDeviation
            )
        }

        /**
         * The gap this row should be aiming for, which is the designed value unless the grouping
         * rule applies.
         */
        private fun targetGap(index: Int, candidate: Candidate, gaps: IntArray): Int {
            val rule = context.spec.groupingRule
            if (rule == null || index != groupedRowIndex || index == 0) return preferredGaps[index]

            val band = candidate.bands[rows[index].id] ?: return preferredGaps[index]
            val above = candidate.bands[rows.getOrNull(index + 1)?.id]
                ?: return preferredGaps[index]
            val frames = context.spec.viewfinders.map { candidate.frames.getValue(it.id) }

            // Is this row inside the primary frame rather than below it?
            val rowInsidePrimaryFrame = frames.first().second > band.bottom.value
            // Does any other frame's lower edge run through the gap above this row?
            val borderSplitsTheGap = frames.drop(1).any { frame ->
                frame.second > above.bottom.value && frame.second < band.top.value
            }

            return if (rowInsidePrimaryFrame && !borderSplitsTheGap) {
                min(rule.tightenedGapAbove.value.roundToJs(), gaps[index - 1])
            } else {
                preferredGaps[index]
            }
        }

        private fun isBetter(a: List<Float>, b: List<Float>): Boolean {
            for (i in a.indices) {
                if (i >= b.size) return false
                if (a[i] != b[i]) return a[i] < b[i]
            }
            return false
        }
    }

    // -----------------------------------------------------------------------------------------
    // Numeric helpers
    // -----------------------------------------------------------------------------------------

    /** Rounds to [QUANTUM] dp, matching the reference implementation's candidate quantization. */
    private fun quantize(value: Float): Float = floor(value / QUANTUM + 0.5f) * QUANTUM

    /**
     * Half-up rounding.
     *
     * [kotlin.math.round] rounds half away from zero, which disagrees with the reference
     * implementation for negative halves. Positions near the top of the window can be negative, so
     * the difference is reachable rather than theoretical.
     */
    private fun Float.roundToJsFloat(): Float = floor(this + 0.5f)

    private fun Float.roundToJs(): Int = roundToJsFloat().toInt()
}
