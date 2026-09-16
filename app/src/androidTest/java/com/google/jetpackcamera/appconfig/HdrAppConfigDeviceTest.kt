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

import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.TruthJUnit.assume
import com.google.jetpackcamera.AppModule
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.settings.model.CameraFeaturePolicy
import com.google.jetpackcamera.settings.model.OptionVisibility
import com.google.jetpackcamera.settings.model.SettingConfig
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_HDR_OPTION_ON
import com.google.jetpackcamera.ui.components.capture.FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_HDR
import com.google.jetpackcamera.utils.DEFAULT_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.assume
import com.google.jetpackcamera.utils.getCurrentLensFacing
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.visitQuickSettings
import com.google.jetpackcamera.utils.waitForCaptureButton
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class HdrAppConfigDeviceTest(
    private val lensFacing: LensFacing
) {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setUp() {
        val pm = InstrumentationRegistry.getInstrumentation().targetContext.packageManager
        if (lensFacing == LensFacing.FRONT) {
            assume()
                .withMessage("Device does not have a front camera")
                .that(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT))
                .isTrue()
        } else {
            assume()
                .withMessage("Device does not have a back camera")
                .that(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA))
                .isTrue()
        }
    }

    @After
    fun tearDown() {
        AppModule.testCameraFeaturePolicy = null
    }

    private fun ComposeTestRule.prepareLens() {
        waitForCaptureButton()
        val currentLens = getCurrentLensFacing()
        if (currentLens != lensFacing) {
            onNodeWithTag(FLIP_CAMERA_BUTTON).assume(isEnabled()) {
                "Device does not have a $lensFacing camera to flip to."
            }.performClick()
            waitUntil(DEFAULT_TIMEOUT_MILLIS) { getCurrentLensFacing() == lensFacing }
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    //
    // Section A: Default Values
    //
    // /////////////////////////////////////////////////////////////////////////

    @Test
    fun defaultImageFormat_ultraHdr_startsHdrOnIfSupportedInImageOnly() {
        AppModule.testCameraFeaturePolicy = CameraFeaturePolicy(
            captureMode = SettingConfig(defaultValue = CaptureMode.IMAGE_ONLY),
            imageFormat = SettingConfig(defaultValue = ImageOutputFormat.JPEG_ULTRA_HDR)
        )

        runMainActivityScenarioTest {
            composeTestRule.prepareLens()
            composeTestRule.visitQuickSettings {
                val hdrExists = onAllNodesWithTag(ROW_QUICK_SETTINGS_HDR)
                    .fetchSemanticsNodes().isNotEmpty()
                if (hdrExists) {
                    onNodeWithTag(BTN_QUICK_SETTINGS_HDR_OPTION_ON).assertIsOn()
                } else {
                    onNodeWithTag(ROW_QUICK_SETTINGS_HDR).assertDoesNotExist()
                }
            }
        }
    }

    @Test
    fun defaultDynamicRange_hlg10_startsHdrOnIfSupportedInVideoOnly() {
        AppModule.testCameraFeaturePolicy = CameraFeaturePolicy(
            captureMode = SettingConfig(defaultValue = CaptureMode.VIDEO_ONLY),
            dynamicRange = SettingConfig(defaultValue = DynamicRange.HLG10)
        )

        runMainActivityScenarioTest {
            composeTestRule.prepareLens()
            composeTestRule.visitQuickSettings {
                val hdrExists = onAllNodesWithTag(ROW_QUICK_SETTINGS_HDR)
                    .fetchSemanticsNodes().isNotEmpty()
                if (hdrExists) {
                    onNodeWithTag(BTN_QUICK_SETTINGS_HDR_OPTION_ON).assertIsOn()
                } else {
                    onNodeWithTag(ROW_QUICK_SETTINGS_HDR).assertDoesNotExist()
                }
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    //
    // Section B: Hidden
    //
    // /////////////////////////////////////////////////////////////////////////

    @Test
    fun hiddenImageFormat_removesHdrControlInImageOnly() {
        AppModule.testCameraFeaturePolicy = CameraFeaturePolicy(
            captureMode = SettingConfig(defaultValue = CaptureMode.IMAGE_ONLY),
            imageFormat = SettingConfig(
                defaultValue = ImageOutputFormat.JPEG,
                visibility = OptionVisibility.Hidden
            )
        )

        runMainActivityScenarioTest {
            composeTestRule.prepareLens()
            composeTestRule.visitQuickSettings {
                onNodeWithTag(ROW_QUICK_SETTINGS_HDR).assertDoesNotExist()
            }
        }
    }

    @Test
    fun hiddenDynamicRange_removesHdrControlInVideoOnly() {
        AppModule.testCameraFeaturePolicy = CameraFeaturePolicy(
            captureMode = SettingConfig(defaultValue = CaptureMode.VIDEO_ONLY),
            dynamicRange = SettingConfig(
                defaultValue = DynamicRange.SDR,
                visibility = OptionVisibility.Hidden
            )
        )

        runMainActivityScenarioTest {
            composeTestRule.prepareLens()
            composeTestRule.visitQuickSettings {
                onNodeWithTag(ROW_QUICK_SETTINGS_HDR).assertDoesNotExist()
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "lensFacing_{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(LensFacing.BACK),
            arrayOf(LensFacing.FRONT)
        )
    }
}
