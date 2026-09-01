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
package com.google.jetpackcamera.core.camera

import android.hardware.camera2.CameraMetadata
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusMeteringTest {

    private val standardDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(standardDispatcher)

    @Test
    fun awaitClearFocusLock_whenNoMetadata_triggers15sFallback() = testScope.runTest {
        val sceneChangeStatusChannel = Channel<Int?>()
        var completed = false
        val job = launch {
            awaitClearFocusLock(sceneChangeStatusChannel.receiveAsFlow())
            completed = true
        }

        advanceTimeBy(10000)
        assertThat(completed).isFalse() // Hasn't completed yet

        advanceTimeBy(6000)
        assertThat(completed).isTrue() // Timeout reached!
    }

    @Test
    fun awaitClearFocusLock_whenMetadataProvidesSceneChange_completesRapidly() = testScope.runTest {
        val sceneChangeStatusChannel = Channel<Int?>()
        var completed = false
        val job = launch {
            awaitClearFocusLock(sceneChangeStatusChannel.receiveAsFlow())
            completed = true
        }

        advanceTimeBy(1000)
        assertThat(completed).isFalse() // Waiting for frames...

        // Emulate 3 successful frames
        sceneChangeStatusChannel.send(CameraMetadata.CONTROL_AF_SCENE_CHANGE_DETECTED)
        sceneChangeStatusChannel.send(CameraMetadata.CONTROL_AF_SCENE_CHANGE_DETECTED)
        sceneChangeStatusChannel.send(CameraMetadata.CONTROL_AF_SCENE_CHANGE_DETECTED)

        advanceTimeBy(100)
        assertThat(completed).isTrue() // Completed early without hitting 15s timeout
    }

    @Test
    fun awaitClearFocusLock_resetsCount_whenNotDetected() = testScope.runTest {
        val sceneChangeStatusChannel = Channel<Int?>()
        var completed = false
        val job = launch {
            awaitClearFocusLock(sceneChangeStatusChannel.receiveAsFlow())
            completed = true
        }

        // Emulate 2 successful frames, then a failure, then 3 successful
        sceneChangeStatusChannel.send(CameraMetadata.CONTROL_AF_SCENE_CHANGE_DETECTED)
        sceneChangeStatusChannel.send(CameraMetadata.CONTROL_AF_SCENE_CHANGE_DETECTED)
        sceneChangeStatusChannel.send(CameraMetadata.CONTROL_AF_SCENE_CHANGE_NOT_DETECTED)

        advanceTimeBy(100)
        assertThat(completed).isFalse() // Should have reset count to 0!

        sceneChangeStatusChannel.send(CameraMetadata.CONTROL_AF_SCENE_CHANGE_DETECTED)
        sceneChangeStatusChannel.send(CameraMetadata.CONTROL_AF_SCENE_CHANGE_DETECTED)
        sceneChangeStatusChannel.send(CameraMetadata.CONTROL_AF_SCENE_CHANGE_DETECTED)

        advanceTimeBy(100)
        assertThat(completed).isTrue()
    }
}
