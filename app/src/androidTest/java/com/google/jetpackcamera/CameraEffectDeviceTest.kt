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

import android.os.Build
import android.os.Bundle
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.TruthJUnit.assume
import com.google.jetpackcamera.feature.postcapture.ui.VIEWER_POST_CAPTURE_IMAGE
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_STREAM_CONFIG_OPTION_SINGLE_STREAM_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_STREAM_CONFIG_TAG
import com.google.jetpackcamera.ui.components.capture.CAPTURE_BUTTON
import com.google.jetpackcamera.ui.components.capture.PREVIEW_DISPLAY
import com.google.jetpackcamera.utils.APP_START_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.IMAGE_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.visitSettingDialog
import com.google.jetpackcamera.utils.visitSettingsScreen
import com.google.jetpackcamera.utils.waitForCaptureButton
import com.google.jetpackcamera.utils.waitForNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented device test verifying the camera effect initialization, preview start, and image capture.
 */
@RunWith(AndroidJUnit4::class)
class CameraEffectDeviceTest {

    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private fun assumeSupportsSingleStream() {
        // The GMD emulators with API <=28 do not support single-stream configs.
        assume().that(Build.HARDWARE == "ranchu" && Build.VERSION.SDK_INT <= 28).isFalse()
    }

    @Test
    fun cameraEffect_singleStream_previewStartsAndCaptures() = runMainActivityScenarioTest(
        extras = Bundle().apply { putBoolean("KEY_REVIEW_AFTER_CAPTURE", true) }
    ) {
        // Skip on devices that don't support single stream
        assumeSupportsSingleStream()

        // Wait for the capture button to be displayed initially
        composeTestRule.waitForCaptureButton()

        // Navigate to settings and select single_stream (SingleStreamEffect)
        composeTestRule.visitSettingsScreen {
            visitSettingDialog(
                settingTestTag = BTN_OPEN_DIALOG_SETTING_STREAM_CONFIG_TAG,
                dialogTestTag = BTN_DIALOG_STREAM_CONFIG_OPTION_SINGLE_STREAM_TAG,
                disabledMessage = "Stream configuration component is disabled"
            ) {
                val singleStreamNode =
                    onNodeWithTag(BTN_DIALOG_STREAM_CONFIG_OPTION_SINGLE_STREAM_TAG)
                if (isNotSelected().matches(singleStreamNode.fetchSemanticsNode())) {
                    singleStreamNode.performClick()
                }
            }
        }

        // We are now back on the Preview screen. Wait for the preview display to successfully render again,
        // which proves the OpenGL/EGL shaders compiled and context initialized without errors!
        composeTestRule.waitForNodeWithTag(PREVIEW_DISPLAY, APP_START_TIMEOUT_MILLIS)
        composeTestRule.waitForCaptureButton()

        // Trigger a photo capture to verify the image capture pipeline works with the effect active
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).performClick()

        // Wait for the post-capture viewer screen to display, verifying the capture succeeded
        composeTestRule.waitForNodeWithTag(VIEWER_POST_CAPTURE_IMAGE, IMAGE_CAPTURE_TIMEOUT_MILLIS)
    }
}
