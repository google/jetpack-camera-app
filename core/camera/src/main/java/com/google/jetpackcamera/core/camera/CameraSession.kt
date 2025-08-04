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

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraInfo
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.TorchState
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileDescriptorOutputOptions
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NONE
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.asFlow
import com.google.jetpackcamera.core.camera.CameraCoreUtil.getDefaultMediaSaveLocation
import com.google.jetpackcamera.core.camera.effects.SingleSurfaceForcingEffect
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LowLightBoostState
import com.google.jetpackcamera.model.SaveLocation
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.model.TestPattern
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.model.VideoQuality.FHD
import com.google.jetpackcamera.model.VideoQuality.HD
import com.google.jetpackcamera.model.VideoQuality.SD
import com.google.jetpackcamera.model.VideoQuality.UHD
import java.io.File
import java.util.Date
import java.util.concurrent.Executor
import kotlin.coroutines.ContinuationInterceptor
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "CameraSession"
private val QUALITY_RANGE_MAP = mapOf(
    UHD to Range.create(2160, 4319),
    FHD to Range.create(1080, 1439),
    HD to Range.create(720, 1079),
    SD to Range.create(241, 719)
)

context(CameraSessionContext)
internal suspend fun runSingleCameraSession(
    sessionSettings: PerpetualSessionSettings.SingleCamera,
    // TODO(tm): ImageCapture should go through an event channel like VideoCapture
    onImageCaptureCreated: (ImageCapture) -> Unit = {}
) = coroutineScope {
    Log.d(TAG, "Starting new single camera session")
    val initialCameraSelector = transientSettings.filterNotNull().first()
        .primaryLensFacing.toCameraSelector()

    // only create video use case in standard or video_only
    val videoCaptureUseCase = when (sessionSettings.captureMode) {
        CaptureMode.STANDARD, CaptureMode.VIDEO_ONLY ->
            createVideoUseCase(
                cameraProvider.getCameraInfo(initialCameraSelector),
                sessionSettings.aspectRatio,
                sessionSettings.targetFrameRate,
                sessionSettings.stabilizationMode,
                sessionSettings.dynamicRange,
                sessionSettings.videoQuality,
                backgroundDispatcher
            )
        else -> {
            null
        }
    }

    launch {
        processVideoControlEvents(
            videoCaptureUseCase,
            captureTypeSuffix = when (sessionSettings.streamConfig) {
                StreamConfig.MULTI_STREAM -> "MultiStream"
                StreamConfig.SINGLE_STREAM -> "SingleStream"
            }
        )
    }

    transientSettings
        .filterNotNull()
        .distinctUntilChanged { old, new -> old.primaryLensFacing == new.primaryLensFacing }
        .collectLatest { currentTransientSettings ->
            cameraProvider.unbindAll()
            val currentCameraSelector = currentTransientSettings.primaryLensFacing
                .toCameraSelector()
            val useCaseGroup = createUseCaseGroup(
                cameraInfo = cameraProvider.getCameraInfo(currentCameraSelector),
                videoCaptureUseCase = videoCaptureUseCase,
                initialTransientSettings = currentTransientSettings,
                stabilizationMode = sessionSettings.stabilizationMode,
                aspectRatio = sessionSettings.aspectRatio,
                dynamicRange = sessionSettings.dynamicRange,
                imageFormat = sessionSettings.imageFormat,
                captureMode = sessionSettings.captureMode,
                effect = when (sessionSettings.streamConfig) {
                    StreamConfig.SINGLE_STREAM -> SingleSurfaceForcingEffect(this@coroutineScope)
                    StreamConfig.MULTI_STREAM -> null
                }
            ).apply {
                getImageCapture()?.let(onImageCaptureCreated)
            }

            cameraProvider.runWith(
                currentCameraSelector,
                useCaseGroup
            ) { camera ->
                Log.d(TAG, "Camera session started")
                launch {
                    processFocusMeteringEvents(camera.cameraControl)
                }

                launch {
                    camera.cameraInfo.torchState.asFlow().collectLatest { torchState ->
                        currentCameraState.update { old ->
                            old.copy(torchEnabled = torchState == TorchState.ON)
                        }
                    }
                }

                if (videoCaptureUseCase != null) {
                    val videoQuality = getVideoQualityFromResolution(
                        videoCaptureUseCase.resolutionInfo?.resolution
                    )
                    if (videoQuality != sessionSettings.videoQuality) {
                        Log.e(
                            TAG,
                            "Failed to select video quality: $sessionSettings.videoQuality. " +
                                "Fallback: $videoQuality"
                        )
                    }
                    launch {
                        currentCameraState.update { old ->
                            old.copy(
                                videoQualityInfo = VideoQualityInfo(
                                    videoQuality,
                                    getWidthFromCropRect(
                                        videoCaptureUseCase.resolutionInfo?.cropRect
                                    ),
                                    getHeightFromCropRect(
                                        videoCaptureUseCase.resolutionInfo?.cropRect
                                    )
                                )
                            )
                        }
                    }
                }

                // update camerastate to mirror current zoomstate

                launch {
                    camera.cameraInfo.zoomState
                        .asFlow()
                        .filterNotNull()
                        .distinctUntilChanged()
                        .onCompletion {
                            // reset current camera state when changing cameras.
                            currentCameraState.update { old ->
                                old.copy(
                                    zoomRatios = emptyMap(),
                                    linearZoomScales = emptyMap()
                                )
                            }
                        }
                        .collectLatest { zoomState ->
                            // TODO(b/405987189): remove checks after buggy zoomState is fixed
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                                if (zoomState.zoomRatio != 1.0f ||
                                    zoomState.zoomRatio == currentTransientSettings
                                        .zoomRatios[currentTransientSettings.primaryLensFacing]
                                ) {
                                    currentCameraState.update { old ->
                                        old.copy(
                                            zoomRatios = old.zoomRatios
                                                .toMutableMap()
                                                .apply {
                                                    put(
                                                        camera.cameraInfo.appLensFacing,
                                                        zoomState.zoomRatio
                                                    )
                                                }.toMap(),
                                            linearZoomScales = old.linearZoomScales
                                                .toMutableMap()
                                                .apply {
                                                    put(
                                                        camera.cameraInfo.appLensFacing,
                                                        zoomState.linearZoom
                                                    )
                                                }.toMap()
                                        )
                                    }
                                }
                            }
                        }
                }

                applyDeviceRotation(currentTransientSettings.deviceRotation, useCaseGroup)
                processTransientSettingEvents(
                    camera,
                    useCaseGroup,
                    currentTransientSettings,
                    transientSettings
                )
            }
        }
}

