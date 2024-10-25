/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.provider.MediaStore
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.feature.preview.R
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_BUTTON
import com.google.jetpackcamera.feature.preview.ui.VIDEO_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.utils.APP_START_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.VIDEO_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.VIDEO_RECORDING_START_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.idleForVideoDuration
import com.google.jetpackcamera.utils.onNodeWithStateDescription
import com.google.jetpackcamera.utils.runMediaStoreAutoDeleteScenarioTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoAudioTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setUp() {
        with(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())) {
            assertThat(isScreenOn).isTrue()
        }
    }

    @Test
    fun audioIncomingWhenEnabled() = runMediaStoreAutoDeleteScenarioTest<MainActivity>(
        mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ) {
        // check audio visualizer composable for muted/unmuted icon.
        // icon will only be unmuted if audio is nonzero
        with(composeTestRule) {
            waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
            }

            // start recording video
            onNodeWithTag(CAPTURE_BUTTON)
                .assertExists()
                .performTouchInput {
                    down(center)
                }

            try {
                // assert hot amplitude tag visible
                waitUntil(timeoutMillis = VIDEO_RECORDING_START_TIMEOUT_MILLIS) {
                    onNodeWithStateDescription(
                        R.string.audio_visualizer_recording_state_description
                    ).isDisplayed()
                }

                // Ensure we record long enough to create a successful recording
                idleForVideoDuration()
            } finally {
                // finish recording video
                onNodeWithTag(CAPTURE_BUTTON)
                    .assertExists()
                    .performTouchInput {
                        up()
                    }

                // Wait for recording to finish
                waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
                    onNodeWithTag(VIDEO_CAPTURE_SUCCESS_TAG).isDisplayed()
                }
            }
        }
    }
}
