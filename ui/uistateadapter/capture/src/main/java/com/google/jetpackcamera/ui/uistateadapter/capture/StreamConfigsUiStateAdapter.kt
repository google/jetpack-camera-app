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

import com.example.uistateadapter.Utils
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.ui.uistate.capture.StreamConfigUiState

private val ORDERED_UI_SUPPORTED_STREAM_CONFIGS = listOf(
    StreamConfig.SINGLE_STREAM,
    StreamConfig.MULTI_STREAM
)

/**
 * Creates a [StreamConfigUiState] based on the provided camera settings.
 *
 * This function determines the availability and selection state of stream configurations, which
 * control whether the camera operates using a single stream or multiple streams. The UI for this
 * setting is made available only if more than one stream configuration is supported. The control
 * is marked as inactive (disabled) if concurrent camera mode is active or if the image format
 * is set to Ultra HDR, as these features are incompatible with changing the stream configuration.
 *
 * @param cameraAppSettings The current application settings, which provide the selected
 *   [StreamConfig], [ConcurrentCameraMode], and [ImageOutputFormat].
 *
 * @return A [StreamConfigUiState] which will be:
 * - [StreamConfigUiState.Available] if multiple stream configs are supported, containing the
 *   current selection, the list of available options, and whether the UI control should be active.
 * - [StreamConfigUiState.Unavailable] if only one or zero stream configs are supported, meaning
 *   the user cannot change this setting.
 */
fun StreamConfigUiState.Companion.from(cameraAppSettings: CameraAppSettings): StreamConfigUiState {
    return createFrom(
        cameraAppSettings.streamConfig,
        ORDERED_UI_SUPPORTED_STREAM_CONFIGS.toSet(),
        cameraAppSettings.concurrentCameraMode,
        cameraAppSettings.imageFormat
    )
}

private fun createFrom(
    selectedStreamConfig: StreamConfig,
    supportedStreamConfigs: Set<StreamConfig>,
    concurrentCameraMode: ConcurrentCameraMode,
    imageOutputFormat: ImageOutputFormat
): StreamConfigUiState {
    // Ensure we at least support one flash mode
    check(supportedStreamConfigs.isNotEmpty()) {
        "No stream config supported."
    }

    val availableStreamConfigs =
        Utils.getSelectableListFromValues(
            supportedStreamConfigs,
            ORDERED_UI_SUPPORTED_STREAM_CONFIGS
        )

    return if (supportedStreamConfigs.size <= 1) {
        // If we only support one lens, then return "Unavailable".
        StreamConfigUiState.Unavailable
    } else {
        StreamConfigUiState.Available(
            selectedStreamConfig = selectedStreamConfig,
            availableStreamConfigs = availableStreamConfigs,
            isActive = !(
                concurrentCameraMode == ConcurrentCameraMode.DUAL ||
                    imageOutputFormat == ImageOutputFormat.JPEG_ULTRA_HDR
                )
        )
    }
}
