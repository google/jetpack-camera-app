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

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_BUTTON
import com.google.jetpackcamera.feature.preview.ui.IMAGE_CAPTURE_FAILURE_TAG
import com.google.jetpackcamera.feature.preview.ui.IMAGE_CAPTURE_SUCCESS_TAG
import java.io.File
import java.net.URLConnection
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ImageCaptureDeviceTest {
    // TODO(b/319733374): Return bitmap for external mediastore capture without URI

    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(APP_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var activityScenario: ActivityScenario<MainActivity>? = null
    private val uiDevice = UiDevice.getInstance(instrumentation)

    @Test
    fun image_capture() =  runScenarioTest<MainActivity> {
        val timeStamp = System.currentTimeMillis()
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .assertExists()
            .performClick()
        composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isDisplayed()
        }
        assert(File(DIR_PATH).lastModified() > timeStamp)
        deleteFilesInDirAfterTimestamp(timeStamp)
    }

    @Test
    fun image_capture_external() {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(timeStamp)
        activityScenario =
            runScenarioTestForResult<MainActivity>(getIntent(timeStamp, uri)) {
                // Wait for the capture button to be displayed
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
                }

                composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                    .assertExists()
                    .performClick()
                /* This line needs to be uiDevice.wait() instead of composeTestRule.waitUntil()
                * because the latter causes a strange behavior where the node is not found. */
                uiDevice.wait(
                    Until.findObject(By.res(IMAGE_CAPTURE_SUCCESS_TAG)),
                    5000
                )
            }
        assert(activityScenario!!.result.resultCode == Activity.RESULT_OK)
        assert(doesImageFileExist(uri))
        deleteFilesInDirAfterTimestamp(timeStamp)
    }

    @Test
    fun image_capture_external_illegal_uri()  {
        val timeStamp = System.currentTimeMillis()
        val uri = Uri.parse("asdfasdf")
        activityScenario =
            runScenarioTestForResult<MainActivity>(getIntent(timeStamp, uri)) {
                // Wait for the capture button to be displayed
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
                }

                composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                    .assertExists()
                    .performClick()
                /* This line needs to be uiDevice.wait() instead of composeTestRule.waitUntil()
                * because the latter causes a strange behavior where the node is not found. */
                uiDevice.wait(
                    Until.findObject(By.res(IMAGE_CAPTURE_FAILURE_TAG)),
                    5000
                )
                uiDevice.pressBack()
            }
        assert(activityScenario!!.result.resultCode == Activity.RESULT_CANCELED)
        assert(!doesImageFileExist(uri))
    }

    private fun doesImageFileExist(uri: Uri): Boolean {
        val file = File(uri.path)
        if (file.exists()) {
            val mimeType = URLConnection.guessContentTypeFromName(uri.path)
            return mimeType != null && mimeType.startsWith("image")
        }
        return false
    }

    private fun deleteFilesInDirAfterTimestamp(timeStamp: Long): Boolean {
        var hasDeletedFile = false
        for (file in File(DIR_PATH).listFiles()) {
            if (file.lastModified() >= timeStamp) {
                file.delete()
                if (file.exists()) {
                    file.getCanonicalFile().delete()
                    if (file.exists()) {
                        instrumentation.targetContext.applicationContext.deleteFile(file.getName())
                    }
                }
                hasDeletedFile = true
            }
        }
        return hasDeletedFile
    }

    private fun getTestUri(timeStamp: Long): Uri {
        return Uri.fromFile(
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "$timeStamp.jpg"
            )
        )
    }

    private fun getIntent(timeStamp: Long, uri: Uri): Intent {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.setComponent(
            ComponentName(
                "com.google.jetpackcamera",
                "com.google.jetpackcamera.MainActivity"
            )
        )
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        return intent
    }

    companion object {
        val DIR_PATH: String =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path
    }
}
