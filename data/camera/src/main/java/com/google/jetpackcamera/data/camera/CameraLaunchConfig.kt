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
package com.google.jetpackcamera.data.camera

import android.content.Intent
import android.provider.MediaStore
import android.util.Log
import com.google.jetpackcamera.model.DebugSettings
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.LensFacing
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "CameraLaunchConfig"
internal const val KEY_DEBUG_MODE = "KEY_DEBUG_MODE"
internal const val KEY_DEBUG_SINGLE_LENS_MODE = "KEY_DEBUG_SINGLE_LENS_MODE"

/**
 * Retained configuration options supplied at launch (such as from intent extras).
 */
data class CameraLaunchConfig(
    val externalCaptureMode: ExternalCaptureMode = ExternalCaptureMode.Standard,
    val debugSettings: DebugSettings = DebugSettings()
)

/**
 * Parses [ExternalCaptureMode] from an [Intent].
 */
internal fun Intent.toExternalCaptureMode(): ExternalCaptureMode = when (action) {
    MediaStore.ACTION_IMAGE_CAPTURE -> ExternalCaptureMode.ImageCapture
    MediaStore.ACTION_VIDEO_CAPTURE -> ExternalCaptureMode.VideoCapture
    MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA -> ExternalCaptureMode.MultipleImageCapture
    else -> {
        if (action != null) {
            Log.w(TAG, "Ignoring external intent with unknown action: $action")
        }
        ExternalCaptureMode.Standard
    }
}

/**
 * Parses [DebugSettings] from an [Intent].
 */
internal fun Intent.toDebugSettings(): DebugSettings = DebugSettings(
    isDebugModeEnabled = getBooleanExtra(KEY_DEBUG_MODE, false),
    singleLensMode = getStringExtra(KEY_DEBUG_SINGLE_LENS_MODE)?.let {
        when (it.lowercase()) {
            "back" -> LensFacing.BACK
            "front" -> LensFacing.FRONT
            else -> {
                Log.e(
                    TAG,
                    "Invalid debug single lens mode argument: \"$it\". Valid values are \"FRONT\" or \"BACK\""
                )
                null
            }
        }
    }
)

/**
 * Activity-retained provider that manages [CameraLaunchConfig] reactively.
 */
@ActivityRetainedScoped
class CameraLaunchConfigProvider @Inject constructor() {
    private val _config = MutableStateFlow(CameraLaunchConfig())

    /**
     * The current [CameraLaunchConfig] state flow.
     */
    val config: StateFlow<CameraLaunchConfig> = _config.asStateFlow()

    /**
     * Updates the configuration by parsing the provided [intent].
     */
    fun setIntent(intent: Intent?) {
        if (intent == null) return
        _config.value = CameraLaunchConfig(
            externalCaptureMode = intent.toExternalCaptureMode(),
            debugSettings = intent.toDebugSettings()
        )
    }

    /**
     * Directly sets a new [config] value.
     */
    fun setConfig(config: CameraLaunchConfig) {
        _config.value = config
    }
}
