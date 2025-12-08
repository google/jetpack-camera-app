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
package com.google.jetpackcamera.core.camera

import android.util.Log
import androidx.camera.core.CameraInfo
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraConstraints
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.forCurrentLens
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "FeatureGroupHandler"

/**
 * Handles logic related to CameraX feature groups.
 *
 * This class encapsulates the operations required to ensure camera settings are compatible with
 * device capabilities using the CameraX feature group API.
 *
 * Key functionalities include:
 * - [filterSystemConstraints]: Validates the current camera settings against feature group requirements
 *   and updates [CameraSystemConstraints] to filter out incompatible options.
 * - [isGroupingSupported]: Checks if a specific combination of [CameraAppSettings] is supported
 *   as a valid feature group on the device.
 */
internal class FeatureGroupHandler(
    // TODO: Remove the CameraXCameraSystem dependency from here by refactoring out all camera
    //   setting applying APIs (e.g. applyDynamicRange) from CameraXCameraSystem
    private val cameraSystem: CameraXCameraSystem,
    private val cameraProvider: ProcessCameraProvider,
    private val defaultCameraSessionContext: CameraSessionContext,
    private val defaultDispatcher: CoroutineDispatcher
) {
    private var isHdrSupportedWithJpegR = atomic<Boolean?>(null)

    /**
     * Filters the [CameraSystemConstraints] based on feature group compatibility.
     *
     * This function checks various combinations of settings (dynamic range, frame rate,
     * stabilization, etc.) against the device's capabilities using the CameraX feature groups API.
     * It filters out unsupported options from the system constraints, ensuring that the UI only
     * presents valid combinations to the user.
     */
    suspend fun filterSystemConstraints(
        currentSettings: CameraAppSettings,
        initialSystemConstraints: CameraSystemConstraints,
        currentSystemConstraints: CameraSystemConstraints
    ): CameraSystemConstraints {
        val initialCameraConstraints =
            requireNotNull(initialSystemConstraints.forCurrentLens(currentSettings))

        Log.d(
            TAG,
            "filterSystemConstraints: cameraAppSettings = $currentSettings" +
                ", initialCameraConstraints = $initialCameraConstraints"
        )

        // Access internal extension function from CameraXCameraSystem
        val sessionSettings = with(cameraSystem) {
            currentSettings.toSingleCameraSessionSettings(initialCameraConstraints)
        }
        val featureDataSet = sessionSettings.toFeatureGroupabilities()

        if (featureDataSet.isInvalid()) {
            Log.i(
                TAG,
                "filterSystemConstraints: since the settings is incompatible" +
                    " with CameraX feature groups API, falling back to initial" +
                    " system constraints without using feature groups. featureDataSet = " +
                    " $featureDataSet."
            )
            return initialSystemConstraints
        }

        val cameraInfo =
            cameraProvider.getCameraInfo(currentSettings.cameraLensFacing.toCameraSelector())

        // TODO: More stabilization + FPS pairs can be supported with CameraX feature group API.
        //  However, while the following code does provide such support, this function is called
        //  only when camera session is recreated. So, updating unsupportedStabilizationFpsMap now
        //  can cause regressions in scenarios where user tries to change both stabilization mode
        //  and FPS mode from settings page directly. We need to ensure this function is used
        //  for each setting value update to avoid that.

//        val unsupportedStabilizationFpsMap = buildMap {
//            initialCameraConstraints
//                .unsupportedStabilizationFpsMap
//                .forEach { (stabilizationMode, fpsList) ->
//                    if (stabilizationMode.toFeatureGroupability() is Nongroupable) {
//                        put(stabilizationMode, fpsList)
//                        return@forEach
//                    }
//
//                    fpsList.forEach { fps ->
//                        if (fps.toFpsFeatureGroupability() is Nongroupable) {
//                            put(stabilizationMode, fpsList)
//                            return@forEach
//                        }
//
//                        if (!cameraAppSettings.copyStabilizationMode(stabilizationMode)
//                                .copyTargetFrameRate(fps).isGroupingSupported(cameraInfo)
//                        ) {
//                            put(stabilizationMode, fpsList)
//                        }
//                    }
//                }
//        }

        val updatedPerLensConstraints = initialSystemConstraints.perLensConstraints.toMutableMap()

        updatedPerLensConstraints[currentSettings.cameraLensFacing] =
            initialCameraConstraints
                .copy(
                    supportedDynamicRanges = filterGroupableDynamicRanges(
                        currentSettings,
                        initialSystemConstraints,
                        initialCameraConstraints,
                        cameraInfo
                    ),
                    supportedFixedFrameRates = filterGroupableFrameRates(
                        currentSettings,
                        initialSystemConstraints,
                        initialCameraConstraints,
                        cameraInfo
                    ),
                    supportedStabilizationModes = filterGroupableStabilizationModes(
                        currentSettings,
                        initialSystemConstraints,
                        initialCameraConstraints,
                        cameraInfo
                    ),
                    supportedImageFormatsMap = filterGroupableImageFormatsMap(
                        currentSettings,
                        initialSystemConstraints,
                        initialCameraConstraints,
                        cameraInfo
                    ),
                    supportedVideoQualitiesMap = filterGroupableVideoQualitiesMap(
                        currentSettings,
                        initialSystemConstraints,
                        initialCameraConstraints,
                        cameraInfo
                    ),
                    supportedStreamConfigs = filterStreamConfig(
                        currentSettings,
                        initialSystemConstraints,
                        initialCameraConstraints,
                        cameraInfo
                    )
//                    unsupportedStabilizationFpsMap = unsupportedStabilizationFpsMap
                )

        val newConstraints = currentSystemConstraints.copy(
            perLensConstraints = updatedPerLensConstraints
        )

        Log.d(TAG, "filterSystemConstraints: updated systemConstraints = $newConstraints")

        cacheConcurrentHdrJpegRCapability(currentSettings, newConstraints)

        return newConstraints
    }

    /**
     * Filters supported [DynamicRange]s by checking groupability with current settings.
     */
    private suspend fun filterGroupableDynamicRanges(
        cameraAppSettings: CameraAppSettings,
        initialSystemConstraints: CameraSystemConstraints,
        initialCameraConstraints: CameraConstraints,
        cameraInfo: CameraInfo
    ): Set<DynamicRange> {
        Log.d(TAG, "filterGroupableDynamicRanges")

        return initialCameraConstraints.supportedDynamicRanges.filter {
            val settings = with(cameraSystem) {
                cameraAppSettings.applyDynamicRange(it, initialSystemConstraints)
            }
            isGroupingSupported(settings, cameraInfo, initialSystemConstraints)
        }.toSet()
    }

    /**
     * Filters supported frame rates by checking groupability with current settings.
     */
    private suspend fun filterGroupableFrameRates(
        cameraAppSettings: CameraAppSettings,
        initialSystemConstraints: CameraSystemConstraints,
        initialCameraConstraints: CameraConstraints,
        cameraInfo: CameraInfo
    ): Set<Int> {
        Log.d(TAG, "filterGroupableFrameRates")

        return initialCameraConstraints.supportedFixedFrameRates.filter {
            val settings = with(cameraSystem) {
                cameraAppSettings.applyTargetFrameRate(it, initialSystemConstraints)
            }
            isGroupingSupported(settings, cameraInfo, initialSystemConstraints)
        }.toSet()
    }

    /**
     * Filters supported [StabilizationMode]s by checking groupability with current settings.
     */
    private suspend fun filterGroupableStabilizationModes(
        cameraAppSettings: CameraAppSettings,
        initialSystemConstraints: CameraSystemConstraints,
        initialCameraConstraints: CameraConstraints,
        cameraInfo: CameraInfo
    ): Set<StabilizationMode> {
        Log.d(TAG, "filterGroupableStabilizationModes")

        return initialCameraConstraints.supportedStabilizationModes.filter {
            Log.d(TAG, "filterGroupableStabilizationModes: it = $it")

            val resolvedStabilizationMode = with(cameraSystem) {
                resolveStabilizationMode(
                    requestedStabilizationMode = it,
                    cameraAppSettings = cameraAppSettings,
                    cameraConstraints = initialCameraConstraints
                )
            }
            val settings = with(cameraSystem) {
                cameraAppSettings.applyStabilizationMode(resolvedStabilizationMode)
            }
            isGroupingSupported(settings, cameraInfo, initialSystemConstraints)
        }.toSet()
    }

    /**
     * Filters supported [ImageOutputFormat]s by checking groupability with current settings.
     */
    private suspend fun filterGroupableImageFormatsMap(
        cameraAppSettings: CameraAppSettings,
        initialSystemConstraints: CameraSystemConstraints,
        initialCameraConstraints: CameraConstraints,
        cameraInfo: CameraInfo
    ): Map<StreamConfig, Set<ImageOutputFormat>> {
        Log.d(TAG, "filterGroupableImageFormatsMap")

        return buildMap {
            initialCameraConstraints
                .supportedImageFormatsMap
                .forEach { (streamConfig, imageFormats) ->
                    put(
                        streamConfig,
                        imageFormats.filter {
                            val settings = with(cameraSystem) {
                                cameraAppSettings
                                    .applyStreamConfig(streamConfig, initialSystemConstraints)
                                    .run {
                                        if (it == ImageOutputFormat.JPEG_ULTRA_HDR) {
                                            // tryApplyImageFormatConstraints changes capture mode
                                            // to VIDEO_ONLY if both JPEG_R and HDR are supported
                                            // with STANDARD capture mode. VIDEO_ONLY capture mode
                                            // leads to IMAGE_ULTRA_HDR GroupableFeature not being
                                            // set to CameraX. To workaround this issue, capture
                                            // mode is manually set to IMAGE_ONLY to ensure the
                                            // capability checking is correct.

                                            applyCaptureMode(
                                                CaptureMode.IMAGE_ONLY,
                                                initialSystemConstraints
                                            )
                                        } else {
                                            this
                                        }
                                    }
                                    .applyImageFormat(it, initialSystemConstraints)
                            }
                            isGroupingSupported(settings, cameraInfo, initialSystemConstraints)
                        }.toSet()
                    )
                }
        }
    }

    /**
     * Filters supported [VideoQuality]s by checking groupability with current settings.
     */
    private suspend fun filterGroupableVideoQualitiesMap(
        cameraAppSettings: CameraAppSettings,
        initialSystemConstraints: CameraSystemConstraints,
        initialCameraConstraints: CameraConstraints,
        cameraInfo: CameraInfo
    ): Map<DynamicRange, List<VideoQuality>> {
        Log.d(TAG, "filterGroupableVideoQualitiesMap")

        return buildMap {
            initialCameraConstraints
                .supportedVideoQualitiesMap
                .forEach { (dynamicRange, videoQualities) ->
                    put(
                        dynamicRange,
                        videoQualities.filter {
                            val settings = with(cameraSystem) {
                                cameraAppSettings
                                    .applyDynamicRange(dynamicRange, initialSystemConstraints)
                                    .applyVideoQuality(it, initialSystemConstraints)
                            }
                            isGroupingSupported(settings, cameraInfo, initialSystemConstraints)
                        }
                    )
                }
        }
    }

    /**
     * Filters supported [StreamConfig]s by checking groupability with current settings.
     */
    private suspend fun filterStreamConfig(
        cameraAppSettings: CameraAppSettings,
        initialSystemConstraints: CameraSystemConstraints,
        initialCameraConstraints: CameraConstraints,
        cameraInfo: CameraInfo
    ): Set<StreamConfig> {
        return initialCameraConstraints.supportedStreamConfigs.filter {
            val settings = with(cameraSystem) {
                cameraAppSettings.applyStreamConfig(it)
            }
            isGroupingSupported(settings, cameraInfo, initialSystemConstraints)
        }.toSet()
    }

    /**
     * Returns whether a [CameraAppSettings] is supported together as a group.
     *
     * High quality features sometimes can be supported individually while being unsupported
     * together as a group. This API utilizes the CameraX feature group APIs to know if a
     * [CameraAppSettings] is supported as a group.
     *
     * However, not all [CameraAppSettings] feature values are queryable through the feature group
     * APIs. So, this API works in a best-effort manner by using only the queryable features
     */
    internal suspend fun isGroupingSupported(
        cameraAppSettings: CameraAppSettings,
        cameraInfo: CameraInfo,
        initialSystemConstraints: CameraSystemConstraints
    ): Boolean {
        // TODO: Optimize feature group queries via pre-calculation and persistence.
        //  Reconstructing the full SessionConfig and UseCases for every query is expensive.
        //  Consider pre-calculating the 16 possible combinations of groupable features
        //  (HDR, 60FPS, etc.) and persisting the results in a database. This would make UI checks
        //  O(1) across app launches since hardware capabilities are generally static.

        val cameraConstraints =
            requireNotNull(initialSystemConstraints.forCurrentLens(cameraAppSettings))

        val transientSettings =
            with(cameraSystem) { cameraAppSettings.toTransientSessionSettings() }

        val sessionSettings = with(cameraSystem) {
            cameraAppSettings.toSingleCameraSessionSettings(cameraConstraints)
        }

        if (sessionSettings.toFeatureGroupabilities().isInvalid()) {
            Log.d(
                TAG,
                "isGroupingSupported: CameraX feature group is not compatible with this" +
                    " session settings, so returning unsupported early." +
                    " sessionSettings = $sessionSettings"
            )
            return false
        }

        // An explicit job is used here because simpler approach like `coroutineScope { ... }`
        // seems to get stuck forever for StreamConfig.SINGLE_STREAM. The code flow for
        // SINGLE_STREAM seems to be keeping the coroutine scope forever busy and thus the
        // `coroutineScope` block never completes.
        val job = Job()

        val sessionConfig = with(
            defaultCameraSessionContext.copy(
                transientSettings = MutableStateFlow(transientSettings).asStateFlow()
            )
        ) {
            val videoCaptureUseCase =
                createVideoUseCase(
                    cameraInfo,
                    cameraAppSettings.aspectRatio,
                    cameraAppSettings.captureMode,
                    backgroundDispatcher,
                    cameraAppSettings.targetFrameRate.takeIfFeatureGroupInvalid(sessionSettings),
                    cameraAppSettings.stabilizationMode.takeIfFeatureGroupInvalid(sessionSettings),
                    cameraAppSettings.dynamicRange.takeIfFeatureGroupInvalid(sessionSettings),
                    cameraAppSettings.videoQuality.takeIfFeatureGroupInvalid(sessionSettings)
                )

            createSessionConfig(
                cameraConstraints = cameraConstraints,
                initialTransientSettings = transientSettings,
                videoCaptureUseCase = videoCaptureUseCase,
                sessionSettings = sessionSettings,
                sessionScope = CoroutineScope(defaultDispatcher + job)
            )
        }

        return cameraInfo.isSessionConfigSupported(sessionConfig).apply {
            job.cancel()
        }
    }

    private suspend fun cacheConcurrentHdrJpegRCapability(
        cameraAppSettings: CameraAppSettings,
        systemConstraints: CameraSystemConstraints
    ) {
        with(cameraSystem) {
            val supported = isGroupingSupported(
                cameraAppSettings
                    .copy(dynamicRange = cameraAppSettings.dynamicRange)
                    .applyImageFormat(
                        imageFormat = ImageOutputFormat.JPEG_ULTRA_HDR,
                        systemConstraints = systemConstraints.copy()
                    ),
                cameraProvider
                    .getCameraInfo(
                        cameraAppSettings
                            .cameraLensFacing
                            .toCameraSelector()
                    ),
                systemConstraints
            )

            isHdrSupportedWithJpegR.value = supported
        }
    }

    /**
     * Returns whether HDR is supported with JPEG_R, a null value represents the result is still
     * unknown.
     */
    fun isHdrSupportedWithJpegR(): Boolean? = isHdrSupportedWithJpegR.value
}
