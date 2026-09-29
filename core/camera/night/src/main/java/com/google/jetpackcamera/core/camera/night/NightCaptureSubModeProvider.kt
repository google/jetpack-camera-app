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
package com.google.jetpackcamera.core.camera.night

import android.content.Context
import androidx.camera.core.CameraInfo
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import com.google.jetpackcamera.core.camera.submode.CameraSessionBinding
import com.google.jetpackcamera.core.camera.submode.CaptureSubModeProvider
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.CaptureSubModeDescriptor
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.NONE_EFFECT_ID
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraFeaturePolicy
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.OptionVisibility
import com.google.jetpackcamera.settings.model.SettingConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NightCaptureSubModeProvider(
    private val applicationContext: Context
) : CaptureSubModeProvider {
    private val mutex = Mutex()
    private var cachedExtensionsManager: ExtensionsManager? = null

    override val descriptor = CaptureSubModeDescriptor(
        id = NIGHT_SUB_MODE_ID,
        parentCaptureMode = CaptureMode.IMAGE_ONLY,
        labelResId = R.string.capture_mode_night,
        sortOrder = 10,
        quickSettingsTitleResId = R.string.quick_settings_title_night_settings
    )

    override val featurePolicy = CameraFeaturePolicy(
        dynamicRange = SettingConfig(
            defaultValue = DynamicRange.SDR,
            visibility = OptionVisibility.Hidden
        ),
        imageFormat = SettingConfig(
            defaultValue = ImageOutputFormat.JPEG,
            visibility = OptionVisibility.Hidden
        ),
        flashMode = SettingConfig(
            defaultValue = FlashMode.OFF,
            visibility = OptionVisibility.from(
                FlashMode.OFF,
                FlashMode.ON,
                FlashMode.AUTO
            )
        )
    )

    private suspend fun getExtensionsManager(
        cameraProvider: ProcessCameraProvider
    ): ExtensionsManager = mutex.withLock {
        cachedExtensionsManager ?: ExtensionsManager.getInstanceAsync(
            applicationContext,
            cameraProvider
        ).await().also { cachedExtensionsManager = it }
    }

    override suspend fun isSupported(
        context: Context,
        cameraProvider: ProcessCameraProvider,
        cameraInfo: CameraInfo
    ): Boolean {
        val extensionsManager = getExtensionsManager(cameraProvider)
        return extensionsManager.isExtensionAvailable(
            cameraInfo.cameraSelector,
            ExtensionMode.NIGHT
        )
    }

    override fun isCompatibleWith(
        settings: CameraAppSettings,
        systemConstraints: CameraSystemConstraints,
        externalCaptureMode: ExternalCaptureMode
    ): Boolean = settings.captureMode == CaptureMode.IMAGE_ONLY &&
        settings.selectedCameraEffect == NONE_EFFECT_ID &&
        externalCaptureMode != ExternalCaptureMode.VideoCapture

    @Suppress("DEPRECATION")
    override val sessionBinding =
        CameraSessionBinding.SingleCamera { cameraProvider, baseSelector ->
            val extensionsManager = getExtensionsManager(cameraProvider)
            if (extensionsManager.isExtensionAvailable(baseSelector, ExtensionMode.NIGHT)) {
                extensionsManager.getExtensionEnabledCameraSelector(
                    baseSelector,
                    ExtensionMode.NIGHT
                )
            } else {
                baseSelector
            }
        }
}
