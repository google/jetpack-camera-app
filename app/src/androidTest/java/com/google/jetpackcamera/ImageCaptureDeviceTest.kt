/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.io.File
import java.net.URLConnection
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ImageCaptureDeviceTest {

    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var activityScenario: ActivityScenario<MainActivity>? = null
    private val uiDevice = UiDevice.getInstance(instrumentation)

    @Test
    fun image_capture_external() = run {
        val timeStamp = System.currentTimeMillis()
        val launchIntent = Intent()
        launchIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE)
        launchIntent.putExtra(MediaStore.EXTRA_OUTPUT, TEST_URI)
        launchIntent.setComponent(
            ComponentName("com.google.jetpackcamera", "com.google.jetpackcamera.MainActivity")
        )
        getTestRegistry {
            activityScenario = ActivityScenario.launchActivityForResult(launchIntent)
            uiDevice.wait(
                Until.findObject(By.res("CaptureButton")),
                5000
            )
            uiDevice.findObject(By.res("CaptureButton")).click()
            uiDevice.wait(
                Until.findObject(By.res("ImageCaptureSuccessToast")),
                5000
            )
            activityScenario!!.result.resultCode
        }.register("key", ActivityResultContracts.TakePicture()) { result ->
            assert(result)
            assert(doesImageFileExist(TEST_URI))
        }.launch(TEST_URI)
        deleteFilesInDirAfterTimestamp(timeStamp)
    }

    @Test // TODO(b/319733374): Return bitmap for external mediastore capture without URI
    fun image_capture_external_no_uri() = runTest {
        val launchIntent = Intent()
        launchIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE)
        launchIntent.setComponent(
            ComponentName("com.google.jetpackcamera", "com.google.jetpackcamera.MainActivity")
        )
        getTestRegistry {
            activityScenario = ActivityScenario.launchActivityForResult(launchIntent)
            uiDevice.wait(
                Until.findObject(By.res("CaptureButton")),
                5000
            )
            uiDevice.findObject(By.res("CaptureButton")).click()
            uiDevice.wait(
                Until.findObject(By.res("ImageCaptureFailureToast")),
                5000
            )
            activityScenario!!.result.resultCode
        }.register("key", ActivityResultContracts.TakePicture()) { result ->
            assert(!result)
        }.launch(null)
    }

    @Test
    fun image_capture_external_illegal_uri() = runTest {
        val inputUri = Uri.parse("asdfasdf")
        val launchIntent = Intent()
        launchIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE)
        launchIntent.putExtra(MediaStore.EXTRA_OUTPUT, inputUri)
        launchIntent.setComponent(
            ComponentName("com.google.jetpackcamera", "com.google.jetpackcamera.MainActivity")
        )
        getTestRegistry {
            activityScenario = ActivityScenario.launchActivityForResult(launchIntent)
            uiDevice.wait(
                Until.findObject(By.res("CaptureButton")),
                5000
            )
            uiDevice.findObject(By.res("CaptureButton")).click()
            uiDevice.wait(
                Until.findObject(By.res("ImageCaptureFailureToast")),
                5000
            )
            activityScenario!!.result.resultCode
        }.register("key", ActivityResultContracts.TakePicture()) { result ->
            assert(!result)
        }.launch(inputUri)
    }

    private fun doesImageFileExist(uri: Uri): Boolean {
        val file = File(uri.path)
        if (file.exists()) {
            val mimeType = URLConnection.guessContentTypeFromName(uri.path)
            return mimeType != null && mimeType.startsWith("image")
        }
        return false
    }

    private fun deleteFilesInDirAfterTimestamp(timeStamp: Long) {
        for (file in File(DIR_PATH).listFiles()) {
            if (file.lastModified() > timeStamp) {
                file.delete()
            }
        }
    }

    private fun getTestRegistry(launch: () -> Int): ActivityResultRegistry {
        val testRegistry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                val result: Int = launch()
                dispatchResult(result, null)
            }
        }
        return testRegistry
    }

    companion object {
        val DIR_PATH = Environment.getExternalStorageDirectory()
            .toString() + "/" + Environment.DIRECTORY_PICTURES
        val TEST_URI = Uri.fromFile(
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "imageCaptureTest.jpg"
            )
        )!!
    }
}
