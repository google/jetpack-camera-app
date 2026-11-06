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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.MultiWindowModeChangedInfo
import androidx.core.util.Consumer
import kotlin.math.abs

private object CameraLayoutModeTokens {
    val SlimDimensionBreakpoint = 360.dp
    val ShortDimensionBreakpoint = 480.dp
    val MediumWidthBreakpoint = 600.dp
    val HysteresisHalfBand = 7.dp

    val EnterSlimThreshold = SlimDimensionBreakpoint - HysteresisHalfBand
    val ExitSlimThreshold = SlimDimensionBreakpoint + HysteresisHalfBand

    val EnterAtLeast480Threshold = ShortDimensionBreakpoint + HysteresisHalfBand
    val ExitAtLeast480Threshold = ShortDimensionBreakpoint - HysteresisHalfBand

    val EnterWide600Threshold = MediumWidthBreakpoint + HysteresisHalfBand
    val ExitWide600Threshold = MediumWidthBreakpoint - HysteresisHalfBand

    val MinFullScreenCompactLongEdge = 480.dp

    const val MIN_VALID_SMALLEST_SCREEN_WIDTH_DP = 1
    const val COMPACT_SMALLEST_SCREEN_WIDTH_UPPER_BOUND_DP = 600
    const val FULL_SCREEN_DIMENSION_TOLERANCE_DP = 2f
}

/**
 * Adaptive layout classification for the camera capture screen based on available window width,
 * height, and multi-window posture.
 */
enum class CameraLayoutMode {
    /**
     * Upright single-column bottom control stack used on full-screen portrait compact phones and
     * portrait-shaped multi-window panes (`widthDp < 600.dp`, `heightDp >= 480.dp`, and
     * `minOf(widthDp, heightDp) >= 360.dp`).
     */
    COMPACT_PORTRAIT,

    /**
     * Horizontally oriented (`90°`-rotated) compact layout used when a window is short vertically
     * (`heightDp < 480.dp`) with sufficient horizontal span (`widthDp >= 480.dp` and
     * `minOf(widthDp, heightDp) >= 360.dp`), or when a full-screen compact phone window is wider
     * than tall.
     */
    COMPACT_HORIZONTAL,

    /**
     * Narrow/slim compact posture (`minOf(widthDp, heightDp) < 360.dp` and
     * `maxOf(widthDp, heightDp) >= 480.dp`, e.g., slim outer foldable displays or 1/3 split-screen
     * panes). Currently delegates to `CompactPortraitLayout` while providing a dedicated mode for
     * future component hiding/compaction.
     */
    COMPACT_SLIM,

    /**
     * Compact, short-on-both-axes posture (`widthDp < 480.dp` and `heightDp < 480.dp` with
     * `minOf(widthDp, heightDp) >= 360.dp`, e.g., 50/50 phone split-screen panes or clamshell cover
     * screens).
     */
    COMPACT_SHORT,

    /**
     * Ultra-compact slim and short posture (`widthDp < 480.dp`, `heightDp < 480.dp`, and
     * `minOf(widthDp, heightDp) < 360.dp`, e.g., small cover screens or compact freeform windows).
     */
    COMPACT_SHORT_SLIM,

    /**
     * Dual start and end vertical control rails over a full-bleed viewfinder used on unfolded
     * foldables and tablets (`widthDp >= 600.dp` and `heightDp >= 480.dp`).
     */
    TWO_SIDE_RAIL
}

/**
 * CompositionLocal providing the active [CameraLayoutMode] resolved by `PreviewLayout`.
 *
 * Defaults to [CameraLayoutMode.COMPACT_PORTRAIT] when read outside an adaptive orchestrator.
 */
val LocalCameraLayoutMode: ProvidableCompositionLocal<CameraLayoutMode> =
    compositionLocalOf { CameraLayoutMode.COMPACT_PORTRAIT }

/**
 * Classifies the capture window geometry into a [CameraLayoutMode], applying a `14.dp` (`±7.dp`)
 * hysteresis deadband around the `360.dp` slim (`[353.dp, 367.dp]`), `480.dp` short
 * (`[473.dp, 487.dp]`), and `600.dp` wide (`[593.dp, 607.dp]`) breakpoints to prevent layout
 * thrashing during live split-screen divider drags or desktop freeform window resizing.
 *
 * @param widthDp Available capture container width in `dp`.
 * @param heightDp Available capture container height in `dp`.
 * @param isFullScreenCompactPhone Whether the container occupies a full-screen compact phone
 *   window (`sw < 600dp`, not in multi-window mode, with long edge `>= 480.dp`).
 * @param previousMode Previously committed [CameraLayoutMode] used to bias threshold crossings
 *   across the hysteresis deadband, or `null` on initial classification.
 * @return The resolved [CameraLayoutMode] for the given window dimensions.
 */
