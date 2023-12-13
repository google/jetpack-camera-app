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

import android.os.Environment
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
        val dirPath = Environment.getExternalStorageDirectory()
            .toString() + "/" + Environment.DIRECTORY_PICTURES
        var directory = File(dirPath)
        val files = directory.listFiles()
        activityScenario = ActivityScenario.launchActivityForResult(MainActivity::class.java)
        uiDevice.wait(
            Until.findObject(By.res("CaptureButton")),
            5000
        )
        uiDevice.findObject(By.res("CaptureButton")).click()
        uiDevice.wait(
            Until.findObject(By.res("ImageCaptureSuccessToast")),
            5000
        )
        directory = File(dirPath)
        val pictureTaken = (files.size + 1) == directory.listFiles().size
        assert(pictureTaken)
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