context(CameraSessionContext)
@OptIn(ExperimentalCamera2Interop::class)
internal suspend fun processTransientSettingEvents(
    camera: Camera,
    useCaseGroup: UseCaseGroup,
    initialTransientSettings: TransientSessionSettings,
    transientSettings: StateFlow<TransientSessionSettings?>
) {
    // Immediately Apply camera zoom from current settings when opening a new camera
    camera.cameraControl.setZoomRatio(
        initialTransientSettings.zoomRatios[camera.cameraInfo.appLensFacing] ?: 1f
    )

    val camera2OptionsBuilder = CaptureRequestOptions.Builder()
    updateCamera2RequestOptions(camera, null, initialTransientSettings, camera2OptionsBuilder)

    var prevTransientSettings = initialTransientSettings
    val isFrontFacing = camera.cameraInfo.appLensFacing == LensFacing.FRONT
    var torchOn = false
    fun setTorch(newTorchOn: Boolean) {
        if (newTorchOn != torchOn) {
            camera.cameraControl.enableTorch(newTorchOn)
            torchOn = newTorchOn
        }
    }
    combine(
        transientSettings.filterNotNull(),
        currentCameraState.asStateFlow().transform { emit(it.videoRecordingState) }
    ) { newTransientSettings, videoRecordingState ->
        return@combine Pair(newTransientSettings, videoRecordingState)
    }.collect { transientPair ->
        val newTransientSettings = transientPair.first
        val videoRecordingState = transientPair.second

        // todo(): handle torch on Auto FlashMode
        // enable torch only while recording is in progress
        if ((videoRecordingState !is VideoRecordingState.Inactive) &&
            newTransientSettings.flashMode == FlashMode.ON &&
            !isFrontFacing
        ) {
            setTorch(true)
        } else {
            setTorch(false)
        }

        // apply camera torch mode to image capture
        useCaseGroup.getImageCapture()?.let { imageCapture ->
            if (prevTransientSettings.flashMode != newTransientSettings.flashMode) {
                setFlashModeInternal(
                    imageCapture = imageCapture,
                    flashMode = newTransientSettings.flashMode,
                    isFrontFacing = camera.cameraInfo.appLensFacing == LensFacing.FRONT
                )
            }
        }

        if (prevTransientSettings.deviceRotation
            != newTransientSettings.deviceRotation
        ) {
            Log.d(
                TAG,
                "Updating device rotation from " +
                    "${prevTransientSettings.deviceRotation} -> " +
                    "${newTransientSettings.deviceRotation}"
            )
            applyDeviceRotation(newTransientSettings.deviceRotation, useCaseGroup)
        }

        // setzoomratio when the primary zoom value changes.
        if (prevTransientSettings.primaryLensFacing == newTransientSettings.primaryLensFacing &&
            prevTransientSettings.zoomRatios[prevTransientSettings.primaryLensFacing] !=
            newTransientSettings.zoomRatios[newTransientSettings.primaryLensFacing]
        ) {
            newTransientSettings.primaryLensFacing.let {
                camera.cameraControl.setZoomRatio(newTransientSettings.zoomRatios[it] ?: 1f)
            }
        }

        updateCamera2RequestOptions(
            camera,
            prevTransientSettings,
            newTransientSettings,
            camera2OptionsBuilder
        )

        prevTransientSettings = newTransientSettings
    }
}

