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
 * ## Where the stack sits
 *
 * The stack is positioned from the bottom of the window. Its preferred position holds the bottom
 * row at a fixed height above the bottom of the window, so the height of the navigation bar only
 * changes the bottom padding: a taller bar, such as 3-button navigation, takes its height out of
 * the padding, and a shorter bar, or none, adds the difference to it. The preferred position is
 * therefore the same in every navigation configuration. An immersive window, which hides its bar
 * to fit a tall frame, keeps the designed padding instead. The stack leaves the preferred position
 * only to resolve a collision, or by a small incidental lift when that lift puts the primary frame
 * flush with the top bar. A frame that would need a larger lift is shifted down instead. When a
 * collision does force the stack up, it rises to the lowest collision-free position the search
 * finds and at most the same incidental lift beyond it.
 *
 * A tall frame that cannot clear the stack from above may enclose it instead: the frame starts at
 * the top bar, or a few dp below it behind the top-start control, and its bottom edge runs below
 * the bottom row. Only when no enclosing position is clean does the frame expand to the top of the
 * window.
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

    /**
     * Largest lift that still counts as incidental.
     *
     * Measured from the preferred bottom padding, it bounds the lift accepted to put the primary
     * frame back flush with the top bar, the highest padding at which the tall frame may enclose
     * the stack, and the lift the repair search may spend when the tall frame already clears every
     * control. When a collision forces the stack up, it also bounds how far the stack may rise
     * beyond the lowest collision-free position. A layout that needs more lift shifts the frame
     * down instead.
     *
     * On the reference windows, putting the primary frame flush takes a lift of 4 dp on a 412 x 925
     * window and 6 dp on a 411 x 914 window, where the designs keep it flush, and 18 dp on a
     * 360 x 740 window, where the design shifts the frame down instead. A 384 x 824 window needs
     * 15 dp above its lowest collision-free position to place the primary frame directly below the
     * top-start control. Every value from 15 to 17 reproduces all four.
     */
    private const val INCIDENTAL_LIFT_BUDGET = 16

    /**
     * Hard ceiling on candidate evaluations per solve.
     *
     * The repair search is a cartesian sweep, so its cost depends on the window it is handed, and
     * we do not control that: an app can be launched into a split-screen pane or a foldable posture
     * of any shape. Without a ceiling the worst window measured cost 677,000 evaluations, which is
     * roughly a second of a stalled UI thread.
     *
     * Measured on a desktop JVM, an evaluation costs about 1.3 to 2 us:
     *
     * | case                                    | evaluations | wall clock |
     * |-----------------------------------------|-------------|------------|
     * | window that solves on the first attempt | 1           | 4 us       |
     * | 412 x 925 phone, one padding sweep      | 24          | 48 us      |
     * | worst supported window in the sweep     | 587         | 1.2 ms     |
     * | this budget                             | 5,000       | ~6 ms      |
     *
     * 5,000 leaves more than eight times the headroom over the worst supported window while capping
     * a pathological one below a single frame at 60 Hz. An earlier revision used 20,000, which
     * measured at 25 ms: still bounded, but a frame and a half of jank for geometry we do not
     * support anyway.
     *
     * Tripping the budget is not an error, and no supported geometry comes close to it. The search
     * keeps the best layout it has found, and [CameraLayoutSolution.collisions] reports whether
     * that layout is actually clean.
     *
     * The figures above are not left to drift: every solve reports its own cost through
     * [CameraLayoutSolution.evaluationCount], and the property tests assert the worst supported
     * window against the number in this table.
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

        // The padding floor keeps the toolbar out of the bottom gesture zone. A visible navigation
        // bar that covers that zone already keeps it out, so only the control clearance margin is
        // required between the toolbar and the navigation bar. The test is inclusive because
        // 3-button navigation reports a gesture zone exactly as tall as the bar itself.
        val minBottomPadding = if (navInset > 0f && navInset >= window.gestureInset.value) {
            clearance.roundToJs()
        } else {
            val gestureDemand = window.gestureInset.value - navInset
            max(spec.bottomPadding.min.value, gestureDemand.roundToJsFloat()).roundToJs()
        }

        // Derived bounds for the toolbar-compaction band:
        // - compactBandMin: smallest black bar that can fit minToolbarHeight + clearance + padding.
        // - compactBandMax: black bar where the full nominalToolbarHeight already fits with the
        //   minimum bottom padding, so no compaction is needed.
        val compactBandMin = minToolbarHeight + clearance + minBottomPadding
        val compactBandMax = nominalToolbarHeight + clearance + minBottomPadding
        val inCompactBand = useCompaction && !requiresImmersive &&
            blackBarBelowTallest >= compactBandMin && blackBarBelowTallest < compactBandMax

        val compactToolbarHeight = max(
            minToolbarHeight,
            min(nominalToolbarHeight, floor(blackBarBelowTallest - clearance - minBottomPadding))
        )

        // The preferred padding keeps the bottom row at the same height above the bottom of the
        // window in every navigation configuration. The bottom inset varies on a single device:
        // 48 dp for 3-button navigation, 24 dp or 16 dp for gesture navigation depending on the
        // platform release, and zero when no bar is reported. A taller bar takes its height out of
        // the padding and a shorter one adds the difference to it. An immersive window has hidden
        // its bar, so it keeps the designed padding, capped at the anchor.
        val designedPadding = spec.bottomPadding.preferred.value.roundToJs()
        val preferredPadding = spec.bottomRowAnchor?.let { anchor ->
            val anchoredPadding = (anchor.value - navInset).roundToJs()
            if (requiresImmersive) {
                max(minBottomPadding, min(designedPadding, anchoredPadding))
            } else {
                max(minBottomPadding, anchoredPadding)
            }
        } ?: designedPadding

        val preferredGaps = IntArray(spec.rows.size) {
            spec.rows[it].gapAbove.preferred.value.roundToJs()
        }

        // The highest padding the stack may reach while its lift still counts as incidental.
        val incidentalLiftCeiling = max(minBottomPadding, preferredPadding) + INCIDENTAL_LIFT_BUDGET

        fun contextFor(toolbar: Float) = SolveContext(
            spec = spec,
            window = window,
            clearance = clearance,
            topInset = topInset,
            navTop = navTop,
            requiresImmersive = requiresImmersive,
            toolbarHeight = toolbar,
            minBottomPadding = minBottomPadding,
            enclosureMaxPadding = incidentalLiftCeiling,
            windowHeight = h,
            windowWidth = w
        )

        // --- Pass 0: try the preferred spacing ------------------------------------------------

        var evaluations = 1
        var best = contextFor(nominalToolbarHeight).evaluate(preferredPadding, preferredGaps)

        // Compaction only exists to fit the toolbar below a tall frame that is flush with the
        // window top. If the full-height toolbar already lays out cleanly, the tall frame found
        // another position and there is nothing to compact. The decision is still made once per
        // window and applies to every aspect ratio.
        val isCompactBand = inCompactBand && (best == null || best.collisions.isNotEmpty())
        val toolbarHeight = if (isCompactBand) compactToolbarHeight else nominalToolbarHeight
        val context = contextFor(toolbarHeight)
        if (isCompactBand) {
            evaluations++
            best = context.evaluate(preferredPadding, preferredGaps)
        }
        var lifted = false

        // --- Incidental lift to keep the primary frame flush -----------------------------------
        // The preferred spacing is clean, but the primary frame had to shift down to clear the
        // controls. A small lift of the stack is worth it only if it puts the frame back flush
        // with the top bar. Beyond the incidental budget the stack keeps its preferred position
        // and the frame keeps its shifted position.

        val firstPass = best
        val primaryId = spec.viewfinders.first().id
        val primaryFlush = { candidate: Candidate ->
            candidate.frames.getValue(primaryId).first <= topInset + 0.05f
        }
        if (firstPass != null && firstPass.collisions.isEmpty() && !primaryFlush(firstPass)) {
            val search = RepairSearch(context, preferredPadding, preferredGaps)
            search.sweepPadding(minBottomPadding, incidentalLiftCeiling, allowTightDaylight = false)
            evaluations += search.evaluations
            val flushed = search.best
            if (flushed != null && primaryFlush(flushed)) {
                lifted = flushed.bottomPadding > preferredPadding
                best = flushed
            }
        }

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
            val padHardMax = if (
                isCompactBand &&
                spec.topStartPlacement == TopStartPlacement.VIEWFINDER_TOP_START
            ) {
                maxCompactPad
            } else {
                max(minBottomPadding, (navTop - topInset).roundToJs())
            }

            val search = RepairSearch(context, preferredPadding, preferredGaps)

            search.sweepPadding(minBottomPadding, padCeiling, allowTightDaylight = false)
            if (spec.topStartPlacement == TopStartPlacement.FIXED_BELOW_TOP_BAR) {
                val tallId = CameraViewfinderIds.RATIO_9_16
                val tallDropped = search.best?.frames?.get(tallId)?.let {
                    it.first > topInset + 0.05f
                } ?: true
                if (search.best == null || tallDropped) {
                    search.sweepZeroDrop(minBottomPadding, padCeiling)
                }
            }
            if (search.best == null) {
                search.sweepPaddingAndGaps(minBottomPadding, padCeiling, allowTightDaylight = false)
            }
            if (
                search.best == null &&
                spec.topStartPlacement == TopStartPlacement.VIEWFINDER_TOP_START
            ) {
                search.sweepPaddingAndGaps(minBottomPadding, padCeiling, allowTightDaylight = true)
            }
            if (search.best == null) {
                if (spec.topStartPlacement == TopStartPlacement.FIXED_BELOW_TOP_BAR) {
                    search.sweepPadding(minBottomPadding, padHardMax, allowTightDaylight = false)
                }
                if (search.best == null) {
                    search.sweepPaddingAndGaps(
                        minBottomPadding,
                        padHardMax,
                        allowTightDaylight = false
                    )
                }
                if (
                    search.best == null &&
                    spec.topStartPlacement == TopStartPlacement.FIXED_BELOW_TOP_BAR
                ) {
                    search.sweepPaddingAndGaps(
                        minBottomPadding,
                        padCeiling,
                        allowTightDaylight = true
                    )
                }
                if (search.best == null) {
                    search.sweepPaddingAndGaps(
                        minBottomPadding,
                        padHardMax,
                        allowTightDaylight = true
                    )
                }
            }

            evaluations += search.evaluations
            // A collision decides how far the stack has to rise, and the incidental budget bounds
            // how much further it may go. The search ranks frames first, so with the full
            // CameraLayoutSpec.maxStackLift available it would otherwise spend all of it to keep a
            // frame flush, where the layout prefers to shift the frame down.
            val lowestClean = search.lowestAcceptablePadding
            val repaired = lowestClean?.let {
                search.bestAtOrBelow(max(incidentalLiftCeiling, it + INCIDENTAL_LIFT_BUDGET))
            }
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
            liftedStack = lifted,
            evaluationCount = evaluations
        )
    }

    // -----------------------------------------------------------------------------------------
    // Candidate evaluation
    // -----------------------------------------------------------------------------------------

    /**
     * The top-start control's keep-out band as one frame sees it. A frame top collides with the
     * control when it lies between [behindLimit] and [bottom] plus the clearance: lower than the
     * control's visible top less the minimum gap, but not yet clear below it.
     */
    private class TopStartKeepOut(val bottom: Float, val behindLimit: Float)

    private class SolveContext(
        val spec: CameraLayoutSpec,
        val window: CameraWindow,
        val clearance: Float,
        val topInset: Float,
        val navTop: Float,
        val requiresImmersive: Boolean,
        val toolbarHeight: Float,
        val minBottomPadding: Int,
        /**
         * Highest bottom padding at which a tall frame may enclose the stack: the preferred
         * padding plus [INCIDENTAL_LIFT_BUDGET].
         *
         * Enclosing is what lets the stack keep its preferred position on screens where the tall
         * frame cannot clear it from above. Offering it to a stack that has been lifted further
         * would let the repair search raise the stack by up to [CameraLayoutSpec.maxStackLift]
         * only to keep the primary frame flush, where the layout prefers to shift the frame down.
         * Because the limit depends only on a candidate's padding, every pass places a given
         * candidate identically.
         */
        val enclosureMaxPadding: Int,
        val windowHeight: Float,
        val windowWidth: Float
    ) {
        val enforceFixedTopStart: Boolean =
            spec.topStartPlacement == TopStartPlacement.FIXED_BELOW_TOP_BAR
        val fixedTopStartBottom: Float = topInset + spec.topStartHeight.value

        /**
         * Lowest frame top that still passes behind the top-start control rather than bisecting
         * it: the control's visible top, less the minimum edge gap. Without a fixed control there
         * is nothing to pass behind, so a frame top must sit flush below the top bar.
         */
        val behindTopStartLimit: Float = if (enforceFixedTopStart) {
            topInset + max(0f, spec.topStartVisibleInset.value - spec.topStartMinEdgeGap.value)
        } else {
            topInset
        }

        /**
         * Whether a [PlacementPolicy.BINARY_TOP_EDGE] frame of [rawHeight] has any position that
         * starts below the top bar, high enough to pass behind the top-start control, and ends
         * clear of the navigation bar. Only such a frame can enclose the control stack instead of
         * expanding to the top of the window.
         */
        fun canEncloseStack(rawHeight: Float): Boolean {
            val maxBottom = if (requiresImmersive) windowHeight else navTop
            return topInset + rawHeight <= min(maxBottom, behindTopStartLimit + rawHeight) + 0.05f
        }

        /**
         * Whether a [PlacementPolicy.BINARY_TOP_EDGE] frame of [rawHeight] still clears the
         * bottom-most row when it starts below the top bar. The 0.05 guard absorbs float noise at
         * the exact boundary.
         */
        private fun fitsBelowTopBar(rawHeight: Float): Boolean {
            val maxBottom = if (requiresImmersive) windowHeight else navTop
            return (topInset + rawHeight) <=
                (maxBottom - minBottomPadding - toolbarHeight - clearance + 0.05f)
        }

        /**
         * Lowest top at which [slot] counts as passing behind the top-start control rather than
         * colliding with it.
         *
         * Only a tall frame that cannot clear the stack from above gets the allowance, because that
         * is the one frame whose enclosing position depends on it. Every other frame is either
         * flush with the top bar or clear of the control's keep-out band.
         */
        private fun behindLimitFor(slot: ViewfinderSlot, rawHeight: Float): Float =
            if (slot.policy == PlacementPolicy.BINARY_TOP_EDGE && !fitsBelowTopBar(rawHeight)) {
                behindTopStartLimit
            } else {
                topInset
            }

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
            val mayEnclose = bottomPadding <= enclosureMaxPadding

            for (slot in spec.viewfinders) {
                val rawHeight = windowWidth / slot.aspectRatio
                val keepOut = TopStartKeepOut(
                    bottom = effectiveTopStartBottom(slot, rawHeight, bands, frames),
                    behindLimit = behindLimitFor(slot, rawHeight)
                )
                val placed = place(slot, rawHeight, obstacles, frames, keepOut, mayEnclose)
                frames[slot.id] = placed
                collisions += intrusions(
                    slot.id,
                    placed,
                    obstacles,
                    clearance - COLLISION_TEST_SLACK,
                    keepOut
                )
                hardCollisions += intrusions(slot.id, placed, obstacles, 0f, keepOut)
                minClearance = min(minClearance, edgeClearance(placed, obstacles, keepOut))
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

        private fun effectiveTopStartBottom(
            slot: ViewfinderSlot,
            rawHeight: Float,
            bands: Map<String, DpRange>,
            placed: Map<String, Pair<Float, Float>>
        ): Float {
            if (!enforceFixedTopStart || slot.policy != PlacementPolicy.CENTERED_IN_REFERENCE) {
                return fixedTopStartBottom
            }
            val reference = placed[slot.referenceId] ?: return fixedTopStartBottom
            val captureBand = bands[CameraRowIds.CAPTURE_ROW] ?: return fixedTopStartBottom
            val maxTopAboveShutter =
                floor((captureBand.top.value - clearance) - rawHeight - clearance)
            return if (
                reference.second > captureBand.bottom.value &&
                maxTopAboveShutter >= topInset + 28f &&
                maxTopAboveShutter < fixedTopStartBottom
            ) {
                maxTopAboveShutter
            } else {
                fixedTopStartBottom
            }
        }

        /**
         * Places one frame according to its policy, then resolves any collision.
         *
         * @param mayEnclose whether a [PlacementPolicy.BINARY_TOP_EDGE] frame may enclose the
         *   stack at this candidate's padding. See [enclosureMaxPadding].
         */
        private fun place(
            slot: ViewfinderSlot,
            rawHeight: Float,
            obstacles: List<Obstacle>,
            placed: Map<String, Pair<Float, Float>>,
            keepOut: TopStartKeepOut,
            mayEnclose: Boolean
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
                    cappedBottom(rawHeight, topInset, navTop, keepOut.bottom),
                    preferred,
                    obstacles,
                    keepOut
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
                    cappedBottom(rawHeight, topInset, navTop, keepOut.bottom),
                    preferred,
                    obstacles,
                    keepOut
                )
            }

            PlacementPolicy.BINARY_TOP_EDGE -> {
                val maxBottom = if (requiresImmersive) windowHeight else navTop
                if (!fitsBelowTopBar(rawHeight)) {
                    // The frame cannot clear the stack from above, but it may still sit below the
                    // top bar if it encloses the controls instead, staying high enough to pass
                    // behind the top-start control and low enough to clear the navigation bar.
                    val lowestBottom = min(maxBottom, keepOut.behindLimit + rawHeight)
                    val enclosing = if (mayEnclose && canEncloseStack(rawHeight)) {
                        minimax(
                            rawHeight,
                            topInset,
                            lowestBottom,
                            topInset + rawHeight,
                            obstacles,
                            keepOut
                        )
                    } else {
                        null
                    }
                    if (
                        enclosing != null &&
                        intrusions(
                            slot.id,
                            enclosing,
                            obstacles,
                            clearance - COLLISION_TEST_SLACK,
                            keepOut
                        ).isEmpty()
                    ) {
                        enclosing
                    } else {
                        // Expand to the very top of the window. Partial overlap with the top bar is
                        // never used: it reads as a mistake rather than a decision.
                        0f to quantize(rawHeight)
                    }
                } else {
                    minimax(
                        rawHeight,
                        topInset,
                        cappedBottom(rawHeight, topInset, maxBottom, keepOut.bottom),
                        topInset + rawHeight,
                        obstacles,
                        keepOut
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
         * The navigation bar reads as black for the same reason, so the band below the frame is
         * measured to the bottom of the window rather than to the top of the navigation bar.
         * [maxBottom] still keeps the frame itself clear of the navigation bar.
         *
         * The top-justified position always stays reachable, even on a device already past the
         * balance point.
         */
        private fun cappedBottom(
            rawHeight: Float,
            minTop: Float,
            maxBottom: Float,
            topStartBottom: Float = fixedTopStartBottom
        ): Float {
            if (!spec.enforceTopWeighting) return maxBottom
            val firstShiftedTop = if (enforceFixedTopStart && minTop >= topInset - 0.05f) {
                max(minTop, topStartBottom + clearance)
            } else {
                minTop
            }
            val balancedTop = max(firstShiftedTop, (windowHeight - rawHeight) / 2f)
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
            obstacles: List<Obstacle>,
            keepOut: TopStartKeepOut
        ): Pair<Float, Float> {
            val lowerBound = min(maxBottom, minTop + rawHeight)
            val upperBound = maxBottom

            val candidates = LinkedHashSet<Float>()
            candidates += preferredBottom.coerceIn(lowerBound, upperBound)
            candidates += lowerBound
            candidates += upperBound

            if (enforceFixedTopStart) {
                val afterTopStart = quantize(keepOut.bottom + clearance + rawHeight)
                if (afterTopStart >= lowerBound && afterTopStart <= upperBound) {
                    candidates += afterTopStart
                }
            }

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
                val cost = penalty(candidate, rawHeight, preferredBottom, obstacles, keepOut)
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
            obstacles: List<Obstacle>,
            keepOut: TopStartKeepOut
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
            if (enforceFixedTopStart) {
                val keepOutBottom = keepOut.bottom + clearance
                if (top > keepOut.behindLimit + 0.05f && top < keepOutBottom) {
                    val depth = min(top - keepOut.behindLimit, keepOutBottom - top)
                    total += INTRUSION_COST + 8f * depth * depth
                }
            }
            val distance = bottom - preferredBottom
            return total + PREFERENCE_WEIGHT * distance * distance
        }

        private fun intrusions(
            viewfinderId: String,
            frame: Pair<Float, Float>,
            obstacles: List<Obstacle>,
            margin: Float,
            keepOut: TopStartKeepOut
        ): List<Collision> {
            val result = obstacles.filter { obstacle ->
                val lo = obstacle.top - margin
                val hi = obstacle.bottom + margin
                (frame.second > lo && frame.second < hi) || (frame.first > lo && frame.first < hi)
            }.mapTo(ArrayList()) { Collision(viewfinderId, it.id) }
            if (
                enforceFixedTopStart &&
                frame.first > keepOut.behindLimit + 0.05f &&
                frame.first < keepOut.bottom + margin
            ) {
                result += Collision(viewfinderId, "topStartContent")
            }
            return result
        }

        private fun edgeClearance(
            frame: Pair<Float, Float>,
            obstacles: List<Obstacle>,
            keepOut: TopStartKeepOut
        ): Float {
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
            if (enforceFixedTopStart && frame.first > keepOut.behindLimit + 0.05f) {
                if (frame.first <= keepOut.bottom) return 0f
                val distance = frame.first - keepOut.bottom
                if (distance < smallest) smallest = distance
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
     *
     * [best] is the top-ranked candidate over everything visited, and the pruning and early exits
     * below are built around it. The solver may instead take [bestAtOrBelow] to cap how far the
     * stack rises. That only chooses among candidates already visited, so it never changes what the
     * search evaluates.
     */
    private class RepairSearch(
        private val context: SolveContext,
        private val preferredPadding: Int,
        private val preferredGaps: IntArray
    ) {
        var best: Candidate? = null
            private set
        private var bestScore: List<Float>? = null

        /** The top-ranked acceptable candidate at each padding, under the ranking [best] uses. */
        private val bestByPadding = HashMap<Int, Pair<Candidate, List<Float>>>()

        /** The lowest padding at which any acceptable candidate was found, or null if none was. */
        val lowestAcceptablePadding: Int?
            get() = bestByPadding.keys.minOrNull()

        /**
         * The top-ranked acceptable candidate with a padding no higher than [maxPadding], or null
         * if there is none. This is [best] whenever [best] qualifies. Otherwise equal ranks
         * resolve to the lower padding.
         */
        fun bestAtOrBelow(maxPadding: Int): Candidate? {
            val overall = best ?: return null
            if (overall.bottomPadding <= maxPadding) return overall
            var pick: Pair<Candidate, List<Float>>? = null
            for (padding in bestByPadding.keys.sorted()) {
                if (padding > maxPadding) break
                val entry = bestByPadding.getValue(padding)
                if (pick == null || isBetter(entry.second, pick.second)) pick = entry
            }
            return pick?.first
        }

        /** Evaluations spent so far, across every rung. */
        var evaluations = 0
            private set

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

        private fun rowClear(edge: Float, rowTop: Float, rowBottom: Float, clr: Float): Boolean =
            edge <= rowTop - clr || edge >= rowBottom + clr

        /**
         * Bottom edges at which the 9:16 frame encloses the stack from just below the top bar, or
         * null when it has no such position. The pruning below has to admit these, or it would
         * skip paddings that placement accepts.
         */
        private val enclosed916Bottoms: ClosedFloatingPointRange<Float>? = run {
            val raw916H = context.windowWidth / (9f / 16f)
            if (!context.canEncloseStack(raw916H)) return@run null
            val maxBottom = if (context.requiresImmersive) context.windowHeight else context.navTop
            val highest = min(maxBottom, context.behindTopStartLimit + raw916H)
            min(highest, context.topInset + raw916H)..highest
        }

        /** The enclosed 9:16 bottom edges placement may use at [padding], or null. */
        private fun enclosed916At(padding: Int): ClosedFloatingPointRange<Float>? =
            if (padding <= context.enclosureMaxPadding) enclosed916Bottoms else null

        /**
         * Whether a row leaves at least one bottom edge in [range] clear of it. False when there
         * is no range.
         *
         * This is a per-row necessary condition, which is what pruning needs: it can let through a
         * stack whose rows block the range jointly, and [consider] then rejects it, but it never
         * rejects a stack that has a clean enclosed position.
         */
        private fun leavesEnclosureRoom(
            range: ClosedFloatingPointRange<Float>?,
            rowTop: Float,
            rowBottom: Float,
            clr: Float
        ): Boolean {
            if (range == null) return false
            return rowClear(range.start, rowTop, rowBottom, clr) ||
                rowClear(range.endInclusive, rowTop, rowBottom, clr)
        }

        fun sweepPadding(from: Int, to: Int, allowTightDaylight: Boolean) {
            if (!context.enforceFixedTopStart || rows.size != 4) {
                for (padding in from..to) {
                    placedAny = false
                    consider(padding, preferredGaps, allowTightDaylight)
                    if (!placedAny || evaluations >= EVALUATION_BUDGET) break
                }
                return
            }

            val clr = if (allowTightDaylight) 0f else context.clearance - COLLISION_TEST_SLACK
            val raw34H = context.windowWidth / (3f / 4f)
            val raw916H = context.windowWidth / (9f / 16f)
            val maxBot916 = if (context.requiresImmersive) context.windowHeight else context.navTop
            val minStackBelow916 =
                context.minBottomPadding + context.toolbarHeight + context.clearance
            val fitsBelowTopBar =
                (context.topInset + raw916H) <= (maxBot916 - minStackBelow916 + 0.05f)
            val forcedFlush916 = !context.requiresImmersive && !fitsBelowTopBar
            val raw34Bottom = context.topInset + raw34H
            val firstShiftedTop34 = max(context.topInset, context.fixedTopStartBottom + clr)
            val dropCap34 =
                max(firstShiftedTop34, (context.windowHeight - raw34H) / 2f) + raw34H
            val maxBot34 = max(raw34Bottom, min(context.navTop, dropCap34))
            val minShiftedBot34 = min(maxBot34, firstShiftedTop34 + raw34H)

            val h0 = context.toolbarHeight
            val h1 = rows[1].height.value
            val h2 = rows[2].height.value
            val h3 = rows[3].height.value

            for (padding in from..to) {
                if (evaluations >= EVALUATION_BUDGET) break
                val lBot = context.navTop - padding
                val lTop = lBot - h0
                val mBot = lTop - preferredGaps[0]
                val mTop = mBot - h1
                val sBot = mTop - preferredGaps[1]
                val sTop = sBot - h2
                val nzBot = sTop - preferredGaps[2]
                val nzTop = nzBot - h3
                if (nzTop < context.topInset) break
                fun clear4(b: Float): Boolean = rowClear(b, lTop, lBot, clr) &&
                    rowClear(b, mTop, mBot, clr) &&
                    rowClear(b, sTop, sBot, clr) &&
                    rowClear(b, nzTop, nzBot, clr)
                if (forcedFlush916 && !clear4(raw916H)) {
                    val enclosure = enclosed916At(padding)
                    val enclosable = leavesEnclosureRoom(enclosure, lTop, lBot, clr) &&
                        leavesEnclosureRoom(enclosure, mTop, mBot, clr) &&
                        leavesEnclosureRoom(enclosure, sTop, sBot, clr) &&
                        leavesEnclosureRoom(enclosure, nzTop, nzBot, clr)
                    if (!enclosable) continue
                }

                var canPlace34 = clear4(raw34Bottom)
                val bestPrimaryTop = best?.frames?.get(CameraViewfinderIds.RATIO_3_4)?.first
                val primaryDropped =
                    bestPrimaryTop == null || bestPrimaryTop > context.topInset + 0.05f
                if (!canPlace34 && primaryDropped) {
                    val cands34 = floatArrayOf(
                        minShiftedBot34,
                        maxBot34,
                        nzTop - clr,
                        nzBot + clr,
                        sTop - clr,
                        sBot + clr,
                        mTop - clr,
                        mBot + clr,
                        lTop - clr
                    )
                    for (c in cands34) {
                        if (c >= minShiftedBot34 - 0.05f && c <= maxBot34 + 0.05f && clear4(c)) {
                            canPlace34 = true
                            break
                        }
                    }
                }
                if (!canPlace34) continue
                placedAny = false
                consider(padding, preferredGaps, allowTightDaylight)
                if (!placedAny || evaluations >= EVALUATION_BUDGET) break
            }
        }

        fun sweepZeroDrop(from: Int, to: Int) {
            if (rows.size != 4) return
            val clr = context.clearance - COLLISION_TEST_SLACK
            val raw34H = context.windowWidth / (3f / 4f)
            val raw916H = context.windowWidth / (9f / 16f)
            val maxBot916 = if (context.requiresImmersive) context.windowHeight else context.navTop
            val minStackBelow916 =
                context.minBottomPadding + context.toolbarHeight + context.clearance
            val fitsBelowTopBar =
                (context.topInset + raw916H) <= (maxBot916 - minStackBelow916 + 0.05f)
            val forcedFlush916 = !context.requiresImmersive && !fitsBelowTopBar
            val raw34Bottom = context.topInset + raw34H
            val raw916Bottom = (if (forcedFlush916) 0f else context.topInset) + raw916H
            val check916Raw = !context.requiresImmersive
            val enclosedBottom916 = context.topInset + raw916H

            val h0 = context.toolbarHeight
            val h1 = rows[1].height.value
            val h2 = rows[2].height.value
            val h3 = rows[3].height.value
            val minStackBelowZoom =
                h0 + h1 + h2 + 16f + context.minBottomPadding + context.clearance
            val shutterInside34 = (context.navTop - raw34Bottom) < minStackBelowZoom

            val g0 = gapGrid(0, preferredGaps[0])
            val g1 = gapGrid(1, preferredGaps[1])
            val g2 = gapGrid(
                2,
                preferredGaps[2],
                if (shutterInside34) preferredGaps[2] else max(preferredGaps[2], 38)
            )
            val working = IntArray(preferredGaps.size)
            working[3] = preferredGaps[3]

            for (padding in from..to) {
                if (evaluations >= EVALUATION_BUDGET) break
                val lBot = context.navTop - padding
                val lTop = lBot - h0
                if (lTop - 8f - h1 - 8f - h2 - 8f - h3 < context.topInset) break
                if (!rowClear(raw34Bottom, lTop, lBot, clr)) continue
                // A 9:16 frame that cannot clear the stack from above has a second zero-drop
                // position when it may enclose the stack: flush below the top bar.
                val enclosable916 = forcedFlush916 && enclosed916At(padding) != null
                val lFlush = rowClear(raw916Bottom, lTop, lBot, clr)
                val lEnclosed = enclosable916 && rowClear(enclosedBottom916, lTop, lBot, clr)
                if (check916Raw && !lFlush && !lEnclosed) continue

                var foundAtPad = false
                for (v0 in g0) {
                    val mBot = lTop - v0
                    val mTop = mBot - h1
                    if (!rowClear(raw34Bottom, mTop, mBot, clr)) continue
                    val mFlush = lFlush && rowClear(raw916Bottom, mTop, mBot, clr)
                    val mEnclosed = lEnclosed && rowClear(enclosedBottom916, mTop, mBot, clr)
                    if (check916Raw && !mFlush && !mEnclosed) continue
                    working[0] = v0

                    for (v1 in g1) {
                        val sBot = mTop - v1
                        val sTop = sBot - h2
                        if (!rowClear(raw34Bottom, sTop, sBot, clr)) continue
                        val sFlush = mFlush && rowClear(raw916Bottom, sTop, sBot, clr)
                        val sEnclosed = mEnclosed && rowClear(enclosedBottom916, sTop, sBot, clr)
                        if (check916Raw && !sFlush && !sEnclosed) continue
                        working[1] = v1

                        for (v2 in g2) {
                            val nzBot = sTop - v2
                            val nzTop = nzBot - h3
                            if (nzTop < context.topInset) continue
                            if (!rowClear(raw34Bottom, nzTop, nzBot, clr)) continue
                            val nzFlush = sFlush && rowClear(raw916Bottom, nzTop, nzBot, clr)
                            val nzEnclosed =
                                sEnclosed && rowClear(enclosedBottom916, nzTop, nzBot, clr)
                            if (check916Raw && !nzFlush && !nzEnclosed) continue
                            if (evaluations >= EVALUATION_BUDGET) break
                            working[2] = v2
                            consider(padding, working, allowTightDaylight = false)
                            val currentBest = best
                            val best34Top =
                                currentBest?.frames?.get(CameraViewfinderIds.RATIO_3_4)?.first
                                    ?: Float.MAX_VALUE
                            val best916Top =
                                currentBest?.frames?.get(CameraViewfinderIds.RATIO_9_16)?.first
                                    ?: Float.MAX_VALUE
                            if (
                                currentBest != null &&
                                best34Top <= context.topInset + 0.05f &&
                                best916Top <= context.topInset + 0.05f
                            ) {
                                foundAtPad = true
                                break
                            }
                        }
                        if (foundAtPad) break
                    }
                    if (foundAtPad) break
                }
                if (foundAtPad && padding >= preferredPadding) break
            }
        }

        fun sweepPaddingAndGaps(from: Int, to: Int, allowTightDaylight: Boolean) {
            if (!context.enforceFixedTopStart || rows.size != 4) {
                val grids =
                    preferredGaps.mapIndexed { index, preferred -> gapGrid(index, preferred) }
                val working = IntArray(preferredGaps.size)
                for (padding in from..to) {
                    placedAny = false
                    permute(grids, working, 0) { consider(padding, working, allowTightDaylight) }
                    if (!placedAny || evaluations >= EVALUATION_BUDGET) break
                }
                return
            }

            val g0 = gapGrid(0, preferredGaps[0])
            val g1 = gapGrid(1, preferredGaps[1])
            val g2 = gapGrid(2, preferredGaps[2])
            val working = IntArray(preferredGaps.size)
            working[3] = preferredGaps[3]

            val clr = if (allowTightDaylight) 0f else context.clearance - COLLISION_TEST_SLACK
            val raw34H = context.windowWidth / (3f / 4f)
            val raw916H = context.windowWidth / (9f / 16f)
            val maxBot916 = if (context.requiresImmersive) context.windowHeight else context.navTop
            val minStackBelow916 =
                context.minBottomPadding + context.toolbarHeight + context.clearance
            val fitsBelowTopBar =
                (context.topInset + raw916H) <= (maxBot916 - minStackBelow916 + 0.05f)
            val forcedFlush916 = !context.requiresImmersive && !fitsBelowTopBar
            val raw34Bottom = context.topInset + raw34H
            val firstShiftedTop34 = max(context.topInset, context.fixedTopStartBottom + clr)
            val dropCap34 =
                max(firstShiftedTop34, (context.windowHeight - raw34H) / 2f) + raw34H
            val maxBot34 = max(raw34Bottom, min(context.navTop, dropCap34))
            val minShiftedBot34 = min(maxBot34, firstShiftedTop34 + raw34H)

            val h0 = context.toolbarHeight
            val h1 = rows[1].height.value
            val h2 = rows[2].height.value
            val h3 = rows[3].height.value

            for (padding in from..to) {
                if (evaluations >= EVALUATION_BUDGET) break
                val lBot = context.navTop - padding
                val lTop = lBot - h0
                if (lTop - 8f - h1 - 8f - h2 - 8f - h3 < context.topInset) break
                // Track the two places a 9:16 that cannot clear the stack from above may land:
                // flush with the window top, or enclosing the stack.
                val enclosure = enclosed916At(padding)
                val lFlush = rowClear(raw916H, lTop, lBot, clr)
                val lEnclosed = leavesEnclosureRoom(enclosure, lTop, lBot, clr)
                if (forcedFlush916 && !lFlush && !lEnclosed) continue

                for (v0 in g0) {
                    val mBot = lTop - v0
                    val mTop = mBot - h1
                    if (mTop - 8f - h2 - 8f - h3 < context.topInset) continue
                    val mFlush = lFlush && rowClear(raw916H, mTop, mBot, clr)
                    val mEnclosed = lEnclosed && leavesEnclosureRoom(enclosure, mTop, mBot, clr)
                    if (forcedFlush916 && !mFlush && !mEnclosed) continue
                    working[0] = v0

                    for (v1 in g1) {
                        val sBot = mTop - v1
                        val sTop = sBot - h2
                        if (sTop - 8f - h3 < context.topInset) continue
                        val sFlush = mFlush && rowClear(raw916H, sTop, sBot, clr)
                        val sEnclosed =
                            mEnclosed && leavesEnclosureRoom(enclosure, sTop, sBot, clr)
                        if (forcedFlush916 && !sFlush && !sEnclosed) continue

                        fun clear3Rows(b: Float): Boolean = rowClear(b, lTop, lBot, clr) &&
                            rowClear(b, mTop, mBot, clr) &&
                            rowClear(b, sTop, sBot, clr)

                        var canPlace34 = clear3Rows(raw34Bottom)
                        if (!canPlace34) {
                            val cands34 = floatArrayOf(
                                minShiftedBot34,
                                maxBot34,
                                sTop - clr,
                                sBot + clr,
                                mTop - clr,
                                mBot + clr,
                                lTop - clr
                            )
                            for (c in cands34) {
                                if (
                                    c >= minShiftedBot34 - 0.05f &&
                                    c <= maxBot34 + 0.05f &&
                                    clear3Rows(c)
                                ) {
                                    canPlace34 = true
                                    break
                                }
                            }
                        }
                        if (!canPlace34) continue
                        working[1] = v1

                        for (v2 in g2) {
                            val nzBot = sTop - v2
                            val nzTop = nzBot - h3
                            if (nzTop < context.topInset) continue
                            val nzFlush = sFlush && rowClear(raw916H, nzTop, nzBot, clr)
                            val nzEnclosed =
                                sEnclosed && leavesEnclosureRoom(enclosure, nzTop, nzBot, clr)
                            if (forcedFlush916 && !nzFlush && !nzEnclosed) continue
                            working[2] = v2
                            consider(padding, working, allowTightDaylight)
                        }
                    }
                }
                if (best != null) break
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
        private fun gapGrid(
            index: Int,
            preferred: Int,
            ceilingOverride: Int = preferred
        ): List<Int> {
            if (index == preferredGaps.lastIndex) return listOf(preferred)
            val minGap = rows[index].gapAbove.min.value.roundToJs()
            val values = linkedSetOf(preferred)
            var v = minGap
            val ceiling = max(max(preferred, ceilingOverride), minGap)
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
            val atPadding = bestByPadding[padding]
            if (atPadding == null || isBetter(ranked, atPadding.second)) {
                bestByPadding[padding] = candidate to ranked
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
