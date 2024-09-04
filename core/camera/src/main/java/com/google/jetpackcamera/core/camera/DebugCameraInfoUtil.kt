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

package com.google.jetpackcamera.core.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

private const val TAG = "DebugCameraInfoUtil"
object DebugCameraInfoUtil {
    @RequiresApi(Build.VERSION_CODES.P)
    fun getAllCamerasPropertiesJSONArray(cameraManager: CameraManager): JSONArray {
        val result = JSONArray()
        for (logicalCameraId in cameraManager.cameraIdList) {
            val logicalCameraData = JSONObject()
            logicalCameraData.put(
                "logical-$logicalCameraId",
                getCameraPropertiesJSONObject(logicalCameraId, cameraManager)
            )
            for (physicalCameraId in
            cameraManager.getCameraCharacteristics(logicalCameraId).physicalCameraIds) {
                logicalCameraData.put(
                    "physical-$physicalCameraId",
                    getCameraPropertiesJSONObject(physicalCameraId, cameraManager)
                )
            }
            result.put(logicalCameraData)
        }
        return result
    }

    private fun getCameraPropertiesJSONObject(
        cameraId: String,
        cameraManager: CameraManager
    ): JSONObject {
        val cameraCharacteristics =
            cameraManager.getCameraCharacteristics(cameraId)
        val jsonObject = JSONObject()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cameraCharacteristics.get(CameraCharacteristics.LENS_POSE_ROTATION)
                ?.let { jsonObject.put(CameraCharacteristics.LENS_POSE_ROTATION.name, it) }
            cameraCharacteristics.get(CameraCharacteristics.LENS_POSE_TRANSLATION)
                ?.let { jsonObject.put(CameraCharacteristics.LENS_POSE_TRANSLATION.name, it) }
            cameraCharacteristics.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)
                ?.let { jsonObject.put(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION.name, it) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cameraCharacteristics.get(CameraCharacteristics.LENS_DISTORTION)
                ?.let { jsonObject.put(CameraCharacteristics.LENS_DISTORTION.name, it) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            cameraCharacteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                ?.let { jsonObject.put(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE.name, it) }
        }
        cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            ?.let {
                jsonObject.put(
                    CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS.name,
                    it
                )
            }
        cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
            ?.let {
                jsonObject.put(
                    CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE.name,
                    it
                )
            }
        cameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            ?.let { jsonObject.put(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES.name, it) }

        return jsonObject
    }

    fun writeFileExternalStorage(file: File, textToWrite: String) {
        //Checking the availability state of the External Storage.
        val state = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED != state) {
            //If it isn't mounted - we can't write into it.
            return
        }

        //This point and below is responsible for the write operation
        var outputStream: FileOutputStream? = null
        file.createNewFile()
        //second argument of FileOutputStream constructor indicates whether
        //to append or create new file if one exists
        outputStream = FileOutputStream(file, true)

        FileOutputStream(file).use { outputStream ->
            outputStream.write(textToWrite.toByteArray())
        }
        outputStream.close()
    }
}