@ExperimentalCamera2Interop
private fun updateCamera2RequestOptions(
    camera: Camera,
    prevTransientSettings: TransientSessionSettings?,
    newTransientSettings: TransientSessionSettings,
    optionsBuilder: CaptureRequestOptions.Builder
) {
    var needsUpdate = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
        prevTransientSettings?.flashMode != newTransientSettings.flashMode
    ) {
        when (newTransientSettings.flashMode) {
            FlashMode.LOW_LIGHT_BOOST -> {
                optionsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY
                )
            }
            else -> {
                optionsBuilder.clearCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE)
            }
        }
        needsUpdate = true
    }

    val newTestPattern = newTransientSettings.testPattern
    if (prevTransientSettings?.testPattern != newTestPattern) {
        val (mode: Int?, data: IntArray?) = when (newTestPattern) {
            TestPattern.Off -> Pair(null, null)
            TestPattern.ColorBars -> Pair(CameraMetadata.SENSOR_TEST_PATTERN_MODE_COLOR_BARS, null)
            TestPattern.ColorBarsFadeToGray -> Pair(
                CameraMetadata.SENSOR_TEST_PATTERN_MODE_COLOR_BARS_FADE_TO_GRAY,
                null
            )
            TestPattern.PN9 -> Pair(CameraMetadata.SENSOR_TEST_PATTERN_MODE_PN9, null)
            TestPattern.Custom1 -> Pair(CameraMetadata.SENSOR_TEST_PATTERN_MODE_CUSTOM1, null)
            is TestPattern.SolidColor -> {
                Pair(
                    CameraMetadata.SENSOR_TEST_PATTERN_MODE_SOLID_COLOR,
                    intArrayOf(
                        newTestPattern.red.toInt(),
                        newTestPattern.greenEven.toInt(),
                        newTestPattern.greenOdd.toInt(),
                        newTestPattern.blue.toInt()
                    )
                )
            }
        }
        if (mode != null) {
            optionsBuilder.setCaptureRequestOption(
                CaptureRequest.SENSOR_TEST_PATTERN_MODE,
                mode
            )
        } else {
            optionsBuilder.clearCaptureRequestOption(CaptureRequest.SENSOR_TEST_PATTERN_MODE)
        }

        if (data != null) {
            optionsBuilder.setCaptureRequestOption(
                CaptureRequest.SENSOR_TEST_PATTERN_DATA,
                data
            )
        } else {
            optionsBuilder.clearCaptureRequestOption(CaptureRequest.SENSOR_TEST_PATTERN_DATA)
        }
        needsUpdate = true
    }

    if (needsUpdate) {
        Camera2CameraControl.from(camera.cameraControl)
            .setCaptureRequestOptions(optionsBuilder.build())
    }
}

internal fun applyDeviceRotation(deviceRotation: DeviceRotation, useCaseGroup: UseCaseGroup) {
    val targetRotation = deviceRotation.toUiSurfaceRotation()
    useCaseGroup.useCases.forEach {
        when (it) {
            is Preview -> {
                // Preview's target rotation should not be updated with device rotation.
                // Instead, preview rotation should match the display rotation.
                // When Preview is created, it is initialized with the display rotation.
                // This will need to be updated separately if the display rotation is not
                // locked. Currently the app is locked to portrait orientation.
            }

            is ImageCapture -> {
                it.targetRotation = targetRotation
            }

            is VideoCapture<*> -> {
                it.targetRotation = targetRotation
            }
        }
    }
}

context(CameraSessionContext)
internal fun createUseCaseGroup(
    cameraInfo: CameraInfo,
    initialTransientSettings: TransientSessionSettings,
    stabilizationMode: StabilizationMode,
    aspectRatio: AspectRatio,
    videoCaptureUseCase: VideoCapture<Recorder>?,
    dynamicRange: DynamicRange,
    imageFormat: ImageOutputFormat,
    captureMode: CaptureMode,
    effect: CameraEffect? = null
): UseCaseGroup {
    val previewUseCase =
        createPreviewUseCase(
            cameraInfo,
            aspectRatio,
            stabilizationMode
        )

    // only create image use case in image or standard
    val imageCaptureUseCase = if (captureMode != CaptureMode.VIDEO_ONLY) {
        createImageUseCase(cameraInfo, aspectRatio, dynamicRange, imageFormat)
    } else {
        null
    }

    imageCaptureUseCase?.let {
        setFlashModeInternal(
            imageCapture = imageCaptureUseCase,
            flashMode = initialTransientSettings.flashMode,
            isFrontFacing = cameraInfo.appLensFacing == LensFacing.FRONT
        )
    }

    return UseCaseGroup.Builder().apply {
        Log.d(
            TAG,
            "Setting initial device rotation to ${initialTransientSettings.deviceRotation}"
        )
        setViewPort(
            ViewPort.Builder(
                aspectRatio.ratio,
                // Initialize rotation to Preview's rotation, which comes from Display rotation
                previewUseCase.targetRotation
            ).build()
        )
        addUseCase(previewUseCase)

        // image and video use cases are only created if supported by the configuration
        imageCaptureUseCase?.let { addUseCase(imageCaptureUseCase) }
        videoCaptureUseCase?.let { addUseCase(videoCaptureUseCase) }

        effect?.let { addEffect(it) }
    }.build()
}

