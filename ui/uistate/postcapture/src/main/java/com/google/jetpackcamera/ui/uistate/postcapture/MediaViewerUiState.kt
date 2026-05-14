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
package com.google.jetpackcamera.ui.uistate.postcapture

import android.graphics.Bitmap
import androidx.media3.common.Player

/**
 * Defines the UI state for the content viewer within the PostCaptureScreen.
 * This sealed interface represents the various states the media viewer can be in, such as loading,
 * displaying an error, or showing content.
 */
sealed interface MediaViewerUiState {
    /**
     * The viewer is in a loading state, typically shown when media is being prepared for display.
     */
    object Loading : MediaViewerUiState

    /**
     * An error has occurred, and the media cannot be displayed.
     */
    object Error : MediaViewerUiState

    /**
     * The viewer has content to display, which can be either a video or an image.
     */
    sealed interface Content : MediaViewerUiState {
        /**
         * Represents the state of video content.
         *
         * @property thumbnail An optional thumbnail to display while the video is loading.
         */
        sealed interface Video : Content {
            val thumbnail: Bitmap?

            /**
             * The video content is currently loading.
             *
             * @param thumbnail An optional thumbnail to display during loading.
             */
            data class Loading(override val thumbnail: Bitmap?) : Video

            /**
             * The video is ready for playback.
             *
             * @param player The [Player] instance used for video playback.
             * @param thumbnail An optional thumbnail that was displayed before playback started.
             */
            data class Ready(
                val player: Player,
                override val thumbnail: Bitmap?
            ) : Video
        }

        /**
         * The viewer is displaying an image.
         *
         * @param imageBitmap The [Bitmap] of the image to display.
         */
        data class Image(val imageBitmap: Bitmap) : Content
    }

    companion object
}
