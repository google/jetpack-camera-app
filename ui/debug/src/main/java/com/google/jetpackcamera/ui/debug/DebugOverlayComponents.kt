/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.jetpackcamera.ui.debug

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.MultiWindowModeChangedInfo
import androidx.core.util.Consumer
import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.model.TestPattern
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TAG = "DebugOverlayComponents"

@Composable
fun DebugDialogContainerToggle(toggleIsOpen: () -> Unit, modifier: Modifier = Modifier) {
    Button(modifier = modifier.testTag(DEBUG_OVERLAY_BUTTON), onClick = { toggleIsOpen() }) {
        Text(text = stringResource(R.string.debug_overlay_toggle_btn_text))
    }
}

@Composable
private fun LogicalCameraIdText(logicalCameraId: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DebugTextBar(
            title = stringResource(R.string.debug_text_logical_camera_id_prefix),
            value = logicalCameraId ?: "---",
            tag = LOGICAL_CAMERA_ID_TAG
        )
    }
}

@Composable
private fun PhysicalCameraIdText(physicalCameraId: String?) {
    DebugTextBar(
        title = stringResource(R.string.debug_text_physical_camera_id_prefix),
        value = physicalCameraId ?: "---",
        tag = PHYSICAL_CAMERA_ID_TAG
    )
}

@Composable
private fun ZoomRatioText(modifier: Modifier = Modifier, primaryZoomRatio: Float?) {
    DebugTextBar(
        modifier = modifier,
        title = "Zoom Ratio: ",
        value = stringResource(id = R.string.debug_zoom_ratio_text, primaryZoomRatio ?: 1f),
        tag = ZOOM_RATIO_TAG
    )
}

/**
 * Window form-factor bucket derived from available window container dimensions in `dp`, matching
 * the `360dp` / `480dp` / `600dp` adaptive capture layout mode classification.
 */
enum class DebugFormFactor {
    COMPACT_PORTRAIT,
    COMPACT_HORIZONTAL,
    COMPACT_SLIM,
    COMPACT_SHORT,
    COMPACT_SHORT_SLIM,
    TWO_SIDE_RAIL,
    UNKNOWN;

    companion object {
        internal const val SLIM_BREAKPOINT_DP = 360
        internal const val SHORT_BREAKPOINT_DP = 480
        internal const val MEDIUM_WIDTH_BREAKPOINT_DP = 600
        internal const val MIN_VALID_SMALLEST_SCREEN_WIDTH_DP = 1
        internal const val FULL_SCREEN_DIMENSION_TOLERANCE_DP = 2

        /**
         * Classifies a window container of [widthDp] by [heightDp] into a [DebugFormFactor]
         * bucket matching `CameraLayoutMode`'s breakpoint rules.
         *
         * @param widthDp Available capture container width in `dp`.
         * @param heightDp Available capture container height in `dp`.
         * @param isFullScreenCompactPhone Whether the container occupies a full-screen compact
         *   phone window (`sw < 600dp`, not in multi-window mode, with long edge `>= 480dp`).
         */
        fun fromWindowDp(
            widthDp: Int,
            heightDp: Int,
            isFullScreenCompactPhone: Boolean = false
        ): DebugFormFactor {
            if (widthDp <= 0 || heightDp <= 0) return UNKNOWN
            val minDim = minOf(widthDp, heightDp)
            if (isFullScreenCompactPhone) {
                return when {
                    minDim < SLIM_BREAKPOINT_DP -> COMPACT_SLIM
                    widthDp > heightDp -> COMPACT_HORIZONTAL
                    else -> COMPACT_PORTRAIT
                }
            }
            return when {
                widthDp >= MEDIUM_WIDTH_BREAKPOINT_DP && heightDp >= SHORT_BREAKPOINT_DP ->
                    TWO_SIDE_RAIL
                widthDp < SHORT_BREAKPOINT_DP && heightDp < SHORT_BREAKPOINT_DP ->
                    if (minDim < SLIM_BREAKPOINT_DP) {
                        COMPACT_SHORT_SLIM
                    } else {
                        COMPACT_SHORT
                    }
                minDim < SLIM_BREAKPOINT_DP -> COMPACT_SLIM
                heightDp < SHORT_BREAKPOINT_DP -> COMPACT_HORIZONTAL
                else -> COMPACT_PORTRAIT
            }
        }
    }
}

