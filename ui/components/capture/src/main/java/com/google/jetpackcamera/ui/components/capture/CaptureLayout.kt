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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.darkColorScheme
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
 * Adaptive 7-mode orchestrator layout for the camera capture screen.
 *
 * Evaluates the available container geometry via [rememberCameraLayoutMode] with `14.dp` (`±7.dp`)
 * hysteresis deadbands, provides the resolved [CameraLayoutMode] via [LocalCameraLayoutMode], wraps
 * [viewfinder] in [movableContentOf] so the live camera surface is reparented across posture
 * transitions without disposal, and dispatches across the 5 dedicated posture layouts:
 * - [CameraLayoutMode.COMPACT] and [CameraLayoutMode.COMPACT_SLIM] -> [CompactPortraitLayout]
 * - [CameraLayoutMode.COMPACT_SHORT] and [CameraLayoutMode.COMPACT_SHORT_SLIM] ->
 *   [CompactShortLayout]
 * - [CameraLayoutMode.FOLDABLE] -> [FoldableLayout]
 * - [CameraLayoutMode.TABLET_PORTRAIT] -> [TabletPortraitLayout]
 * - [CameraLayoutMode.TABLET_LANDSCAPE] -> [TabletLandscapeLayout]
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
 * @param indicatorRow the status indicator row/column composable.
 * @param captureModeToggle the capture mode toggle composable.
 * @param captureModeCarousel content for the fixed-height slot reserved between the shutter row
 *   and the lower controls row on compact layouts, or the bottom-center HUD on side-rail layouts.
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
 * @param deviceRotation physical gravity orientation of the device.
 * @param windowRotation display/window rotation reported by Android.
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
    errorDialog: @Composable (Modifier) -> Unit = {},
    deviceRotation: DeviceRotation = rememberDeviceRotation(),
    windowRotation: DeviceRotation = rememberWindowRotation()
) {
    CaptureScaffoldLayout(
        modifier = modifier,
        state = state,
        deviceRotation = deviceRotation,
        windowRotation = windowRotation,
        onDismissQuickSettings = onDismissQuickSettings,
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
        overlay = overlay,
        controlsVisible = controlsVisible,
        screenFlashOverlay = screenFlashOverlay,
        snackBar = snackBar,
        topStartContent = topStartContent,
        viewfinderTopEndContent = viewfinderTopEndContent,
        topBarTrailingContent = topBarTrailingContent,
        onRequiresImmersiveChange = onRequiresImmersiveChange,
        errorDialog = errorDialog,
        slots = null
    )
}

@Composable
internal fun CaptureScaffoldLayout(
    modifier: Modifier = Modifier,
    state: CaptureScaffoldState = rememberCaptureScaffoldState(),
    deviceRotation: DeviceRotation = LocalDeviceRotation.current,
    windowRotation: DeviceRotation = LocalWindowRotation.current,
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
    errorDialog: @Composable (Modifier) -> Unit = {},
    isErrorDialogShowing: Boolean = false,
    slots: CaptureSlots? = null
) {
    val movableViewfinder = remember(viewfinder) {
        movableContentOf<Modifier> { vfModifier ->
            viewfinder(vfModifier)
        }
    }

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

        CompositionLocalProvider(LocalCameraLayoutMode provides layoutMode) {
            val resolvedSlots = slots ?: CaptureSlots(
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
                quickSettingsOverlay = { slotModifier ->
                    quickSettingsOverlay(slotModifier)
                },
                snackBar = snackBar,
                errorDialog = errorDialog
            )

            when (layoutMode) {
                CameraLayoutMode.COMPACT,
                CameraLayoutMode.COMPACT_SLIM -> {
                    CompactPortraitLayout(
                        slots = resolvedSlots,
                        viewfinder = movableViewfinder,
                        state = state,
                        deviceRotation = deviceRotation,
                        windowRotation = windowRotation,
                        controlsVisible = controlsVisible,
                        isErrorDialogShowing = isErrorDialogShowing,
                        includeStatusBarsInTopBar = !isFullScreenCompactPhone,
                        onDismissQuickSettings = onDismissQuickSettings,
                        onRequiresImmersiveChange = onRequiresImmersiveChange,
                        overlay = overlay,
                        screenFlashOverlay = screenFlashOverlay
                    )
                }

                CameraLayoutMode.FOLDABLE -> {
                    FoldableLayout(
                        slots = resolvedSlots,
                        viewfinder = movableViewfinder,
                        state = state,
                        controlsVisible = controlsVisible,
                        isErrorDialogShowing = isErrorDialogShowing,
                        onDismissQuickSettings = onDismissQuickSettings,
                        overlay = overlay,
                        screenFlashOverlay = screenFlashOverlay
                    )
                }

                CameraLayoutMode.TABLET_PORTRAIT -> {
                    TabletPortraitLayout(
                        slots = resolvedSlots,
                        viewfinder = movableViewfinder,
                        state = state,
                        controlsVisible = controlsVisible,
                        isErrorDialogShowing = isErrorDialogShowing,
                        onDismissQuickSettings = onDismissQuickSettings,
                        overlay = overlay,
                        screenFlashOverlay = screenFlashOverlay
                    )
                }

                CameraLayoutMode.TABLET_LANDSCAPE -> {
                    TabletLandscapeLayout(
                        slots = resolvedSlots,
                        viewfinder = movableViewfinder,
                        state = state,
                        controlsVisible = controlsVisible,
                        isErrorDialogShowing = isErrorDialogShowing,
                        onDismissQuickSettings = onDismissQuickSettings,
                        overlay = overlay,
                        screenFlashOverlay = screenFlashOverlay
                    )
                }

                CameraLayoutMode.COMPACT_SHORT,
                CameraLayoutMode.COMPACT_SHORT_SLIM -> {
                    CompactShortLayout(
                        slots = resolvedSlots,
                        viewfinder = movableViewfinder,
                        state = state,
                        controlsVisible = controlsVisible,
                        isErrorDialogShowing = isErrorDialogShowing,
                        onDismissQuickSettings = onDismissQuickSettings,
                        overlay = overlay,
                        screenFlashOverlay = screenFlashOverlay
                    )
                }
            }
        }
    }
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

