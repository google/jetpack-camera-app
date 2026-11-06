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
 * initial 6-mode classification, split-screen rotation pair consistency, full-screen compact phone
 * classification, and the `14.dp` (`±7.dp`) hysteresis deadbands around `360.dp` slim
 * (`[353.dp, 367.dp]`), `480.dp` short (`[473.dp, 487.dp]`), and `600.dp` wide
 * (`[593.dp, 607.dp]`).
 */
@RunWith(JUnit4::class)
class CameraLayoutModeTest {

    @Test
    fun classifyCameraLayoutMode_initialClassification_resolvesAllSixModes() {
        // COMPACT_PORTRAIT: width < 600dp, height >= 480dp, minDim >= 360dp
        assertThat(
            classifyCameraLayoutMode(widthDp = 412.dp, heightDp = 915.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT_PORTRAIT)
        assertThat(
            classifyCameraLayoutMode(widthDp = 599.dp, heightDp = 480.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT_PORTRAIT)

        // COMPACT_HORIZONTAL: width >= 480dp, height < 480dp, minDim >= 360dp
        assertThat(
            classifyCameraLayoutMode(widthDp = 800.dp, heightDp = 400.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT_HORIZONTAL)
        assertThat(
            classifyCameraLayoutMode(widthDp = 530.dp, heightDp = 412.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT_HORIZONTAL)

        // COMPACT_SLIM: minDim < 360dp, maxDim >= 480dp
        assertThat(
            classifyCameraLayoutMode(widthDp = 320.dp, heightDp = 640.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT_SLIM)
        assertThat(
            classifyCameraLayoutMode(widthDp = 640.dp, heightDp = 320.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT_SLIM)

        // COMPACT_SHORT: width < 480dp, height < 480dp, minDim >= 360dp
        assertThat(
            classifyCameraLayoutMode(widthDp = 412.dp, heightDp = 440.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT_SHORT)
        assertThat(
            classifyCameraLayoutMode(widthDp = 445.dp, heightDp = 412.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT_SHORT)
        assertThat(
            classifyCameraLayoutMode(widthDp = 360.dp, heightDp = 360.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT_SHORT)

        // COMPACT_SHORT_SLIM: width < 480dp, height < 480dp, minDim < 360dp
        assertThat(
            classifyCameraLayoutMode(widthDp = 320.dp, heightDp = 340.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT_SHORT_SLIM)
        assertThat(
            classifyCameraLayoutMode(widthDp = 340.dp, heightDp = 320.dp)
        ).isEqualTo(CameraLayoutMode.COMPACT_SHORT_SLIM)

        // TWO_SIDE_RAIL: width >= 600dp, height >= 480dp
        assertThat(
            classifyCameraLayoutMode(widthDp = 852.dp, heightDp = 883.dp)
        ).isEqualTo(CameraLayoutMode.TWO_SIDE_RAIL)
        assertThat(
            classifyCameraLayoutMode(widthDp = 600.dp, heightDp = 480.dp)
        ).isEqualTo(CameraLayoutMode.TWO_SIDE_RAIL)
    }

    @Test
    fun classifyCameraLayoutMode_splitScreenRotationPairs_preserveLayoutFamily() {
        // 60/40 compact split rotated: COMPACT_PORTRAIT <-> COMPACT_HORIZONTAL
        val portraitSplit = classifyCameraLayoutMode(widthDp = 412.dp, heightDp = 530.dp)
        val landscapeSplit = classifyCameraLayoutMode(
            widthDp = 530.dp,
            heightDp = 412.dp,
            previousMode = portraitSplit
        )
        assertThat(portraitSplit).isEqualTo(CameraLayoutMode.COMPACT_PORTRAIT)
        assertThat(landscapeSplit).isEqualTo(CameraLayoutMode.COMPACT_HORIZONTAL)

        // 50/50 compact split rotated: stays COMPACT_SHORT
        val squarePortrait = classifyCameraLayoutMode(widthDp = 412.dp, heightDp = 445.dp)
        val squareLandscape = classifyCameraLayoutMode(
            widthDp = 445.dp,
            heightDp = 412.dp,
            previousMode = squarePortrait
        )
        assertThat(squarePortrait).isEqualTo(CameraLayoutMode.COMPACT_SHORT)
        assertThat(squareLandscape).isEqualTo(CameraLayoutMode.COMPACT_SHORT)

        // Slim split rotated: stays COMPACT_SLIM
        val slimPortrait = classifyCameraLayoutMode(widthDp = 320.dp, heightDp = 640.dp)
        val slimLandscape = classifyCameraLayoutMode(
            widthDp = 640.dp,
            heightDp = 320.dp,
            previousMode = slimPortrait
        )
        assertThat(slimPortrait).isEqualTo(CameraLayoutMode.COMPACT_SLIM)
        assertThat(slimLandscape).isEqualTo(CameraLayoutMode.COMPACT_SLIM)
    }

    @Test
    fun classifyCameraLayoutMode_fullScreenCompactPhone_resolvesPortraitHorizontalOrSlim() {
        assertThat(
            classifyCameraLayoutMode(
                widthDp = 360.dp,
                heightDp = 740.dp,
                isFullScreenCompactPhone = true
            )
        ).isEqualTo(CameraLayoutMode.COMPACT_PORTRAIT)

        assertThat(
            classifyCameraLayoutMode(
                widthDp = 740.dp,
                heightDp = 360.dp,
                isFullScreenCompactPhone = true
            )
        ).isEqualTo(CameraLayoutMode.COMPACT_HORIZONTAL)

        assertThat(
            classifyCameraLayoutMode(
                widthDp = 915.dp,
                heightDp = 500.dp,
                isFullScreenCompactPhone = true
            )
        ).isEqualTo(CameraLayoutMode.COMPACT_HORIZONTAL)

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

        // Expanding from compact: stays COMPACT_PORTRAIT inside [593dp, 606dp], enters at >= 607dp.
        assertThat(stepWidth(580.dp)).isEqualTo(CameraLayoutMode.COMPACT_PORTRAIT)
        assertThat(stepWidth(600.dp)).isEqualTo(CameraLayoutMode.COMPACT_PORTRAIT)
        assertThat(stepWidth(604.dp)).isEqualTo(CameraLayoutMode.COMPACT_PORTRAIT)
        assertThat(stepWidth(606.dp)).isEqualTo(CameraLayoutMode.COMPACT_PORTRAIT)
        assertThat(stepWidth(608.dp)).isEqualTo(CameraLayoutMode.TWO_SIDE_RAIL)

        // Shrinking from wide: stays TWO_SIDE_RAIL down to 593dp, exits below 593dp.
        assertThat(stepWidth(600.dp)).isEqualTo(CameraLayoutMode.TWO_SIDE_RAIL)
        assertThat(stepWidth(595.dp)).isEqualTo(CameraLayoutMode.TWO_SIDE_RAIL)
        assertThat(stepWidth(594.dp)).isEqualTo(CameraLayoutMode.TWO_SIDE_RAIL)
        assertThat(stepWidth(593.dp)).isEqualTo(CameraLayoutMode.TWO_SIDE_RAIL)
        assertThat(stepWidth(592.dp)).isEqualTo(CameraLayoutMode.COMPACT_PORTRAIT)
    }

    @Test
    fun classifyCameraLayoutMode_widthHysteresisSweep_shortHeight_compactShortToHorizontal() {
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

        // Expanding across 480dp width while short: [473dp, 487dp] deadband
        assertThat(stepWidth(460.dp)).isEqualTo(CameraLayoutMode.COMPACT_SHORT)
        assertThat(stepWidth(480.dp)).isEqualTo(CameraLayoutMode.COMPACT_SHORT)
        assertThat(stepWidth(486.dp)).isEqualTo(CameraLayoutMode.COMPACT_SHORT)
        assertThat(stepWidth(487.dp)).isEqualTo(CameraLayoutMode.COMPACT_HORIZONTAL)

        assertThat(stepWidth(480.dp)).isEqualTo(CameraLayoutMode.COMPACT_HORIZONTAL)
        assertThat(stepWidth(474.dp)).isEqualTo(CameraLayoutMode.COMPACT_HORIZONTAL)
        assertThat(stepWidth(473.dp)).isEqualTo(CameraLayoutMode.COMPACT_HORIZONTAL)
        assertThat(stepWidth(472.dp)).isEqualTo(CameraLayoutMode.COMPACT_SHORT)
    }

    @Test
    fun classifyCameraLayoutMode_heightHysteresisSweep_respects14dpDeadbandAround480dp_wide() {
        var mode: CameraLayoutMode? = null
        fun stepHeight(height: Dp): CameraLayoutMode {
            mode = classifyCameraLayoutMode(
                widthDp = 800.dp,
                heightDp = height,
                isFullScreenCompactPhone = false,
                previousMode = mode
            )
            return checkNotNull(mode)
        }

        // Shrinking height from tall: stays TWO_SIDE_RAIL down to 473dp, exits below 473dp.
        assertThat(stepHeight(500.dp)).isEqualTo(CameraLayoutMode.TWO_SIDE_RAIL)
        assertThat(stepHeight(480.dp)).isEqualTo(CameraLayoutMode.TWO_SIDE_RAIL)
        assertThat(stepHeight(474.dp)).isEqualTo(CameraLayoutMode.TWO_SIDE_RAIL)
        assertThat(stepHeight(473.dp)).isEqualTo(CameraLayoutMode.TWO_SIDE_RAIL)
        assertThat(stepHeight(472.dp)).isEqualTo(CameraLayoutMode.COMPACT_HORIZONTAL)

        // Growing height from short: stays COMPACT_HORIZONTAL up to 486dp, enters at >= 487dp.
        assertThat(stepHeight(480.dp)).isEqualTo(CameraLayoutMode.COMPACT_HORIZONTAL)
        assertThat(stepHeight(486.dp)).isEqualTo(CameraLayoutMode.COMPACT_HORIZONTAL)
        assertThat(stepHeight(488.dp)).isEqualTo(CameraLayoutMode.TWO_SIDE_RAIL)
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

        assertThat(stepWidth(380.dp)).isEqualTo(CameraLayoutMode.COMPACT_PORTRAIT)
        assertThat(stepWidth(360.dp)).isEqualTo(CameraLayoutMode.COMPACT_PORTRAIT)
        assertThat(stepWidth(354.dp)).isEqualTo(CameraLayoutMode.COMPACT_PORTRAIT)
        assertThat(stepWidth(353.dp)).isEqualTo(CameraLayoutMode.COMPACT_PORTRAIT)
        assertThat(stepWidth(352.dp)).isEqualTo(CameraLayoutMode.COMPACT_SLIM)

        assertThat(stepWidth(360.dp)).isEqualTo(CameraLayoutMode.COMPACT_SLIM)
        assertThat(stepWidth(366.dp)).isEqualTo(CameraLayoutMode.COMPACT_SLIM)
        assertThat(stepWidth(367.dp)).isEqualTo(CameraLayoutMode.COMPACT_PORTRAIT)
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
    }
}
