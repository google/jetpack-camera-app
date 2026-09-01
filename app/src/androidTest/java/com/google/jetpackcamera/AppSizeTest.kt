/*
 * Copyright (C) 2026 The Android Open Source Project
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
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSizeTest {
    @Test
    fun testSizes() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        for (id in cm.cameraIdList) {
            val chars = cm.getCameraCharacteristics(id)
            val extChars = try {
                cm.getCameraExtensionCharacteristics(id)
            } catch (
                e: Exception
            ) {
                null
            }

            val standardSizes = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?.getOutputSizes(ImageFormat.JPEG)

            val nightSizes = if (extChars?.supportedExtensions?.contains(
                    CameraExtensionCharacteristics.EXTENSION_NIGHT
                ) == true
            ) {
                extChars.getExtensionSupportedSizes(
                    CameraExtensionCharacteristics.EXTENSION_NIGHT,
                    ImageFormat.JPEG
                )
            } else {
                null
            }

            Log.d(
                "SIZETEST",
                "Camera $id Standard JPEG max: ${standardSizes?.maxByOrNull { it.width * it.height }}"
            )
            Log.d(
                "SIZETEST",
                "Camera $id Night Mode JPEG max: ${nightSizes?.maxByOrNull { it.width * it.height }}"
            )
        }
    }
}
