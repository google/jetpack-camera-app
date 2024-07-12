/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.google.jetpackcamera.feature.preview

import android.content.ContentResolver
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.SurfaceRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tracing.Trace
import androidx.tracing.traceAsync
import com.google.jetpackcamera.core.camera.CameraUseCase
import com.google.jetpackcamera.core.common.traceFirstFramePreview
import com.google.jetpackcamera.feature.preview.ui.IMAGE_CAPTURE_FAILURE_TAG
import com.google.jetpackcamera.feature.preview.ui.IMAGE_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.feature.preview.ui.SnackbarData
import com.google.jetpackcamera.feature.preview.ui.VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
import com.google.jetpackcamera.settings.ConstraintsRepository
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraConstraints
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.DeviceRotation
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.LowLightBoost
import com.google.jetpackcamera.settings.model.SystemConstraints
import com.google.jetpackcamera.settings.model.forCurrentLens
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.time.Duration.Companion.seconds
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "PreviewViewModel"
private const val IMAGE_CAPTURE_TRACE = "JCA Image Capture"

/**
 * [ViewModel] for [PreviewScreen].
 */
@HiltViewModel(assistedFactory = PreviewViewModel.Factory::class)
class PreviewViewModel @AssistedInject constructor(
    @Assisted val previewMode: PreviewMode,
    private val cameraUseCase: CameraUseCase,
    private val constraintsRepository: ConstraintsRepository
) : ViewModel() {
    private val _previewUiState: MutableStateFlow<PreviewUiState> =
        MutableStateFlow(PreviewUiState.NotReady)

    val previewUiState: StateFlow<PreviewUiState> =
        _previewUiState.asStateFlow()

    val surfaceRequest: StateFlow<SurfaceRequest?> = cameraUseCase.getSurfaceRequest()

    private var runningCameraJob: Job? = null

    private var recordingJob: Job? = null

    val screenFlash = ScreenFlash(cameraUseCase, viewModelScope)

    private val snackBarCount = atomic(0)
    private val videoCaptureStartedCount = atomic(0)

    // Eagerly initialize the CameraUseCase and encapsulate in a Deferred that can be
    // used to ensure we don't start the camera before initialization is complete.
    private var initializationDeferred: Deferred<Unit> = viewModelScope.async {
        cameraUseCase.initialize(previewMode is PreviewMode.ExternalImageCaptureMode)
    }

    init {
        viewModelScope.launch {
            combine(
                cameraUseCase.getCurrentSettings().filterNotNull(),
                constraintsRepository.systemConstraints.filterNotNull(),
                cameraUseCase.getCurrentCameraState()
            ) { cameraAppSettings, systemConstraints, cameraState ->
                _previewUiState.update { old ->
                    when (old) {
                        is PreviewUiState.Ready ->
                            old.copy(
                                currentCameraSettings = cameraAppSettings,
                                systemConstraints = systemConstraints,
                                zoomScale = cameraState.zoomScale,
                                sessionFirstFrameTimestamp = cameraState.sessionFirstFrameTimestamp,
                                captureModeToggleUiState = getCaptureToggleUiState(
                                    systemConstraints,
                                    cameraAppSettings
                                )
                            )

                        is PreviewUiState.NotReady ->
                            PreviewUiState.Ready(
                                currentCameraSettings = cameraAppSettings,
                                systemConstraints = systemConstraints,
                                zoomScale = cameraState.zoomScale,
                                sessionFirstFrameTimestamp = cameraState.sessionFirstFrameTimestamp,
                                previewMode = previewMode,
                                captureModeToggleUiState = getCaptureToggleUiState(
                                    systemConstraints,
                                    cameraAppSettings
                                )
                            )
                    }
                }
            }.collect {}
        }
    }

    private fun getCaptureToggleUiState(
        systemConstraints: SystemConstraints,
        cameraAppSettings: CameraAppSettings
    ): CaptureModeToggleUiState {
        val cameraConstraints: CameraConstraints? = systemConstraints.forCurrentLens(
            cameraAppSettings
        )
        val hdrDynamicRangeSupported = cameraConstraints?.let {
            it.supportedDynamicRanges.size > 1
        } ?: false
        val hdrImageFormatSupported =
            cameraConstraints?.supportedImageFormatsMap?.get(cameraAppSettings.captureMode)?.let {
                it.size > 1
            } ?: false
        val isShown = previewMode is PreviewMode.ExternalImageCaptureMode ||
            cameraAppSettings.imageFormat == ImageOutputFormat.JPEG_ULTRA_HDR ||
            cameraAppSettings.dynamicRange == DynamicRange.HLG10
        val enabled = previewMode !is PreviewMode.ExternalImageCaptureMode &&
            hdrDynamicRangeSupported && hdrImageFormatSupported
        return if (isShown) {
            val currentMode = if (previewMode is PreviewMode.ExternalImageCaptureMode ||
                cameraAppSettings.imageFormat == ImageOutputFormat.JPEG_ULTRA_HDR
            ) {
                CaptureModeToggleUiState.ToggleMode.CAPTURE_TOGGLE_IMAGE
            } else {
                CaptureModeToggleUiState.ToggleMode.CAPTURE_TOGGLE_VIDEO
            }
            if (enabled) {
                CaptureModeToggleUiState.Enabled(currentMode)
            } else {
                CaptureModeToggleUiState.Disabled(
                    currentMode,
                    getCaptureToggleUiStateDisabledReason(
                        hdrDynamicRangeSupported,
                        hdrImageFormatSupported,
                        systemConstraints,
                        cameraAppSettings.cameraLensFacing,
                        cameraAppSettings.captureMode
                    )
                )
            }
        } else {
            CaptureModeToggleUiState.Invisible
        }
    }

    private fun getCaptureToggleUiStateDisabledReason(
        hdrDynamicRangeSupported: Boolean,
        hdrImageFormatSupported: Boolean,
        systemConstraints: SystemConstraints,
        currentLensFacing: LensFacing,
        currentCaptureMode: CaptureMode
    ): CaptureModeToggleUiState.DisabledReason {
        if (previewMode is PreviewMode.ExternalImageCaptureMode) {
            return CaptureModeToggleUiState.DisabledReason.VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED
        }
        if (!hdrImageFormatSupported) {
            // First assume HDR image is only unsupported on this capture mode
            var disabledReason = when (currentCaptureMode) {
                CaptureMode.MULTI_STREAM ->
                    CaptureModeToggleUiState.DisabledReason.HDR_IMAGE_UNSUPPORTED_ON_MULTI_STREAM
                CaptureMode.SINGLE_STREAM ->
                    CaptureModeToggleUiState.DisabledReason.HDR_IMAGE_UNSUPPORTED_ON_SINGLE_STREAM
            }
            // Check if other capture modes supports HDR image on this lens
            systemConstraints
                .perLensConstraints[currentLensFacing]
                ?.supportedImageFormatsMap
                ?.filterKeys { it != currentCaptureMode }
                ?.values
                ?.forEach { supportedFormats ->
                    if (supportedFormats.size > 1) {
                        // Found another capture mode that supports HDR image,
                        // return previously discovered disabledReason
                        return disabledReason
                    }
                }
            // HDR image is not supported by this lens
            disabledReason = CaptureModeToggleUiState.DisabledReason.HDR_IMAGE_UNSUPPORTED_ON_LENS
            // Check if any other lens supports HDR image
            systemConstraints
                .perLensConstraints
                .filterKeys { it != currentLensFacing }
                .values
                .forEach { constraints ->
                    constraints.supportedImageFormatsMap.values.forEach { supportedFormats ->
                        if (supportedFormats.size > 1) {
                            // Found another lens that supports HDR image,
                            // return previously discovered disabledReason
                            return disabledReason
                        }
                    }
                }
            // No lenses support HDR image on device
            return CaptureModeToggleUiState.DisabledReason.HDR_IMAGE_UNSUPPORTED_ON_DEVICE
        } else if (!hdrDynamicRangeSupported) {
            systemConstraints.perLensConstraints.forEach { entry ->
                if (entry.key != currentLensFacing) {
                    val cameraConstraints = systemConstraints.perLensConstraints[entry.key]
                    if (cameraConstraints?.let { it.supportedDynamicRanges.size > 1 } == true) {
                        return CaptureModeToggleUiState.DisabledReason.HDR_VIDEO_UNSUPPORTED_ON_LENS
                    }
                }
            }
            return CaptureModeToggleUiState.DisabledReason.HDR_VIDEO_UNSUPPORTED_ON_DEVICE
        } else {
            throw RuntimeException("Unknown CaptureModeUnsupportedReason.")
        }
    }

    fun startCamera() {
        Log.d(TAG, "startCamera")
        stopCamera()
        runningCameraJob = viewModelScope.launch {
            if (Trace.isEnabled()) {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    val startTraceTimestamp: Long = SystemClock.elapsedRealtimeNanos()
                    traceFirstFramePreview(cookie = 1) {
                        _previewUiState.transformWhile {
                            var continueCollecting = true
                            (it as? PreviewUiState.Ready)?.let { uiState ->
                                if (uiState.sessionFirstFrameTimestamp > startTraceTimestamp) {
                                    emit(Unit)
                                    continueCollecting = false
                                }
                            }
                            continueCollecting
                        }.collect {}
                    }
                }
            }
            // Ensure CameraUseCase is initialized before starting camera
            initializationDeferred.await()
            // TODO(yasith): Handle Exceptions from binding use cases
            cameraUseCase.runCamera()
        }
    }

    fun stopCamera() {
        Log.d(TAG, "stopCamera")
        runningCameraJob?.apply {
            if (isActive) {
                cancel()
            }
        }
    }

    fun setFlash(flashMode: FlashMode) {
        viewModelScope.launch {
            // apply to cameraUseCase
            cameraUseCase.setFlashMode(flashMode)
        }
    }

    fun setAspectRatio(aspectRatio: AspectRatio) {
        viewModelScope.launch {
            cameraUseCase.setAspectRatio(aspectRatio)
        }
    }

    fun setCaptureMode(captureMode: CaptureMode) {
        viewModelScope.launch {
            cameraUseCase.setCaptureMode(captureMode)
        }
    }

    /** Sets the camera to a designated lens facing */
    fun setLensFacing(newLensFacing: LensFacing) {
        viewModelScope.launch {
            // apply to cameraUseCase
            cameraUseCase.setLensFacing(newLensFacing)
        }
    }

    fun setAudioMuted(shouldMuteAudio: Boolean) {
        viewModelScope.launch {
            cameraUseCase.setAudioMuted(shouldMuteAudio)
        }

        Log.d(
            TAG,
            "Toggle Audio ${
                (previewUiState.value as PreviewUiState.Ready)
                    .currentCameraSettings.audioMuted
            }"
        )
    }

    fun captureImage() {
        Log.d(TAG, "captureImage")
        viewModelScope.launch {
            captureImageInternal(
                doTakePicture = {
                    cameraUseCase.takePicture {
                        _previewUiState.update { old ->
                            (old as? PreviewUiState.Ready)?.copy(
                                lastBlinkTimeStamp = System.currentTimeMillis()
                            ) ?: old
                        }
                    }
                }
            )
        }
    }

    fun captureImageWithUri(
        contentResolver: ContentResolver,
        imageCaptureUri: Uri?,
        ignoreUri: Boolean = false,
        onImageCapture: (ImageCaptureEvent) -> Unit
    ) {
        Log.d(TAG, "captureImageWithUri")
        viewModelScope.launch {
            captureImageInternal(
                doTakePicture = {
                    cameraUseCase.takePicture({
                        _previewUiState.update { old ->
                            (old as? PreviewUiState.Ready)?.copy(
                                lastBlinkTimeStamp = System.currentTimeMillis()
                            ) ?: old
                        }
                    }, contentResolver, imageCaptureUri, ignoreUri).savedUri
                },
                onSuccess = { savedUri -> onImageCapture(ImageCaptureEvent.ImageSaved(savedUri)) },
                onFailure = { exception ->
                    onImageCapture(ImageCaptureEvent.ImageCaptureError(exception))
                }
            )
        }
    }

    private suspend fun <T> captureImageInternal(
        doTakePicture: suspend () -> T,
        onSuccess: (T) -> Unit = {},
        onFailure: (exception: Exception) -> Unit = {}
    ) {
        val cookieInt = snackBarCount.incrementAndGet()
        val cookie = "Image-$cookieInt"
        try {
            traceAsync(IMAGE_CAPTURE_TRACE, cookieInt) {
                doTakePicture()
            }.also { result ->
                onSuccess(result)
            }
            Log.d(TAG, "cameraUseCase.takePicture success")
            SnackbarData(
                cookie = cookie,
                stringResource = R.string.toast_image_capture_success,
                withDismissAction = true,
                testTag = IMAGE_CAPTURE_SUCCESS_TAG
            )
        } catch (exception: Exception) {
            onFailure(exception)
            Log.d(TAG, "cameraUseCase.takePicture error", exception)
            SnackbarData(
                cookie = cookie,
                stringResource = R.string.toast_capture_failure,
                withDismissAction = true,
                testTag = IMAGE_CAPTURE_FAILURE_TAG
            )
        }.also { snackBarData ->
            _previewUiState.update { old ->
                (old as? PreviewUiState.Ready)?.copy(
                    // todo: remove snackBar after postcapture screen implemented
                    snackBarToShow = snackBarData
                ) ?: old
            }
        }
    }

    fun showSnackBarForDisabledHdrToggle(disabledReason: CaptureModeToggleUiState.DisabledReason) {
        val cookieInt = snackBarCount.incrementAndGet()
        val cookie = "DisabledHdrToggle-$cookieInt"
        viewModelScope.launch {
            _previewUiState.update { old ->
                (old as? PreviewUiState.Ready)?.copy(
                    snackBarToShow = SnackbarData(
                        cookie = cookie,
                        stringResource = disabledReason.reasonTextResId,
                        withDismissAction = true,
                        testTag = disabledReason.testTag
                    )
                ) ?: old
            }
        }
    }

    fun startVideoRecording() {
        if (previewUiState.value is PreviewUiState.Ready &&
            (previewUiState.value as PreviewUiState.Ready).previewMode is
                PreviewMode.ExternalImageCaptureMode
        ) {
            Log.d(TAG, "externalVideoRecording")
            viewModelScope.launch {
                _previewUiState.update { old ->
                    (old as? PreviewUiState.Ready)?.copy(
                        snackBarToShow = SnackbarData(
                            cookie = "Video-ExternalImageCaptureMode",
                            stringResource = R.string.toast_video_capture_external_unsupported,
                            withDismissAction = true,
                            testTag = VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
                        )
                    ) ?: old
                }
            }
            return
        }
        Log.d(TAG, "startVideoRecording")
        recordingJob = viewModelScope.launch {
            val cookie = "Video-${videoCaptureStartedCount.incrementAndGet()}"
            try {
                cameraUseCase.startVideoRecording {
                    var audioAmplitude = 0.0
                    var snackbarToShow: SnackbarData? = null
                    when (it) {
                        CameraUseCase.OnVideoRecordEvent.OnVideoRecorded -> {
                            snackbarToShow = SnackbarData(
                                cookie = cookie,
                                stringResource = R.string.toast_video_capture_success,
                                withDismissAction = true
                            )
                        }

                        CameraUseCase.OnVideoRecordEvent.OnVideoRecordError -> {
                            snackbarToShow = SnackbarData(
                                cookie = cookie,
                                stringResource = R.string.toast_video_capture_failure,
                                withDismissAction = true
                            )
                        }

                        is CameraUseCase.OnVideoRecordEvent.OnVideoRecordStatus -> {
                            audioAmplitude = it.audioAmplitude
                        }
                    }

                    viewModelScope.launch {
                        _previewUiState.update { old ->
                            (old as? PreviewUiState.Ready)?.copy(
                                snackBarToShow = snackbarToShow,
                                audioAmplitude = audioAmplitude
                            ) ?: old
                        }
                    }
                }
                _previewUiState.update { old ->
                    (old as? PreviewUiState.Ready)?.copy(
                        videoRecordingState = VideoRecordingState.ACTIVE
                    ) ?: old
                }
                Log.d(TAG, "cameraUseCase.startRecording success")
            } catch (exception: IllegalStateException) {
                Log.d(TAG, "cameraUseCase.startVideoRecording error", exception)
            }
        }
    }

    fun stopVideoRecording() {
        Log.d(TAG, "stopVideoRecording")
        viewModelScope.launch {
            _previewUiState.update { old ->
                (old as? PreviewUiState.Ready)?.copy(
                    videoRecordingState = VideoRecordingState.INACTIVE
                ) ?: old
            }
        }
        cameraUseCase.stopVideoRecording()
        recordingJob?.cancel()
    }

    fun setZoomScale(scale: Float) {
        cameraUseCase.setZoomScale(scale = scale)
    }

    fun setDynamicRange(dynamicRange: DynamicRange) {
        viewModelScope.launch {
            cameraUseCase.setDynamicRange(dynamicRange)
        }
    }

    fun setLowLightBoost(lowLightBoost: LowLightBoost) {
        viewModelScope.launch {
            cameraUseCase.setLowLightBoost(lowLightBoost)
        }
    }

    fun setImageFormat(imageFormat: ImageOutputFormat) {
        viewModelScope.launch {
            cameraUseCase.setImageFormat(imageFormat)
        }
    }

    // modify ui values
    fun toggleQuickSettings() {
        viewModelScope.launch {
            _previewUiState.update { old ->
                (old as? PreviewUiState.Ready)?.copy(
                    quickSettingsIsOpen = !old.quickSettingsIsOpen
                ) ?: old
            }
        }
    }

    fun tapToFocus(x: Float, y: Float) {
        Log.d(TAG, "tapToFocus")
        viewModelScope.launch {
            cameraUseCase.tapToFocus(x, y)
        }
    }

    /**
     * Sets current value of [PreviewUiState.Ready.toastMessageToShow] to null.
     */
    fun onToastShown() {
        viewModelScope.launch {
            // keeps the composable up on screen longer to be detected by UiAutomator
            delay(2.seconds)
            _previewUiState.update { old ->
                (old as? PreviewUiState.Ready)?.copy(
                    toastMessageToShow = null
                ) ?: old
            }
        }
    }

    fun onSnackBarResult(cookie: String) {
        viewModelScope.launch {
            _previewUiState.update { old ->
                (old as? PreviewUiState.Ready)?.snackBarToShow?.let {
                    if (it.cookie == cookie) {
                        // If the latest snackbar had a result, then clear snackBarToShow
                        old.copy(snackBarToShow = null)
                    } else {
                        old
                    }
                } ?: old
            }
        }
    }

    fun setDisplayRotation(deviceRotation: DeviceRotation) {
        viewModelScope.launch {
            cameraUseCase.setDeviceRotation(deviceRotation)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(previewMode: PreviewMode): PreviewViewModel
    }

    sealed interface ImageCaptureEvent {
        data class ImageSaved(
            val savedUri: Uri? = null
        ) : ImageCaptureEvent

        data class ImageCaptureError(
            val exception: Exception
        ) : ImageCaptureEvent
    }
}
