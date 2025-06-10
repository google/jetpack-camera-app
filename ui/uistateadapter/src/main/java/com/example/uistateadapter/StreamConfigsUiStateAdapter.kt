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

package com.example.uistateadapter

import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.ConcurrentCameraMode
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.StreamConfig
import com.google.jetpackcamera.ui.uistate.StreamConfigUiState

object StreamConfigsUiStateAdapter {
    private val ORDERED_UI_SUPPORTED_STREAM_CONFIGS = listOf(
        StreamConfig.SINGLE_STREAM,
        StreamConfig.MULTI_STREAM
    )

    fun getUiState(cameraAppSettings: CameraAppSettings): StreamConfigUiState {
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
                isActive = !(concurrentCameraMode == ConcurrentCameraMode.DUAL ||
                        imageOutputFormat == ImageOutputFormat.JPEG_ULTRA_HDR)
            )
        }
    }
}