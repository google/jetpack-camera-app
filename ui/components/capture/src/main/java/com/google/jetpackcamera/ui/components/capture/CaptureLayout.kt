/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.google.jetpackcamera.ui.components.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutMode
import com.google.jetpackcamera.ui.components.capture.layout.LocalCameraLayoutMode
import com.google.jetpackcamera.ui.components.capture.layout.rememberCameraLayoutMode
import com.google.jetpackcamera.ui.components.capture.layout.rememberIsFullScreenCompactPhone

private object PreviewLayoutTokens {
    const val PREVIEW_VIEWFINDER_ASPECT_RATIO = 3f / 4f
    val PreviewCaptureButtonSize = 80.dp
    val PreviewControlSize = 48.dp
    val PreviewElapsedHeight = 24.dp
    const val PREVIEW_HALF_WIDTH_FRACTION = 0.5f
    val PreviewWideShortWidth = 800.dp
    val PreviewWideShortHeight = 400.dp
}

/**
 * The top edge of the visible viewfinder, reported by a viewfinder that fills its slot and offsets
 * its own frame (such as [PreviewDisplay]).
 *
 * [PreviewLayout] reads this line to anchor viewfinder-relative content when the viewfinder slot
 * itself starts at the top of the window. A viewfinder that does not report the line is treated
 * as starting at its own top edge.
 */
internal val ViewfinderTopLine = HorizontalAlignmentLine { a, b -> minOf(a, b) }

