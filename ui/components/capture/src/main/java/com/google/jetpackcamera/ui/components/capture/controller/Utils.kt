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
package com.google.jetpackcamera.ui.components.capture.controller

import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.data.media.MediaRepository
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.IntProgress
import com.google.jetpackcamera.model.SaveLocation
import com.google.jetpackcamera.model.SaveMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object Utils {
    /**
     * Posts the given [MediaDescriptor] to the [MediaRepository] as the current media.
     *
     * @param viewModelScope The [CoroutineScope] for launching the coroutine.
     * @param mediaRepository The repository to update.
     * @param mediaDescriptor The media to set as current.
     */
    fun postCurrentMediaToMediaRepository(
        viewModelScope: CoroutineScope,
        mediaRepository: MediaRepository,
        mediaDescriptor: MediaDescriptor
    ) {
        viewModelScope.launch {
            mediaRepository.setCurrentMedia(mediaDescriptor)
        }
    }

    fun nextSaveLocation(
        saveMode: SaveMode,
        externalCaptureMode: ExternalCaptureMode,
        externalCapturesCallback: () -> Pair<SaveLocation, IntProgress?>
    ): Pair<SaveLocation, IntProgress?> {
        return when (externalCaptureMode) {
            ExternalCaptureMode.ImageCapture,
            ExternalCaptureMode.MultipleImageCapture,
            ExternalCaptureMode.VideoCapture -> {
                externalCapturesCallback()
            }

            ExternalCaptureMode.Standard -> {
                val defaultSaveLocation =
                    if (saveMode is SaveMode.CacheAndReview) {
                        SaveLocation.Cache(saveMode.cacheDir)
                    } else {
                        SaveLocation.Default
                    }
                Pair(defaultSaveLocation, null)
            }
        }
    }
}
