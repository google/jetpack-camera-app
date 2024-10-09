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
import android.app.Activity
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.MainActivity
import com.google.jetpackcamera.feature.preview.R
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_CAPTURE_MODE_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_DROP_DOWN
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_HDR_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_RATIO_1_1_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_RATIO_BUTTON
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_BUTTON
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_MODE_TOGGLE_BUTTON
import com.google.jetpackcamera.feature.preview.ui.FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.feature.preview.ui.IMAGE_CAPTURE_UNSUPPORTED_CONCURRENT_CAMERA_TAG
import com.google.jetpackcamera.feature.preview.ui.VIDEO_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.settings.model.ConcurrentCameraMode
import com.google.jetpackcamera.utils.APP_START_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.VIDEO_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.assume
import com.google.jetpackcamera.utils.getResString
import com.google.jetpackcamera.utils.longClickForVideoRecording
import com.google.jetpackcamera.utils.runMediaStoreAutoDeleteScenarioTest
import com.google.jetpackcamera.utils.runScenarioTest
import com.google.jetpackcamera.utils.stateDescriptionMatches
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
    fun concurrentCameraMode_canBeEnabled() = runConcurrentCameraScenarioTest<MainActivity> {
        val concurrentCameraModes = mutableListOf<ConcurrentCameraMode>()
        with(composeTestRule) {
            onNodeWithTag(QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON)
                .assertExists().apply {
                    // Check the original mode
                    fetchSemanticsNode().let { node ->
                        concurrentCameraModes.add(node.fetchConcurrentCameraMode())
                    }
                }
                // Enable concurrent camera
                .performClick().apply {
                    // Check the mode has changed
                    fetchSemanticsNode().let { node ->
                        concurrentCameraModes.add(node.fetchConcurrentCameraMode())
                    }
                }

            // Exit quick settings
            onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
                .assertExists()
                .performClick()

            // Assert that the flip camera button is visible
            onNodeWithTag(FLIP_CAMERA_BUTTON)
                .assertIsDisplayed()
        }

        assertThat(concurrentCameraModes).containsExactly(
            ConcurrentCameraMode.OFF,
            ConcurrentCameraMode.DUAL
        ).inOrder()
    }

    @Test
    fun concurrentCameraMode_whenEnabled_canBeDisabled() =
        runConcurrentCameraScenarioTest<MainActivity> {
            val concurrentCameraModes = mutableListOf<ConcurrentCameraMode>()
            with(composeTestRule) {
                onNodeWithTag(QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON)
                    .assertExists().apply {
                        // Check the original mode
                        fetchSemanticsNode().let { node ->
                            concurrentCameraModes.add(node.fetchConcurrentCameraMode())
                        }
                    }
                    // Enable concurrent camera
                    .performClick().apply {
                        // Check the mode has changed
                        fetchSemanticsNode().let { node ->
                            concurrentCameraModes.add(node.fetchConcurrentCameraMode())
                        }
                    }

                // Exit quick settings
                onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
                    .assertExists()
                    .performClick()

                // Assert that the flip camera button is visible
                onNodeWithTag(FLIP_CAMERA_BUTTON)
                    .assertIsDisplayed()

                // Enter quick settings
                onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
                    .assertExists()
                    .performClick()

                onNodeWithTag(QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON)
                    .assertExists()
                    // Disable concurrent camera
                    .performClick().apply {
                        // Check the mode is back to OFF
                        fetchSemanticsNode().let { node ->
                            concurrentCameraModes.add(node.fetchConcurrentCameraMode())
                        }
                    }

                // Exit quick settings
                onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
                    .assertExists()
                    .performClick()

                // Assert that the flip camera button is visible
                onNodeWithTag(FLIP_CAMERA_BUTTON)
                    .assertIsDisplayed()
            }

            assertThat(concurrentCameraModes).containsExactly(
                ConcurrentCameraMode.OFF,
                ConcurrentCameraMode.DUAL,
                ConcurrentCameraMode.OFF
            ).inOrder()
        }

    @Test
    fun concurrentCameraMode_whenEnabled_canFlipCamera() =
        runConcurrentCameraScenarioTest<MainActivity> {
            with(composeTestRule) {
                // Check device has multiple cameras
                onNodeWithTag(QUICK_SETTINGS_FLIP_CAMERA_BUTTON)
                    .assertExists()
                    .assume(isEnabled()) {
                        "Device does not have multiple cameras."
                    }

                onNodeWithTag(QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON)
                    .assertExists()
                    .assertConcurrentCameraMode(ConcurrentCameraMode.OFF)
                    // Enable concurrent camera
                    .performClick()
                    .assertConcurrentCameraMode(ConcurrentCameraMode.DUAL)

                onNodeWithTag(QUICK_SETTINGS_FLIP_CAMERA_BUTTON)
                    .assertExists()
                    .performClick()

                // Exit quick settings
                onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
                    .assertExists()
                    .performClick()

                // Assert that the flip camera button is visible
                onNodeWithTag(FLIP_CAMERA_BUTTON)
                    .assertIsDisplayed()
            }
        }

    @Test
    fun concurrentCameraMode_whenEnabled_canSwitchAspectRatio() =
        runConcurrentCameraScenarioTest<MainActivity> {
            with(composeTestRule) {
                onNodeWithTag(QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON)
                    .assertExists()
                    .assertConcurrentCameraMode(ConcurrentCameraMode.OFF)
                    // Enable concurrent camera
                    .performClick()
                    .assertConcurrentCameraMode(ConcurrentCameraMode.DUAL)

                // Click the ratio button
                composeTestRule.onNodeWithTag(QUICK_SETTINGS_RATIO_BUTTON)
                    .assertExists()
                    .performClick()

                // Click the 1:1 ratio button
                composeTestRule.onNodeWithTag(QUICK_SETTINGS_RATIO_1_1_BUTTON)
                    .assertExists()
                    .performClick()

                // Exit quick settings
                onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
                    .assertExists()
                    .performClick()

                // Assert that the flip camera button is visible
                onNodeWithTag(FLIP_CAMERA_BUTTON)
                    .assertIsDisplayed()
            }
        }

    @Test
    fun concurrentCameraMode_whenEnabled_disablesOtherSettings() =
        runConcurrentCameraScenarioTest<MainActivity> {
            with(composeTestRule) {
                onNodeWithTag(QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON)
                    .assertExists()
                    .assertConcurrentCameraMode(ConcurrentCameraMode.OFF)
                    // Enable concurrent camera
                    .performClick()
                    .assertConcurrentCameraMode(ConcurrentCameraMode.DUAL)

                // Assert the capture mode button is disabled
                onNodeWithTag(QUICK_SETTINGS_CAPTURE_MODE_BUTTON)
                    .assertExists()
                    .assert(isNotEnabled())

                // Assert the HDR button is disabled
                onNodeWithTag(QUICK_SETTINGS_HDR_BUTTON)
                    .assertExists()
                    .assert(isNotEnabled())

                // Exit quick settings
                onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
                    .assertExists()
                    .performClick()

                onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON)
                    .assertExists()
                    .assert(
                        stateDescriptionMatches(
                            getResString(R.string.capture_mode_video_recording_content_description)
                        )
                    ).performClick()

                waitUntil {
                    onNodeWithTag(IMAGE_CAPTURE_UNSUPPORTED_CONCURRENT_CAMERA_TAG).isDisplayed()
                }
            }
        }

    @Test
    fun concurrentCameraMode_canRecordVideo() = runConcurrentCameraScenarioTest<MainActivity>(
        mediaUriForSavedFiles = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ) {
        with(composeTestRule) {
            onNodeWithTag(QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON)
                .assertExists()
                .assertConcurrentCameraMode(ConcurrentCameraMode.OFF)
                // Enable concurrent camera
                .performClick()
                .assertConcurrentCameraMode(ConcurrentCameraMode.DUAL)

            // Exit quick settings
            onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
                .assertExists()
                .performClick()

            longClickForVideoRecording()

            waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(VIDEO_CAPTURE_SUCCESS_TAG).isDisplayed()
            }
        }
    }

    // Ensures the app has launched and checks that the device supports concurrent camera before
    // running the test.
    // This test will start with quick settings visible
    private inline fun <reified T : Activity> runConcurrentCameraScenarioTest(
        mediaUriForSavedFiles: Uri? = null,
        expectedMediaFiles: Int = 1,
        crossinline block: ActivityScenario<T>.() -> Unit
    ) {
        val wrappedBlock: ActivityScenario<T>.() -> Unit = {
            // Wait for the capture button to be displayed
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
            }

            // ///////////////////////////////////////////////////
            // Check that the device supports concurrent camera //
            // ///////////////////////////////////////////////////
            // Navigate to quick settings
            composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
                .assertExists()
                .performClick()

            // Check that the concurrent camera button is enabled
            composeTestRule.onNodeWithTag(QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON)
                .assertExists()
                .assume(isEnabled()) {
                    "Device does not support concurrent camera."
                }

            // ///////////////////////////////////////////////////
            //               Run the actual test                //
            // ///////////////////////////////////////////////////
            block()
        }

        if (mediaUriForSavedFiles != null) {
            runMediaStoreAutoDeleteScenarioTest(
                mediaUri = mediaUriForSavedFiles,
                expectedNumFiles = expectedMediaFiles,
                block = wrappedBlock
            )
        } else {
            runScenarioTest(wrappedBlock)
        }
    }

    context(SemanticsNodeInteractionsProvider)
    private fun SemanticsNode.fetchConcurrentCameraMode(): ConcurrentCameraMode {
        config[SemanticsProperties.ContentDescription].any { description ->
            when (description) {
                getResString(R.string.quick_settings_concurrent_camera_off_description) ->
                    return ConcurrentCameraMode.OFF

                getResString(R.string.quick_settings_concurrent_camera_dual_description) ->
                    return ConcurrentCameraMode.DUAL

                else -> false
            }
        }
        throw AssertionError("Unable to determine concurrent camera mode from quick settings")
    }

    context(SemanticsNodeInteractionsProvider)
    private fun SemanticsNodeInteraction.assertConcurrentCameraMode(
        mode: ConcurrentCameraMode
    ): SemanticsNodeInteraction {
        assertThat(fetchSemanticsNode().fetchConcurrentCameraMode())
            .isEqualTo(mode)
        return this
    }
}
