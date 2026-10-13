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
import android.util.Log
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
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
import com.google.jetpackcamera.model.NONE_EFFECT_ID
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraFeaturePolicy
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.OptionVisibility
import com.google.jetpackcamera.settings.model.SettingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "NightSubModeProvider"

/**
 * [CaptureSubModeProvider] for the Night Mode CameraX extension under [CaptureMode.IMAGE_ONLY].
 *
 * Queries [ExtensionsManager] for [ExtensionMode.NIGHT] support per camera lens, transforms the
 * [CameraSelector] when Night Mode is active, and restricts dynamic range to SDR, stabilization
 * to off, flash to standard modes (excluding Low Light Boost), and the camera effect to none.
 *
 * Night Mode can't start while a camera effect is selected ([isCompatibleWith]), so the camera
 * effect restriction does not change the settings; it disables the stream configuration setting
 * while Night Mode is active.
 */
class NightCaptureSubModeProvider internal constructor(
    private val applicationContext: Context,
    private val isNightAvailable: (ExtensionsManager, CameraSelector) -> Boolean,
    private val enableNightSelector: (ExtensionsManager, CameraSelector) -> CameraSelector,
    private val extensionsManagerFactory: suspend (ProcessCameraProvider) -> ExtensionsManager =
        { cameraProvider ->
            ExtensionsManager.getInstanceAsync(applicationContext, cameraProvider).await()
        }
) : CaptureSubModeProvider {

    constructor(applicationContext: Context) : this(
        applicationContext = applicationContext,
        isNightAvailable = { manager, selector ->
            manager.isExtensionAvailable(selector, ExtensionMode.NIGHT)
        },
        enableNightSelector = { manager, selector ->
            @Suppress("DEPRECATION")
            manager.getExtensionEnabledCameraSelector(selector, ExtensionMode.NIGHT)
        }
    )

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
        stabilizationMode = SettingConfig(
            defaultValue = StabilizationMode.OFF,
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
        cameraEffect = SettingConfig(
            defaultValue = NONE_EFFECT_ID,
            visibility = OptionVisibility.Hidden
        )
    )

    private suspend fun getExtensionsManager(
        cameraProvider: ProcessCameraProvider
    ): ExtensionsManager = mutex.withLock {
        cachedExtensionsManager ?: extensionsManagerFactory(cameraProvider).also {
            cachedExtensionsManager = it
        }
    }

    override suspend fun isSupported(
        context: Context,
        cameraProvider: ProcessCameraProvider,
        cameraInfo: CameraInfo
    ): Boolean = try {
        val extensionsManager = getExtensionsManager(cameraProvider)
        isNightAvailable(extensionsManager, cameraInfo.cameraSelector)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Failed to query Night extension availability", e)
        false
    }

    override fun isCompatibleWith(
        settings: CameraAppSettings,
        systemConstraints: CameraSystemConstraints,
        externalCaptureMode: ExternalCaptureMode
    ): Boolean = super.isCompatibleWith(settings, systemConstraints, externalCaptureMode) &&
        settings.selectedCameraEffect == NONE_EFFECT_ID &&
        externalCaptureMode != ExternalCaptureMode.VideoCapture

    @Suppress("DEPRECATION")
    override val sessionBinding =
        CameraSessionBinding.SingleCamera { cameraProvider, baseSelector ->
            val extensionsManager = getExtensionsManager(cameraProvider)
            check(isNightAvailable(extensionsManager, baseSelector)) {
                "Night extension is not available for selector $baseSelector"
            }
            enableNightSelector(extensionsManager, baseSelector)
        }
}
