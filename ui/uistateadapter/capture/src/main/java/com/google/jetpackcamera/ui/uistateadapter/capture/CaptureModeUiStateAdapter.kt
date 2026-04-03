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
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.settings.api.OptionRestrictionConfig
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraConstraints
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.forCurrentLens
import com.google.jetpackcamera.ui.components.capture.DisabledReason
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState.*

private val ORDERED_UI_SUPPORTED_CAPTURE_MODES = listOf(
    CaptureMode.STANDARD,
    CaptureMode.IMAGE_ONLY,
    CaptureMode.VIDEO_ONLY
)

/**
 * Creates a [CaptureModeToggleUiState] based on the current camera and system state.
 *
 * This adapter determines whether the simplified capture mode toggle (between IMAGE_ONLY and
 * VIDEO_ONLY) should be available and what its state should be. The toggle is generally
 * unavailable if video is recording or if the current capture mode is STANDARD.
 *
 * @param systemConstraints The constraints of the entire camera system.
 * @param cameraAppSettings The current settings of the camera.
 * @param cameraState The real-time state of the camera hardware.
 * @param externalCaptureMode The mode influencing UI based on how the camera was launched.
 * @return A [CaptureModeToggleUiState] which is either [CaptureModeToggleUiState.Available]
 * containing the states for the image and video-only modes, or
 * [CaptureModeToggleUiState.Unavailable] if the toggle should not be shown.
 */
