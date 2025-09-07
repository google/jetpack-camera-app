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

import android.app.Activity
import android.app.Instrumentation
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.components.capture.CAPTURE_BUTTON
import com.google.jetpackcamera.ui.components.capture.DEBUG_OVERLAY_BUTTON
import com.google.jetpackcamera.ui.components.capture.DEBUG_OVERLAY_SET_TEST_PATTERN_BUTTON
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.ui.components.capture.TEST_PATTERN_COLOR_BARS_FADE_TO_GRAY_BUTTON
import com.google.jetpackcamera.ui.components.capture.TEST_PATTERN_DIALOG_CLOSE_BUTTON
import com.google.jetpackcamera.utils.APP_START_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.DEFAULT_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.IMAGE_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.assume
import com.google.jetpackcamera.utils.calculateAverageLuminance
import com.google.jetpackcamera.utils.getMultipleImageCaptureIntent
import com.google.jetpackcamera.utils.runMainActivityScenarioTestForResult
import com.google.jetpackcamera.utils.setFlashMode
import com.google.jetpackcamera.utils.setLensFacing
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class LowLightBoostTest(private val lensFacing: LensFacing) {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO
    )

    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var externalFilesDir: File

    @Before
    fun setUp() {
        externalFilesDir =
            instrumentation.targetContext.getExternalFilesDir(null)!!
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "lensFacing={0}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(LensFacing.BACK),
                arrayOf(LensFacing.FRONT)
            )
        }
    }

    @Test
    fun low_light_boost_test() {
        val imageFiles = List(2) {
            File(externalFilesDir, "image_$it.jpg")
        }
        val imageUriStrings = ArrayList(imageFiles.map { Uri.fromFile(it).toString() })

        val intent = getMultipleImageCaptureIntent(
            imageUriStrings,
            MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
        ).apply {
            putExtra(MainActivity.KEY_DEBUG_MODE, true)
        }
        try {
            runMainActivityScenarioTestForResult(intent) {
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
                }

                // Set lens facing
                composeTestRule.setLensFacing(lensFacing)

                // Set flash mode to LLB
                composeTestRule.setFlashMode(FlashMode.LOW_LIGHT_BOOST)

                // Set test pattern to COLOR_BARS_FADE_TO_GRAY
                composeTestRule.onNodeWithTag(DEBUG_OVERLAY_BUTTON).performClick()
                composeTestRule.onNodeWithTag(DEBUG_OVERLAY_SET_TEST_PATTERN_BUTTON)
                    .assume(isEnabled()) { "Current lens does not support any test patterns." }
                    .performClick()

                val testPatternAvailable =
                    composeTestRule.onAllNodesWithTag(TEST_PATTERN_COLOR_BARS_FADE_TO_GRAY_BUTTON)
                        .fetchSemanticsNodes().isNotEmpty()
                assumeTrue(
                    "Device does not support COLOR_BARS_FADE_TO_GRAY test pattern",
                    testPatternAvailable
                )
                composeTestRule.onNodeWithTag(TEST_PATTERN_COLOR_BARS_FADE_TO_GRAY_BUTTON)
                    .performClick()

                composeTestRule.onNodeWithTag(TEST_PATTERN_DIALOG_CLOSE_BUTTON).performClick()

                composeTestRule.waitForIdle()

                // Exit debug overlay
                UiDevice.getInstance(instrumentation).pressBack()

                // Ensure we've exited the debug overlay by waiting for the button to be visible
                composeTestRule.waitUntil(timeoutMillis = DEFAULT_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(DEBUG_OVERLAY_BUTTON).isDisplayed()
                }

                // Take first picture
                composeTestRule.onNodeWithTag(CAPTURE_BUTTON).performClick()

                // Wait for capture to complete
                composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isDisplayed()
                }

                // Set flash mode to OFF
                composeTestRule.setFlashMode(FlashMode.OFF)

                // Take second picture
                composeTestRule.onNodeWithTag(CAPTURE_BUTTON).performClick()
            }.let { result -> assertThat(result.resultCode).isEqualTo(Activity.RESULT_OK) }

            // Analyze the captured images
            val contentResolver = instrumentation.targetContext.contentResolver
            val llbAvgLuminance = ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(contentResolver, Uri.parse(imageUriStrings[0]))
            ).calculateAverageLuminance()
            val nonLlbAvgLuminance = ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(contentResolver, Uri.parse(imageUriStrings[1]))
            ).calculateAverageLuminance()

            // Assert that the image with low light boost is brighter
            assertThat(llbAvgLuminance).isGreaterThan(nonLlbAvgLuminance)
        } finally {
            imageFiles.forEach {
                if (it.exists()) {
                    it.delete()
                }
            }
        }
    }
}