private fun getVideoQualityFromResolution(resolution: Size?): VideoQuality =
    resolution?.let { res ->
        QUALITY_RANGE_MAP.firstNotNullOfOrNull {
            if (it.value.contains(res.height)) it.key else null
        }
    } ?: VideoQuality.UNSPECIFIED

private fun getWidthFromCropRect(cropRect: Rect?): Int {
    if (cropRect == null) {
        return 0
    }
    return abs(cropRect.top - cropRect.bottom)
}

private fun getHeightFromCropRect(cropRect: Rect?): Int {
    if (cropRect == null) {
        return 0
    }
    return abs(cropRect.left - cropRect.right)
}

private fun createImageUseCase(
    cameraInfo: CameraInfo,
    aspectRatio: AspectRatio,
    dynamicRange: DynamicRange,
    imageFormat: ImageOutputFormat
): ImageCapture {
    val builder = ImageCapture.Builder()
    builder.setResolutionSelector(
        getResolutionSelector(cameraInfo.sensorLandscapeRatio, aspectRatio)
    )
    if (dynamicRange != DynamicRange.SDR && imageFormat == ImageOutputFormat.JPEG_ULTRA_HDR
    ) {
        builder.setOutputFormat(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
    }
    return builder.build()
}

internal fun createVideoUseCase(
    cameraInfo: CameraInfo,
    aspectRatio: AspectRatio,
    targetFrameRate: Int,
    stabilizationMode: StabilizationMode,
    dynamicRange: DynamicRange,
    videoQuality: VideoQuality,
    backgroundDispatcher: CoroutineDispatcher
): VideoCapture<Recorder> {
    val sensorLandscapeRatio = cameraInfo.sensorLandscapeRatio
    val recorder = Recorder.Builder()
        .setAspectRatio(
            getAspectRatioForUseCase(sensorLandscapeRatio, aspectRatio)
        )
        .setExecutor(backgroundDispatcher.asExecutor())
        .apply {
            videoQuality.toQuality()?.let { quality ->
                // No fallback strategy is used. The app will crash if the quality is unsupported
                setQualitySelector(
                    QualitySelector.from(
                        quality,
                        FallbackStrategy.lowerQualityOrHigherThan(quality)
                    )
                )
            }
        }.build()

    return VideoCapture.Builder(recorder).apply {
        // set video stabilization
        if (stabilizationMode == StabilizationMode.HIGH_QUALITY) {
            setVideoStabilizationEnabled(true)
        }
        // set target fps
        if (targetFrameRate != TARGET_FPS_AUTO) {
            setTargetFrameRate(Range(targetFrameRate, targetFrameRate))
        }

        setDynamicRange(dynamicRange.toCXDynamicRange())
    }.build()
}

private fun getAspectRatioForUseCase(sensorLandscapeRatio: Float, aspectRatio: AspectRatio): Int =
    when (aspectRatio) {
        AspectRatio.THREE_FOUR -> androidx.camera.core.AspectRatio.RATIO_4_3
        AspectRatio.NINE_SIXTEEN -> androidx.camera.core.AspectRatio.RATIO_16_9
        else -> {
            // Choose the aspect ratio which maximizes FOV by being closest to the sensor ratio
            if (
                abs(sensorLandscapeRatio - AspectRatio.NINE_SIXTEEN.landscapeRatio.toFloat()) <
                abs(sensorLandscapeRatio - AspectRatio.THREE_FOUR.landscapeRatio.toFloat())
            ) {
                androidx.camera.core.AspectRatio.RATIO_16_9
            } else {
                androidx.camera.core.AspectRatio.RATIO_4_3
            }
        }
    }

context(CameraSessionContext)
private fun createPreviewUseCase(
    cameraInfo: CameraInfo,
    aspectRatio: AspectRatio,
    stabilizationMode: StabilizationMode
): Preview = Preview.Builder().apply {
    updateCameraStateWithCaptureResults(targetCameraInfo = cameraInfo)

    // set preview stabilization
    when (stabilizationMode) {
        StabilizationMode.ON -> setPreviewStabilizationEnabled(true)
        StabilizationMode.OPTICAL -> setOpticalStabilizationModeEnabled(true)
        StabilizationMode.OFF -> setOpticalStabilizationModeEnabled(false)
        StabilizationMode.HIGH_QUALITY -> {} // No-op. Handled by VideoCapture use case.
        else -> throw UnsupportedOperationException(
            "Unexpected stabilization mode: $stabilizationMode. Stabilization mode should always " +
                "an explicit mode, such as ON, OPTICAL, OFF or HIGH_QUALITY"
        )
    }

    setResolutionSelector(
        getResolutionSelector(cameraInfo.sensorLandscapeRatio, aspectRatio)
    )
}.build()
    .apply {
        setSurfaceProvider { surfaceRequest ->
            surfaceRequests.update { surfaceRequest }
        }
    }

@OptIn(ExperimentalCamera2Interop::class)
private fun Preview.Builder.setOpticalStabilizationModeEnabled(enabled: Boolean): Preview.Builder {
    Camera2Interop.Extender(this)
        .setCaptureRequestOption(
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
            if (enabled) {
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
            } else {
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF
            }
        )
    return this
}

private fun getResolutionSelector(
    sensorLandscapeRatio: Float,
    aspectRatio: AspectRatio
): ResolutionSelector {
    val aspectRatioStrategy = when (aspectRatio) {
        AspectRatio.THREE_FOUR -> AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
        AspectRatio.NINE_SIXTEEN -> AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
        else -> {
            // Choose the resolution selector strategy which maximizes FOV by being closest
            // to the sensor aspect ratio
            if (
                abs(sensorLandscapeRatio - AspectRatio.NINE_SIXTEEN.landscapeRatio.toFloat()) <
                abs(sensorLandscapeRatio - AspectRatio.THREE_FOUR.landscapeRatio.toFloat())
            ) {
                AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
            } else {
                AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
            }
        }
    }
    return ResolutionSelector.Builder().setAspectRatioStrategy(aspectRatioStrategy).build()
}

context(CameraSessionContext)
private fun setFlashModeInternal(
    imageCapture: ImageCapture,
    flashMode: FlashMode,
    isFrontFacing: Boolean
) {
    val isScreenFlashRequired =
        isFrontFacing && (flashMode == FlashMode.ON || flashMode == FlashMode.AUTO)

    if (isScreenFlashRequired) {
        imageCapture.screenFlash = object : ImageCapture.ScreenFlash {
            override fun apply(
                expirationTimeMillis: Long,
                listener: ImageCapture.ScreenFlashListener
            ) {
                Log.d(TAG, "ImageCapture.ScreenFlash: apply")
                screenFlashEvents.trySend(
                    CameraUseCase.ScreenFlashEvent(CameraUseCase.ScreenFlashEvent.Type.APPLY_UI) {
                        listener.onCompleted()
                    }
                )
            }

            override fun clear() {
                Log.d(TAG, "ImageCapture.ScreenFlash: clear")
                screenFlashEvents.trySend(
                    CameraUseCase.ScreenFlashEvent(CameraUseCase.ScreenFlashEvent.Type.CLEAR_UI) {}
                )
            }
        }
    }

    imageCapture.flashMode = when (flashMode) {
        FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF // 2

        FlashMode.ON -> if (isScreenFlashRequired) {
            ImageCapture.FLASH_MODE_SCREEN // 3
        } else {
            ImageCapture.FLASH_MODE_ON // 1
        }

        FlashMode.AUTO -> if (isScreenFlashRequired) {
            ImageCapture.FLASH_MODE_SCREEN // 3
        } else {
            ImageCapture.FLASH_MODE_AUTO // 0
        }

        FlashMode.LOW_LIGHT_BOOST -> ImageCapture.FLASH_MODE_OFF // 2
    }
    Log.d(TAG, "Set flash mode to: ${imageCapture.flashMode}")
}

private fun getPendingRecording(
    context: Context,
    videoCaptureUseCase: VideoCapture<Recorder>,
    maxDurationMillis: Long,
    captureTypeSuffix: String,
    saveLocation: SaveLocation,
    onVideoRecord: (CameraUseCase.OnVideoRecordEvent) -> Unit
): PendingRecording? {
    Log.d(TAG, "getPendingRecording")
    return when (saveLocation) {
        is SaveLocation.Explicit ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    videoCaptureUseCase.output.prepareRecording(
                        context,
                        FileDescriptorOutputOptions.Builder(
                            context.applicationContext.contentResolver.openFileDescriptor(
                                saveLocation.locationUri,
                                "rw"
                            )!!
                        ).build()
                    )
                } catch (e: Exception) {
                    onVideoRecord(
                        CameraUseCase.OnVideoRecordEvent.OnVideoRecordError(e)
                    )
                    null
                }
            } else {
                if (saveLocation.locationUri.scheme == "file") {
                    val fileOutputOptions = FileOutputOptions.Builder(
                        File(saveLocation.locationUri.path!!)
                    ).build()
                    videoCaptureUseCase.output.prepareRecording(context, fileOutputOptions)
                } else {
                    onVideoRecord(
                        CameraUseCase.OnVideoRecordEvent.OnVideoRecordError(
                            RuntimeException("Uri scheme not supported.")
                        )
                    )
                    null
                }
            }

        is SaveLocation.Default -> {
            val name = "JCA-recording-${Date()}-$captureTypeSuffix.mp4"
            val contentValues =
                ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, name)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+
                        put(MediaStore.Video.Media.RELATIVE_PATH, getDefaultMediaSaveLocation())
                    }
                }
            val mediaStoreOutput =
                MediaStoreOutputOptions.Builder(
                    context.contentResolver,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                )
                    .setDurationLimitMillis(maxDurationMillis)
                    .setContentValues(contentValues)
                    .build()
            videoCaptureUseCase.output.prepareRecording(context, mediaStoreOutput)
        }
    }
}

