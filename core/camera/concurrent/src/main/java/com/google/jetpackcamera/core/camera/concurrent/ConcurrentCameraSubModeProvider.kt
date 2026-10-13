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
@file:kotlin.OptIn(InternalCameraApi::class)

package com.google.jetpackcamera.core.camera.concurrent

import android.content.Context
import androidx.camera.core.CameraInfo
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.jetpackcamera.core.camera.CameraSessionContext
import com.google.jetpackcamera.core.camera.InternalCameraApi
import com.google.jetpackcamera.core.camera.PerpetualSessionSettings
import com.google.jetpackcamera.core.camera.appLensFacing
import com.google.jetpackcamera.core.camera.submode.CameraSessionBinding
import com.google.jetpackcamera.core.camera.submode.CaptureSubModeProvider
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.CaptureSubModeDescriptor
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.NONE_EFFECT_ID
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.TARGET_FPS_AUTO
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraFeaturePolicy
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.OptionVisibility
import com.google.jetpackcamera.settings.model.SettingConfig

/**
 * [CaptureSubModeProvider] for the concurrent dual-camera capture sub-mode under
 * [CaptureMode.VIDEO_ONLY].
 *
 * Binds both the primary and opposite-facing cameras simultaneously via a custom concurrent
 * camera session, recording at [com.google.jetpackcamera.model.VideoQuality.UNSPECIFIED], and
 * restricts dynamic range to SDR, flash to standard modes (excluding Low Light Boost),
 * stabilization to [StabilizationMode.OFF], and target frame rate to [TARGET_FPS_AUTO].
 */
class ConcurrentCameraSubModeProvider internal constructor(
    private val availableConcurrentCameraInfos: (ProcessCameraProvider) -> List<List<CameraInfo>>
) : CaptureSubModeProvider {

    constructor() : this(
        availableConcurrentCameraInfos = { it.availableConcurrentCameraInfos }
    )

    override val descriptor = CaptureSubModeDescriptor(
        id = CONCURRENT_SUB_MODE_ID,
        parentCaptureMode = CaptureMode.VIDEO_ONLY,
        labelResId = R.string.capture_mode_concurrent_dual,
        sortOrder = 10,
        quickSettingsTitleResId = R.string.quick_settings_title_concurrent_dual_settings
    )

    override val featurePolicy = CameraFeaturePolicy(
        dynamicRange = SettingConfig(
            defaultValue = DynamicRange.SDR,
            visibility = OptionVisibility.Hidden
        ),
        flashMode = SettingConfig(
            defaultValue = FlashMode.OFF,
            visibility = OptionVisibility.from(
                FlashMode.OFF,
                FlashMode.ON,
                FlashMode.AUTO
            )
        ),
        stabilizationMode = SettingConfig(
            defaultValue = StabilizationMode.OFF,
            visibility = OptionVisibility.Hidden
        ),
        targetFrameRate = SettingConfig(
            defaultValue = TARGET_FPS_AUTO,
            visibility = OptionVisibility.Hidden
        )
    )

    override suspend fun isSupported(
        context: Context,
        cameraProvider: ProcessCameraProvider,
        cameraInfo: CameraInfo
    ): Boolean = availableConcurrentCameraInfos(cameraProvider).any { group ->
        group.map { it.appLensFacing }.toSet() == setOf(LensFacing.FRONT, LensFacing.BACK)
    }

    override fun isCompatibleWith(
        settings: CameraAppSettings,
        systemConstraints: CameraSystemConstraints,
        externalCaptureMode: ExternalCaptureMode
    ): Boolean = super.isCompatibleWith(settings, systemConstraints, externalCaptureMode) &&
        settings.selectedCameraEffect == NONE_EFFECT_ID &&
        externalCaptureMode == ExternalCaptureMode.Standard

    override val sessionBinding = CameraSessionBinding.Custom { sessionScope ->
        val context = requireNotNull(sessionScope as? CameraSessionContext) {
            "Expected CameraSessionContext as CameraSessionScope, got ${sessionScope::class.java}"
        }
        val customSettings =
            requireNotNull(context.sessionSettings as? PerpetualSessionSettings.CustomSession) {
                "Expected PerpetualSessionSettings.CustomSession, got ${context.sessionSettings}"
            }
        with(context) {
            runConcurrentCameraSession(
                sessionSettings = customSettings,
                availableConcurrentCameraInfos = availableConcurrentCameraInfos(cameraProvider)
            )
        }
    }
}
