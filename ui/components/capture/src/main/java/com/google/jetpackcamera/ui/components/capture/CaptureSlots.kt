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

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared dimension, typography, and color tokens for [CaptureSlots] placeholder scaffolding.
 */
internal object CaptureSlotTokens {
    val PillCornerRadius = 24.dp
    val ImageWellCornerRadius = 12.dp
    val OverlayCornerRadius = 24.dp

    val IndicatorCrossAxisSize = 48.dp
    val IndicatorStandardMainAxisSize = 96.dp
    val IndicatorCompactMainAxisSize = 64.dp
    val HorizontalTopBarHeight = 36.dp
    val HorizontalTopBarStandardWidth = 120.dp
    val HorizontalTopBarCompactWidth = 96.dp

    val ZoomCrossAxisSize = 48.dp
    val ZoomStandardMainAxisSize = 176.dp
    val ZoomCompactMainAxisSize = 148.dp

    val IconTouchTargetSize = 48.dp
    val SecondaryActionStandardSize = 52.dp
    val SecondaryActionCompactSize = 48.dp

    val ShutterStandardDiameter = 86.dp
    val ShutterCompactDiameter = 72.dp

    val ModeToggleStandardWidth = 96.dp
    val ModeToggleCompactWidth = 72.dp
    val ModeToggleStandardHeight = 56.dp
    val ModeToggleCompactHeight = 48.dp

    val CarouselStandardWidth = 180.dp
    val CarouselCompactWidth = 140.dp
    val CarouselHeight = 32.dp

    val ElapsedTimeWidth = 72.dp
    val ElapsedTimeStandardHeight = 32.dp
    val ElapsedTimeCompactHeight = 28.dp

    val QuickSettingsStandardWidth = 280.dp
    val QuickSettingsCompactWidth = 240.dp
    val QuickSettingsStandardHeight = 220.dp
    val QuickSettingsCompactHeight = 160.dp

    val ErrorDialogWidth = 280.dp
    val ErrorDialogHeight = 160.dp

    val PlaceholderLabelFontSize = 11.sp
    val ViewfinderLabelFontSize = 14.sp

    val DefaultControlColor = Color(0xFF3949AB)
    val IndicatorColor = Color(0xFF00897B)
    val ZoomColor = Color(0xFF5E35B1)
    val QuickSettingsButtonColor = Color(0xFF039BE5)
    val ShutterColor = Color(0xFFE53935)
    val FlipCameraColor = Color(0xFF43A047)
    val ImageWellColor = Color(0xFFFB8C00)
    val ModeToggleColor = Color(0xFF8E24AA)
    val CarouselColor = Color(0xFF546E7A)
    val ElapsedTimeColor = Color(0xFFC62828)
    val OverlayPlaceholderColor = Color(0xFF263238)
    val ErrorDialogColor = Color(0xFFB71C1C)
    val ViewfinderColor = Color(0xFF1E272C)
    val PlaceholderLabelColor = Color.White
}

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
 * overlap styling and container layout metadata ([orientation], [isCompact]) into a
 * [PreviewOverlaySlotScope].
 */
