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
import android.os.Build
import android.os.Environment
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import java.io.File
import java.io.FileOutputStream
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "DebugCameraInfoUtil"
object DebugCameraInfoUtil {
    @OptIn(ExperimentalCamera2Interop::class)
    @RequiresApi(Build.VERSION_CODES.P)
    fun getAllCamerasPropertiesJSONArray(cameraInfos: List<CameraInfo>): JSONArray {
        val result = JSONArray()
        for (cameraInfo in cameraInfos) {
            var camera2cameraInfo = Camera2CameraInfo.from(cameraInfo)
            val logicalCameraId = camera2cameraInfo.cameraId
            val logicalCameraData = JSONObject()
            logicalCameraData.put(
                "logical-$logicalCameraId",
                getCameraPropertiesJSONObject(camera2cameraInfo)
            )
            for (cameraInfo in cameraInfo.physicalCameraInfos) {
                camera2cameraInfo = Camera2CameraInfo.from(cameraInfo)
                val physicalCameraId = Camera2CameraInfo.from(cameraInfo).cameraId
                logicalCameraData.put(
                    "physical-$physicalCameraId",
                    getCameraPropertiesJSONObject(camera2cameraInfo)
                )
            }
            result.put(logicalCameraData)
        }
        return result
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun getCameraPropertiesJSONObject(cameraInfo: Camera2CameraInfo): JSONObject {
        val jsonObject = JSONObject()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cameraInfo.getCameraCharacteristic(CameraCharacteristics.LENS_POSE_ROTATION)
                ?.let { jsonObject.put(CameraCharacteristics.LENS_POSE_ROTATION.name, it) }
            cameraInfo.getCameraCharacteristic(CameraCharacteristics.LENS_POSE_TRANSLATION)
                ?.let { jsonObject.put(CameraCharacteristics.LENS_POSE_TRANSLATION.name, it) }
            cameraInfo.getCameraCharacteristic(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)
                ?.let { jsonObject.put(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION.name, it) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cameraInfo.getCameraCharacteristic(CameraCharacteristics.LENS_DISTORTION)
                ?.let { jsonObject.put(CameraCharacteristics.LENS_DISTORTION.name, it) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            cameraInfo.getCameraCharacteristic(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                ?.let { jsonObject.put(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE.name, it) }
        }
        cameraInfo.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            ?.let {
                jsonObject.put(
                    CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS.name,
                    it
                )
            }
        cameraInfo.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
            ?.let {
                jsonObject.put(
                    CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE.name,
                    it
                )
            }
        cameraInfo.getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            ?.let { jsonObject.put(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES.name, it) }

        return jsonObject
    }

    fun writeFileExternalStorage(file: File, textToWrite: String) {
        // Checking the availability state of the External Storage.
        val state = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED != state) {
            // If it isn't mounted - we can't write into it.
            return
        }

        var outputStream: FileOutputStream? = null
        file.createNewFile()
        outputStream = FileOutputStream(file, true)

        FileOutputStream(file).use { outputStream ->
            outputStream.write(textToWrite.toByteArray())
        }
        outputStream.close()
    }
}
