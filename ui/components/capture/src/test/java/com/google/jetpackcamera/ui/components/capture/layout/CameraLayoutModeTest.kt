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
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for [classifyCameraLayoutMode] and [isFullScreenCompactPhoneWindow], verifying
 * initial 7-mode classification (`COMPACT`, `COMPACT_SLIM`, `COMPACT_SHORT`, `COMPACT_SHORT_SLIM`,
 * `FOLDABLE`, `TABLET_PORTRAIT`, `TABLET_LANDSCAPE`), posture rotation pairs, full-screen compact
 * phone classification, and the `14.dp` (`±7.dp`) hysteresis deadbands across all boundaries.
 */
@RunWith(JUnit4::class)
class CameraLayoutModeTest {

    @Test
    fun classifyCameraLayoutMode_initialClassification_resolvesAllSevenModes() {
        // 1. COMPACT: portrait & horizontal windows (maxDim >= 480dp, minDim >= 360dp, not 600x480)
        assertThat(
            classifyCameraLayoutMode(widthDp = 412.dp, heightDp = 915.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(
            classifyCameraLayoutMode(widthDp = 599.dp, heightDp = 480.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(
            classifyCameraLayoutMode(widthDp = 800.dp, heightDp = 400.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(
            classifyCameraLayoutMode(widthDp = 550.dp, heightDp = 500.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT)

        // 2. COMPACT_SLIM: minDim < 360dp, maxDim >= 480dp
        assertThat(
            classifyCameraLayoutMode(widthDp = 320.dp, heightDp = 640.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT_SLIM)
        assertThat(
            classifyCameraLayoutMode(widthDp = 640.dp, heightDp = 320.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT_SLIM)

        // 3. COMPACT_SHORT: width < 480dp, height < 480dp, minDim >= 360dp
        assertThat(
            classifyCameraLayoutMode(widthDp = 412.dp, heightDp = 440.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT_SHORT)
        assertThat(
            classifyCameraLayoutMode(widthDp = 360.dp, heightDp = 360.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT_SHORT)

        // 4. COMPACT_SHORT_SLIM: width < 480dp, height < 480dp, minDim < 360dp
        assertThat(
            classifyCameraLayoutMode(widthDp = 320.dp, heightDp = 340.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT_SHORT_SLIM)
        assertThat(
            classifyCameraLayoutMode(widthDp = 340.dp, heightDp = 320.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT_SHORT_SLIM)

        // 5. FOLDABLE: width >= 600dp, height >= 480dp, maxDim < 960dp, maxDim < minDim * 1.45f
        assertThat(
            classifyCameraLayoutMode(widthDp = 852.dp, heightDp = 883.dp)
        ).isEqualTo(CameraLayoutMode.FOLDABLE)
        assertThat(
            classifyCameraLayoutMode(widthDp = 673.dp, heightDp = 841.dp)
        ).isEqualTo(CameraLayoutMode.FOLDABLE)
        assertThat(
            classifyCameraLayoutMode(widthDp = 841.dp, heightDp = 673.dp)
        ).isEqualTo(CameraLayoutMode.FOLDABLE)

        // 6. TABLET_PORTRAIT: width >= 600dp, height >= 480dp, outside FOLDABLE, height > width
        assertThat(
            classifyCameraLayoutMode(widthDp = 800.dp, heightDp = 1280.dp)
        ).isEqualTo(CameraLayoutMode.TABLET_PORTRAIT)
        assertThat(
            classifyCameraLayoutMode(widthDp = 600.dp, heightDp = 960.dp)
        ).isEqualTo(CameraLayoutMode.TABLET_PORTRAIT)
        assertThat(
            classifyCameraLayoutMode(widthDp = 600.dp, heightDp = 900.dp)
        ).isEqualTo(CameraLayoutMode.TABLET_PORTRAIT)

        // 7. TABLET_LANDSCAPE: width >= 600dp, height >= 480dp, outside FOLDABLE, width >= height
        assertThat(
            classifyCameraLayoutMode(widthDp = 1280.dp, heightDp = 800.dp)
        ).isEqualTo(CameraLayoutMode.TABLET_LANDSCAPE)
        assertThat(
            classifyCameraLayoutMode(widthDp = 960.dp, heightDp = 600.dp)
        ).isEqualTo(CameraLayoutMode.TABLET_LANDSCAPE)
        assertThat(
            classifyCameraLayoutMode(widthDp = 1024.dp, heightDp = 768.dp)
        ).isEqualTo(CameraLayoutMode.TABLET_LANDSCAPE)
    }

    @Test
    fun classifyCameraLayoutMode_rotationPairs_preservePostureFamilies() {
        // 60/40 compact split rotated: both stay COMPACT
        val portraitSplit = classifyCameraLayoutMode(widthDp = 412.dp, heightDp = 530.dp)
        val landscapeSplit = classifyCameraLayoutMode(
            widthDp = 530.dp,
            heightDp = 412.dp,
            previousMode = portraitSplit
        )
        assertThat(portraitSplit).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(landscapeSplit).isEqualTo(CameraLayoutMode.COMPACT)

        // 50/50 compact split rotated: stays COMPACT_SHORT
        val squarePortrait = classifyCameraLayoutMode(widthDp = 412.dp, heightDp = 445.dp)
        val squareLandscape = classifyCameraLayoutMode(
            widthDp = 445.dp,
            heightDp = 412.dp,
            previousMode = squarePortrait
        )
        assertThat(squarePortrait).isEqualTo(CameraLayoutMode.COMPACT_SHORT)
        assertThat(squareLandscape).isEqualTo(CameraLayoutMode.COMPACT_SHORT)

        // Unfolded foldable rotated: stays FOLDABLE
        val foldablePortrait = classifyCameraLayoutMode(widthDp = 673.dp, heightDp = 841.dp)
        val foldableLandscape = classifyCameraLayoutMode(
            widthDp = 841.dp,
            heightDp = 673.dp,
            previousMode = foldablePortrait
        )
        assertThat(foldablePortrait).isEqualTo(CameraLayoutMode.FOLDABLE)
        assertThat(foldableLandscape).isEqualTo(CameraLayoutMode.FOLDABLE)

        // Tablet rotated: swaps TABLET_PORTRAIT <-> TABLET_LANDSCAPE
        val tabletPortrait = classifyCameraLayoutMode(widthDp = 800.dp, heightDp = 1280.dp)
        val tabletLandscape = classifyCameraLayoutMode(
            widthDp = 1280.dp,
            heightDp = 800.dp,
            previousMode = tabletPortrait
        )
        assertThat(tabletPortrait).isEqualTo(CameraLayoutMode.TABLET_PORTRAIT)
        assertThat(tabletLandscape).isEqualTo(CameraLayoutMode.TABLET_LANDSCAPE)
    }

    @Test
    fun classifyCameraLayoutMode_fullScreenCompactPhone_resolvesCompactOrCompactSlim() {
        assertThat(
            classifyCameraLayoutMode(
                widthDp = 360.dp,
                heightDp = 740.dp,
                isFullScreenCompactPhone = true
            )
        ).isEqualTo(CameraLayoutMode.COMPACT)

        assertThat(
            classifyCameraLayoutMode(
                widthDp = 740.dp,
                heightDp = 360.dp,
                isFullScreenCompactPhone = true
            )
        ).isEqualTo(CameraLayoutMode.COMPACT)

        assertThat(
            classifyCameraLayoutMode(
                widthDp = 915.dp,
                heightDp = 500.dp,
                isFullScreenCompactPhone = true
            )
        ).isEqualTo(CameraLayoutMode.COMPACT)

        assertThat(
            classifyCameraLayoutMode(
                widthDp = 320.dp,
                heightDp = 740.dp,
                isFullScreenCompactPhone = true
            )
        ).isEqualTo(CameraLayoutMode.COMPACT_SLIM)
    }

    @Test
    fun classifyCameraLayoutMode_widthHysteresisSweep_respects14dpDeadbandAround600dp() {
        var mode: CameraLayoutMode? = null
        fun stepWidth(width: Dp): CameraLayoutMode {
            mode = classifyCameraLayoutMode(
                widthDp = width,
                heightDp = 800.dp,
                isFullScreenCompactPhone = false,
                previousMode = mode
            )
            return checkNotNull(mode)
        }

        // Expanding from compact: stays COMPACT inside [593dp, 606dp], enters FOLDABLE at >= 607dp.
        assertThat(stepWidth(580.dp)).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(stepWidth(600.dp)).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(stepWidth(606.dp)).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(stepWidth(608.dp)).isEqualTo(CameraLayoutMode.FOLDABLE)

        // Shrinking from foldable: stays FOLDABLE down to 593dp, exits below 593dp.
        assertThat(stepWidth(600.dp)).isEqualTo(CameraLayoutMode.FOLDABLE)
        assertThat(stepWidth(593.dp)).isEqualTo(CameraLayoutMode.FOLDABLE)
        assertThat(stepWidth(592.dp)).isEqualTo(CameraLayoutMode.COMPACT)

        // Horizontal COMPACT window with height in [480dp, 486dp] (e.g., 483dp) widened past 607dp
        // transitions to FOLDABLE / TABLET_LANDSCAPE when wasHeightAtLeast480 is tracked.
        var horizontalMode: CameraLayoutMode? = null
        var wasHeightAtLeast480: Boolean? = null
        fun stepHorizontalWidth(width: Dp, height: Dp = 483.dp): CameraLayoutMode {
            val prevMode = horizontalMode
            val nextMode = classifyCameraLayoutMode(
                widthDp = width,
                heightDp = height,
                isFullScreenCompactPhone = false,
                previousMode = prevMode,
                wasHeightAtLeast480 = wasHeightAtLeast480
            )
            val isLargeScreen = prevMode == CameraLayoutMode.FOLDABLE ||
                prevMode == CameraLayoutMode.TABLET_PORTRAIT ||
                prevMode == CameraLayoutMode.TABLET_LANDSCAPE
            wasHeightAtLeast480 = when {
                prevMode == null -> height >= 480.dp
                isLargeScreen || wasHeightAtLeast480 == true -> height >= 473.dp
                else -> height >= 487.dp
            }
            horizontalMode = nextMode
            return nextMode
        }

        assertThat(stepHorizontalWidth(580.dp)).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(stepHorizontalWidth(606.dp)).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(stepHorizontalWidth(608.dp)).isEqualTo(CameraLayoutMode.FOLDABLE)
        assertThat(stepHorizontalWidth(900.dp)).isEqualTo(CameraLayoutMode.TABLET_LANDSCAPE)
    }

    @Test
    fun classifyCameraLayoutMode_widthHysteresisSweep_shortHeight_compactShortToCompact() {
        var mode: CameraLayoutMode? = null
        fun stepWidth(width: Dp): CameraLayoutMode {
            mode = classifyCameraLayoutMode(
                widthDp = width,
                heightDp = 400.dp,
                isFullScreenCompactPhone = false,
                previousMode = mode
            )
            return checkNotNull(mode)
        }

        assertThat(stepWidth(460.dp)).isEqualTo(CameraLayoutMode.COMPACT_SHORT)
        assertThat(stepWidth(480.dp)).isEqualTo(CameraLayoutMode.COMPACT_SHORT)
        assertThat(stepWidth(486.dp)).isEqualTo(CameraLayoutMode.COMPACT_SHORT)
        assertThat(stepWidth(487.dp)).isEqualTo(CameraLayoutMode.COMPACT)

        assertThat(stepWidth(480.dp)).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(stepWidth(473.dp)).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(stepWidth(472.dp)).isEqualTo(CameraLayoutMode.COMPACT_SHORT)
    }

    @Test
    fun classifyCameraLayoutMode_heightHysteresisSweep_respects14dpDeadbandAround480dp_wide() {
        var mode: CameraLayoutMode? = null
        var wasHeightAtLeast480: Boolean? = null
        fun stepSize(width: Dp = 800.dp, height: Dp): CameraLayoutMode {
            val prevMode = mode
            val nextMode = classifyCameraLayoutMode(
                widthDp = width,
                heightDp = height,
                isFullScreenCompactPhone = false,
                previousMode = prevMode,
                wasHeightAtLeast480 = wasHeightAtLeast480
            )
            val isLargeScreen = prevMode == CameraLayoutMode.FOLDABLE ||
                prevMode == CameraLayoutMode.TABLET_PORTRAIT ||
                prevMode == CameraLayoutMode.TABLET_LANDSCAPE
            wasHeightAtLeast480 = when {
                prevMode == null -> height >= 480.dp
                isLargeScreen -> height >= 473.dp
                prevMode == CameraLayoutMode.COMPACT_SHORT ||
                    prevMode == CameraLayoutMode.COMPACT_SHORT_SLIM -> height >= 487.dp
                wasHeightAtLeast480 == true -> height >= 473.dp
                else -> height >= 487.dp
            }
            mode = nextMode
            return nextMode
        }

        // 800x500dp has maxDim (800) >= minDim * 1.45 (725), so it resolves to TABLET_LANDSCAPE.
        assertThat(stepSize(height = 500.dp)).isEqualTo(CameraLayoutMode.TABLET_LANDSCAPE)
        assertThat(stepSize(height = 480.dp)).isEqualTo(CameraLayoutMode.TABLET_LANDSCAPE)
        assertThat(stepSize(height = 473.dp)).isEqualTo(CameraLayoutMode.TABLET_LANDSCAPE)
        assertThat(stepSize(height = 472.dp)).isEqualTo(CameraLayoutMode.COMPACT)

        // Growing through the deadband, no-op recomposition at 480.dp, and shrinking back to 474.dp
        // all remain COMPACT until height reaches 487.dp.
        assertThat(stepSize(height = 480.dp)).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(stepSize(height = 480.dp)).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(stepSize(height = 474.dp)).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(stepSize(height = 480.dp)).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(stepSize(height = 486.dp)).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(stepSize(height = 488.dp)).isEqualTo(CameraLayoutMode.TABLET_LANDSCAPE)

        // Horizontal drag into the deadband before widening: 580x483 -> 580x478 -> 900x478.
        mode = null
        wasHeightAtLeast480 = null
        assertThat(stepSize(width = 580.dp, height = 483.dp)).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(stepSize(width = 580.dp, height = 478.dp)).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(stepSize(width = 900.dp, height = 478.dp))
            .isEqualTo(CameraLayoutMode.TABLET_LANDSCAPE)
    }

    @Test
    fun classifyCameraLayoutMode_foldableSpanAndAspectHysteresis_preventsTabletBoundaryThrash() {
        var mode: CameraLayoutMode? = null
        fun stepSpan(width: Dp): CameraLayoutMode {
            mode = classifyCameraLayoutMode(
                widthDp = width,
                heightDp = 800.dp,
                isFullScreenCompactPhone = false,
                previousMode = mode
            )
            return checkNotNull(mode)
        }

        // Expanding across 960dp span: stays FOLDABLE up to 966dp, enters TABLET_LANDSCAPE at 967dp
        assertThat(stepSpan(880.dp)).isEqualTo(CameraLayoutMode.FOLDABLE)
        assertThat(stepSpan(960.dp)).isEqualTo(CameraLayoutMode.FOLDABLE)
        assertThat(stepSpan(966.dp)).isEqualTo(CameraLayoutMode.FOLDABLE)
        assertThat(stepSpan(967.dp)).isEqualTo(CameraLayoutMode.TABLET_LANDSCAPE)

        // Shrinking from TABLET_LANDSCAPE: stays TABLET_LANDSCAPE down to 953dp, enters at 952dp
        assertThat(stepSpan(960.dp)).isEqualTo(CameraLayoutMode.TABLET_LANDSCAPE)
        assertThat(stepSpan(953.dp)).isEqualTo(CameraLayoutMode.TABLET_LANDSCAPE)
        assertThat(stepSpan(952.dp)).isEqualTo(CameraLayoutMode.FOLDABLE)

        // Aspect ratio boundary at width = 620dp (620 * 1.45 = 899dp, deadband [892dp, 906dp])
        var aspectMode: CameraLayoutMode? = null
        fun stepTallHeight(height: Dp): CameraLayoutMode {
            aspectMode = classifyCameraLayoutMode(
                widthDp = 620.dp,
                heightDp = height,
                isFullScreenCompactPhone = false,
                previousMode = aspectMode
            )
            return checkNotNull(aspectMode)
        }

        assertThat(stepTallHeight(850.dp)).isEqualTo(CameraLayoutMode.FOLDABLE)
        assertThat(stepTallHeight(905.dp)).isEqualTo(CameraLayoutMode.FOLDABLE)
        assertThat(stepTallHeight(906.dp)).isEqualTo(CameraLayoutMode.TABLET_PORTRAIT)
        assertThat(stepTallHeight(892.dp)).isEqualTo(CameraLayoutMode.TABLET_PORTRAIT)
        assertThat(stepTallHeight(891.dp)).isEqualTo(CameraLayoutMode.FOLDABLE)
    }

    @Test
    fun classifyCameraLayoutMode_tabletSquareDiagonalHysteresis_stabilizesPortraitVsLandscape() {
        var mode: CameraLayoutMode? = null
        fun stepSize(width: Dp, height: Dp): CameraLayoutMode {
            mode = classifyCameraLayoutMode(
                widthDp = width,
                heightDp = height,
                isFullScreenCompactPhone = false,
                previousMode = mode
            )
            return checkNotNull(mode)
        }

        assertThat(stepSize(980.dp, 1020.dp)).isEqualTo(CameraLayoutMode.TABLET_PORTRAIT)
        assertThat(stepSize(1020.dp, 1020.dp)).isEqualTo(CameraLayoutMode.TABLET_PORTRAIT)
        assertThat(stepSize(1026.dp, 1020.dp)).isEqualTo(CameraLayoutMode.TABLET_PORTRAIT)
        assertThat(stepSize(1027.dp, 1020.dp)).isEqualTo(CameraLayoutMode.TABLET_LANDSCAPE)

        assertThat(stepSize(1020.dp, 1020.dp)).isEqualTo(CameraLayoutMode.TABLET_LANDSCAPE)
        assertThat(stepSize(1020.dp, 1026.dp)).isEqualTo(CameraLayoutMode.TABLET_LANDSCAPE)
        assertThat(stepSize(1020.dp, 1027.dp)).isEqualTo(CameraLayoutMode.TABLET_PORTRAIT)
    }

    @Test
    fun classifyCameraLayoutMode_slimHysteresisSweep_respects14dpDeadbandAround360dp() {
        var mode: CameraLayoutMode? = null
        fun stepWidth(width: Dp): CameraLayoutMode {
            mode = classifyCameraLayoutMode(
                widthDp = width,
                heightDp = 700.dp,
                isFullScreenCompactPhone = false,
                previousMode = mode
            )
            return checkNotNull(mode)
        }

        assertThat(stepWidth(380.dp)).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(stepWidth(360.dp)).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(stepWidth(353.dp)).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(stepWidth(352.dp)).isEqualTo(CameraLayoutMode.COMPACT_SLIM)

        assertThat(stepWidth(360.dp)).isEqualTo(CameraLayoutMode.COMPACT_SLIM)
        assertThat(stepWidth(366.dp)).isEqualTo(CameraLayoutMode.COMPACT_SLIM)
        assertThat(stepWidth(367.dp)).isEqualTo(CameraLayoutMode.COMPACT)
    }

    @Test
    fun isFullScreenCompactPhoneWindow_distinguishesFullScreenPhoneFromMultiWindowAndShortSplit() {
        assertThat(
            isFullScreenCompactPhoneWindow(
                widthDp = 360.dp,
                heightDp = 740.dp,
                smallestScreenWidthDp = 360,
                screenWidthDp = 360,
                screenHeightDp = 740,
                isInMultiWindowMode = false
            )
        ).isTrue()

        assertThat(
            isFullScreenCompactPhoneWindow(
                widthDp = 738.5.dp,
                heightDp = 361.5.dp,
                smallestScreenWidthDp = 360,
                screenWidthDp = 740,
                screenHeightDp = 360,
                isInMultiWindowMode = false
            )
        ).isTrue()

        assertThat(
            isFullScreenCompactPhoneWindow(
                widthDp = 360.dp,
                heightDp = 740.dp,
                smallestScreenWidthDp = 360,
                screenWidthDp = 360,
                screenHeightDp = 740,
                isInMultiWindowMode = true
            )
        ).isFalse()

        assertThat(
            isFullScreenCompactPhoneWindow(
                widthDp = 800.dp,
                heightDp = 400.dp,
                smallestScreenWidthDp = 600,
                screenWidthDp = 800,
                screenHeightDp = 400,
                isInMultiWindowMode = false
            )
        ).isFalse()

        assertThat(
            isFullScreenCompactPhoneWindow(
                widthDp = 360.dp,
                heightDp = 360.dp,
                smallestScreenWidthDp = 360,
                screenWidthDp = 360,
                screenHeightDp = 360,
                isInMultiWindowMode = false
            )
        ).isFalse()

        // Physical edge-to-edge phone where BoxWithConstraints includes system bar insets
        // (e.g., 412x915dp full window vs. 412x867dp Configuration.screenHeightDp).
        assertThat(
            isFullScreenCompactPhoneWindow(
                widthDp = 412.dp,
                heightDp = 915.dp,
                smallestScreenWidthDp = 412,
                screenWidthDp = 412,
                screenHeightDp = 867,
                isInMultiWindowMode = false,
                windowContainerWidthDp = 412.dp,
                windowContainerHeightDp = 915.dp
            )
        ).isTrue()

        // Sub-window pane inside an edge-to-edge phone window returns false.
        assertThat(
            isFullScreenCompactPhoneWindow(
                widthDp = 412.dp,
                heightDp = 600.dp,
                smallestScreenWidthDp = 412,
                screenWidthDp = 412,
                screenHeightDp = 867,
                isInMultiWindowMode = false,
                windowContainerWidthDp = 412.dp,
                windowContainerHeightDp = 915.dp
            )
        ).isFalse()

        // Smaller floating/dialog window matching windowContainer* but below screenWidthDp/HeightDp
        // returns false.
        assertThat(
            isFullScreenCompactPhoneWindow(
                widthDp = 360.dp,
                heightDp = 600.dp,
                smallestScreenWidthDp = 412,
                screenWidthDp = 412,
                screenHeightDp = 867,
                isInMultiWindowMode = false,
                windowContainerWidthDp = 360.dp,
                windowContainerHeightDp = 600.dp
            )
        ).isFalse()
    }
}
