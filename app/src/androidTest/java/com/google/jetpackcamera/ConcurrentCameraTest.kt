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

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit.assume
import com.google.jetpackcamera.core.camera.concurrent.CONCURRENT_SUB_MODE_ID
import com.google.jetpackcamera.core.camera.concurrent.R as ConcurrentR
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.CaptureSubModeId
import com.google.jetpackcamera.settings.R as SettingsR
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_FLASH_OPTION_LLB_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_FLASH_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_FPS_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_STREAM_CONFIG_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_VIDEO_STABILIZATION_TAG
import com.google.jetpackcamera.settings.ui.CLOSE_BUTTON
import com.google.jetpackcamera.ui.components.capture.FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_DROP_DOWN
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_CAPTURE_MODE
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_HDR
import com.google.jetpackcamera.ui.components.capture.carouselOptionTag
import com.google.jetpackcamera.ui.uistateadapter.capture.R as StateR
import com.google.jetpackcamera.utils.DEFAULT_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.VIDEO_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.assume
import com.google.jetpackcamera.utils.getCurrentCaptureMode
import com.google.jetpackcamera.utils.getCurrentLensFacing
import com.google.jetpackcamera.utils.getResString
import com.google.jetpackcamera.utils.longClickForVideoRecordingCheckingElapsedTime
import com.google.jetpackcamera.utils.runMainActivityMediaStoreAutoDeleteScenarioTest
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.selectConcurrentCameraSubMode
import com.google.jetpackcamera.utils.selectSingleStreamEffect
import com.google.jetpackcamera.utils.setCaptureMode
import com.google.jetpackcamera.utils.visitSettingsScreen
import com.google.jetpackcamera.utils.waitForCaptureButton
import com.google.jetpackcamera.utils.waitForCaptureModeToggleState
import com.google.jetpackcamera.utils.waitForNodeWithTag
import com.google.jetpackcamera.utils.waitForSettingsText
import com.google.jetpackcamera.utils.waitForSnackbarWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConcurrentCameraTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun concurrentCameraMode_canBeEnabled() = runConcurrentCameraScenarioTest {
        with(composeTestRule) {
            selectConcurrentCameraSubMode(enabled = true)
            onNodeWithTag(CONCURRENT_SUB_MODE_ID.carouselOptionTag)
                .assertExists()
                .assertIsSelected()

            // Assert that the flip camera button is visible on preview
            onNodeWithTag(FLIP_CAMERA_BUTTON)
                .assertIsDisplayed()
        }
    }

    @Test
    fun concurrentCameraMode_whenEnabled_canBeDisabled() = runConcurrentCameraScenarioTest {
        with(composeTestRule) {
            selectConcurrentCameraSubMode(enabled = true)
            selectConcurrentCameraSubMode(enabled = false)
            onNodeWithTag(CaptureSubModeId.DEFAULT.carouselOptionTag)
                .assertExists()
                .assertIsSelected()
        }
    }

    @Test
    fun concurrentCameraMode_whenEnabled_canFlipCamera() = runConcurrentCameraScenarioTest {
        with(composeTestRule) {
            selectConcurrentCameraSubMode(enabled = true)
            val initialLensFacing = getCurrentLensFacing()

            // Flip camera from preview screen directly (it should be available)
            onNodeWithTag(FLIP_CAMERA_BUTTON)
                .assertExists()
                .performClick()

            waitForCaptureButton()
            assertThat(getCurrentLensFacing()).isEqualTo(initialLensFacing.flip())
            // Dual camera stays active after the flip
            onNodeWithTag(CONCURRENT_SUB_MODE_ID.carouselOptionTag).assertIsSelected()
        }
    }

    @Test
    fun concurrentCameraMode_whenEnabled_disablesOtherQuickSettings() =
        runConcurrentCameraScenarioTest {
            with(composeTestRule) {
                selectConcurrentCameraSubMode(enabled = true)

                // Open Quick Settings bottom sheet
                onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
                    .assertExists()
                    .performClick()

                // Assert the HDR row is hidden when concurrent camera sub-mode is active
                onNodeWithTag(ROW_QUICK_SETTINGS_HDR).assertDoesNotExist()

                // Assert the capture mode row does not exist (hidden because locked to video-only)
                onNodeWithTag(ROW_QUICK_SETTINGS_CAPTURE_MODE).assertDoesNotExist()
                assertThat(getCurrentCaptureMode()).isEqualTo(CaptureMode.VIDEO_ONLY)
            }
        }

    @Test
    fun concurrentCameraMode_whenEnabled_disablesOtherSettingsInSettingsScreen() =
        runConcurrentCameraScenarioTest {
            with(composeTestRule) {
                // Select the dual sub-mode, then assert other settings are disabled in settings
                selectConcurrentCameraSubMode(enabled = true)
                visitSettingsScreen {
                    // 1. Assert Stream Config is disabled. The active sub-mode is collected
                    // asynchronously, so wait for it.
                    waitUntil(DEFAULT_TIMEOUT_MILLIS) {
                        onNodeWithTag(BTN_OPEN_DIALOG_SETTING_STREAM_CONFIG_TAG)
                            .isNotEnabledNow()
                    }

                    // 2. Assert Video Stabilization is disabled
                    onNodeWithTag(BTN_OPEN_DIALOG_SETTING_VIDEO_STABILIZATION_TAG)
                        .assertExists()
                        .assert(isNotEnabled())

                    // 3. Assert Flash Low Light Boost option is disabled in dialog. Skip the
                    // test on devices without a flash unit.
                    onNodeWithTag(BTN_OPEN_DIALOG_SETTING_FLASH_TAG)
                        .assertExists()
                        .assume(isEnabled()) { "Flash is not supported by the device" }
                        .performClick()

                    onNodeWithTag(BTN_DIALOG_FLASH_OPTION_LLB_TAG)
                        .assertExists()
                        .assert(isNotEnabled())

                    onNodeWithTag(CLOSE_BUTTON)
                        .assertExists()
                        .performClick()

                    // 4. Assert FPS setting is disabled
                    onNodeWithTag(BTN_OPEN_DIALOG_SETTING_FPS_TAG)
                        .assertExists()
                        .assert(isNotEnabled())
                }
            }
        }

    @Test
    fun concurrentCameraMode_settingsScreen_showsDualRationale_andClearsAfterLeavingDual() =
        runConcurrentCameraScenarioTest {
            with(composeTestRule) {
                // Only meaningful if stream config is enabled before dual camera is selected.
                val enabledBeforeDual = visitSettingsScreen {
                    !onNodeWithTag(BTN_OPEN_DIALOG_SETTING_STREAM_CONFIG_TAG).isNotEnabledNow()
                }
                assume().withMessage("Stream config is unavailable on this device")
                    .that(enabledBeforeDual)
                    .isTrue()

                selectConcurrentCameraSubMode(enabled = true)
                visitSettingsScreen {
                    val rationale = getResString(SettingsR.string.capture_sub_mode_active).format(
                        getResString(SettingsR.string.stream_config_rationale_prefix),
                        getResString(ConcurrentR.string.capture_mode_concurrent_dual)
                    )
                    waitUntil(DEFAULT_TIMEOUT_MILLIS) {
                        onAllNodesWithText(rationale).fetchSemanticsNodes().isNotEmpty()
                    }
                    onNodeWithText(rationale).assertExists()
                }

                selectConcurrentCameraSubMode(enabled = false)
                onNodeWithTag(CONCURRENT_SUB_MODE_ID.carouselOptionTag).assertIsNotSelected()
                visitSettingsScreen {
                    waitUntil(DEFAULT_TIMEOUT_MILLIS) {
                        !onNodeWithTag(BTN_OPEN_DIALOG_SETTING_STREAM_CONFIG_TAG)
                            .isNotEnabledNow()
                    }
                }
            }
        }

    /**
     * Verifies that a camera effect selected before Dual is turned off while Dual records, and is
     * selected again after leaving Dual.
     */
    @Test
    fun concurrentCameraMode_turnsSingleStreamEffectOff_andRestoresItAfterLeavingDual() =
        runConcurrentCameraScenarioTest(
            mediaUriForSavedFiles = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ) {
            with(composeTestRule) {
                selectSingleStreamEffect()

                selectConcurrentCameraSubMode(enabled = true)
                waitForSettingsText(
                    getResString(SettingsR.string.capture_sub_mode_active).format(
                        getResString(SettingsR.string.stream_config_rationale_prefix),
                        getResString(ConcurrentR.string.capture_mode_concurrent_dual)
                    )
                )
                // The concurrent session records without the effect.
                waitForCaptureButton()
                longClickForVideoRecordingCheckingElapsedTime()
                waitForSnackbarWithText(
                    StateR.string.toast_video_capture_success,
                    VIDEO_CAPTURE_TIMEOUT_MILLIS
                )

                selectConcurrentCameraSubMode(enabled = false)
                waitForSettingsText(
                    getResString(SettingsR.string.stream_config_description_single_stream)
                )
            }
        }

    @Test
    fun concurrentCameraMode_canRecordVideo() = runConcurrentCameraScenarioTest(
        mediaUriForSavedFiles = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ) {
        with(composeTestRule) {
            selectConcurrentCameraSubMode(enabled = true)

            // The concurrent camera session starts asynchronously after the sub-mode is
            // selected. The capture button is disabled until it is running, so wait for it
            // before pressing.
            waitForCaptureButton()

            longClickForVideoRecordingCheckingElapsedTime()

            waitForSnackbarWithText(
                StateR.string.toast_video_capture_success,
                VIDEO_CAPTURE_TIMEOUT_MILLIS
            )
        }
    }

    // Ensures the app has launched and checks that the device supports concurrent camera before
    // running the test.
    private inline fun runConcurrentCameraScenarioTest(
        mediaUriForSavedFiles: Uri? = null,
        expectedMediaFiles: Int = 1,
        crossinline block: ActivityScenario<MainActivity>.() -> Unit
    ) {
        val wrappedBlock: ActivityScenario<MainActivity>.() -> Unit = {
            // Wait for the capture button to be displayed
            composeTestRule.waitForCaptureButton()

            // Skip the test if the device can't run its front and back cameras concurrently.
            // This is checked with CameraX directly rather than through the app, so that a
            // missing dual camera option fails the test instead of skipping it.
            assume().withMessage("Device does not support concurrent front and back cameras")
                .that(deviceSupportsConcurrentFrontAndBack())
                .isTrue()

            // Switch to VIDEO_ONLY capture mode so the concurrent camera sub-mode option is shown.
            if (composeTestRule.getCurrentCaptureMode() != CaptureMode.VIDEO_ONLY) {
                composeTestRule.setCaptureMode(CaptureMode.VIDEO_ONLY)
                composeTestRule.waitForCaptureModeToggleState(CaptureMode.VIDEO_ONLY)
            }
            composeTestRule.waitForNodeWithTag(CONCURRENT_SUB_MODE_ID.carouselOptionTag)

            block()
        }

        if (mediaUriForSavedFiles != null) {
            runMainActivityMediaStoreAutoDeleteScenarioTest(
                mediaUri = mediaUriForSavedFiles,
                expectedNumFiles = expectedMediaFiles,
                block = wrappedBlock
            )
        } else {
            runMainActivityScenarioTest(block = wrappedBlock)
        }
    }

    private fun deviceSupportsConcurrentFrontAndBack(): Boolean {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        return cameraProvider.availableConcurrentCameraInfos.any { group ->
            group.map { it.lensFacing }.toSet() ==
                setOf(CameraSelector.LENS_FACING_FRONT, CameraSelector.LENS_FACING_BACK)
        }
    }
}

/** Returns whether this node is currently disabled, without failing if it isn't. */
private fun androidx.compose.ui.test.SemanticsNodeInteraction.isNotEnabledNow(): Boolean =
    isNotEnabled().matches(fetchSemanticsNode())
