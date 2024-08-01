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
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_BUTTON
import com.google.jetpackcamera.feature.preview.ui.IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
import com.google.jetpackcamera.feature.preview.ui.VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
import com.google.jetpackcamera.feature.preview.ui.VIDEO_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.utils.APP_REQUIRED_PERMISSIONS
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class VideoRecordingDeviceTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(APP_REQUIRED_PERMISSIONS).toTypedArray())

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var activityScenario: ActivityScenario<MainActivity>? = null
    private val uiDevice = UiDevice.getInstance(instrumentation)

    @Test
    fun video_capture_external_with_image_capture_intent() = run {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(timeStamp)
        getTestRegistry {
            activityScenario = ActivityScenario.launchActivityForResult(it)
            uiDevice.wait(
                Until.findObject(By.res(CAPTURE_BUTTON)),
                5000
            )
            uiDevice.findObject(By.res(CAPTURE_BUTTON)).longClick()
            uiDevice.wait(
                Until.findObject(By.res(VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED_TAG)),
                5000
            )
            uiDevice.pressBack()
            activityScenario!!.result
        }.register("key", TEST_CONTRACT) { result ->
            assert(!result)
            assert(activityScenario!!.result.resultCode == Activity.RESULT_CANCELED)
        }.launch(uri)
    }

    @Test
    fun video_capture_external_intent() = run {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(timeStamp)
        getTestRegistry {
            activityScenario = ActivityScenario.launchActivityForResult(it)
            // Wait for the capture button to be displayed
            uiDevice.wait(
                Until.findObject(By.res(CAPTURE_BUTTON)),
                5000
            )
            uiDevice.findObject(By.res(CAPTURE_BUTTON)).longClick()
            uiDevice.wait(
                Until.findObject(By.res(VIDEO_CAPTURE_SUCCESS_TAG)),
                5000
            )
            activityScenario!!.result
        }.register("key", VIDEO_TEST_CONTRACT) { _ ->
            assert(activityScenario!!.result.resultCode == Activity.RESULT_OK)
            deleteFilesInDirAfterTimestamp(timeStamp)
        }.launch(uri)
    }

    @Test
    fun image_capture_external_with_video_capture_intent() = run {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(timeStamp)
        getTestRegistry {
            activityScenario = ActivityScenario.launchActivityForResult(it)
            // Wait for the capture button to be displayed
            uiDevice.wait(
                Until.findObject(By.res(CAPTURE_BUTTON)),
                5000
            )
            uiDevice.findObject(By.res(CAPTURE_BUTTON)).click()
            uiDevice.wait(
                Until.findObject(By.res(IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED_TAG)),
                5000
            )
            uiDevice.pressBack()
            activityScenario!!.result
        }.register("key", VIDEO_TEST_CONTRACT) { result ->
            assert(!result)
            assert(activityScenario!!.result.resultCode == Activity.RESULT_CANCELED)
        }.launch(uri)
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
                "$timeStamp.mp4"
            )
        )
    }

    private fun deleteFilesInDirAfterTimestamp(timeStamp: Long): Boolean {
        var hasDeletedFile = false
        for (file in File(ImageCaptureDeviceTest.DIR_PATH).listFiles()) {
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

    companion object {
        private val TEST_CONTRACT = object : ActivityResultContracts.TakePicture() {
            override fun createIntent(context: Context, uri: Uri): Intent {
                return super.createIntent(context, uri).apply {
                    component = ComponentName(
                        ApplicationProvider.getApplicationContext(),
                        MainActivity::class.java
                    )
                }
            }
        }

        private val VIDEO_TEST_CONTRACT = object : ActivityResultContracts.CaptureVideo() {
            override fun createIntent(context: Context, uri: Uri): Intent {
                return super.createIntent(context, uri).apply {
                    component = ComponentName(
                        ApplicationProvider.getApplicationContext(),
                        MainActivity::class.java
                    )
                }
            }
        }
    }
}
