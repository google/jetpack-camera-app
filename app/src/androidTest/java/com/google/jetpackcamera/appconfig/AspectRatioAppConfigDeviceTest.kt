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
package com.google.jetpackcamera.appconfig

import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.jetpackcamera.AppModule
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppConfig
import com.google.jetpackcamera.settings.model.SettingConfig
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_1_1_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_3_4_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_9_16_BUTTON
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.visitQuickSettings
import com.google.jetpackcamera.utils.waitForCaptureButton
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class AspectRatioAppConfigDeviceTest {

    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @After
    fun tearDown() {
        AppModule.testCameraAppConfig = null
    }

    // /////////////////////////////////////////////////////////////////////////
    //
    // Section A: Default Values
    //
    // /////////////////////////////////////////////////////////////////////////

    @Test
    fun defaultAspectRatio_oneOne_startsInOneOne() {
        AppModule.testCameraAppConfig = CameraAppConfig(
            aspectRatio = SettingConfig(defaultValue = AspectRatio.ONE_ONE)
        )

        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            composeTestRule.visitQuickSettings {
                onNodeWithTag(QUICK_SETTINGS_RATIO_1_1_BUTTON).assertIsOn()
            }
        }
    }

    @Test
    fun defaultAspectRatio_threeFour_startsInThreeFour() {
        AppModule.testCameraAppConfig = CameraAppConfig(
            aspectRatio = SettingConfig(defaultValue = AspectRatio.THREE_FOUR)
        )

        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            composeTestRule.visitQuickSettings {
                onNodeWithTag(QUICK_SETTINGS_RATIO_3_4_BUTTON).assertIsOn()
            }
        }
    }

    @Test
    fun defaultAspectRatio_nineSixteen_startsInNineSixteen() {
        AppModule.testCameraAppConfig = CameraAppConfig(
            aspectRatio = SettingConfig(defaultValue = AspectRatio.NINE_SIXTEEN)
        )

        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            composeTestRule.visitQuickSettings {
                onNodeWithTag(QUICK_SETTINGS_RATIO_9_16_BUTTON).assertIsOn()
            }
        }
    }
}
