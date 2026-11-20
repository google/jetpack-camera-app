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
package com.google.jetpackcamera.ui.components.capture

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Policy controlling whether spatial camera chrome containers mirror horizontally under
 * right-to-left locales.
 *
 * Hardware-ergonomic camera chrome defaults to preserving physical left/right placement so
 * controls stay anchored to the user's grip while inner text and directional icons follow the
 * active locale's layout direction.
 *
 * @property mirrorSideRails Whether start and end side rails swap horizontal sides in RTL.
 * @property mirrorCompactBottomBar Whether compact bottom-bar slots mirror horizontally in RTL.
 */
@Immutable
data class CaptureRtlPolicy(
    val mirrorSideRails: Boolean = false,
    val mirrorCompactBottomBar: Boolean = false
)

/**
 * CompositionLocal providing the active [CaptureRtlPolicy] for adaptive capture screen layouts.
 */
val LocalCaptureRtlPolicy: ProvidableCompositionLocal<CaptureRtlPolicy> =
    staticCompositionLocalOf { CaptureRtlPolicy() }

/**
 * Hosts a [CaptureSlots] composable inside an [OverlapAwareStyleProvider], binding dynamic
 * overlap styling and container layout metadata ([orientation], [controlSize]) into a
 * [PreviewOverlaySlotScope].
 */
@Composable
internal fun OverlapAwareCaptureSlot(
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
    controlSize: ControlSize = ControlSize.Standard,
    content: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit
) {
    OverlapAwareStyleProvider(
        modifier = modifier,
        orientation = orientation,
        controlSize = controlSize
    ) {
        content(Modifier)
    }
}

/**
 * Slot table bundling the UI control composables placed by adaptive capture screen layouts.
 *
 * Each slot defaults to an empty composable so layouts and tests can populate only the controls
 * relevant to a scenario while omitted controls collapse cleanly. Slots receive
 * [PreviewOverlaySlotScope], which exposes live viewfinder overlap styling together with
 * [PreviewOverlaySlotScope.orientation] and [PreviewOverlaySlotScope.controlSize] from the hosting
 * layout container (and propagates directly into [CaptureSlotScope] when hosted by
 * [CaptureScaffold]).
 *
 * @property topBar Status indicator bar or column content adapting to
 *   [PreviewOverlaySlotScope.orientation].
 * @property topBarLeadingContent Optional leading content placed directly before [topBar].
 * @property zoomControls Zoom control bar or button cluster, adapting to
 *   [PreviewOverlaySlotScope.orientation].
 * @property quickSettingsButton Trigger button that toggles the Quick Settings surface.
 * @property captureButton Primary photo/video shutter button.
 * @property flipCameraButton Lens-facing toggle button.
 * @property imageWell Thumbnail button opening the most recently captured media item.
 * @property captureModeToggle Photo/video capture mode toggle switch.
 * @property captureModeCarousel Secondary sub-mode selector carousel.
 * @property elapsedTimeDisplay Active video recording duration readout.
 * @property topStartContent Overlay content anchored to the top-start of the capture region.
 * @property viewfinderTopEndContent Overlay content anchored to the top-end of the viewfinder.
 * @property quickSettingsOverlay Expanded Quick Settings panel or sheet content.
 * @property snackBar Transient message host paired with a [SnackbarHostState].
 * @property errorDialog In-hierarchy modal error dialog drawn above capture chrome.
 */
@Immutable
internal data class CaptureSlots(
    val topBar: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    val topBarLeadingContent: (@Composable PreviewOverlaySlotScope.(Modifier) -> Unit)? = null,
    val zoomControls: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    val quickSettingsButton: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    val captureButton: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    val flipCameraButton: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    val imageWell: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    val captureModeToggle: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    val captureModeCarousel: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    val elapsedTimeDisplay: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    val topStartContent: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    val viewfinderTopEndContent: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    val quickSettingsOverlay: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
    val snackBar: @Composable (Modifier, SnackbarHostState) -> Unit = { _, _ -> },
    val errorDialog: @Composable (Modifier) -> Unit = {}
)
