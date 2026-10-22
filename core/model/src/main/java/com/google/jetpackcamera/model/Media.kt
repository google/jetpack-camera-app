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
package com.google.jetpackcamera.model

import android.graphics.Bitmap
import android.net.Uri

/**
 * Media items represented by a [MediaDescriptor].
 *
 * [Image] will have the bitmap data loaded.
 * [Video] is still a reference to the video file, will switch to a loaded version later on.
 *
 * TODO(yasith): Load the video data to the Video object.
 */
sealed interface Media {
    data object None : Media
    data object Error : Media
    class Image(val bitmap: Bitmap) : Media
    class Video(val uri: Uri) : Media
}