/**
 * Remembers the live [DebugFormFactor] for [widthDp] and [heightDp], factoring in whether the
 * host window is a full-screen compact phone (`sw < 600dp` and not in multi-window mode).
 */
@Composable
fun rememberDebugFormFactor(widthDp: Int, heightDp: Int): DebugFormFactor {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val activity = LocalActivity.current ?: remember(context) { context.findDebugActivity() }

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

    val isFullScreenCompactPhone =
        !(isInMultiWindowMode || activity?.isInMultiWindowMode == true) &&
            configuration.smallestScreenWidthDp in
            DebugFormFactor.MIN_VALID_SMALLEST_SCREEN_WIDTH_DP until
            DebugFormFactor.MEDIUM_WIDTH_BREAKPOINT_DP &&
            maxOf(widthDp, heightDp) >= DebugFormFactor.SHORT_BREAKPOINT_DP &&
            abs(widthDp - configuration.screenWidthDp) <=
            DebugFormFactor.FULL_SCREEN_DIMENSION_TOLERANCE_DP &&
            abs(heightDp - configuration.screenHeightDp) <=
            DebugFormFactor.FULL_SCREEN_DIMENSION_TOLERANCE_DP

    return DebugFormFactor.fromWindowDp(
        widthDp = widthDp,
        heightDp = heightDp,
        isFullScreenCompactPhone = isFullScreenCompactPhone
    )
}

private tailrec fun Context.findDebugActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findDebugActivity()
    else -> null
}

/**
 * Snapshot of window container dimensions, derived form factor, and rotation signals displayed by
 * the debug console's layout readout.
 *
 * When passed to [DebugOverlay], these explicit values override the live window-container fallback
 * used when layout readout state is omitted.
 *
 * @property widthDp Current window container width in density-independent pixels.
 * @property heightDp Current window container height in density-independent pixels.
 * @property formFactor Calculated [DebugFormFactor] for the window container dimensions.
 * @property deviceRotation Physical hardware orientation relative to gravity.
 * @property windowRotation OS window/display orientation relative to natural posture.
 * @property cameraLayoutMode Optional active orchestrator layout mode name (e.g.
 *   `COMPACT_PORTRAIT`, `COMPACT_HORIZONTAL`, `TWO_SIDE_RAIL`).
 */
