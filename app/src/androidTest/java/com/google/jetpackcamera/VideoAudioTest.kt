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

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.RequiresDevice
import androidx.test.rule.GrantPermissionRule
import com.google.jetpackcamera.feature.preview.ui.AMPLITUDE_HOT_TAG
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_BUTTON
import com.google.jetpackcamera.feature.preview.ui.SETTINGS_BUTTON
import com.google.jetpackcamera.utils.APP_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.APP_START_TIMEOUT_MILLIS
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@RequiresDevice
class VideoAudioTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(APP_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun audioIncomingWhenEnabled(){
        // check audio visualizer composable for muted/unmuted icon.
        // icon will only be unmuted if audio is nonzero
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        //record video
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .assertExists().performTouchInput { longClick() }

        // assert hot amplitude tag visible
        composeTestRule.onNodeWithTag(AMPLITUDE_HOT_TAG).assertExists()
    }
    //todo assert device has audio settings enabled
    //todo mute while recording
    //todo unmute while recording
}