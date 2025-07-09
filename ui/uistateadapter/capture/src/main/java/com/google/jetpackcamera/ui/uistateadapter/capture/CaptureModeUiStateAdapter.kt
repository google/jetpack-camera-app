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
package com.google.jetpackcamera.ui.uistateadapter.capture

import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraConstraints
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.ConcurrentCameraMode
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.StreamConfig
import com.google.jetpackcamera.settings.model.SystemConstraints
import com.google.jetpackcamera.settings.model.forCurrentLens
import com.google.jetpackcamera.ui.components.capture.DisabledReason
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState

private val ORDERED_UI_SUPPORTED_CAPTURE_MODES = listOf(
    CaptureMode.STANDARD,
    CaptureMode.IMAGE_ONLY,
    CaptureMode.VIDEO_ONLY
)

fun CaptureModeToggleUiState.Companion.from(
    systemConstraints: SystemConstraints,
    cameraAppSettings: CameraAppSettings,
    cameraState: CameraState,
    previewMode: PreviewMode
): CaptureModeToggleUiState =
    if (cameraState.videoRecordingState !is VideoRecordingState.Inactive) {
        CaptureModeToggleUiState.Unavailable
    } else if (cameraAppSettings.imageFormat == ImageOutputFormat.JPEG_ULTRA_HDR ||
        cameraAppSettings.dynamicRange == DynamicRange.HLG10
    ) {
        val availableCaptureModes = getAvailableCaptureModes(
            systemConstraints,
            cameraAppSettings,
            previewMode
        )
        // Find the IMAGE_ONLY and VIDEO_ONLY states
        val imageOnlyState = availableCaptureModes.first { item ->
            when (item) {
                is SingleSelectableUiState.SelectableUi -> item.value == CaptureMode.IMAGE_ONLY
                is SingleSelectableUiState.Disabled -> item.value == CaptureMode.IMAGE_ONLY
            }
        }
        val videoOnlyState = availableCaptureModes.first { item ->
            when (item) {
                is SingleSelectableUiState.SelectableUi -> item.value == CaptureMode.VIDEO_ONLY
                is SingleSelectableUiState.Disabled -> item.value == CaptureMode.VIDEO_ONLY
            }
        }
        CaptureModeToggleUiState.Available(
            selectedCaptureMode = cameraAppSettings.captureMode,
            imageOnlyUiState = imageOnlyState,
            videoOnlyUiState = videoOnlyState
        )
    } else {
        CaptureModeToggleUiState.Unavailable
    }

fun CaptureModeUiState.Companion.from(
    systemConstraints: SystemConstraints,
    cameraAppSettings: CameraAppSettings,
    previewMode: PreviewMode
): CaptureModeUiState {
    val availableCaptureModes = getAvailableCaptureModes(
        systemConstraints,
        cameraAppSettings,
        previewMode
    )
    return CaptureModeUiState.Available(
        selectedCaptureMode = cameraAppSettings.captureMode,
        availableCaptureModes = availableCaptureModes
    )
}

private fun getSupportedCaptureModes(
    cameraAppSettings: CameraAppSettings,
    isHdrOn: Boolean,
    currentHdrDynamicRangeSupported: Boolean,
    currentHdrImageFormatSupported: Boolean,
    previewMode: PreviewMode
): List<CaptureMode> = if (
    previewMode != PreviewMode.EXTERNAL_IMAGE_CAPTURE &&
    previewMode != PreviewMode.EXTERNAL_VIDEO_CAPTURE &&
    currentHdrDynamicRangeSupported &&
    currentHdrImageFormatSupported &&
    cameraAppSettings.concurrentCameraMode == ConcurrentCameraMode.OFF
) {
    // do not allow both use cases to be bound if hdr is on
    if (isHdrOn) {
        listOf(CaptureMode.IMAGE_ONLY, CaptureMode.VIDEO_ONLY)
    } else {
        listOf(CaptureMode.STANDARD, CaptureMode.IMAGE_ONLY, CaptureMode.VIDEO_ONLY)
    }
} else if (
    cameraAppSettings.concurrentCameraMode == ConcurrentCameraMode.OFF &&
    previewMode == PreviewMode.EXTERNAL_IMAGE_CAPTURE ||
    cameraAppSettings.imageFormat == ImageOutputFormat.JPEG_ULTRA_HDR
) {
    listOf(CaptureMode.IMAGE_ONLY)
} else {
    listOf(CaptureMode.VIDEO_ONLY)
}

