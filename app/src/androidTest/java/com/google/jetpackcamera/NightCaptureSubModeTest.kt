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

import android.provider.MediaStore
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.jetpackcamera.core.camera.night.NIGHT_SUB_MODE_ID
import com.google.jetpackcamera.core.camera.night.R as NightR
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.settings.R as SettingsR
import com.google.jetpackcamera.ui.components.capture.CAPTURE_BUTTON
import com.google.jetpackcamera.ui.uistateadapter.capture.R as StateR
import com.google.jetpackcamera.utils.IMAGE_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.getResString
import com.google.jetpackcamera.utils.runMainActivityMediaStoreAutoDeleteScenarioTest
import com.google.jetpackcamera.utils.selectCaptureSubMode
import com.google.jetpackcamera.utils.selectSingleStreamEffect
import com.google.jetpackcamera.utils.waitForCaptureButton
import com.google.jetpackcamera.utils.waitForSettingsText
import com.google.jetpackcamera.utils.waitForSnackbarWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NightCaptureSubModeTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    /**
     * Verifies that a camera effect that is not compatible with Night is turned off while Night
     * captures, and is selected again after leaving Night.
     */
    @Test
    fun nightMode_turnsSingleStreamEffectOff_andRestoresItAfterLeavingNight() =
        runMainActivityMediaStoreAutoDeleteScenarioTest(
            mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            filePrefix = "JCA"
        ) {
            with(composeTestRule) {
                waitForCaptureButton()
                selectSingleStreamEffect()

                selectNightSubMode(enabled = true)
                waitForSettingsText(
                    getResString(SettingsR.string.capture_sub_mode_active).format(
                        getResString(SettingsR.string.stream_config_rationale_prefix),
                        getResString(NightR.string.capture_mode_night)
                    )
                )
                // The Night session captures without the effect.
                waitForCaptureButton()
                onNodeWithTag(CAPTURE_BUTTON)
                    .assertExists()
                    .performClick()
                waitForSnackbarWithText(
                    StateR.string.toast_image_capture_success,
                    IMAGE_CAPTURE_TIMEOUT_MILLIS
                )

                selectNightSubMode(enabled = false)
                waitForSettingsText(
                    getResString(SettingsR.string.stream_config_description_single_stream)
                )
            }
        }

    private fun selectNightSubMode(enabled: Boolean) = composeTestRule.selectCaptureSubMode(
        subModeId = NIGHT_SUB_MODE_ID,
        parentCaptureMode = CaptureMode.IMAGE_ONLY,
        subModeName = "Night",
        enabled = enabled
    )
}