@Immutable
data class DebugLayoutReadoutState(
    val widthDp: Int,
    val heightDp: Int,
    val formFactor: DebugFormFactor = DebugFormFactor.fromWindowDp(widthDp, heightDp),
    val deviceRotation: DeviceRotation = DeviceRotation.Natural,
    val windowRotation: DeviceRotation = DeviceRotation.Natural,
    val cameraLayoutMode: String? = null
) {
    /**
     * Clockwise rotation in degrees needed inside the current window so that icons and overlays
     * point upright against gravity (clamping `180°` to `0°`).
     */
    val iconRotationDegrees: Int
        get() = deviceRotation.toGravityRotationDegrees(windowRotation)

    /**
     * Clockwise rotation in degrees that the capture layout is expected to apply to align its
     * primary axis with the window's dimensions:
     * - [DebugFormFactor.TWO_SIDE_RAIL], [DebugFormFactor.COMPACT_PORTRAIT],
     *   [DebugFormFactor.COMPACT_SHORT], [DebugFormFactor.COMPACT_SHORT_SLIM], and
     *   [DebugFormFactor.UNKNOWN] remain upright (`0°`) in the window.
     * - [DebugFormFactor.COMPACT_HORIZONTAL] rotates `90°` or `270°` so the portrait layout aligns
     *   with the window's horizontal length.
     * - [DebugFormFactor.COMPACT_SLIM] stays `0°` when `heightDp >= widthDp` and rotates `90°` or
     *   `270°` when `widthDp > heightDp`.
     */
    val expectedLayoutRotationDegrees: Int
        get() = when (formFactor) {
            DebugFormFactor.TWO_SIDE_RAIL,
            DebugFormFactor.COMPACT_PORTRAIT,
            DebugFormFactor.COMPACT_SHORT,
            DebugFormFactor.COMPACT_SHORT_SLIM,
            DebugFormFactor.UNKNOWN -> 0
            DebugFormFactor.COMPACT_HORIZONTAL -> resolveHorizontalLayoutRotationDegrees()
            DebugFormFactor.COMPACT_SLIM ->
                if (heightDp >= widthDp) {
                    0
                } else {
                    resolveHorizontalLayoutRotationDegrees()
                }
        }

    private fun resolveHorizontalLayoutRotationDegrees(): Int {
        val gravity = iconRotationDegrees
        val counter =
            ((windowRotation.toWindowCounterRotationDegrees().toInt() % 360) + 360) % 360
        return when {
            gravity == 90 || gravity == 270 -> gravity
            counter == 90 || counter == 270 -> counter
            else -> DEFAULT_WIDE_COMPACT_LAYOUT_ROTATION_DEGREES
        }
    }

    companion object {
        private const val DEFAULT_WIDE_COMPACT_LAYOUT_ROTATION_DEGREES = 270
    }
}

/**
 * Displays the debug layout and rotation readout as individually tagged rows for window container
 * size, [DebugFormFactor], physical device rotation, OS window rotation, expected layout rotation,
 * and derived in-window icon rotation.
 *
 * When [state] is `null`, window dimensions, form factor, and window rotation are derived live from
 * [LocalWindowInfo], [LocalDensity], and [LocalView].
 *
 * @param state Explicit layout readout values to display, or `null` to use live window values.
 * @param modifier Modifier applied to the outer readout container.
 */