context(CameraSessionContext)
private suspend fun startVideoRecordingInternal(
    isInitialAudioEnabled: Boolean,
    context: Context,
    pendingRecord: PendingRecording,
    maxDurationMillis: Long,
    initialRecordingSettings: InitialRecordingSettings,
    onVideoRecord: (CameraUseCase.OnVideoRecordEvent) -> Unit
): Recording {
    // set the camerastate to starting
    currentCameraState.update { old ->
        old.copy(videoRecordingState = VideoRecordingState.Starting(initialRecordingSettings))
    }

    // ok. there is a difference between MUTING and ENABLING audio
    // audio must be enabled in order to be muted
    // if the video recording isn't started with audio enabled, you will not be able to un-mute it
    // the toggle should only affect whether or not the audio is muted.
    // the permission will determine whether or not the audio is enabled.
    val isAudioGranted = checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    pendingRecord.apply {
        if (isAudioGranted) {
            withAudioEnabled(isInitialAudioEnabled)
        }
    }
        .asPersistentRecording()

    val callbackExecutor: Executor =
        (
            currentCoroutineContext()[ContinuationInterceptor] as?
                CoroutineDispatcher
            )?.asExecutor() ?: ContextCompat.getMainExecutor(context)
    return pendingRecord.start(callbackExecutor) { onVideoRecordEvent ->
        Log.d(TAG, onVideoRecordEvent.toString())
        when (onVideoRecordEvent) {
            is VideoRecordEvent.Start -> {
                currentCameraState.update { old ->
                    old.copy(
                        videoRecordingState = VideoRecordingState.Active.Recording(
                            audioAmplitude = onVideoRecordEvent.recordingStats.audioStats
                                .audioAmplitude,
                            maxDurationMillis = maxDurationMillis,
                            elapsedTimeNanos = onVideoRecordEvent.recordingStats
                                .recordedDurationNanos
                        )
                    )
                }
            }

            is VideoRecordEvent.Pause -> {
                currentCameraState.update { old ->
                    old.copy(
                        videoRecordingState = VideoRecordingState.Active.Paused(
                            audioAmplitude = onVideoRecordEvent.recordingStats.audioStats
                                .audioAmplitude,
                            maxDurationMillis = maxDurationMillis,
                            elapsedTimeNanos = onVideoRecordEvent.recordingStats
                                .recordedDurationNanos
                        )
                    )
                }
            }

            is VideoRecordEvent.Resume -> {
                currentCameraState.update { old ->
                    old.copy(
                        videoRecordingState = VideoRecordingState.Active.Recording(
                            audioAmplitude = onVideoRecordEvent.recordingStats.audioStats
                                .audioAmplitude,
                            maxDurationMillis = maxDurationMillis,
                            elapsedTimeNanos = onVideoRecordEvent.recordingStats
                                .recordedDurationNanos
                        )
                    )
                }
            }

            is VideoRecordEvent.Status -> {
                currentCameraState.update { old ->
                    // don't want to change state from paused to recording if status changes while paused
                    if (old.videoRecordingState is VideoRecordingState.Active.Paused) {
                        old.copy(
                            videoRecordingState = VideoRecordingState.Active.Paused(
                                audioAmplitude = onVideoRecordEvent.recordingStats.audioStats
                                    .audioAmplitude,
                                maxDurationMillis = maxDurationMillis,
                                elapsedTimeNanos = onVideoRecordEvent.recordingStats
                                    .recordedDurationNanos
                            )
                        )
                    } else {
                        old.copy(
                            videoRecordingState = VideoRecordingState.Active.Recording(
                                audioAmplitude = onVideoRecordEvent.recordingStats.audioStats
                                    .audioAmplitude,
                                maxDurationMillis = maxDurationMillis,
                                elapsedTimeNanos = onVideoRecordEvent.recordingStats
                                    .recordedDurationNanos
                            )
                        )
                    }
                }
            }

            is VideoRecordEvent.Finalize -> {
                when (onVideoRecordEvent.error) {
                    ERROR_NONE -> {
                        // update recording state to inactive with the final values of the recording.
                        currentCameraState.update { old ->
                            old.copy(
                                videoRecordingState = VideoRecordingState.Inactive(
                                    finalElapsedTimeNanos = onVideoRecordEvent.recordingStats
                                        .recordedDurationNanos
                                )
                            )
                        }
                        onVideoRecord(
                            CameraUseCase.OnVideoRecordEvent.OnVideoRecorded(
                                onVideoRecordEvent.outputResults.outputUri
                            )
                        )
                    }

                    ERROR_DURATION_LIMIT_REACHED -> {
                        currentCameraState.update { old ->
                            old.copy(
                                videoRecordingState = VideoRecordingState.Inactive(
                                    finalElapsedTimeNanos = maxDurationMillis.milliseconds
                                        .inWholeNanoseconds
                                )
                            )
                        }

                        onVideoRecord(
                            CameraUseCase.OnVideoRecordEvent.OnVideoRecorded(
                                onVideoRecordEvent.outputResults.outputUri
                            )
                        )
                    }

                    else -> {
                        onVideoRecord(
                            CameraUseCase.OnVideoRecordEvent.OnVideoRecordError(
                                RuntimeException(
                                    "Recording finished with error: ${onVideoRecordEvent.error}",
                                    onVideoRecordEvent.cause
                                )
                            )
                        )
                        currentCameraState.update { old ->
                            old.copy(
                                videoRecordingState = VideoRecordingState.Inactive(
                                    finalElapsedTimeNanos = onVideoRecordEvent.recordingStats
                                        .recordedDurationNanos
                                )
                            )
                        }
                    }
                }
            }
        }
    }.apply {
        mute(!isInitialAudioEnabled)
    }
}

