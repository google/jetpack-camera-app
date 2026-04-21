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

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.data.media.MediaDescriptor
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FakeImageWellControllerTest {
    @Test
    fun imageWellToRepository_invokesAction() {
        var calledDescriptor: MediaDescriptor? = null
        val controller = FakeImageWellController(
            imageWellToRepositoryAction = { calledDescriptor = it }
        )
        val descriptor = MediaDescriptor.Content.Image(Uri.EMPTY, null)
        controller.imageWellToRepository(descriptor)
        assertThat(calledDescriptor).isEqualTo(descriptor)
    }

    @Test
    fun updateLastCapturedMedia_invokesAction() {
        var called = false
        val controller = FakeImageWellController(updateLastCapturedMediaAction = { called = true })
        controller.updateLastCapturedMedia()
        assertThat(called).isTrue()
    }
}
