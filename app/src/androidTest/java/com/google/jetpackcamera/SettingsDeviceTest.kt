/*
 * Copyright (C) 2025 The Android Open Source Project
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
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.truth.TruthJUnit.assume
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_BUTTON
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_ASPECT_RATIO_OPTION_1_1_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_ASPECT_RATIO_OPTION_3_4_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_ASPECT_RATIO_OPTION_9_16_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_DARK_MODE_OPTION_OFF_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_DARK_MODE_OPTION_ON_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_DARK_MODE_OPTION_SYSTEM_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_FLASH_OPTION_AUTO_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_FLASH_OPTION_LLB_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_FLASH_OPTION_OFF_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_FLASH_OPTION_ON_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_FPS_OPTION_15_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_FPS_OPTION_30_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_FPS_OPTION_60_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_FPS_OPTION_AUTO_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_STREAM_CONFIG_OPTION_MULTI_STREAM_CAPTURE_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_STREAM_CONFIG_OPTION_SINGLE_STREAM_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_VIDEO_DURATION_OPTION_10S_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_VIDEO_DURATION_OPTION_1S_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_VIDEO_DURATION_OPTION_30S_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_VIDEO_DURATION_OPTION_60S_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_VIDEO_DURATION_OPTION_UNLIMITED_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_VIDEO_QUALITY_OPTION_FHD_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_VIDEO_QUALITY_OPTION_HD_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_VIDEO_QUALITY_OPTION_SD_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_VIDEO_QUALITY_OPTION_UHD_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_VIDEO_QUALITY_OPTION_UNSPECIFIED_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_VIDEO_STABILIZATION_OPTION_AUTO_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_VIDEO_STABILIZATION_OPTION_HIGH_QUALITY_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_VIDEO_STABILIZATION_OPTION_OFF_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_VIDEO_STABILIZATION_OPTION_ON_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_VIDEO_STABILIZATION_OPTION_OPTICAL_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_ASPECT_RATIO_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_DARK_MODE_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_FLASH_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_FPS_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_STREAM_CONFIG_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_VIDEO_DURATION_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_VIDEO_QUALITY_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_VIDEO_STABILIZATION_TAG
import com.google.jetpackcamera.utils.APP_START_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.DEFAULT_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.selectLensFacing
import com.google.jetpackcamera.utils.visitSettingDialog
import com.google.jetpackcamera.utils.visitSettingsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SettingsDeviceTest(private val lensFacing: LensFacing) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "lensFacing = {0}")
        fun data(): Array<LensFacing> = arrayOf(LensFacing.FRONT, LensFacing.BACK)
    }

    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private fun openSettings_clickSettingComponent_applyAction(
        componentTestTag: String,
        dialogTestTag: String,
        componentDisabledMessage: String,
        action: ComposeTestRule.() -> Unit
    ): Unit = runMainActivityScenarioTest {
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        composeTestRule.visitSettingsScreen {
            // Ensure appropriate lens facing is selected
            selectLensFacing(lensFacing)

            // Open dialog and run action if the component is enabled
            visitSettingDialog(
                settingTestTag = componentTestTag,
                dialogTestTag = dialogTestTag,
                disabledMessage = componentDisabledMessage,
                block = action
            )
        }
    }

    private fun ComposeTestRule.selectFirstNonSelected(settingOptions: List<String>) {
        // Select first non-selected option
        val selected = settingOptions.firstOrNull {
            onNode(hasTestTag(it) and isEnabled() and isNotSelected()).run {
                if (isDisplayed()) {
                    performClick()
                    waitUntil(timeoutMillis = DEFAULT_TIMEOUT_MILLIS) {
                        isSelected().matches(onNodeWithTag(it).fetchSemanticsNode())
                    }
                    true
                } else {
                    false
                }
            }
        }

        assertWithMessage("Opened dialog but no non-selected setting available to select").that(
            selected
        ).isNotNull()
    }

    @Test
    fun openSettings_setFlashMode() = runMainActivityScenarioTest {
        openSettings_clickSettingComponent_applyAction(
            componentTestTag = BTN_OPEN_DIALOG_SETTING_FLASH_TAG,
            dialogTestTag = BTN_DIALOG_FLASH_OPTION_AUTO_TAG,
            componentDisabledMessage = "Flash mode component is disabled"
        ) {
            selectFirstNonSelected(
                listOf(
                    BTN_DIALOG_FLASH_OPTION_AUTO_TAG,
                    BTN_DIALOG_FLASH_OPTION_ON_TAG,
                    BTN_DIALOG_FLASH_OPTION_OFF_TAG,
                    BTN_DIALOG_FLASH_OPTION_LLB_TAG
                )
            )
        }
    }

    @Test
    fun openSettings_setFrameRate() = runMainActivityScenarioTest {
        openSettings_clickSettingComponent_applyAction(
            componentTestTag = BTN_OPEN_DIALOG_SETTING_FPS_TAG,
            dialogTestTag = BTN_DIALOG_FPS_OPTION_AUTO_TAG,
            componentDisabledMessage = "Frame rate component is disabled"
        ) {
            selectFirstNonSelected(
                listOf(
                    BTN_DIALOG_FPS_OPTION_AUTO_TAG,
                    BTN_DIALOG_FPS_OPTION_15_TAG,
                    BTN_DIALOG_FPS_OPTION_30_TAG,
                    BTN_DIALOG_FPS_OPTION_60_TAG
                )
            )
        }
    }

    @Test
    fun openSettings_setAspectRatio() = runMainActivityScenarioTest {
        openSettings_clickSettingComponent_applyAction(
            componentTestTag = BTN_OPEN_DIALOG_SETTING_ASPECT_RATIO_TAG,
            dialogTestTag = BTN_DIALOG_ASPECT_RATIO_OPTION_9_16_TAG,
            componentDisabledMessage = "Aspect ratio component is disabled"
        ) {
            assumeHalStableOnSelectAspectRatio()

            selectFirstNonSelected(
                listOf(
                    BTN_DIALOG_ASPECT_RATIO_OPTION_9_16_TAG,
                    BTN_DIALOG_ASPECT_RATIO_OPTION_3_4_TAG,
                    BTN_DIALOG_ASPECT_RATIO_OPTION_1_1_TAG
                )
            )
        }
    }

    private fun assumeHalStableOnSelectAspectRatio() {
        // The GMD emulators on API 28, switching the aspect ratio fails and puts the emulator in
        // a bad state. Skip on these devices.
        assume().that(Build.HARDWARE == "ranchu" && Build.VERSION.SDK_INT == 28).isFalse()
    }

    @Test
    fun openSettings_setStreamConfig() = runMainActivityScenarioTest {
        openSettings_clickSettingComponent_applyAction(
            componentTestTag = BTN_OPEN_DIALOG_SETTING_STREAM_CONFIG_TAG,
            dialogTestTag = BTN_DIALOG_STREAM_CONFIG_OPTION_SINGLE_STREAM_TAG,
            componentDisabledMessage = "Stream configuration component is disabled"
        ) {
            selectFirstNonSelected(
                listOf(
                    BTN_DIALOG_STREAM_CONFIG_OPTION_SINGLE_STREAM_TAG,
                    BTN_DIALOG_STREAM_CONFIG_OPTION_MULTI_STREAM_CAPTURE_TAG
                )
            )
        }
    }

    @Test
    fun openSettings_setVideoStabilization() = runMainActivityScenarioTest {
        openSettings_clickSettingComponent_applyAction(
            componentTestTag = BTN_OPEN_DIALOG_SETTING_VIDEO_STABILIZATION_TAG,
            dialogTestTag = BTN_DIALOG_VIDEO_STABILIZATION_OPTION_AUTO_TAG,
            componentDisabledMessage = "Video stabilization component is disabled"
        ) {
            selectFirstNonSelected(
                listOf(
                    BTN_DIALOG_VIDEO_STABILIZATION_OPTION_AUTO_TAG,
                    BTN_DIALOG_VIDEO_STABILIZATION_OPTION_ON_TAG,
                    BTN_DIALOG_VIDEO_STABILIZATION_OPTION_OFF_TAG,
                    BTN_DIALOG_VIDEO_STABILIZATION_OPTION_HIGH_QUALITY_TAG,
                    BTN_DIALOG_VIDEO_STABILIZATION_OPTION_OPTICAL_TAG
                )
            )
        }
    }

    @Test
    fun openSettings_setMaxVideoDuration() = runMainActivityScenarioTest {
        openSettings_clickSettingComponent_applyAction(
            componentTestTag = BTN_OPEN_DIALOG_SETTING_VIDEO_DURATION_TAG,
            dialogTestTag = BTN_DIALOG_VIDEO_DURATION_OPTION_UNLIMITED_TAG,
            componentDisabledMessage = "Max video duration component is disabled"
        ) {
            selectFirstNonSelected(
                listOf(
                    BTN_DIALOG_VIDEO_DURATION_OPTION_UNLIMITED_TAG,
                    BTN_DIALOG_VIDEO_DURATION_OPTION_1S_TAG,
                    BTN_DIALOG_VIDEO_DURATION_OPTION_10S_TAG,
                    BTN_DIALOG_VIDEO_DURATION_OPTION_30S_TAG,
                    BTN_DIALOG_VIDEO_DURATION_OPTION_60S_TAG
                )
            )
        }
    }

    @Test
    fun openSettings_setVideoQuality() = runMainActivityScenarioTest {
        openSettings_clickSettingComponent_applyAction(
            componentTestTag = BTN_OPEN_DIALOG_SETTING_VIDEO_QUALITY_TAG,
            dialogTestTag = BTN_DIALOG_VIDEO_QUALITY_OPTION_UNSPECIFIED_TAG,
            componentDisabledMessage = "Video quality component is disabled"
        ) {
            selectFirstNonSelected(
                listOf(
                    BTN_DIALOG_VIDEO_QUALITY_OPTION_UNSPECIFIED_TAG,
                    BTN_DIALOG_VIDEO_QUALITY_OPTION_SD_TAG,
                    BTN_DIALOG_VIDEO_QUALITY_OPTION_HD_TAG,
                    BTN_DIALOG_VIDEO_QUALITY_OPTION_FHD_TAG,
                    BTN_DIALOG_VIDEO_QUALITY_OPTION_UHD_TAG
                )
            )
        }
    }

    @Test
    fun openSettings_setDarkMode() = runMainActivityScenarioTest {
        openSettings_clickSettingComponent_applyAction(
            componentTestTag = BTN_OPEN_DIALOG_SETTING_DARK_MODE_TAG,
            dialogTestTag = BTN_DIALOG_DARK_MODE_OPTION_ON_TAG,
            componentDisabledMessage = "Dark mode component is disabled"
        ) {
            selectFirstNonSelected(
                listOf(
                    BTN_DIALOG_DARK_MODE_OPTION_ON_TAG,
                    BTN_DIALOG_DARK_MODE_OPTION_OFF_TAG,
                    BTN_DIALOG_DARK_MODE_OPTION_SYSTEM_TAG
                )
            )
        }
    }
}
