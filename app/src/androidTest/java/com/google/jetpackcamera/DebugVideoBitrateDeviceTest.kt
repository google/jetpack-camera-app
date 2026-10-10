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
package com.google.jetpackcamera

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.truth.TruthJUnit.assume
import com.google.jetpackcamera.ui.debug.DEBUG_OVERLAY_AUDIO_BITRATE_TAG
import com.google.jetpackcamera.ui.debug.DEBUG_OVERLAY_BUTTON
import com.google.jetpackcamera.ui.debug.DEBUG_OVERLAY_SET_AUDIO_BITRATE_TEXT_FIELD
import com.google.jetpackcamera.ui.debug.DEBUG_OVERLAY_SET_BITRATE_BUTTON
import com.google.jetpackcamera.ui.debug.DEBUG_OVERLAY_SET_BITRATE_CONFIRM_BUTTON
import com.google.jetpackcamera.ui.debug.DEBUG_OVERLAY_SET_VIDEO_BITRATE_TEXT_FIELD
import com.google.jetpackcamera.ui.debug.DEBUG_OVERLAY_VIDEO_BITRATE_TAG
import com.google.jetpackcamera.utils.MOVIES_DIR_PATH
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.VIDEO_PREFIX
import com.google.jetpackcamera.utils.debugExtra
import com.google.jetpackcamera.utils.deleteFilesInDirAfterTimestamp
import com.google.jetpackcamera.utils.doesMediaExist
import com.google.jetpackcamera.utils.getAudioTrackBitrate
import com.google.jetpackcamera.utils.getSingleImageCaptureIntent
import com.google.jetpackcamera.utils.getTestUri
import com.google.jetpackcamera.utils.getVideoTrackBitrate
import com.google.jetpackcamera.utils.longClickForVideoRecordingCheckingElapsedTime
import com.google.jetpackcamera.utils.runMainActivityScenarioTestForResult
import com.google.jetpackcamera.utils.waitForCaptureButton
import com.google.jetpackcamera.utils.waitForNodeWithTag
import com.google.jetpackcamera.utils.waitForNodeWithTagToDisappear
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device tests verifying that the debug target video and audio bitrates are applied to recorded
 * videos.
 *
 * Encoders treat the target bitrate as a rate-control hint, short clips are noisy, and CameraX
 * clamps the target to the encoder's supported range. Codecs that implement the platform's video
 * minimum-quality floor also raise low targets (around 4 Mbps at 1080p). These tests therefore use
 * targets above that floor and compare measured bitrates relative to each other or against a loose
 * upper bound, rather than asserting an exact value.
 */
