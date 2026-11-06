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

import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.HorizontalAlignmentLine

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
 * The base layout for the camera capture screen.
 *
 * @param modifier the modifier for this component
 * @param scaffoldState the bottom sheet scaffold state
 * @param onDismissQuickSettings callback to dismiss quick settings when clicking the drag handle
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
    errorDialog: @Composable (Modifier) -> Unit = {}
) {
    CompactPortraitLayout(
        modifier = modifier,
        scaffoldState = scaffoldState,
        onDismissQuickSettings = onDismissQuickSettings,
        includeStatusBarsInTopBar = false,
        viewfinder = viewfinder,
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
        errorDialog = errorDialog
    )
}
