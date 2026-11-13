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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.unit.dp

/**
 * The top edge of the visible viewfinder, reported by a viewfinder that fills its slot and offsets
 * its own frame (such as [PreviewDisplay]).
 *
 * [CaptureScaffoldLayout] reads this line to anchor viewfinder-relative content when the viewfinder
 * slot itself starts at the top of the window. A viewfinder that does not report the line is
 * treated as starting at its own top edge.
 */
internal val ViewfinderTopLine = HorizontalAlignmentLine { a, b -> minOf(a, b) }

/**
 * The base adaptive layout for the camera capture screen.
 *
 * Positions the [viewfinder], individual control slots ([captureButton], [imageWell],
 * [flipCameraButton], [zoomLevelDisplay], [elapsedTimeDisplay], [quickSettingsButton],
 * [indicatorRow], [captureModeToggle], [captureModeCarousel]), [quickSettingsOverlay],
 * [topStartContent], [viewfinderTopEndContent], [topBarTrailingContent], [overlay],
 * [screenFlashOverlay], [snackBar], and [errorDialog] without depending on [CaptureUiState].
 *
 * @param modifier the [Modifier] for this component.
 * @param state the mode-agnostic [CaptureScaffoldState] controlling Quick Settings visibility and
 *   the [SnackbarHostState].
 * @param onDismissQuickSettings callback invoked when Quick Settings is dismissed via the drag
 *   handle or scrim.
 * @param viewfinder the viewfinder composable.
 * @param captureButton the capture button composable.
 * @param imageWell the image well composable.
 * @param flipCameraButton the flip camera button composable.
 * @param zoomLevelDisplay the zoom level display composable.
 * @param elapsedTimeDisplay the elapsed time display composable.
 * @param quickSettingsButton the quick settings button composable.
 * @param indicatorRow the top indicator row composable.
 * @param captureModeToggle the capture mode toggle composable.
 * @param captureModeCarousel content for the fixed-height slot reserved between the shutter row
 *   and the lower controls row.
 * @param quickSettingsOverlay the quick settings overlay composable.
 * @param overlay optional overlay composable drawn above the viewfinder and controls.
 * @param controlsVisible whether the camera controls should be composed and visible.
 * @param screenFlashOverlay the screen flash overlay composable, drawn above all other content
 *   and across the entire window, including system bar and display cutout areas.
 * @param snackBar the snack bar composable for showing messages against [SnackbarHostState].
 * @param topStartContent optional composable content placed at the top-start of the capture screen.
 * @param viewfinderTopEndContent optional composable content anchored to the top-end corner of the
 *   viewfinder.
 * @param topBarTrailingContent optional composable content aligned to the end of the top bar.
 * @param onRequiresImmersiveChange invoked with `true` when the window is too short for the
 *   tallest viewfinder to clear a persistent navigation bar, and `false` otherwise or on dispose.
 * @param errorDialog the camera error alert dialog composable.
 */
@Composable
fun CaptureScaffoldLayout(
    modifier: Modifier = Modifier,
    state: CaptureScaffoldState = rememberCaptureScaffoldState(),
    onDismissQuickSettings: () -> Unit = {},
    viewfinder: @Composable (Modifier) -> Unit,
    captureButton: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    imageWell: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    flipCameraButton: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    zoomLevelDisplay: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    elapsedTimeDisplay: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    quickSettingsButton: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    indicatorRow: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    captureModeToggle: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    captureModeCarousel: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    quickSettingsOverlay: @Composable (Modifier) -> Unit = {},
    overlay: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    controlsVisible: Boolean = true,
    screenFlashOverlay: @Composable (Modifier) -> Unit = {},
    snackBar: @Composable (Modifier, snackbarHostState: SnackbarHostState) -> Unit = { _, _ -> },
    topStartContent: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    viewfinderTopEndContent: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    topBarTrailingContent: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    onRequiresImmersiveChange: (Boolean) -> Unit = {},
    errorDialog: @Composable (Modifier) -> Unit = {}
) {
    val slots = CaptureSlots(
        topBar = indicatorRow,
        topBarTrailingContent = topBarTrailingContent,
        zoomControls = zoomLevelDisplay,
        quickSettingsButton = quickSettingsButton,
        captureButton = captureButton,
        flipCameraButton = flipCameraButton,
        imageWell = imageWell,
        captureModeToggle = captureModeToggle,
        captureModeCarousel = captureModeCarousel,
        elapsedTimeDisplay = elapsedTimeDisplay,
        topStartContent = topStartContent,
        viewfinderTopEndContent = viewfinderTopEndContent,
        quickSettingsOverlay = { slotModifier -> quickSettingsOverlay(slotModifier) },
        snackBar = snackBar,
        errorDialog = errorDialog
    )
    CompactPortraitLayout(
        slots = slots,
        viewfinder = viewfinder,
        modifier = modifier,
        state = state,
        controlsVisible = controlsVisible,
        includeStatusBarsInTopBar = false,
        onDismissQuickSettings = onDismissQuickSettings,
        onRequiresImmersiveChange = onRequiresImmersiveChange,
        overlay = overlay,
        screenFlashOverlay = screenFlashOverlay
    )
}

@PreviewPortraitDevices
@Composable
private fun CaptureLayoutPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CaptureScaffoldLayout(
            modifier = Modifier.background(Color.Black),
            viewfinder = { modifier ->
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .background(Color.DarkGray)
                )
            },
            captureButton = { modifier ->
                Box(
                    modifier = modifier
                        .size(80.dp)
                        .background(Color.White)
                )
            },
            flipCameraButton = { modifier ->
                Box(
                    modifier = modifier
                        .size(48.dp)
                        .background(Color.Cyan)
                )
            },
            imageWell = { modifier ->
                Box(
                    modifier = modifier
                        .size(48.dp)
                        .background(Color.Cyan)
                )
            },
            zoomLevelDisplay = { modifier ->
                Box(
                    modifier = modifier
                        .height(48.dp)
                        .fillMaxWidth()
                        .background(Color.Magenta)
                )
            },
            elapsedTimeDisplay = { modifier ->
                Box(
                    modifier = modifier
                        .height(24.dp)
                        .fillMaxWidth(0.5f)
                        .background(Color.Red)
                )
            },
            quickSettingsButton = { modifier ->
                Box(
                    modifier = modifier
                        .size(48.dp)
                        .background(Color.Yellow)
                )
            },
            indicatorRow = { modifier ->
                Box(
                    modifier = modifier
                        .height(48.dp)
                        .fillMaxWidth()
                        .background(Color.Green)
                )
            },
            captureModeToggle = { modifier ->
                Box(
                    modifier = modifier
                        .height(48.dp)
                        .fillMaxWidth(0.5f)
                        .background(Color.Blue)
                )
            }
        )
    }
}
