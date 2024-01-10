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

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.io.File
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
    fun image_capture_default() = runTest {
        val timeStamp = System.currentTimeMillis()
        var directory = File(DIR_PATH)
        val files = directory.listFiles()
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
        uiDevice.wait(
            Until.findObject(By.res("CaptureButton")),
            5000
        )
        uiDevice.findObject(By.res("CaptureButton")).click()
        uiDevice.wait(
            Until.findObject(By.res("ImageCaptureSuccessToast")),
            5000
        )
        val pictureTaken = (files.size + 1) == directory.listFiles().size
        assert(pictureTaken)
        if (pictureTaken) {
            deleteFilesInDirAfterTimestamp(timeStamp)
        }
    }

    @Test
    fun image_capture_external() = runTest {
        val timeStamp = System.currentTimeMillis()
        var directory = File(DIR_PATH)
        val files = directory.listFiles()
        val launchIntent = Intent()
        launchIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE)
        launchIntent.putExtra(MediaStore.EXTRA_OUTPUT, TEST_URI)
        launchIntent.setComponent(
            ComponentName("com.google.jetpackcamera", "com.google.jetpackcamera.MainActivity")
        )
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
        val result = activityScenario!!.result.resultCode
        assert(result == Activity.RESULT_OK)
        val pictureTaken = (files.size + 1) == directory.listFiles().size
        assert(pictureTaken)
        if (pictureTaken) {
            deleteFilesInDirAfterTimestamp(timeStamp)
        }
    }

    @Test
    fun image_capture_external_no_uri() = runTest {
        val launchIntent = Intent()
        launchIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE)
        launchIntent.setComponent(
            ComponentName("com.google.jetpackcamera", "com.google.jetpackcamera.MainActivity")
        )
        activityScenario = ActivityScenario.launchActivityForResult(launchIntent)
        val result = activityScenario!!.result.resultCode
        assert(result == Activity.RESULT_CANCELED)
    }

    @Test
    fun image_capture_external_illegal_uri() = runTest {
        var directory = File(DIR_PATH)
        val files = directory.listFiles()
        val launchIntent = Intent()
        launchIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE)
        launchIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.parse("asdfasdf"))
        launchIntent.setComponent(
            ComponentName("com.google.jetpackcamera", "com.google.jetpackcamera.MainActivity")
        )
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
        val result = activityScenario!!.result.resultCode
        assert(result == Activity.RESULT_CANCELED)
        val pictureTaken = (files.size + 1) == directory.listFiles().size
        assert(!pictureTaken)
    }

    private fun deleteFilesInDirAfterTimestamp(timeStamp: Long) {
        for (file in File(DIR_PATH).listFiles()) {
            if (file.lastModified() > timeStamp) {
                file.delete()
            }
        }
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
