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

/** A device window from the golden fixture, plus the solution the reference produced for it. */
internal data class GoldenCase(
    val device: String,
    val navMode: String,
    val window: CameraWindow,
    val requiresImmersive: Boolean,
    val toolbarHeightDp: Float,
    val bottomPaddingDp: Float,
    val gapAboveToolbarDp: Float,
    val gapAboveModeDp: Float,
    val gapAboveCaptureDp: Float,
    val rows: Map<String, Pair<Float, Float>>,
    val viewfinders: Map<String, Pair<Float, Float>>,
    val minClearanceDp: Float
) {
    val label: String get() = "$device/$navMode"
}

/**
 * Loads the golden fixture captured from the reference prototype.
 *
 * The fixture is plain delimited text rather than JSON so that reading it needs no parser
 * dependency, and so that a change to the layout contract shows up as a readable diff in review.
 */
internal object GoldenFixture {

    private const val RESOURCE = "/layout/solver_golden.txt"

    fun load(): List<GoldenCase> {
        val stream = requireNotNull(GoldenFixture::class.java.getResourceAsStream(RESOURCE)) {
            "Missing golden fixture at $RESOURCE. Regenerate it with scratch/solver_extract."
        }
        return stream.bufferedReader().useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .map(::parse)
                .toList()
        }.also {
            require(it.isNotEmpty()) { "Golden fixture is empty" }
        }
    }

    private fun parse(line: String): GoldenCase {
        val f = line.split('|')
        require(f.size == 21) { "Expected 21 fields, got ${f.size} in: $line" }
        fun band(raw: String): Pair<Float, Float> {
            val (top, bottom) = raw.split(':')
            return top.toFloat() to bottom.toFloat()
        }
        return GoldenCase(
            device = f[0],
            navMode = f[1],
            window = CameraWindow(
                width = f[2].toFloat().dp,
                height = f[3].toFloat().dp,
                topInset = f[4].toFloat().dp,
                navInset = f[5].toFloat().dp,
                gestureInset = f[6].toFloat().dp
            ),
            requiresImmersive = when (f[7]) {
                "immersive" -> true
                "bars" -> false
                else -> error("Unknown bar mode '${f[7]}' in: $line")
            },
            toolbarHeightDp = f[8].toFloat(),
            bottomPaddingDp = f[9].toFloat(),
            gapAboveToolbarDp = f[10].toFloat(),
            gapAboveModeDp = f[11].toFloat(),
            gapAboveCaptureDp = f[12].toFloat(),
            rows = mapOf(
                CameraRowIds.ZOOM_BAR to band(f[13]),
                CameraRowIds.CAPTURE_ROW to band(f[14]),
                CameraRowIds.MODE_SWITCHER to band(f[15]),
                CameraRowIds.BOTTOM_TOOLBAR to band(f[16])
            ),
            viewfinders = mapOf(
                CameraViewfinderIds.RATIO_3_4 to band(f[17]),
                CameraViewfinderIds.RATIO_1_1 to band(f[18]),
                CameraViewfinderIds.RATIO_9_16 to band(f[19])
            ),
            minClearanceDp = f[20].toFloat()
        )
    }
}
