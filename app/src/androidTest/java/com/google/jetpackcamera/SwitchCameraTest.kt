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

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.components.capture.FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.ui.components.capture.PREVIEW_DISPLAY
import com.google.jetpackcamera.utils.APP_START_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.DEFAULT_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.assumeFlipCameraAvailable
import com.google.jetpackcamera.utils.getCurrentLensFacing
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.waitForNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SwitchCameraTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun canFlipCamera_fromPreviewScreenButton() = runFlipCameraTest(composeTestRule) {
        val lensFacingStates = mutableListOf<LensFacing>()
        // Get initial lens facing
        val initialLensFacing = composeTestRule.getCurrentLensFacing()
        lensFacingStates.add(initialLensFacing)

        // Press the flip camera button
        composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).performClick()

        // Get lens facing after first flip
        lensFacingStates.add(composeTestRule.getCurrentLensFacing())

        // Press the flip camera button again
        composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).performClick()

        // Get lens facing after second flip
        lensFacingStates.add(composeTestRule.getCurrentLensFacing())

        assertThat(lensFacingStates).containsExactly(
            initialLensFacing,
            initialLensFacing.flip(),
            initialLensFacing.flip().flip()
        ).inOrder()
    }

    @Test
    fun canFlipCamera_fromPreviewScreenDoubleTap() = runFlipCameraTest(composeTestRule) {
        val lensFacingStates = mutableListOf<LensFacing>()
        // Get initial lens facing
        val initialLensFacing = composeTestRule.getCurrentLensFacing()
        lensFacingStates.add(initialLensFacing)

        // Double click display to flip camera
        composeTestRule.onNodeWithTag(PREVIEW_DISPLAY)
            .performTouchInput { doubleClick() }

        // Get lens facing after first flip
        lensFacingStates.add(composeTestRule.getCurrentLensFacing())

        // Double click display to flip camera again
        composeTestRule.onNodeWithTag(PREVIEW_DISPLAY)
            .performTouchInput { doubleClick() }

        // Get lens facing after second flip
        lensFacingStates.add(composeTestRule.getCurrentLensFacing())

        assertThat(lensFacingStates).containsExactly(
            initialLensFacing,
            initialLensFacing.flip(),
            initialLensFacing.flip().flip()
        ).inOrder()
    }

    @Test
    fun flipCamera_updatesPreviewDisplayOrientationToMatchSensor() =
        runFlipCameraTest(composeTestRule) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val cameraManager = context.getSystemService(CameraManager::class.java)

            fun expectedIsLandscape(lensFacing: LensFacing): Boolean {
                val targetFacing = when (lensFacing) {
                    LensFacing.FRONT -> CameraCharacteristics.LENS_FACING_FRONT
                    LensFacing.BACK -> CameraCharacteristics.LENS_FACING_BACK
                }
                val characteristics = cameraManager.cameraIdList
                    .map { cameraManager.getCameraCharacteristics(it) }
                    .first { it.get(CameraCharacteristics.LENS_FACING) == targetFacing }
                val sensorOrientation =
                    characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
                val sensorRect =
                    characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)!!
                return (sensorOrientation % 180 == 0) == (sensorRect.width() >= sensorRect.height())
            }

            val initialLensFacing = composeTestRule.getCurrentLensFacing()
            composeTestRule.waitForIdle()
            val initialBounds =
                composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).getUnclippedBoundsInRoot()
            assertThat(initialBounds.width > initialBounds.height)
                .isEqualTo(expectedIsLandscape(initialLensFacing))

            composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).performClick()
            val flippedLensFacing = composeTestRule.getCurrentLensFacing()
            assertThat(flippedLensFacing).isEqualTo(initialLensFacing.flip())

            val expectedFlippedIsLandscape = expectedIsLandscape(flippedLensFacing)
            composeTestRule.waitUntil(timeoutMillis = DEFAULT_TIMEOUT_MILLIS) {
                val bounds =
                    composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).getUnclippedBoundsInRoot()
                (bounds.width > bounds.height) == expectedFlippedIsLandscape
            }
        }
}

inline fun runFlipCameraTest(
    composeTestRule: ComposeTestRule,
    crossinline block: ActivityScenario<MainActivity>.() -> Unit
) = runMainActivityScenarioTest {
    // Wait for the preview display to be visible
    composeTestRule.waitForNodeWithTag(PREVIEW_DISPLAY, APP_START_TIMEOUT_MILLIS)

    // If flipping the camera is available, flip it. Otherwise skip test.
    composeTestRule.assumeFlipCameraAvailable()

    block()
}