context(CameraSessionContext)
private suspend fun runVideoRecording(
    videoCapture: VideoCapture<Recorder>,
    captureTypeSuffix: String,
    context: Context,
    maxDurationMillis: Long,
    transientSettings: StateFlow<TransientSessionSettings?>,
    saveLocation: SaveLocation,
    videoControlEvents: Channel<VideoCaptureControlEvent>,
    onVideoRecord: (CameraUseCase.OnVideoRecordEvent) -> Unit
) = coroutineScope {
    var currentSettings = transientSettings.filterNotNull().first()

    getPendingRecording(
        context,
        videoCapture,
        maxDurationMillis,
        captureTypeSuffix,
        saveLocation,
        onVideoRecord
    )?.let {
        startVideoRecordingInternal(
            isInitialAudioEnabled = currentSettings.isAudioEnabled,
            context = context,
            pendingRecord = it,
            maxDurationMillis = maxDurationMillis,
            onVideoRecord = onVideoRecord,
            initialRecordingSettings = InitialRecordingSettings(
                isAudioEnabled = currentSettings.isAudioEnabled,
                lensFacing = currentSettings.primaryLensFacing,
                zoomRatios = currentSettings.zoomRatios
            )
        ).use { recording ->
            val recordingSettingsUpdater = launch {
                fun TransientSessionSettings.isFlashModeOn() = flashMode == FlashMode.ON

                transientSettings.filterNotNull()
                    .collectLatest { newTransientSettings ->
                        if (currentSettings.isAudioEnabled != newTransientSettings.isAudioEnabled) {
                            recording.mute(newTransientSettings.isAudioEnabled)
                        }
                        if (currentSettings.isFlashModeOn() !=
                            newTransientSettings.isFlashModeOn()
                        ) {
                            currentSettings = newTransientSettings
                        }
                    }
            }

            for (event in videoControlEvents) {
                when (event) {
                    is VideoCaptureControlEvent.StartRecordingEvent ->
                        throw IllegalStateException("A recording is already in progress")

                    VideoCaptureControlEvent.StopRecordingEvent -> {
                        recordingSettingsUpdater.cancel()
                        break
                    }

                    VideoCaptureControlEvent.PauseRecordingEvent -> recording.pause()
                    VideoCaptureControlEvent.ResumeRecordingEvent -> recording.resume()
                }
            }
        }
    }
}

