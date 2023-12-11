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

import android.content.ContentValues
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
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
        val dirPath = Environment.getExternalStorageDirectory()
            .toString() + "/" + Environment.DIRECTORY_PICTURES
        var directory = File(dirPath)
        val files = directory.listFiles()
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
        uiDevice.waitForIdle(2000)
        uiDevice.findObject(By.res("CaptureButton")).click()
        Thread.sleep(2000)
        directory = File(dirPath)
        val pictureTaken = (files.size + 1) == directory.listFiles().size
        assert(pictureTaken)
        if (pictureTaken) {
            deleteFilesInDirAfterTimestamp(directory, timeStamp)
        }
    }

    @Test
    fun image_capture_external() = runTest {
        val timeStamp = System.currentTimeMillis()
        val dirPath = Environment.getExternalStorageDirectory()
            .toString() + "/" + Environment.DIRECTORY_PICTURES
        var directory = File(dirPath)
        val files = directory.listFiles()
        val displayName = timeStamp.toString()
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        val intent =
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).putExtra(
                MediaStore.EXTRA_OUTPUT,
                contentValues
            )
        activityScenario = ActivityScenario.launch(intent)
        uiDevice.waitForIdle(5000)
        uiDevice.findObject(By.res("CaptureButton")).click()
        Thread.sleep(2000)
        directory = File(dirPath)
        val pictureTaken = (files.size + 1) == directory.listFiles().size
        assert(pictureTaken)
        var fileExists = false
        for (file in directory.listFiles()) {
            if (file.name.contains(displayName)) {
                fileExists = true
                break
            }
        }
        assert(fileExists)
        if (pictureTaken) {
            deleteFilesInDirAfterTimestamp(directory, timeStamp)
        }
    }

    private fun deleteFilesInDirAfterTimestamp(directory: File, timeStamp: Long) {
        for (file in directory.listFiles()) {
            if (file.lastModified() > timeStamp) {
                file.delete()
            }
        }
    }
}