fun classifyCameraLayoutMode(
    widthDp: Dp,
    heightDp: Dp,
    isFullScreenCompactPhone: Boolean = false,
    previousMode: CameraLayoutMode? = null
): CameraLayoutMode {
    val minDim = minOf(widthDp, heightDp)
    val isSlim = when (previousMode) {
        null -> minDim < CameraLayoutModeTokens.SlimDimensionBreakpoint
        CameraLayoutMode.COMPACT_SLIM,
        CameraLayoutMode.COMPACT_SHORT_SLIM ->
            minDim < CameraLayoutModeTokens.ExitSlimThreshold
        CameraLayoutMode.COMPACT_PORTRAIT,
        CameraLayoutMode.COMPACT_HORIZONTAL,
        CameraLayoutMode.COMPACT_SHORT,
        CameraLayoutMode.TWO_SIDE_RAIL ->
            minDim < CameraLayoutModeTokens.EnterSlimThreshold
    }

    if (isFullScreenCompactPhone) {
        return when {
            isSlim -> CameraLayoutMode.COMPACT_SLIM
            widthDp > heightDp -> CameraLayoutMode.COMPACT_HORIZONTAL
            else -> CameraLayoutMode.COMPACT_PORTRAIT
        }
    }

    val isWide600 = when (previousMode) {
        null -> widthDp >= CameraLayoutModeTokens.MediumWidthBreakpoint
        CameraLayoutMode.TWO_SIDE_RAIL -> widthDp >= CameraLayoutModeTokens.ExitWide600Threshold
        CameraLayoutMode.COMPACT_PORTRAIT,
        CameraLayoutMode.COMPACT_HORIZONTAL,
        CameraLayoutMode.COMPACT_SLIM,
        CameraLayoutMode.COMPACT_SHORT,
        CameraLayoutMode.COMPACT_SHORT_SLIM ->
            widthDp >= CameraLayoutModeTokens.EnterWide600Threshold
    }

    val isWidthAtLeast480 = when (previousMode) {
        null -> widthDp >= CameraLayoutModeTokens.ShortDimensionBreakpoint
        CameraLayoutMode.COMPACT_HORIZONTAL,
        CameraLayoutMode.TWO_SIDE_RAIL ->
            widthDp >= CameraLayoutModeTokens.ExitAtLeast480Threshold
        CameraLayoutMode.COMPACT_SHORT,
        CameraLayoutMode.COMPACT_SHORT_SLIM,
        CameraLayoutMode.COMPACT_PORTRAIT ->
            widthDp >= CameraLayoutModeTokens.EnterAtLeast480Threshold
        CameraLayoutMode.COMPACT_SLIM ->
            if (widthDp >= heightDp) {
                widthDp >= CameraLayoutModeTokens.ExitAtLeast480Threshold
            } else {
                widthDp >= CameraLayoutModeTokens.EnterAtLeast480Threshold
            }
    }

    val isHeightAtLeast480 = when (previousMode) {
        null -> heightDp >= CameraLayoutModeTokens.ShortDimensionBreakpoint
        CameraLayoutMode.COMPACT_PORTRAIT,
        CameraLayoutMode.TWO_SIDE_RAIL ->
            heightDp >= CameraLayoutModeTokens.ExitAtLeast480Threshold
        CameraLayoutMode.COMPACT_HORIZONTAL,
        CameraLayoutMode.COMPACT_SHORT,
        CameraLayoutMode.COMPACT_SHORT_SLIM ->
            heightDp >= CameraLayoutModeTokens.EnterAtLeast480Threshold
        CameraLayoutMode.COMPACT_SLIM ->
            if (heightDp >= widthDp) {
                heightDp >= CameraLayoutModeTokens.ExitAtLeast480Threshold
            } else {
                heightDp >= CameraLayoutModeTokens.EnterAtLeast480Threshold
            }
    }

    return when {
        isWide600 && isHeightAtLeast480 -> CameraLayoutMode.TWO_SIDE_RAIL
        !isWidthAtLeast480 && !isHeightAtLeast480 ->
            if (isSlim) {
                CameraLayoutMode.COMPACT_SHORT_SLIM
            } else {
                CameraLayoutMode.COMPACT_SHORT
            }
        isSlim -> CameraLayoutMode.COMPACT_SLIM
        !isHeightAtLeast480 -> CameraLayoutMode.COMPACT_HORIZONTAL
        else -> CameraLayoutMode.COMPACT_PORTRAIT
    }
}

