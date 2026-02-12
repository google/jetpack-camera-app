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

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.TruthJUnit.assume
import com.google.jetpackcamera.ui.components.capture.FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.isEmulatorWithFakeFrontCamera
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.waitForCaptureButton
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SingleLensModeTest(private val lensFacing: String) {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "lensFacing={0}")
        fun data() = listOf("front", "back")
    }

    @Test
    fun singleLensMode_flipCameraButtonDisabled() {
        val pm = InstrumentationRegistry.getInstrumentation().targetContext.packageManager

        // The GMD API 28 and 34 emulators report having a front camera but don't actually work with it.
        // Skip this test on those specific device configurations if testing front camera.
        if (lensFacing == "front") {
            assume()
                .withMessage("Skipping test on emulator with fake front camera")
                .that(isEmulatorWithFakeFrontCamera)
                .isFalse()

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

        val extras = Bundle().apply {
            putString(MainActivity.KEY_DEBUG_SINGLE_LENS_MODE, lensFacing)
        }

        runMainActivityScenarioTest(extras = extras) {
            // Wait for the capture button to be visible
            composeTestRule.waitForCaptureButton()

            // Assert that the flip camera button is disabled
            composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).assertIsNotEnabled()
        }
    }
}
