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
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.RequiresDevice
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.ui.uistate.AMPLITUDE_HOT_TAG
import com.google.jetpackcamera.ui.uistate.CAPTURE_BUTTON
import com.google.jetpackcamera.utils.APP_START_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.runScenarioTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@RequiresDevice
class VideoAudioTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setUp() {
        assertThat(uiDevice.isScreenOn).isTrue()
    }

    @Test
    fun audioIncomingWhenEnabled() {
        runScenarioTest<MainActivity> {
            // check audio visualizer composable for muted/unmuted icon.
            // icon will only be unmuted if audio is nonzero
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
            }

            // record video
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                .assertExists().performTouchInput { longClick(durationMillis = 5000) }

            // assert hot amplitude tag visible
            uiDevice.wait(
                Until.findObject(By.res(AMPLITUDE_HOT_TAG)),
                5000
            )
        }
    }
}
