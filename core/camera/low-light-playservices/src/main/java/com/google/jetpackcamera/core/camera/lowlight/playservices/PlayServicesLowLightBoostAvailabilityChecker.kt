/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.google.jetpackcamera.core.camera.lowlight.playservices

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import com.google.android.gms.cameralowlight.LowLightBoost
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.jetpackcamera.core.camera.lowlight.LowLightBoostAvailabilityChecker
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

private const val TAG = "PsLlbAvailChecker"

class PlayServicesLowLightBoostAvailabilityChecker @Inject constructor() :
    LowLightBoostAvailabilityChecker {
    @OptIn(ExperimentalCamera2Interop::class)
    override suspend fun isImplementationAvailable(
        cameraInfo: CameraInfo,
        context: Context
    ): Boolean {
        val camera2Info = Camera2CameraInfo.from(cameraInfo)
        // Check for Google LLB support.
        var gLlbSupport = false
        var gLlbAvailable = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val cameraId = camera2Info.cameraId
            try {
                // TODO: Remove when Google LLB beta07 is available with this fixed.
                if (!isGooglePlayServicesWithVideoTimestampFixAvailable(context)) {
                    throw Exception("Google Play Services with video timestamp fix not available.")
                }
                val lowLightBoostClient = LowLightBoost.getClient(context)
                gLlbSupport = lowLightBoostClient.isCameraSupported(cameraId).await()
                gLlbAvailable = lowLightBoostClient.isModuleInstalled().await()
                if (gLlbSupport && !gLlbAvailable) {
                    // Install the module for future use, but the install will take too long to use
                    // now since the camera needs to be opened right away.
                    lowLightBoostClient.installModule(null).addOnSuccessListener {
                        Log.d(
                            TAG,
                            "Low Light Boost module installation successful. " +
                                "App restart required for the feature to be available."
                        )
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Low Light Boost module installation failed.", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set up Google Low Light Boost for camera $cameraId", e)
                gLlbSupport = false
                gLlbAvailable = false
            }
        }
        return gLlbSupport && gLlbAvailable
    }
}

// TODO: Remove when Google LLB beta07 is available with this fixed.
private fun isGooglePlayServicesWithVideoTimestampFixAvailable(context: Context): Boolean {
    val minVersion = 253300000 // (Y25W33)
    return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context, minVersion) ==
        ConnectionResult.SUCCESS
}
