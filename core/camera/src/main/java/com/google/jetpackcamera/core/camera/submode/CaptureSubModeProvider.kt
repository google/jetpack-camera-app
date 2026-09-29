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
package com.google.jetpackcamera.core.camera.submode

import android.content.Context
import androidx.camera.core.CameraInfo
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.jetpackcamera.model.CaptureSubModeDescriptor
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraFeaturePolicy
import com.google.jetpackcamera.settings.model.CameraSystemConstraints

/**
 * SPI implemented by optional capture sub-mode modules (such as Night Mode or Concurrent Camera).
 */
interface CaptureSubModeProvider {
    /**
     * Metadata descriptor (ID, parent [com.google.jetpackcamera.model.CaptureMode], label resource, sort order).
     */
    val descriptor: CaptureSubModeDescriptor

    /**
     * Declarative [CameraFeaturePolicy] enforced while this sub-mode is active.
     *
     * - When this sub-mode is selected, [com.google.jetpackcamera.core.camera.CameraXCameraSystem]
     *   clamps `CameraAppSettings` using [CameraFeaturePolicy.enforceRestrictions] and
     *   `CaptureUiStateAdapter` intersects this policy with the host application's
     *   [CameraFeaturePolicy] to hide or restrict incompatible quick-settings controls
     *   (such as HDR or Flash).
     * - If the host application's [CameraFeaturePolicy] is incompatible with [featurePolicy]
     *   ([CameraFeaturePolicy.isCompatibleWith] returns `false`), this sub-mode is excluded from
     *   the available sub-modes in the UI.
     */
    val featurePolicy: CameraFeaturePolicy
        get() = CameraFeaturePolicy()

    /**
     * Returns true if this sub-mode is supported on the given [cameraInfo] / [cameraProvider].
     */
    suspend fun isSupported(
        context: Context,
        cameraProvider: ProcessCameraProvider,
        cameraInfo: CameraInfo
    ): Boolean

    /**
     * Returns true if this sub-mode can remain active under the given [settings],
     * [systemConstraints], and [externalCaptureMode]. When false, the camera system reverts
     * `captureSubModeId` to [com.google.jetpackcamera.model.CaptureSubModeId.DEFAULT].
     */
    fun isCompatibleWith(
        settings: CameraAppSettings,
        systemConstraints: CameraSystemConstraints,
        externalCaptureMode: ExternalCaptureMode = ExternalCaptureMode.Standard
    ): Boolean = settings.captureMode == descriptor.parentCaptureMode

    /**
     * Provides the [CameraSessionBinding] that executes when this sub-mode is active.
     */
    val sessionBinding: CameraSessionBinding
}
