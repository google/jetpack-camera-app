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
package com.google.jetpackcamera.ui.controller.testing

import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.ui.controller.ImageWellController

/**
 * A fake implementation of [ImageWellController] that allows for configuring actions for its methods.
 *
 * @param imageWellToRepositoryAction The action to perform when [imageWellToRepository] is called.
 * @param updateLastCapturedMediaAction The action to perform when [updateLastCapturedMedia] is called.
 */
class FakeImageWellController(
    var imageWellToRepositoryAction: (MediaDescriptor) -> Unit = {},
    var updateLastCapturedMediaAction: () -> Unit = {}
) : ImageWellController {
    override fun imageWellToRepository(mediaDescriptor: MediaDescriptor) {
        imageWellToRepositoryAction(mediaDescriptor)
    }

    override fun updateLastCapturedMedia() {
        updateLastCapturedMediaAction()
    }
}