@Composable
internal fun DebugLayoutReadout(
    state: DebugLayoutReadoutState? = null,
    modifier: Modifier = Modifier
) {
    val resolvedState = if (state != null) {
        state
    } else {
        val view = LocalView.current
        val liveWindowRotation = DeviceRotation.fromSurfaceRotation(
            runCatching { view.display?.rotation }.getOrNull() ?: Surface.ROTATION_0
        )
        val (liveWidthDp, liveHeightDp) = with(LocalDensity.current) {
            val containerSize = LocalWindowInfo.current.containerSize
            containerSize.width.toDp().value.roundToInt() to
                containerSize.height.toDp().value.roundToInt()
        }
        val liveFormFactor = rememberDebugFormFactor(
            widthDp = liveWidthDp,
            heightDp = liveHeightDp
        )
        DebugLayoutReadoutState(
            widthDp = liveWidthDp,
            heightDp = liveHeightDp,
            formFactor = liveFormFactor,
            deviceRotation = liveWindowRotation,
            windowRotation = liveWindowRotation
        )
    }
    val formFactorText = stringResource(
        when (resolvedState.formFactor) {
            DebugFormFactor.COMPACT_PORTRAIT -> R.string.debug_text_form_factor_compact_portrait
            DebugFormFactor.COMPACT_HORIZONTAL -> R.string.debug_text_form_factor_compact_horizontal
            DebugFormFactor.COMPACT_SLIM -> R.string.debug_text_form_factor_compact_slim
            DebugFormFactor.COMPACT_SHORT -> R.string.debug_text_form_factor_compact_short
            DebugFormFactor.COMPACT_SHORT_SLIM -> R.string.debug_text_form_factor_compact_short_slim
            DebugFormFactor.TWO_SIDE_RAIL -> R.string.debug_text_form_factor_two_side_rail
            DebugFormFactor.UNKNOWN -> R.string.debug_text_form_factor_unknown
        }
    )
    Column(modifier = modifier.testTag(DEBUG_LAYOUT_READOUT_TAG)) {
        DebugTextBar(
            title = stringResource(R.string.debug_text_layout_prefix),
            value = stringResource(
                R.string.debug_text_window_size,
                resolvedState.widthDp,
                resolvedState.heightDp
            ),
            tag = DEBUG_WINDOW_SIZE_TAG
        )
        DebugTextBar(
            title = stringResource(R.string.debug_text_form_factor_prefix),
            value = formFactorText,
            tag = DEBUG_FORM_FACTOR_TAG
        )
        resolvedState.cameraLayoutMode?.let { mode ->
            DebugTextBar(
                title = stringResource(R.string.debug_text_layout_mode_prefix),
                value = mode,
                tag = DEBUG_LAYOUT_MODE_TAG
            )
        }
        DebugTextBar(
            title = stringResource(R.string.debug_text_device_rotation_prefix),
            value = stringResource(
                R.string.debug_text_rotation_degrees,
                resolvedState.deviceRotation.toClockwiseRotationDegrees()
            ),
            tag = DEBUG_DEVICE_ROTATION_TAG
        )
        DebugTextBar(
            title = stringResource(R.string.debug_text_window_rotation_prefix),
            value = stringResource(
                R.string.debug_text_rotation_degrees,
                resolvedState.windowRotation.toClockwiseRotationDegrees()
            ),
            tag = DEBUG_WINDOW_ROTATION_TAG
        )
        DebugTextBar(
            title = stringResource(R.string.debug_text_expected_layout_rotation_prefix),
            value = stringResource(
                R.string.debug_text_rotation_degrees,
                resolvedState.expectedLayoutRotationDegrees
            ),
            tag = DEBUG_EXPECTED_LAYOUT_ROTATION_TAG
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            DebugTextBar(
                title = stringResource(R.string.debug_text_icon_rotation_prefix),
                value = stringResource(
                    R.string.debug_text_rotation_degrees,
                    resolvedState.iconRotationDegrees
                ),
                tag = DEBUG_ICON_ROTATION_TAG
            )
            DebugUpwardTriangleIndicator(
                rotationDegrees = resolvedState.iconRotationDegrees,
                modifier = Modifier.testTag(DEBUG_ICON_ROTATION_INDICATOR_TAG)
            )
        }
    }
}

/**
 * Draws an upward-pointing triangle rotated clockwise by [rotationDegrees] so its apex continuously
 * points upward against physical gravity inside the current window.
 */
@Composable
private fun DebugUpwardTriangleIndicator(rotationDegrees: Int, modifier: Modifier = Modifier) {
    val targetAngle = rotationDegrees.toFloat()
    var cumulativeTargetDegrees by remember { mutableFloatStateOf(targetAngle) }
    val currentNormalized = ((cumulativeTargetDegrees % 360f) + 360f) % 360f
    var delta = targetAngle - currentNormalized
    if (delta > 180f) delta -= 360f
    if (delta <= -180f) delta += 360f
    if (delta != 0f) {
        cumulativeTargetDegrees += delta
    }
    val animatedDegrees = animateFloatAsState(
        targetValue = cumulativeTargetDegrees,
        animationSpec = tween(durationMillis = 300),
        label = "DebugUpwardTriangleRotation"
    )
    val triangleColor = LocalContentColor.current
    val rotationDescription = stringResource(R.string.debug_text_rotation_degrees, rotationDegrees)

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = .4f))
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .size(14.dp)
            .semantics {
                stateDescription = rotationDescription
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(10.dp)
                .graphicsLayer {
                    rotationZ = animatedDegrees.value
                }
        ) {
            val path = Path().apply {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path = path, color = triangleColor)
        }
    }
}