@Composable
internal fun OverlapAwareCaptureSlot(
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
    isCompact: Boolean = false,
    content: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit
) {
    OverlapAwareStyleProvider(
        modifier = modifier,
        orientation = orientation,
        isCompact = isCompact
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
 * [PreviewOverlaySlotScope.orientation] and [PreviewOverlaySlotScope.isCompact] from the hosting
 * layout container (and propagates directly into [CaptureSlotScope] when hosted by
 * [CaptureScaffold]).
 *
 * @property topBar Status indicator bar or column content adapting to
 *   [PreviewOverlaySlotScope.orientation].
 * @property topBarTrailingContent Optional trailing content aligned to the end of [topBar].
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
    val topBarTrailingContent: @Composable PreviewOverlaySlotScope.(Modifier) -> Unit = {},
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

/**
 * Minimal colored placeholder box representing a single camera control in Compose previews and
 * layout geometry tests.
 *
 * Applies [modifier] before `.size(width, height)` so hosting layouts can prepend `Modifier.sizeIn`
 * constraints to clamp dimensions in condensed windows.
 *
 * @param label Short debug label centered inside the control box.
 * @param width Unclamped target width of the placeholder control.
 * @param height Unclamped target height of the placeholder control.
 * @param modifier [Modifier] applied ahead of the default size modifier.
 * @param color Fill color of the placeholder shape.
 * @param shape Clip shape of the placeholder box.
 * @param testTag Optional semantics test tag attached to the outer box.
 */
@Composable
internal fun PlaceholderControlBox(
    label: String,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    color: Color = CaptureSlotTokens.DefaultControlColor,
    shape: Shape = RoundedCornerShape(CaptureSlotTokens.PillCornerRadius),
    testTag: String? = null
) {
    val taggedModifier = if (testTag != null) {
        modifier.testTag(testTag)
    } else {
        modifier
    }
    Box(
        modifier = taggedModifier
            .size(width = width, height = height)
            .clip(shape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                color = CaptureSlotTokens.PlaceholderLabelColor,
                fontSize = CaptureSlotTokens.PlaceholderLabelFontSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )
    }
}

/**
 * Minimal tinted placeholder surface representing the camera viewfinder in layout previews and
 * structural tests, tagged with [PREVIEW_DISPLAY].
 *
 * @param modifier [Modifier] applied to the viewfinder box before filling its allotted bounds.
 * @param label Debug label centered inside the placeholder frame.
 */
@Composable
internal fun PlaceholderViewfinder(modifier: Modifier = Modifier, label: String = "Viewfinder") {
    Box(
        modifier = modifier
            .testTag(PREVIEW_DISPLAY)
            .fillMaxSize()
            .background(CaptureSlotTokens.ViewfinderColor),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                color = CaptureSlotTokens.PlaceholderLabelColor,
                fontSize = CaptureSlotTokens.ViewfinderLabelFontSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )
    }
}

/**
 * Factory of colored [PlaceholderControlBox]-backed [CaptureSlots] configurations for Compose
 * previews and structural layout tests.
 */
internal object PlaceholderCaptureSlots {
    /**
     * Returns the default placeholder [CaptureSlots] set, delegating to [sideRail].
     *
     * @param isCompactHeight Whether placeholder controls default to compact-height dimensions.
     */
    fun default(isCompactHeight: Boolean = false): CaptureSlots =
        sideRail(isCompactHeight = isCompactHeight)

    /**
     * Builds placeholder [CaptureSlots] recycling existing capture test tags for side-rail and
     * adaptive layout verification.
     *
     * @param isCompactHeight Whether controls default to compact-height dimensions even when the
     *   hosting [PreviewOverlaySlotScope.isCompact] flag is `false`.
     * @param includeImageWell Whether to populate [CaptureSlots.imageWell] or leave it empty.
     * @param includeElapsedTime Whether to populate [CaptureSlots.elapsedTimeDisplay] or leave it
     *   empty.
     * @param tagQuickSettingsOverlay Whether the inner [CaptureSlots.quickSettingsOverlay] carries
     *   [QUICK_SETTINGS_BOTTOM_SHEET]; defaults to `false` so outer Quick Settings card containers
     *   can own that tag without duplicate semantics nodes.
     * @param includeErrorDialog Whether to populate [CaptureSlots.errorDialog] with a placeholder
     *   tagged with [CAMERA_ERROR_DIALOG_TAG].
     */
    fun sideRail(
        isCompactHeight: Boolean = false,
        includeImageWell: Boolean = true,
        includeElapsedTime: Boolean = false,
        tagQuickSettingsOverlay: Boolean = false,
        includeErrorDialog: Boolean = false
    ): CaptureSlots = CaptureSlots(
        topBar = { slotModifier ->
            val compact = isCompactHeight || isCompact
            if (orientation == Orientation.Vertical) {
                PlaceholderControlBox(
                    label = "Ind",
                    width = CaptureSlotTokens.IndicatorCrossAxisSize,
                    height = if (compact) {
                        CaptureSlotTokens.IndicatorCompactMainAxisSize
                    } else {
                        CaptureSlotTokens.IndicatorStandardMainAxisSize
                    },
                    modifier = slotModifier,
                    color = CaptureSlotTokens.IndicatorColor,
                    testTag = VIDEO_QUALITY_TAG
                )
            } else {
                PlaceholderControlBox(
                    label = "Ind",
                    width = if (compact) {
                        CaptureSlotTokens.HorizontalTopBarCompactWidth
                    } else {
                        CaptureSlotTokens.HorizontalTopBarStandardWidth
                    },
                    height = CaptureSlotTokens.HorizontalTopBarHeight,
                    modifier = slotModifier,
                    color = CaptureSlotTokens.IndicatorColor,
                    testTag = VIDEO_QUALITY_TAG
                )
            }
        },
        zoomControls = { slotModifier ->
            val compact = isCompactHeight || isCompact
            val mainAxisSize = if (compact) {
                CaptureSlotTokens.ZoomCompactMainAxisSize
            } else {
                CaptureSlotTokens.ZoomStandardMainAxisSize
            }
            val width = if (orientation == Orientation.Vertical) {
                CaptureSlotTokens.ZoomCrossAxisSize
            } else {
                mainAxisSize
            }
            val height = if (orientation == Orientation.Vertical) {
                mainAxisSize
            } else {
                CaptureSlotTokens.ZoomCrossAxisSize
            }
            PlaceholderControlBox(
                label = "Zoom",
                width = width,
                height = height,
                modifier = slotModifier,
                color = CaptureSlotTokens.ZoomColor,
                testTag = ZOOM_BUTTON_ROW_TAG
            )
        },
        quickSettingsButton = { slotModifier ->
            PlaceholderControlBox(
                label = "QS",
                width = CaptureSlotTokens.IconTouchTargetSize,
                height = CaptureSlotTokens.IconTouchTargetSize,
                modifier = slotModifier,
                color = CaptureSlotTokens.QuickSettingsButtonColor,
                shape = CircleShape,
                testTag = QUICK_SETTINGS_DROP_DOWN
            )
        },
        captureButton = { slotModifier ->
            val compact = isCompactHeight || isCompact
            val diameter = if (compact) {
                CaptureSlotTokens.ShutterCompactDiameter
            } else {
                CaptureSlotTokens.ShutterStandardDiameter
            }
            PlaceholderControlBox(
                label = "Cap",
                width = diameter,
                height = diameter,
                modifier = slotModifier,
                color = CaptureSlotTokens.ShutterColor,
                shape = CircleShape,
                testTag = CAPTURE_BUTTON
            )
        },
        flipCameraButton = { slotModifier ->
            val compact = isCompactHeight || isCompact
            val size = if (compact) {
                CaptureSlotTokens.SecondaryActionCompactSize
            } else {
                CaptureSlotTokens.SecondaryActionStandardSize
            }
            PlaceholderControlBox(
                label = "Flip",
                width = size,
                height = size,
                modifier = slotModifier,
                color = CaptureSlotTokens.FlipCameraColor,
                shape = CircleShape,
                testTag = FLIP_CAMERA_BUTTON
            )
        },
        imageWell = if (includeImageWell) {
            { slotModifier ->
                val compact = isCompactHeight || isCompact
                val size = if (compact) {
                    CaptureSlotTokens.SecondaryActionCompactSize
                } else {
                    CaptureSlotTokens.SecondaryActionStandardSize
                }
                PlaceholderControlBox(
                    label = "Well",
                    width = size,
                    height = size,
                    modifier = slotModifier,
                    color = CaptureSlotTokens.ImageWellColor,
                    shape = RoundedCornerShape(CaptureSlotTokens.ImageWellCornerRadius),
                    testTag = IMAGE_WELL_TAG
                )
            }
        } else {
            {}
        },
        captureModeToggle = { slotModifier ->
            val compact = isCompactHeight || isCompact
            PlaceholderControlBox(
                label = "Mode",
                width = if (compact) {
                    CaptureSlotTokens.ModeToggleCompactWidth
                } else {
                    CaptureSlotTokens.ModeToggleStandardWidth
                },
                height = if (compact) {
                    CaptureSlotTokens.ModeToggleCompactHeight
                } else {
                    CaptureSlotTokens.ModeToggleStandardHeight
                },
                modifier = slotModifier,
                color = CaptureSlotTokens.ModeToggleColor,
                testTag = CAPTURE_MODE_TOGGLE_BUTTON
            )
        },
        captureModeCarousel = { slotModifier ->
            val compact = isCompactHeight || isCompact
            PlaceholderControlBox(
                label = "Modes",
                width = if (compact) {
                    CaptureSlotTokens.CarouselCompactWidth
                } else {
                    CaptureSlotTokens.CarouselStandardWidth
                },
                height = CaptureSlotTokens.CarouselHeight,
                modifier = slotModifier,
                color = CaptureSlotTokens.CarouselColor,
                testTag = CAPTURE_MODE_CAROUSEL
            )
        },
        elapsedTimeDisplay = if (includeElapsedTime) {
            { slotModifier ->
                val compact = isCompactHeight || isCompact
                PlaceholderControlBox(
                    label = "00:12",
                    width = CaptureSlotTokens.ElapsedTimeWidth,
                    height = if (compact) {
                        CaptureSlotTokens.ElapsedTimeCompactHeight
                    } else {
                        CaptureSlotTokens.ElapsedTimeStandardHeight
                    },
                    modifier = slotModifier,
                    color = CaptureSlotTokens.ElapsedTimeColor,
                    testTag = ELAPSED_TIME_TAG
                )
            }
        } else {
            {}
        },
        quickSettingsOverlay = { slotModifier ->
            val compact = isCompactHeight || isCompact
            PlaceholderControlBox(
                label = "Quick Settings",
                width = if (compact) {
                    CaptureSlotTokens.QuickSettingsCompactWidth
                } else {
                    CaptureSlotTokens.QuickSettingsStandardWidth
                },
                height = if (compact) {
                    CaptureSlotTokens.QuickSettingsCompactHeight
                } else {
                    CaptureSlotTokens.QuickSettingsStandardHeight
                },
                modifier = slotModifier,
                color = CaptureSlotTokens.OverlayPlaceholderColor,
                shape = RoundedCornerShape(CaptureSlotTokens.OverlayCornerRadius),
                testTag = if (tagQuickSettingsOverlay) QUICK_SETTINGS_BOTTOM_SHEET else null
            )
        },
        errorDialog = if (includeErrorDialog) {
            { dialogModifier ->
                PlaceholderControlBox(
                    label = "Camera Error",
                    width = CaptureSlotTokens.ErrorDialogWidth,
                    height = CaptureSlotTokens.ErrorDialogHeight,
                    modifier = dialogModifier,
                    color = CaptureSlotTokens.ErrorDialogColor,
                    shape = RoundedCornerShape(CaptureSlotTokens.OverlayCornerRadius),
                    testTag = CAMERA_ERROR_DIALOG_TAG
                )
            }
        } else {
            {}
        }
    )
}