@RunWith(AndroidJUnit4::class)
class DebugVideoBitrateDeviceTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiDevice = UiDevice.getInstance(instrumentation)

    @Before
    fun setUp() {
        // The emulator's synthetic camera frames compress far below any realistic target, so the
        // recorded bitrate does not reflect the configured target.
        assume().withMessage("Recorded bitrate is not representative on emulators")
            .that(Build.HARDWARE == "ranchu")
            .isFalse()
        assertThat(uiDevice.isScreenOn).isTrue()
    }

    @Test
    fun targetBitrate_viaIntentExtra_controlsRecordedVideoAndAudioBitrates() {
        val lowBitrates = recordVideoAndMeasureBitrates(
            extras = debugBitrateExtras(
                videoBitrateBps = LOW_TARGET_VIDEO_BITRATE_BPS,
                audioBitrateBps = LOW_TARGET_AUDIO_BITRATE_BPS
            )
        )
        val highBitrates = recordVideoAndMeasureBitrates(
            extras = debugBitrateExtras(
                videoBitrateBps = HIGH_TARGET_VIDEO_BITRATE_BPS,
                audioBitrateBps = HIGH_TARGET_AUDIO_BITRATE_BPS
            )
        )

        assertWithMessage("Video bitrate with high target should exceed bitrate with low target")
            .that(highBitrates.videoBitrate)
            .isGreaterThan(lowBitrates.videoBitrate.scaledBy(MIN_SEPARATION_FACTOR))
        assertWithMessage("Video bitrate with low target should be near the target")
            .that(lowBitrates.videoBitrate)
            .isAtMost(LOW_TARGET_VIDEO_BITRATE_BPS.toLong() * BITRATE_TOLERANCE_FACTOR)
        assertWithMessage("Video bitrate with high target should be near the target")
            .that(highBitrates.videoBitrate)
            .isAtMost(HIGH_TARGET_VIDEO_BITRATE_BPS.toLong() * BITRATE_TOLERANCE_FACTOR)

        assertWithMessage("Audio bitrate with high target should exceed bitrate with low target")
            .that(highBitrates.audioBitrate)
            .isGreaterThan(lowBitrates.audioBitrate.scaledBy(MIN_SEPARATION_FACTOR))
        assertWithMessage("Audio bitrate with low target should be near the target")
            .that(lowBitrates.audioBitrate)
            .isAtMost(LOW_TARGET_AUDIO_BITRATE_BPS.toLong() * BITRATE_TOLERANCE_FACTOR)
        assertWithMessage("Audio bitrate with high target should be near the target")
            .that(highBitrates.audioBitrate)
            .isAtMost(HIGH_TARGET_AUDIO_BITRATE_BPS.toLong() * BITRATE_TOLERANCE_FACTOR)
    }

    @Test
    fun targetBitrate_viaIntentExtra_isIgnoredWhenDebugModeDisabled() {
        val lowBitrates = recordVideoAndMeasureBitrates(
            extras = debugBitrateExtras(
                videoBitrateBps = LOW_TARGET_VIDEO_BITRATE_BPS,
                audioBitrateBps = LOW_TARGET_AUDIO_BITRATE_BPS
            )
        )
        val ignoredBitrates = recordVideoAndMeasureBitrates(
            extras = Bundle().apply {
                putInt(KEY_DEBUG_VIDEO_BITRATE, LOW_TARGET_VIDEO_BITRATE_BPS)
                putInt(KEY_DEBUG_AUDIO_BITRATE, LOW_TARGET_AUDIO_BITRATE_BPS)
            }
        )

        assertWithMessage("Video bitrate extra should be ignored when debug mode is disabled")
            .that(ignoredBitrates.videoBitrate)
            .isGreaterThan(lowBitrates.videoBitrate.scaledBy(MIN_SEPARATION_FACTOR))
        assertWithMessage("Audio bitrate extra should be ignored when debug mode is disabled")
            .that(ignoredBitrates.audioBitrate)
            .isGreaterThan(lowBitrates.audioBitrate.scaledBy(MIN_SEPARATION_FACTOR))
    }

    @Test
    fun lowTargetBitrate_viaDebugOverlay_reducesRecordedVideoAndAudioBitrates() {
        val defaultBitrates = recordVideoAndMeasureBitrates(extras = debugExtra)
        val lowBitrates = recordVideoAndMeasureBitrates(extras = debugExtra) {
            setBitratesViaDebugOverlay(
                videoBitrateBps = LOW_TARGET_VIDEO_BITRATE_BPS,
                audioBitrateBps = LOW_TARGET_AUDIO_BITRATE_BPS
            )
        }

        assertWithMessage("Video bitrate with low target should be lower than default bitrate")
            .that(defaultBitrates.videoBitrate)
            .isGreaterThan(lowBitrates.videoBitrate.scaledBy(MIN_SEPARATION_FACTOR))
        assertWithMessage("Video bitrate with low target should be near the target")
            .that(lowBitrates.videoBitrate)
            .isAtMost(LOW_TARGET_VIDEO_BITRATE_BPS.toLong() * BITRATE_TOLERANCE_FACTOR)

        assertWithMessage("Audio bitrate with low target should be lower than default bitrate")
            .that(defaultBitrates.audioBitrate)
            .isGreaterThan(lowBitrates.audioBitrate.scaledBy(MIN_SEPARATION_FACTOR))
        assertWithMessage("Audio bitrate with low target should be near the target")
            .that(lowBitrates.audioBitrate)
            .isAtMost(LOW_TARGET_AUDIO_BITRATE_BPS.toLong() * BITRATE_TOLERANCE_FACTOR)
    }

    private data class RecordedBitrates(val videoBitrate: Long, val audioBitrate: Long)

    /**
     * Records a video to an explicit file URI via [MediaStore.ACTION_VIDEO_CAPTURE], returns the
     * measured bitrates of its video and audio tracks, and deletes the recorded file.
     *
     * @param extras extras passed to the activity.
     * @param beforeRecording actions to perform after the camera is ready and before recording.
     */
    private fun recordVideoAndMeasureBitrates(
        extras: Bundle,
        beforeRecording: () -> Unit = {}
    ): RecordedBitrates {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(MOVIES_DIR_PATH, timeStamp, "mp4")
        try {
            val result = runMainActivityScenarioTestForResult(
                getSingleImageCaptureIntent(uri, MediaStore.ACTION_VIDEO_CAPTURE),
                extras = extras
            ) {
                composeTestRule.waitForCaptureButton()
                beforeRecording()
                composeTestRule.waitForCaptureButton()
                composeTestRule.longClickForVideoRecordingCheckingElapsedTime(
                    durationMillis = RECORDING_DURATION_MILLIS
                )
            }
            assertThat(result.resultCode).isEqualTo(Activity.RESULT_OK)
            assertThat(doesMediaExist(uri, VIDEO_PREFIX)).isTrue()

            val filePath = checkNotNull(uri.path)
            val videoBitrate = getVideoTrackBitrate(filePath)
            val audioBitrate = getAudioTrackBitrate(filePath)
            Log.d(
                TAG,
                "Measured bitrates (video=$videoBitrate bps, audio=$audioBitrate bps) " +
                    "for extras: $extras"
            )
            return RecordedBitrates(videoBitrate = videoBitrate, audioBitrate = audioBitrate)
        } finally {
            deleteFilesInDirAfterTimestamp(MOVIES_DIR_PATH, instrumentation, timeStamp)
        }
    }

    private fun setBitratesViaDebugOverlay(videoBitrateBps: Int, audioBitrateBps: Int) {
        composeTestRule.onNodeWithTag(DEBUG_OVERLAY_BUTTON).performClick()
        composeTestRule.waitForNodeWithTag(DEBUG_OVERLAY_SET_BITRATE_BUTTON)
        composeTestRule.onNodeWithTag(DEBUG_OVERLAY_SET_BITRATE_BUTTON).performClick()

        composeTestRule.waitForNodeWithTag(DEBUG_OVERLAY_SET_VIDEO_BITRATE_TEXT_FIELD)
        composeTestRule.onNodeWithTag(DEBUG_OVERLAY_SET_VIDEO_BITRATE_TEXT_FIELD)
            .performTextInput(videoBitrateBps.toString())
        composeTestRule.onNodeWithTag(DEBUG_OVERLAY_SET_AUDIO_BITRATE_TEXT_FIELD)
            .performTextInput(audioBitrateBps.toString())
        composeTestRule.onNodeWithTag(DEBUG_OVERLAY_SET_BITRATE_CONFIRM_BUTTON)
            .performClick()

        // Wait for the debug menu to reflect the new bitrates, then close the debug overlay.
        composeTestRule.waitForNodeWithTag(DEBUG_OVERLAY_SET_BITRATE_BUTTON)
        composeTestRule.onNodeWithTag(DEBUG_OVERLAY_VIDEO_BITRATE_TAG, useUnmergedTree = true)
            .assertTextEquals("$videoBitrateBps bps")
        composeTestRule.onNodeWithTag(DEBUG_OVERLAY_AUDIO_BITRATE_TAG, useUnmergedTree = true)
            .assertTextEquals("$audioBitrateBps bps")
        uiDevice.pressBack()
        composeTestRule.waitForNodeWithTagToDisappear(DEBUG_OVERLAY_SET_BITRATE_BUTTON)
    }

    private fun debugBitrateExtras(videoBitrateBps: Int, audioBitrateBps: Int): Bundle =
        Bundle(debugExtra).apply {
            putInt(KEY_DEBUG_VIDEO_BITRATE, videoBitrateBps)
            putInt(KEY_DEBUG_AUDIO_BITRATE, audioBitrateBps)
        }

    private fun Long.scaledBy(factor: Double): Long = (this * factor).toLong()

    private companion object {
        const val TAG = "DebugVideoBitrateTest"
        const val KEY_DEBUG_VIDEO_BITRATE = "KEY_DEBUG_VIDEO_BITRATE"
        const val KEY_DEBUG_AUDIO_BITRATE = "KEY_DEBUG_AUDIO_BITRATE"
        const val RECORDING_DURATION_MILLIS = 3_000L

        // Above the codec minimum-quality floor at 1080p and well below the default bitrate.
        const val LOW_TARGET_VIDEO_BITRATE_BPS = 6_000_000
        const val HIGH_TARGET_VIDEO_BITRATE_BPS = 24_000_000

        // Within the AAC encoder's supported range and well below/above the 192 kbps default.
        const val LOW_TARGET_AUDIO_BITRATE_BPS = 32_000
        const val HIGH_TARGET_AUDIO_BITRATE_BPS = 256_000

        // Minimum ratio between measured bitrates that are expected to differ.
        const val MIN_SEPARATION_FACTOR = 1.5

        // Loose upper bound to account for encoder rate-control variance on short clips.
        const val BITRATE_TOLERANCE_FACTOR = 2
    }
}