@Composable
private fun DebugTextBar(modifier: Modifier = Modifier, title: String, value: String, tag: String) {
    Row(modifier = modifier) {
        Text(modifier = Modifier.background(Color.Black.copy(alpha = .7f)), text = title)
        Text(
            modifier = Modifier
                .background(Color.Black.copy(alpha = .4f))
                .testTag(tag),
            text = value
        )
    }
}

@Composable
private fun ToggleVisibilityButton(
    modifier: Modifier = Modifier,
    onToggleHidingComponents: () -> Unit,
    isHidingComponents: Boolean
) {
    val stateDescption = if (isHidingComponents) {
        stringResource(id = R.string.debug_hide_components_desc)
    } else {
        stringResource(R.string.debug_show_components_desc)
    }

    IconButton(
        modifier = modifier
            .semantics {
                testTag = BTN_DEBUG_HIDE_COMPONENTS_TAG
                stateDescription = stateDescption
            },
        onClick = { onToggleHidingComponents() }
    ) {
        if (isHidingComponents) {
            Icon(
                painter = painterResource(R.drawable.ic_visibility_off),
                contentDescription = null,
                modifier = Modifier.alpha(.5f)
            )
        } else {
            Icon(painter = painterResource(R.drawable.ic_visibility), contentDescription = null)
        }
    }
}

/**
 * Renders the top-level debug console and, when open, the modal debug inspection dialogs over the
 * camera preview.
 *
 * When [DebugUiState.Enabled.debugHidingComponents] is `true`, only the visibility toggle button is
 * shown so the camera feed can be inspected unobstructed.
 *
 * @param modifier Modifier applied to the root overlay container.
 * @param onChangeZoomRatio Callback invoked when the user sets an explicit zoom ratio in the debug
 *   dialog.
 * @param debugUiState Current enabled debug UI state.
 * @param extraControls Additional debug action composables rendered alongside the debug menu
 *   button.
 * @param debugController Controller handling visibility, dialog, and test-pattern interactions.
 * @param layoutReadoutState Explicit adaptive layout posture readout state, or `null` to derive
 *   window dimensions from the current window container.
 */
@Composable
fun DebugOverlay(
    modifier: Modifier = Modifier,
    onChangeZoomRatio: (Float) -> Unit,
    debugUiState: DebugUiState.Enabled,
    vararg extraControls: @Composable () -> Unit,
    debugController: DebugController,
    layoutReadoutState: DebugLayoutReadoutState? = null
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(top = 100.dp)) {
            ToggleVisibilityButton(
                onToggleHidingComponents = debugController::toggleDebugHidingComponents,
                isHidingComponents = debugUiState.debugHidingComponents
            )
            if (!debugUiState.debugHidingComponents) {
                DebugConsole(
                    debugUiState = debugUiState,
                    onToggleDebugOverlay = debugController::toggleDebugOverlay,
                    extraControls = extraControls,
                    layoutReadoutState = layoutReadoutState
                )
            }
        }
        (debugUiState as? DebugUiState.Enabled.Open)?.let {
            if (!debugUiState.debugHidingComponents) {
                DebugDialogContainer(
                    modifier = Modifier,
                    onChangeZoomRatio = onChangeZoomRatio,
                    onSetTestPattern = debugController::setTestPattern,
                    toggleIsOpen = debugController::toggleDebugOverlay,
                    debugUiState = it
                )
            }
        }
    }
}

/**
 * A row of components visible at the top of the debug screen.
 * The first button will always be the [DebugDialogContainerToggle], followed by any components passed
 * into [extraControls].
 *
 * @param debugUiState  the current [DebugUiState.Enabled]
 * @param onToggleDebugOverlay a callback to open and hide the [DebugDialogContainer]
 * @param extraControls additional composable functions to be displayed in the debug top row.
 * These should NOT include components intended to be exclusive to the debug screen.
 * @param modifier modifier applied to the root console column.
 * @param layoutReadoutState explicit layout posture values for [DebugLayoutReadout], or `null` to
 *   derive window dimensions from the current window container.
 */
