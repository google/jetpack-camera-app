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

import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_BUTTON
import com.google.jetpackcamera.feature.preview.ui.IMAGE_CAPTURE_SUCCESS_TOAST
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

    @Test
    fun image_capture() = runTest {
        val timeStamp = System.currentTimeMillis()
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .assertExists()
            .performClick()
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TOAST).isDisplayed()
        }
        assert(deleteFilesInDirAfterTimestamp(timeStamp))
    }

    @Test
    fun image_capture_external() = runTest {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(timeStamp)
        getTestRegistry {
            activityScenario = ActivityScenario.launchActivityForResult(it)
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
            }
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                .assertExists()
                .performClick()
            activityScenario!!.result
        }.register("key", TEST_CONTRACT) { result ->
            assert(result)
            assert(doesImageFileExist(uri))
        }.launch(uri)
        deleteFilesInDirAfterTimestamp(timeStamp)
    }

    @Test
    fun image_capture_external_illegal_uri() = runTest {
        val timeStamp = System.currentTimeMillis()
        val uri = Uri.parse("asdfasdf")
        getTestRegistry {
            activityScenario = ActivityScenario.launchActivityForResult(it)
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
            }
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                .assertExists()
                .performClick()
            activityScenario!!.result
        }.register("key", TEST_CONTRACT) { result ->
            assert(!result)
        }.launch(uri)
        deleteFilesInDirAfterTimestamp(timeStamp)
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

    private fun getTestRegistry(
        launch: (Intent) -> Instrumentation.ActivityResult
    ): ActivityResultRegistry {
        val testRegistry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                // contract.create
                val launchIntent = contract.createIntent(
                    ApplicationProvider.getApplicationContext(),
                    input
                )
                val result: Instrumentation.ActivityResult = launch(launchIntent)
                dispatchResult(requestCode, result.resultCode, result.resultData)
            }
        }
        return testRegistry
    }

    private fun getTestUri(timeStamp: Long): Uri {
        return Uri.fromFile(
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "$timeStamp.jpg"
            )
        )
    }

    companion object {
        val DIR_PATH: String =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path

        val TEST_CONTRACT = object : ActivityResultContracts.TakePicture() {
            override fun createIntent(context: Context, uri: Uri): Intent {
                return super.createIntent(context, uri).apply {
                    component = ComponentName(
                        "com.google.jetpackcamera",
                        "com.google.jetpackcamera.MainActivity"
                    )
                }
            }
        }
    }
}
