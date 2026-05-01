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

import android.content.ContentResolver
import com.google.jetpackcamera.model.CaptureEvent
import com.google.jetpackcamera.ui.controller.CaptureController
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * A fake implementation of [CaptureController] that allows for configuring actions for its methods.
 *
 * @param captureEvents The [ReceiveChannel] for [CaptureEvent]s.
 * @param captureImageAction The action to perform when [captureImage] is called.
 * @param startVideoRecordingAction The action to perform when [startVideoRecording] is called.
 * @param stopVideoRecordingAction The action to perform when [stopVideoRecording] is called.
 * @param setLockedRecordingAction The action to perform when [setLockedRecording] is called.
 * @param setPausedAction The action to perform when [setPaused] is called.
 * @param setAudioEnabledAction The action to perform when [setAudioEnabled] is called.
 */
class FakeCaptureController(
    override val captureEvents: ReceiveChannel<CaptureEvent> = Channel(Channel.UNLIMITED),
    var captureImageAction: (ContentResolver) -> Unit = {},
    var startVideoRecordingAction: () -> Unit = {},
    var stopVideoRecordingAction: () -> Unit = {},
    var setLockedRecordingAction: (Boolean) -> Unit = {},
    var setPausedAction: (Boolean) -> Unit = {},
    var setAudioEnabledAction: (Boolean) -> Unit = {}
) : CaptureController {
    /**
     * Simulates a [CaptureEvent] being emitted by the controller.
     * This relies on the [captureEvents] instance being a [Channel].
     */
    fun simulateCaptureEvent(event: CaptureEvent) {
        (captureEvents as? Channel<CaptureEvent>)?.trySend(event)
            ?: throw IllegalStateException("captureEvents is not a Channel")
    }

    override fun captureImage(contentResolver: ContentResolver) {
        captureImageAction(contentResolver)
    }

    override fun startVideoRecording() {
        startVideoRecordingAction()
    }

    override fun stopVideoRecording() {
        stopVideoRecordingAction()
    }

    override fun setLockedRecording(isLocked: Boolean) {
        setLockedRecordingAction(isLocked)
    }

    override fun setPaused(shouldBePaused: Boolean) {
        setPausedAction(shouldBePaused)
    }

    override fun setAudioEnabled(shouldEnableAudio: Boolean) {
        setAudioEnabledAction(shouldEnableAudio)
    }
}