@Composable
private fun DebugConsole(
    debugUiState: DebugUiState.Enabled,
    onToggleDebugOverlay: () -> Unit,
    vararg extraControls: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    layoutReadoutState: DebugLayoutReadoutState? = null
) {
    Column(modifier = modifier) {
        FlowRow(
            verticalArrangement = Arrangement.Center,
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            // debug menu button
            DebugDialogContainerToggle(toggleIsOpen = onToggleDebugOverlay)
            extraControls.forEach { it() }
        }
        LogicalCameraIdText(debugUiState.currentLogicalCameraId)
        PhysicalCameraIdText(debugUiState.currentPhysicalCameraId)
        ZoomRatioText(
            modifier = Modifier,
            primaryZoomRatio = debugUiState.currentPrimaryZoomRatio
        )
        DebugLayoutReadout(state = layoutReadoutState)
    }
}

// Debug Dialogs

@Composable
private fun DebugDialogContainer(
    modifier: Modifier = Modifier,
    onChangeZoomRatio: (Float) -> Unit,
    onSetTestPattern: (TestPattern) -> Unit,
    toggleIsOpen: () -> Unit,
    debugUiState: DebugUiState.Enabled.Open
) {
    var selectedDialog by remember { mutableStateOf(SelectedDialog.None) }
    val backgroundColor = Color.Black.copy(
        alpha =
        when (selectedDialog) {
            SelectedDialog.None,
            SelectedDialog.SetTestPattern -> 0.7f

            else -> 0.9f
        }
    )

    BackHandler(onBack = { toggleIsOpen() })

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = backgroundColor)
    ) {
        AnimatedContent(
            modifier = Modifier.safeContentPadding(),
            targetState = selectedDialog,
            transitionSpec = { fadeIn() togetherWith fadeOut() using null }
        ) { dialog ->
            when (dialog) {
                SelectedDialog.None ->
                    DebugDialogOptionsMenuDialog(
                        debugUiState,
                        onMoveToComponent = { selectedDialog = it },
                        onClose = { toggleIsOpen() }
                    )

                SelectedDialog.CameraJSON ->
                    CameraPropertiesJSONDialog(debugUiState.cameraPropertiesJSON) {
                        selectedDialog = SelectedDialog.None
                    }

                SelectedDialog.SetZoom ->
                    SetZoomRatioDialog(onChangeZoomRatio) {
                        selectedDialog = SelectedDialog.None
                    }

                SelectedDialog.SetTestPattern ->
                    SetTestPatternDialog(
                        onSetTestPattern = onSetTestPattern,
                        selectedTestPattern = debugUiState.selectedTestPattern,
                        availableTestPatterns = debugUiState.availableTestPatterns,
                        onClose = { selectedDialog = SelectedDialog.None }
                    )
            }
        }
    }
}

@Composable
private fun DebugDialogOptionsMenuDialog(
    debugUiState: DebugUiState.Enabled.Open,
    onMoveToComponent: (SelectedDialog) -> Unit,
    onClose: () -> Unit
) {
    // Buttons
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .noIndicationClickable(onClick = onClose),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            Text("Video resolution: ")
            val size = debugUiState.videoResolution
            val videoResText = if (size == null) {
                "null"
            } else {
                "${size.width}x${size.height}"
            }
            Text(
                modifier = Modifier.testTag(
                    DEBUG_OVERLAY_VIDEO_RESOLUTION_TAG
                ),
                text = videoResText
            )
        }

        // show camera properties json button
        Button(
            modifier = Modifier.testTag(
                DEBUG_OVERLAY_SHOW_CAMERA_PROPERTIES_BUTTON
            ),
            onClick = {
                onMoveToComponent(SelectedDialog.CameraJSON)
            }
        ) {
            Text(text = "Show Camera Properties JSON")
        }

        // set zoom ratio
        Button(
            modifier = Modifier.testTag(
                DEBUG_OVERLAY_SET_ZOOM_RATIO_BUTTON
            ),
            onClick = {
                onMoveToComponent(SelectedDialog.SetZoom)
            }
        ) {
            Text(text = "Set Zoom Ratio")
        }

        // set test pattern
        Button(
            enabled = debugUiState.availableTestPatterns.size > 1,
            onClick = {
                onMoveToComponent(SelectedDialog.SetTestPattern)
            }
        ) {
            Text(text = "Set Test Pattern")
        }
    }
}