private fun getAvailableCaptureModes(
    systemConstraints: SystemConstraints,
    cameraAppSettings: CameraAppSettings,
    previewMode: PreviewMode
): List<SingleSelectableUiState<CaptureMode>> {
    val cameraConstraints: CameraConstraints? = systemConstraints.forCurrentLens(
        cameraAppSettings
    )
    val isHdrOn = cameraAppSettings.dynamicRange == DynamicRange.HLG10 ||
        cameraAppSettings.imageFormat == ImageOutputFormat.JPEG_ULTRA_HDR
    val currentHdrDynamicRangeSupported =
        if (isHdrOn) {
            cameraConstraints?.supportedDynamicRanges?.contains(DynamicRange.HLG10) == true
        } else {
            true
        }

    val currentHdrImageFormatSupported =
        if (isHdrOn) {
            cameraConstraints?.supportedImageFormatsMap?.get(
                cameraAppSettings.streamConfig
            )?.contains(ImageOutputFormat.JPEG_ULTRA_HDR) == true
        } else {
            true
        }
    val supportedCaptureModes = getSupportedCaptureModes(
        cameraAppSettings,
        isHdrOn,
        currentHdrDynamicRangeSupported,
        currentHdrImageFormatSupported,
        previewMode
    )
    // if all capture modes are supported, return capturemodeuistate
    if (supportedCaptureModes.containsAll(ORDERED_UI_SUPPORTED_CAPTURE_MODES)) {
        return ORDERED_UI_SUPPORTED_CAPTURE_MODES.filter {
            it in supportedCaptureModes
        }.map { supportedCaptureMode ->
            SingleSelectableUiState.SelectableUi(supportedCaptureMode)
        }
    }
    // if all capture modes are not supported, give disabledReason
    // if image or video is not supported, default will also be disabled
    else {
        if (!supportedCaptureModes.contains(CaptureMode.VIDEO_ONLY)) {
            val disabledReason =
                getCaptureModeDisabledReason(
                    disabledCaptureMode = CaptureMode.VIDEO_ONLY,
                    hdrDynamicRangeSupported = currentHdrDynamicRangeSupported,
                    hdrImageFormatSupported = currentHdrImageFormatSupported,
                    systemConstraints = systemConstraints,
                    cameraAppSettings.cameraLensFacing,
                    cameraAppSettings.streamConfig,
                    cameraAppSettings.concurrentCameraMode,
                    previewMode = previewMode
                )
            return listOf(
                SingleSelectableUiState.SelectableUi(CaptureMode.IMAGE_ONLY),
                SingleSelectableUiState.Disabled(
                    CaptureMode.VIDEO_ONLY,
                    disabledReason = disabledReason
                ),
                SingleSelectableUiState.Disabled(
                    CaptureMode.STANDARD,
                    disabledReason = disabledReason
                )
            )
        } else if (!supportedCaptureModes.contains(CaptureMode.IMAGE_ONLY)) {
            val disabledReason =
                getCaptureModeDisabledReason(
                    disabledCaptureMode = CaptureMode.IMAGE_ONLY,
                    currentHdrDynamicRangeSupported,
                    currentHdrImageFormatSupported,
                    systemConstraints,
                    cameraAppSettings.cameraLensFacing,
                    cameraAppSettings.streamConfig,
                    cameraAppSettings.concurrentCameraMode,
                    previewMode = previewMode
                )
            return listOf(
                SingleSelectableUiState.SelectableUi(CaptureMode.VIDEO_ONLY),
                SingleSelectableUiState.Disabled(
                    CaptureMode.IMAGE_ONLY,
                    disabledReason = disabledReason
                ),
                SingleSelectableUiState.Disabled(
                    CaptureMode.STANDARD,
                    disabledReason = disabledReason
                )
            )
        } else {
            return listOf(
                SingleSelectableUiState.SelectableUi(CaptureMode.VIDEO_ONLY),
                SingleSelectableUiState.SelectableUi(CaptureMode.IMAGE_ONLY),
                SingleSelectableUiState.Disabled(
                    CaptureMode.STANDARD,
                    disabledReason = DisabledReason.HDR_SIMULTANEOUS_IMAGE_VIDEO_UNSUPPORTED
                )
            )
        }
    }
}