/**
 * Determines whether a capture container represents a full-screen compact phone window.
 *
 * Returns `false` when the activity is in multi-window mode, when the device's smallest screen
 * width is outside `1 until 600` (tablets and unfolded foldables), when both container edges are
 * shorter than `480.dp` (such as a `360x360dp` clamshell cover display), or when the container
 * bounds differ from the full screen configuration by more than `2.dp` (such as sub-window panes
 * or dynamic resize containers).
 */
@VisibleForTesting
internal fun isFullScreenCompactPhoneWindow(
    widthDp: Dp,
    heightDp: Dp,
    smallestScreenWidthDp: Int,
    screenWidthDp: Int,
    screenHeightDp: Int,
    isInMultiWindowMode: Boolean
): Boolean {
    if (isInMultiWindowMode) return false
    if (
        smallestScreenWidthDp !in CameraLayoutModeTokens.MIN_VALID_SMALLEST_SCREEN_WIDTH_DP until
        CameraLayoutModeTokens.COMPACT_SMALLEST_SCREEN_WIDTH_UPPER_BOUND_DP
    ) {
        return false
    }
    if (maxOf(widthDp, heightDp) < CameraLayoutModeTokens.MinFullScreenCompactLongEdge) {
        return false
    }
    return abs(widthDp.value - screenWidthDp) <=
        CameraLayoutModeTokens.FULL_SCREEN_DIMENSION_TOLERANCE_DP &&
        abs(heightDp.value - screenHeightDp) <=
        CameraLayoutModeTokens.FULL_SCREEN_DIMENSION_TOLERANCE_DP
}

/**
 * Remembers whether the current Compose container occupies a full-screen compact phone window,
 * observing `LocalConfiguration` and `Activity.isInMultiWindowMode` (API 24+).
 */
@Composable
internal fun rememberIsFullScreenCompactPhone(widthDp: Dp, heightDp: Dp): Boolean {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val activity = LocalActivity.current ?: remember(context) { context.findActivity() }

    var isInMultiWindowMode by remember(activity) {
        mutableStateOf(activity?.isInMultiWindowMode ?: false)
    }
    DisposableEffect(activity) {
        isInMultiWindowMode = activity?.isInMultiWindowMode ?: false
        val componentActivity = activity as? ComponentActivity
        val listener = Consumer<MultiWindowModeChangedInfo> { info ->
            isInMultiWindowMode = info.isInMultiWindowMode
        }
        componentActivity?.addOnMultiWindowModeChangedListener(listener)
        onDispose {
            componentActivity?.removeOnMultiWindowModeChangedListener(listener)
        }
    }

    return isFullScreenCompactPhoneWindow(
        widthDp = widthDp,
        heightDp = heightDp,
        smallestScreenWidthDp = configuration.smallestScreenWidthDp,
        screenWidthDp = configuration.screenWidthDp,
        screenHeightDp = configuration.screenHeightDp,
        isInMultiWindowMode = isInMultiWindowMode || (activity?.isInMultiWindowMode == true)
    )
}

/**
 * Remembers the active [CameraLayoutMode] for [widthDp] and [heightDp], persisting the last
 * committed mode across recompositions via [SideEffect] so the `14.dp` (`±7.dp`) hysteresis
 * deadband prevents rapid oscillation near the `360.dp`, `480.dp`, and `600.dp` breakpoints.
 *
 * @param widthDp Available capture container width in `dp`.
 * @param heightDp Available capture container height in `dp`.
 * @param isFullScreenCompactPhone Whether the container occupies a full-screen compact phone
 *   window; defaults to [rememberIsFullScreenCompactPhone].
 * @return The hysteresis-stabilized [CameraLayoutMode].
 */
@Composable
fun rememberCameraLayoutMode(
    widthDp: Dp,
    heightDp: Dp,
    isFullScreenCompactPhone: Boolean = rememberIsFullScreenCompactPhone(widthDp, heightDp)
): CameraLayoutMode {
    val previousModeHolder = remember { PreviousLayoutModeHolder() }
    val resolvedMode = classifyCameraLayoutMode(
        widthDp = widthDp,
        heightDp = heightDp,
        isFullScreenCompactPhone = isFullScreenCompactPhone,
        previousMode = previousModeHolder.mode
    )
    SideEffect {
        previousModeHolder.mode = resolvedMode
    }
    return resolvedMode
}

private class PreviousLayoutModeHolder(var mode: CameraLayoutMode? = null)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