fun CaptureModeToggleUiState.Companion.from(
    systemConstraints: CameraSystemConstraints,
    cameraAppSettings: CameraAppSettings,
    cameraState: CameraState,
    externalCaptureMode: ExternalCaptureMode,
    restrictionConfig: OptionRestrictionConfig<CaptureMode>
): CaptureModeToggleUiState =
    if (cameraState.videoRecordingState !is VideoRecordingState.Inactive ||
        cameraAppSettings.captureMode == CaptureMode.STANDARD
    ) {
        CaptureModeToggleUiState.Unavailable
    } else {
        val availableCaptureModes = getAvailableCaptureModes(
            systemConstraints,
            cameraAppSettings,
            externalCaptureMode,
            restrictionConfig
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
        if (imageOnlyState is SingleSelectableUiState.Disabled || videoOnlyState is SingleSelectableUiState.Disabled)
            CaptureModeToggleUiState.Unavailable
        else
            CaptureModeToggleUiState.Available(
                selectedCaptureMode = cameraAppSettings.captureMode,
                imageOnlyUiState = imageOnlyState,
                videoOnlyUiState = videoOnlyState
            )
    }

/**
 * Creates a [CaptureModeUiState] for the full capture mode selection UI (e.g., in quick settings).
 *
 * This adapter is responsible for determining the list of all available and selectable capture
 * modes ([CaptureMode.STANDARD], [CaptureMode.IMAGE_ONLY], [CaptureMode.VIDEO_ONLY]) based on the
 * current system and camera constraints.
 *
 * @param systemConstraints The constraints of the entire camera system.
 * @param cameraAppSettings The current settings of the camera.
 * @param externalCaptureMode The mode influencing UI based on how the camera was launched.
 * @return A [CaptureModeUiState.Available] object containing the currently selected capture mode
 * and a list of all available modes, each represented as a [SingleSelectableUiState].
 */
fun CaptureModeUiState.Companion.from(
    systemConstraints: CameraSystemConstraints,
    restrictionConfig: OptionRestrictionConfig<CaptureMode>,
    cameraAppSettings: CameraAppSettings,
    externalCaptureMode: ExternalCaptureMode
): CaptureModeUiState {
    val availableCaptureModes = getAvailableCaptureModes(
        systemConstraints,
        cameraAppSettings,
        externalCaptureMode,
        restrictionConfig
    )
    return when (restrictionConfig) {
        is OptionRestrictionConfig.FullyRestricted -> CaptureModeUiState.Unavailable
        is OptionRestrictionConfig.NotRestricted, is OptionRestrictionConfig.OptionsEnabled ->
            CaptureModeUiState.Available(
                selectedCaptureMode = cameraAppSettings.captureMode,
                availableCaptureModes = availableCaptureModes
            )

    }
}

private fun getSupportedCaptureModes(
    cameraAppSettings: CameraAppSettings,
    config: OptionRestrictionConfig<CaptureMode>,
    isHdrOn: Boolean,
    currentHdrDynamicRangeSupported: Boolean,
    currentHdrImageFormatSupported: Boolean,
    externalCaptureMode: ExternalCaptureMode
): List<CaptureMode> {
    return when (config) {
        is OptionRestrictionConfig.NotRestricted -> ORDERED_UI_SUPPORTED_CAPTURE_MODES
        is OptionRestrictionConfig.FullyRestricted -> emptyList()
        is OptionRestrictionConfig.OptionsEnabled -> ORDERED_UI_SUPPORTED_CAPTURE_MODES
            .filter { it in config.enabledOptions }
    }.filter { captureMode ->
        when (captureMode) {
            // image-only supported if externalcaptureMode is NOT VideoCapture and Concurrent Camera is off
            CaptureMode.IMAGE_ONLY ->
                externalCaptureMode != ExternalCaptureMode.VideoCapture &&
                        cameraAppSettings
                            .concurrentCameraMode == ConcurrentCameraMode.OFF

            // video-only supported if externalcapturemode is neither imageCapture nor multipleImageCapture
            CaptureMode.VIDEO_ONLY ->
                externalCaptureMode != ExternalCaptureMode.ImageCapture &&
                        externalCaptureMode != ExternalCaptureMode.MultipleImageCapture

            //hybrid capture supported if external capture mode is standard, if HDR mode is off, and if concurrent camera is off
            CaptureMode.STANDARD ->
                externalCaptureMode == ExternalCaptureMode.Standard &&
                        currentHdrDynamicRangeSupported &&
                        currentHdrImageFormatSupported &&
                        !isHdrOn &&
                        cameraAppSettings
                            .concurrentCameraMode == ConcurrentCameraMode.OFF

        }
    }
}

private fun getAvailableCaptureModes(
    systemConstraints: CameraSystemConstraints,
    cameraAppSettings: CameraAppSettings,
    externalCaptureMode: ExternalCaptureMode,
    config: OptionRestrictionConfig<CaptureMode>
): List<SingleSelectableUiState<CaptureMode>> {
    // 1. start with all UI supported modes
    // 2. filter out modes that are restricted by config
    // 3. filter out modes that are not supported by the device given current settings
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
        config,
        isHdrOn,
        currentHdrDynamicRangeSupported,
        currentHdrImageFormatSupported,
        externalCaptureMode
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
                    restrictionConfig = config,
                    cameraAppSettings.cameraLensFacing,
                    cameraAppSettings.streamConfig,
                    cameraAppSettings.concurrentCameraMode,
                    externalCaptureMode = externalCaptureMode
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
                    config,
                    cameraAppSettings.cameraLensFacing,
                    cameraAppSettings.streamConfig,
                    cameraAppSettings.concurrentCameraMode,
                    externalCaptureMode = externalCaptureMode
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
    systemConstraints: CameraSystemConstraints,
    restrictionConfig: OptionRestrictionConfig<CaptureMode>,
    currentLensFacing: LensFacing,
    currentStreamConfig: StreamConfig,
    concurrentCameraMode: ConcurrentCameraMode,
    externalCaptureMode: ExternalCaptureMode
): DisabledReason {
    when (disabledCaptureMode) {
        CaptureMode.IMAGE_ONLY -> {
            if (externalCaptureMode == ExternalCaptureMode.VideoCapture) {
                return DisabledReason
                    .IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED
            }
            when (restrictionConfig) {
                is OptionRestrictionConfig.FullyRestricted -> return DisabledReason.IMAGE_CAPTURE_RESTRICTED
                is OptionRestrictionConfig.OptionsEnabled -> {
                    if (!restrictionConfig.enabledOptions.contains(disabledCaptureMode))
                        return DisabledReason.IMAGE_CAPTURE_RESTRICTED
                }

                is OptionRestrictionConfig.NotRestricted -> {}
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
            if (externalCaptureMode == ExternalCaptureMode.ImageCapture ||
                externalCaptureMode == ExternalCaptureMode.MultipleImageCapture
            ) {
                return DisabledReason
                    .VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED
            }

            when (restrictionConfig) {
                is OptionRestrictionConfig.FullyRestricted -> return DisabledReason.VIDEO_CAPTURE_RESTRICTED
                is OptionRestrictionConfig.OptionsEnabled -> {
                    if (!restrictionConfig.enabledOptions.contains(disabledCaptureMode))
                        return DisabledReason.VIDEO_CAPTURE_RESTRICTED
                }

                is OptionRestrictionConfig.NotRestricted -> {}
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
            when (restrictionConfig) {
                is OptionRestrictionConfig.FullyRestricted -> return DisabledReason.HYBRID_CAPTURE_RESTRICTED
                is OptionRestrictionConfig.OptionsEnabled -> {
                    if (!restrictionConfig.enabledOptions.contains(disabledCaptureMode))
                        return DisabledReason.HYBRID_CAPTURE_RESTRICTED
                }

                is OptionRestrictionConfig.NotRestricted -> {}
            }
            throw RuntimeException("Unknown DisabledReason for hybrid mode.")
        }
    }
}

private fun CameraSystemConstraints.anySupportsHdrDynamicRange(
    lensFilter: (LensFacing) -> Boolean
): Boolean = perLensConstraints.asSequence().firstOrNull {
    lensFilter(it.key) && it.value.supportedDynamicRanges.size > 1
} != null

private fun Map<StreamConfig, Set<ImageOutputFormat>>.anySupportsUltraHdr(
    captureModeFilter: (StreamConfig) -> Boolean
): Boolean = asSequence().firstOrNull {
    captureModeFilter(it.key) && it.value.contains(ImageOutputFormat.JPEG_ULTRA_HDR)
} != null

private fun CameraSystemConstraints.anySupportsUltraHdr(
    captureModeFilter: (StreamConfig) -> Boolean = { true },
    lensFilter: (LensFacing) -> Boolean
): Boolean = perLensConstraints.asSequence().firstOrNull { lensConstraints ->
    lensFilter(lensConstraints.key) &&
            lensConstraints.value.supportedImageFormatsMap.anySupportsUltraHdr {
                captureModeFilter(it)
            }
} != null