@Composable
private fun CameraPropertiesJSONDialog(cameraPropertiesJSON: String, onClose: () -> Unit) {
    BackHandler(onBack = { onClose() })
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = scrollState)
            .noIndicationClickable(onClick = onClose)
    ) {
        Text(
            modifier = Modifier.testTag(DEBUG_OVERLAY_CAMERA_PROPERTIES_TAG),
            text = cameraPropertiesJSON,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun SetZoomRatioDialog(onChangeZoomRatio: (Float) -> Unit, onClose: () -> Unit) {
    val zoomRatioText = remember { mutableStateOf("") }
    BackHandler(onBack = { onClose() })
    Box(
        modifier = Modifier
            .fillMaxSize()
            .noIndicationClickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Enter and confirm zoom ratio")
            TextField(
                modifier = Modifier.testTag(DEBUG_OVERLAY_SET_ZOOM_RATIO_TEXT_FIELD),
                value = zoomRatioText.value,
                onValueChange = { zoomRatioText.value = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Button(
                modifier = Modifier.testTag(
                    DEBUG_OVERLAY_SET_ZOOM_RATIO_SET_BUTTON
                ),
                onClick = {
                    try {
                        // no-op if confirmed with empty entry
                        if (zoomRatioText.value.isEmpty()) {
                            onClose()
                        } else {
                            val newRatio = zoomRatioText.value.toFloat()
                            onChangeZoomRatio(newRatio)
                        }
                    } catch (_: NumberFormatException) {
                        Log.d(TAG, "Zoom ratio should be a float")
                    }
                    onClose()
                }
            ) {
                Text(text = "Confirm")
            }
        }
    }
}

@Composable
private fun SetTestPatternDialog(
    onSetTestPattern: (TestPattern) -> Unit,
    selectedTestPattern: TestPattern,
    availableTestPatterns: Set<TestPattern>,
    onClose: () -> Unit
) {
    BackHandler(onBack = { onClose() })
    Column(
        modifier = Modifier
            .fillMaxSize()
            .noIndicationClickable(onClick = onClose)
    ) {
        Text(text = "Select test pattern")
        val sortedTestPatterns = remember(availableTestPatterns) {
            availableTestPatterns.sortedBy { if (it == TestPattern.Off) "" else it.toString() }
        }
        LazyColumn {
            items(sortedTestPatterns) { testPattern ->
                FilledIconToggleButton(
                    modifier = Modifier.fillMaxWidth(),
                    checked = testPattern == selectedTestPattern,
                    onCheckedChange = { selected ->
                        if (selected) {
                            onSetTestPattern(testPattern)
                        }
                    }
                ) {
                    Text("$testPattern")
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            TextButton(
                onClick = { onClose() }
            ) {
                Text(text = "Close")
            }
        }
    }
}

@Composable
private fun Modifier.noIndicationClickable(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick
)

private enum class SelectedDialog {
    None,
    CameraJSON,
    SetZoom,
    SetTestPattern
}

@Preview(name = "Compact", showBackground = true)
@Composable
private fun DebugLayoutReadoutCompactPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        DebugLayoutReadout(
            state = DebugLayoutReadoutState(
                widthDp = 412,
                heightDp = 915
            )
        )
    }
}

@Preview(name = "Foldable", showBackground = true)
@Composable
private fun DebugLayoutReadoutFoldablePreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        DebugLayoutReadout(
            state = DebugLayoutReadoutState(
                widthDp = 673,
                heightDp = 841
            )
        )
    }
}