@Preview(
    name = "1. COMPACT - Portrait (412x915dp)",
    device = "spec:width=412dp,height=915dp",
    showBackground = true
)
@Composable
private fun OrchestratorCompactPortraitPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CaptureScaffoldLayout(
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
            slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true)
        )
    }
}

@Preview(
    name = "2. COMPACT - Horizontal (800x400dp)",
    device = "spec:width=800dp,height=400dp",
    showBackground = true
)
@Composable
private fun OrchestratorCompactHorizontalPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CaptureScaffoldLayout(
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
            slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true)
        )
    }
}

@Preview(
    name = "3. COMPACT_SLIM - Portrait (320x740dp)",
    device = "spec:width=320dp,height=740dp",
    showBackground = true
)
@Composable
private fun OrchestratorCompactSlimPortraitPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CaptureScaffoldLayout(
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
            slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true)
        )
    }
}

@Preview(
    name = "4. COMPACT_SLIM - Horizontal (740x320dp)",
    device = "spec:width=740dp,height=320dp",
    showBackground = true
)
@Composable
private fun OrchestratorCompactSlimHorizontalPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CaptureScaffoldLayout(
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
            slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true)
        )
    }
}

@Preview(
    name = "5. COMPACT_SHORT - Square Split/Cover (360x360dp)",
    device = "spec:width=360dp,height=360dp",
    showBackground = true
)
@Composable
private fun OrchestratorCompactShortPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CaptureScaffoldLayout(
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
            slots = PlaceholderCaptureSlots.sideRail(
                isCompactHeight = true,
                includeElapsedTime = true
            )
        )
    }
}

@Preview(
    name = "6. COMPACT_SHORT_SLIM - Tiny Pane (320x340dp)",
    device = "spec:width=320dp,height=340dp",
    showBackground = true
)
@Composable
private fun OrchestratorCompactShortSlimPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CaptureScaffoldLayout(
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
            slots = PlaceholderCaptureSlots.sideRail(
                isCompactHeight = true,
                includeElapsedTime = true
            )
        )
    }
}

@Preview(
    name = "7. FOLDABLE - Unfolded Inner Display (852x883dp)",
    device = "spec:width=852dp,height=883dp",
    showBackground = true
)
@Composable
private fun OrchestratorFoldablePreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CaptureScaffoldLayout(
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
            slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true)
        )
    }
}

@Preview(
    name = "8. TABLET_PORTRAIT - Lower Thumb Zone (800x1280dp)",
    device = "spec:width=800dp,height=1280dp",
    showBackground = true
)
@Composable
private fun OrchestratorTabletPortraitPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CaptureScaffoldLayout(
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
            slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true)
        )
    }
}

@Preview(
    name = "9. TABLET_LANDSCAPE - Generous Rails (1280x800dp)",
    device = "spec:width=1280dp,height=800dp",
    showBackground = true
)
@Composable
private fun OrchestratorTabletLandscapePreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CaptureScaffoldLayout(
            viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
            slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true)
        )
    }
}
