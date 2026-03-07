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
package com.google.jetpackcamera.ui.controller

import com.google.jetpackcamera.data.media.MediaDescriptor

/**
 * Interface for controlling the image well.
 */
interface ImageWellController {
    /**
     * Sets the specified media as the current item in the media repository, typically before
     * navigating to the post-capture screen.
     *
     * @param mediaDescriptor The media descriptor to be set.
     */
    fun imageWellToRepository(mediaDescriptor: MediaDescriptor)
    /**
     * Updates the image well to display the most recently captured media from the repository.
     */
    fun updateLastCapturedMedia()
}