context(CameraSessionContext)
internal suspend fun processFocusMeteringEvents(cameraControl: CameraControl) {
    surfaceRequests.map { surfaceRequest ->
        surfaceRequest?.resolution?.run {
            Log.d(
                TAG,
                "Waiting to process focus points for surface with resolution: " +
                    "$width x $height"
            )
            SurfaceOrientedMeteringPointFactory(width.toFloat(), height.toFloat())
        }
    }.collectLatest { meteringPointFactory ->
        for (event in focusMeteringEvents) {
            meteringPointFactory?.apply {
                Log.d(TAG, "tapToFocus, processing event: $event")
                val meteringPoint = createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(meteringPoint).build()
                cameraControl.startFocusAndMetering(action)
            } ?: run {
                Log.w(TAG, "Ignoring event due to no SurfaceRequest: $event")
            }
        }
    }
}

context(CameraSessionContext)
internal suspend fun processVideoControlEvents(
    videoCapture: VideoCapture<Recorder>?,
    captureTypeSuffix: String
) = coroutineScope {
    for (event in videoCaptureControlEvents) {
        when (event) {
            is VideoCaptureControlEvent.StartRecordingEvent -> {
                if (videoCapture == null) {
                    throw RuntimeException(
                        "Attempted video recording with null videoCapture"
                    )
                }
                runVideoRecording(
                    videoCapture,
                    captureTypeSuffix,
                    context,
                    event.maxVideoDuration,
                    transientSettings,
                    event.saveLocation,
                    videoCaptureControlEvents,
                    event.onVideoRecord
                )
            }

            else -> {}
        }
    }
}