private fun getCaptureModeDisabledReason(
    disabledCaptureMode: CaptureMode,
    hdrDynamicRangeSupported: Boolean,
    hdrImageFormatSupported: Boolean,
    systemConstraints: SystemConstraints,
    currentLensFacing: LensFacing,
    currentStreamConfig: StreamConfig,
    concurrentCameraMode: ConcurrentCameraMode,
    previewMode: PreviewMode
): DisabledReason {
    when (disabledCaptureMode) {
        CaptureMode.IMAGE_ONLY -> {
            if (previewMode == PreviewMode.EXTERNAL_VIDEO_CAPTURE) {
                return DisabledReason
                    .IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED
            }

            if (concurrentCameraMode == ConcurrentCameraMode.DUAL) {
                return DisabledReason
                    .IMAGE_CAPTURE_UNSUPPORTED_CONCURRENT_CAMERA
            }

            if (!hdrImageFormatSupported) {
                // First check if Ultra HDR image is supported on other capture modes
                if (systemConstraints
                        .perLensConstraints[currentLensFacing]
                        ?.supportedImageFormatsMap
                        ?.anySupportsUltraHdr { it != currentStreamConfig } == true
                ) {
                    return when (currentStreamConfig) {
                        StreamConfig.MULTI_STREAM ->
                            DisabledReason
                                .HDR_IMAGE_UNSUPPORTED_ON_MULTI_STREAM

                        StreamConfig.SINGLE_STREAM ->
                            DisabledReason
                                .HDR_IMAGE_UNSUPPORTED_ON_SINGLE_STREAM
                    }
                }

                // Check if any other lens supports HDR image
                if (systemConstraints.anySupportsUltraHdr { it != currentLensFacing }) {
                    return DisabledReason.HDR_IMAGE_UNSUPPORTED_ON_LENS
                }

                // No lenses support HDR image on device
                return DisabledReason.HDR_IMAGE_UNSUPPORTED_ON_DEVICE
            }

            throw RuntimeException("Unknown DisabledReason for capture mode.")
        }

        CaptureMode.VIDEO_ONLY -> {
            if (previewMode == PreviewMode.EXTERNAL_IMAGE_CAPTURE ||
                previewMode == PreviewMode.EXTERNAL_MULTIPLE_IMAGE_CAPTURE
            ) {
                return DisabledReason
                    .VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED
            }

            if (!hdrDynamicRangeSupported) {
                if (systemConstraints.anySupportsHdrDynamicRange { it != currentLensFacing }) {
                    return DisabledReason.HDR_VIDEO_UNSUPPORTED_ON_LENS
                }
                return DisabledReason.HDR_VIDEO_UNSUPPORTED_ON_DEVICE
            }

            throw RuntimeException("Unknown DisabledReason for video mode.")
        }

        CaptureMode.STANDARD -> {
            TODO()
        }
    }
}

private fun SystemConstraints.anySupportsHdrDynamicRange(
    lensFilter: (LensFacing) -> Boolean
): Boolean = perLensConstraints.asSequence().firstOrNull {
    lensFilter(it.key) && it.value.supportedDynamicRanges.size > 1
} != null

private fun Map<StreamConfig, Set<ImageOutputFormat>>.anySupportsUltraHdr(
    captureModeFilter: (StreamConfig) -> Boolean
): Boolean = asSequence().firstOrNull {
    captureModeFilter(it.key) && it.value.contains(ImageOutputFormat.JPEG_ULTRA_HDR)
} != null

private fun SystemConstraints.anySupportsUltraHdr(
    captureModeFilter: (StreamConfig) -> Boolean = { true },
    lensFilter: (LensFacing) -> Boolean
): Boolean = perLensConstraints.asSequence().firstOrNull { lensConstraints ->
    lensFilter(lensConstraints.key) &&
        lensConstraints.value.supportedImageFormatsMap.anySupportsUltraHdr {
            captureModeFilter(it)
        }
} != null