/**
 * Adaptive orchestrator layout for the camera capture screen.
 *
 * Evaluates the available container geometry via [rememberCameraLayoutMode] with a `14.dp`
 * (`±7.dp`) hysteresis deadband around `600.dp` width and `480.dp` height, provides the resolved
 * [CameraLayoutMode] via [LocalCameraLayoutMode], wraps [viewfinder] in [movableContentOf] so the
 * live camera surface is reparented across layout transitions without disposal, and delegates to
 * [CompactPortraitLayout], [TwoSideRailLayout], or [WideShortLayout].
 *
 * @param modifier the modifier for this component
 * @param scaffoldState the bottom sheet scaffold state
 * @param onDismissQuickSettings callback to dismiss quick settings when clicking the drag handle
 *   or scrim
 * @param viewfinder the viewfinder composable
 * @param captureButton the capture button composable
 * @param imageWell the image well composable
 * @param flipCameraButton the flip camera button composable
 * @param zoomLevelDisplay the zoom level display composable
 * @param elapsedTimeDisplay the elapsed time display composable
 * @param quickSettingsButton the quick settings button composable
 * @param indicatorRow the indicator row composable
 * @param captureModeToggle the capture mode toggle composable
 * @param captureModeCarousel content for the fixed-height slot reserved between the shutter row
 * and the lower controls row. The slot is always reserved, so supplying or omitting content here
 * never moves the surrounding controls. The content is centered on the slot and may be taller than
 * it, for example to provide a larger touch target, extending equally into the gaps above and
 * below the slot without overlapping the neighboring rows.
 * @param quickSettingsOverlay the quick settings overlay composable
 * @param debugOverlay the debug overlay composable
 * @param debugVisibilityWrapper A wrapper that conditionally hides its contents based on debug
 * settings
 * @param screenFlashOverlay the screen flash overlay composable, drawn above all other content
 *   (including the quick settings sheet) and across the entire window, including the system bar
 *   and display cutout areas
 * @param snackBar the snack bar composable for showing messages
 * @param topStartContent optional composable content placed at the top-start of the capture
 * screen, at a fixed position directly below the top status/indicator bar. Because the viewfinder
 * can move below this content, the content receives a [PreviewOverlaySlotScope] that reports
 * whether it is currently over the viewfinder.
 * @param viewfinderTopEndContent optional composable content anchored to the top-end corner of the
 * viewfinder. While [topStartContent] is inside the viewfinder, this content shares its vertical
 * origin directly below the top bar, so both top slots stay aligned. When the viewfinder shifts
 * below [topStartContent], this content moves down with the top edge of the viewfinder while
 * [topStartContent] stays in place. Like [topStartContent], this content receives a
 * [PreviewOverlaySlotScope].
 * @param topBarTrailingContent optional composable content aligned to the end of the top
 * status/indicator bar
 * @param onRequiresImmersiveChange invoked with `true` when the window is too short for the
 * tallest viewfinder to clear a persistent navigation bar, and with `false` when it no longer is
 * or when the adaptive portrait layout leaves the composition. The owner of the system bars should
 * hide the navigation bar while this is `true`, for example by passing
 * [CameraSystemBarsPolicy.HideStatusAndNavigationBars] to [CameraSystemBarsEffect]. This layout
 * does not change the system bars itself.
 * @param errorDialog the camera error alert dialog composable
 * @param slots optional [CaptureSlots] slot table for adaptive layouts; when `null`,
 *   [CameraLayoutMode.COMPACT_PORTRAIT] uses the individual control composables and side-rail
 *   modes fall back to [PlaceholderCaptureSlots] bridged with [snackBar] and [errorDialog].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewLayout(
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState = rememberCompactPortraitScaffoldState(),
    onDismissQuickSettings: () -> Unit = {},
    viewfinder: @Composable (Modifier) -> Unit,
    captureButton: @Composable (Modifier) -> Unit,
    imageWell: @Composable (Modifier) -> Unit,
    flipCameraButton: @Composable (Modifier) -> Unit,
    zoomLevelDisplay: @Composable (Modifier) -> Unit,
    elapsedTimeDisplay: @Composable (Modifier) -> Unit,
    quickSettingsButton: @Composable (Modifier) -> Unit,
    indicatorRow: @Composable (Modifier) -> Unit,
    captureModeToggle: @Composable (Modifier) -> Unit,
    captureModeCarousel: @Composable (Modifier) -> Unit = {},
    quickSettingsOverlay: @Composable (Modifier) -> Unit,
    debugOverlay: @Composable (Modifier) -> Unit,
    debugVisibilityWrapper: (@Composable (@Composable () -> Unit) -> Unit),
    screenFlashOverlay: @Composable (Modifier) -> Unit,
    snackBar: @Composable (Modifier, snackbarHostState: SnackbarHostState) -> Unit,
    topStartContent: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    viewfinderTopEndContent: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    topBarTrailingContent: @Composable (Modifier) -> Unit = {},
    onRequiresImmersiveChange: (Boolean) -> Unit = {},
    errorDialog: @Composable (Modifier) -> Unit = {},
    slots: CaptureSlots? = null,
    deviceRotation: DeviceRotation = DeviceRotation.Natural,
    windowRotation: DeviceRotation = rememberWindowRotation()
) {
    val movableViewfinder = remember(viewfinder) {
        movableContentOf<Modifier> { vfModifier ->
            viewfinder(vfModifier)
        }
    }
    val gravityRotationState = rememberGravityRotationState(
        deviceRotation = deviceRotation,
        windowRotation = windowRotation
    )

    CompositionLocalProvider(
        LocalDeviceRotation provides deviceRotation,
        LocalWindowRotation provides windowRotation,
        LocalGravityRotationState provides gravityRotationState
    ) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val isFullScreenCompactPhone = rememberIsFullScreenCompactPhone(maxWidth, maxHeight)
            val layoutMode = rememberCameraLayoutMode(
                widthDp = maxWidth,
                heightDp = maxHeight,
                isFullScreenCompactPhone = isFullScreenCompactPhone
            )
            val isQuickSettingsOpen = scaffoldState.bottomSheetState.isVisible ||
                scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded
            val isCompactPortraitBacked = layoutMode == CameraLayoutMode.COMPACT_PORTRAIT ||
                layoutMode == CameraLayoutMode.COMPACT_HORIZONTAL ||
                layoutMode == CameraLayoutMode.COMPACT_SLIM
            val modeGravityRotationState =
                if (isCompactPortraitBacked) {
                    gravityRotationState
                } else {
                    DefaultGravityRotationState
                }

            CompositionLocalProvider(
                LocalCameraLayoutMode provides layoutMode,
                LocalGravityRotationState provides modeGravityRotationState
            ) {
                when (layoutMode) {
                    CameraLayoutMode.COMPACT_PORTRAIT,
                    CameraLayoutMode.COMPACT_HORIZONTAL,
                    CameraLayoutMode.COMPACT_SLIM -> {
                        if (slots != null) {
                            CompactPortraitLayout(
                                slots = slots,
                                viewfinder = movableViewfinder,
                                scaffoldState = scaffoldState,
                                includeStatusBarsInTopBar = !isFullScreenCompactPhone,
                                onDismissQuickSettings = onDismissQuickSettings,
                                onRequiresImmersiveChange = onRequiresImmersiveChange,
                                debugOverlay = debugOverlay,
                                debugVisibilityWrapper = debugVisibilityWrapper,
                                screenFlashOverlay = screenFlashOverlay,
                                deviceRotation = deviceRotation,
                                windowRotation = windowRotation
                            )
                        } else {
                            CompactPortraitLayout(
                                scaffoldState = scaffoldState,
                                onDismissQuickSettings = onDismissQuickSettings,
                                includeStatusBarsInTopBar = !isFullScreenCompactPhone,
                                viewfinder = movableViewfinder,
                                captureButton = captureButton,
                                imageWell = imageWell,
                                flipCameraButton = flipCameraButton,
                                zoomLevelDisplay = zoomLevelDisplay,
                                elapsedTimeDisplay = elapsedTimeDisplay,
                                quickSettingsButton = quickSettingsButton,
                                indicatorRow = indicatorRow,
                                captureModeToggle = captureModeToggle,
                                captureModeCarousel = captureModeCarousel,
                                quickSettingsOverlay = quickSettingsOverlay,
                                debugOverlay = debugOverlay,
                                debugVisibilityWrapper = debugVisibilityWrapper,
                                screenFlashOverlay = screenFlashOverlay,
                                snackBar = snackBar,
                                topStartContent = topStartContent,
                                viewfinderTopEndContent = viewfinderTopEndContent,
                                topBarTrailingContent = topBarTrailingContent,
                                onRequiresImmersiveChange = onRequiresImmersiveChange,
                                errorDialog = errorDialog,
                                deviceRotation = deviceRotation,
                                windowRotation = windowRotation
                            )
                        }
                    }

                    CameraLayoutMode.TWO_SIDE_RAIL -> {
                        val effectiveSlots = slots ?: remember(snackBar, errorDialog) {
                            PlaceholderCaptureSlots.default().copy(
                                snackBar = snackBar,
                                errorDialog = errorDialog
                            )
                        }
                        TwoSideRailLayout(
                            slots = effectiveSlots,
                            viewfinder = movableViewfinder,
                            snackbarHostState = scaffoldState.snackbarHostState,
                            isQuickSettingsOpen = isQuickSettingsOpen,
                            onDismissQuickSettings = onDismissQuickSettings,
                            debugOverlay = debugOverlay,
                            debugVisibilityWrapper = debugVisibilityWrapper,
                            screenFlashOverlay = screenFlashOverlay
                        )
                    }

                    CameraLayoutMode.COMPACT_SHORT,
                    CameraLayoutMode.COMPACT_SHORT_SLIM -> {
                        val effectiveSlots = slots ?: remember(snackBar, errorDialog) {
                            PlaceholderCaptureSlots.sideRail(isCompactHeight = true).copy(
                                snackBar = snackBar,
                                errorDialog = errorDialog
                            )
                        }
                        WideShortLayout(
                            slots = effectiveSlots,
                            viewfinder = movableViewfinder,
                            snackbarHostState = scaffoldState.snackbarHostState,
                            isQuickSettingsOpen = isQuickSettingsOpen,
                            onDismissQuickSettings = onDismissQuickSettings,
                            debugOverlay = debugOverlay,
                            debugVisibilityWrapper = debugVisibilityWrapper,
                            screenFlashOverlay = screenFlashOverlay
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdaptivePreviewLayoutSample(
    modifier: Modifier = Modifier,
    slots: CaptureSlots? = null
) {
    MaterialTheme {
        PreviewLayout(
            modifier = modifier.background(Color.Black),
            viewfinder = { vfModifier ->
                if (slots != null) {
                    PlaceholderViewfinder(modifier = vfModifier)
                } else {
                    Box(
                        modifier = vfModifier
                            .fillMaxWidth()
                            .aspectRatio(PreviewLayoutTokens.PREVIEW_VIEWFINDER_ASPECT_RATIO)
                            .background(Color.DarkGray)
                    )
                }
            },
            captureButton = { btnModifier ->
                Box(
                    modifier = btnModifier
                        .size(PreviewLayoutTokens.PreviewCaptureButtonSize)
                        .background(Color.White)
                )
            },
            flipCameraButton = { btnModifier ->
                Box(
                    modifier = btnModifier
                        .size(PreviewLayoutTokens.PreviewControlSize)
                        .background(Color.Cyan)
                )
            },
            imageWell = { wellModifier ->
                Box(
                    modifier = wellModifier
                        .size(PreviewLayoutTokens.PreviewControlSize)
                        .background(Color.Cyan)
                )
            },
            zoomLevelDisplay = { zoomModifier ->
                Box(
                    modifier = zoomModifier
                        .height(PreviewLayoutTokens.PreviewControlSize)
                        .fillMaxWidth()
                        .background(Color.Magenta)
                )
            },
            elapsedTimeDisplay = { elapsedModifier ->
                Box(
                    modifier = elapsedModifier
                        .height(PreviewLayoutTokens.PreviewElapsedHeight)
                        .fillMaxWidth(PreviewLayoutTokens.PREVIEW_HALF_WIDTH_FRACTION)
                        .background(Color.Red)
                )
            },
            quickSettingsButton = { qsModifier ->
                Box(
                    modifier = qsModifier
                        .size(PreviewLayoutTokens.PreviewControlSize)
                        .background(Color.Yellow)
                )
            },
            indicatorRow = { indModifier ->
                Box(
                    modifier = indModifier
                        .height(PreviewLayoutTokens.PreviewControlSize)
                        .fillMaxWidth()
                        .background(Color.Green)
                )
            },
            captureModeToggle = { toggleModifier ->
                Box(
                    modifier = toggleModifier
                        .height(PreviewLayoutTokens.PreviewControlSize)
                        .fillMaxWidth(PreviewLayoutTokens.PREVIEW_HALF_WIDTH_FRACTION)
                        .background(Color.Blue)
                )
            },
            quickSettingsOverlay = {},
            debugOverlay = {},
            screenFlashOverlay = {},
            snackBar = { _, _ -> },
            debugVisibilityWrapper = { content -> content() },
            slots = slots
        )
    }
}

@PreviewPortraitDevices
@Composable
private fun CaptureLayoutPreview() {
    AdaptivePreviewLayoutSample()
}

@Preview(
    name = "PreviewLayout - COMPACT_PORTRAIT (412x915dp)",
    device = "spec:width=412dp,height=915dp",
    showBackground = true
)
@Composable
private fun PreviewLayoutCompactPortraitPreview() {
    AdaptivePreviewLayoutSample(
        slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true)
    )
}

@Preview(
    name = "PreviewLayout - TWO_SIDE_RAIL (852x883dp)",
    device = "spec:width=852dp,height=883dp",
    showBackground = true
)
@Composable
private fun PreviewLayoutTwoSideRailPreview() {
    AdaptivePreviewLayoutSample(
        slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true)
    )
}

@Preview(
    name = "PreviewLayout - WIDE_SHORT Split (800x400dp on sw600dp)",
    device = "spec:width=800dp,height=600dp",
    showBackground = true
)
@Composable
private fun PreviewLayoutWideShortPreview() {
    AdaptivePreviewLayoutSample(
        modifier = Modifier.size(
            width = PreviewLayoutTokens.PreviewWideShortWidth,
            height = PreviewLayoutTokens.PreviewWideShortHeight
        ),
        slots = PlaceholderCaptureSlots.sideRail(
            isCompactHeight = true,
            includeElapsedTime = true
        )
    )
}

@Preview(
    name = "PreviewLayout - COMPACT_SHORT (360x360dp)",
    device = "spec:width=360dp,height=360dp",
    showBackground = true
)
@Composable
private fun PreviewLayoutCompactShortPreview() {
    AdaptivePreviewLayoutSample(
        slots = PlaceholderCaptureSlots.sideRail(
            isCompactHeight = true,
            includeElapsedTime = true
        )
    )
}