/**
 * Applies a CaptureCallback to the provided image capture builder
 */
context(CameraSessionContext)
@OptIn(ExperimentalCamera2Interop::class)
private fun Preview.Builder.updateCameraStateWithCaptureResults(
    targetCameraInfo: CameraInfo
): Preview.Builder {
    val isFirstFrameTimestampUpdated = atomic(false)
    val targetCameraLogicalId = Camera2CameraInfo.from(targetCameraInfo).cameraId
    Camera2Interop.Extender(this).setSessionCaptureCallback(
        object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                super.onCaptureCompleted(session, request, result)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    val nativeBoostState = result.get(CaptureResult.CONTROL_LOW_LIGHT_BOOST_STATE)
                    val boostState = when (nativeBoostState) {
                        CameraMetadata.CONTROL_LOW_LIGHT_BOOST_STATE_ACTIVE ->
                            LowLightBoostState.ACTIVE
                        else -> LowLightBoostState.INACTIVE
                    }
                    currentCameraState.update { old ->
                        if (old.lowLightBoostState != boostState) {
                            old.copy(lowLightBoostState = boostState)
                        } else {
                            old
                        }
                    }
                }
                val logicalCameraId = session.device.id

                // todo(b/405987189): remove completely after buggy zoomState is fixed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    logicalCameraId == targetCameraLogicalId
                ) {
                    // update camerastate with zoom ratio
                    val newZoomRatio = result.get(CaptureResult.CONTROL_ZOOM_RATIO)
                    currentCameraState.update { old ->
                        if (newZoomRatio != null &&
                            old.zoomRatios[targetCameraInfo.appLensFacing] != newZoomRatio
                        ) {
                            Log.d(
                                TAG,
                                "newZoomRatio: $newZoomRatio on lens ${targetCameraInfo.appLensFacing}"
                            )

                            old.copy(
                                zoomRatios = old.zoomRatios
                                    .toMutableMap()
                                    .apply {
                                        put(targetCameraInfo.appLensFacing, newZoomRatio)
                                    }.toMap()
                            )
                        } else {
                            old
                        }
                    }
                }

                if (logicalCameraId != targetCameraLogicalId) return
                try {
                    val physicalCameraId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        result.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID)
                    } else {
                        null
                    }
                    currentCameraState.update { old ->
                        if (old.debugInfo.logicalCameraId != logicalCameraId ||
                            old.debugInfo.physicalCameraId != physicalCameraId
                        ) {
                            old.copy(
                                debugInfo = DebugInfo(logicalCameraId, physicalCameraId)
                            )
                        } else {
                            old
                        }
                    }
                    if (!isFirstFrameTimestampUpdated.value) {
                        currentCameraState.update { old ->
                            old.copy(
                                sessionFirstFrameTimestamp = SystemClock.elapsedRealtimeNanos()
                            )
                        }
                        isFirstFrameTimestampUpdated.value = true
                    }
                    // Publish stabilization state
                    publishStabilizationMode(result)
                } catch (_: Exception) {
                }
            }
        }
    )
    return this
}

context(CameraSessionContext)
private fun publishStabilizationMode(result: TotalCaptureResult) {
    val nativeVideoStabilizationMode = result.get(CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE)
    val stabilizationMode = when (nativeVideoStabilizationMode) {
        CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION ->
            StabilizationMode.ON

        CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE_ON -> StabilizationMode.HIGH_QUALITY
        else -> {
            result.get(CaptureResult.LENS_OPTICAL_STABILIZATION_MODE)?.let {
                if (it == CaptureResult.LENS_OPTICAL_STABILIZATION_MODE_ON) {
                    StabilizationMode.OPTICAL
                } else {
                    StabilizationMode.OFF
                }
            } ?: StabilizationMode.OFF
        }
    }

    currentCameraState.update { old ->
        if (old.stabilizationMode != stabilizationMode) {
            old.copy(stabilizationMode = stabilizationMode)
        } else {
            old
        }
    }
}
