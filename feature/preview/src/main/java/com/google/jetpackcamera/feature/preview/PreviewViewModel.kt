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
import android.util.Log
import android.view.Display
import androidx.camera.core.SurfaceRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tracing.traceAsync
import com.google.jetpackcamera.domain.camera.CameraUseCase
import com.google.jetpackcamera.feature.preview.ui.ToastMessage
import com.google.jetpackcamera.settings.SettingsRepository
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.FlashMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val TAG = "PreviewViewModel"
private const val IMAGE_CAPTURE_TRACE = "JCA Image Capture"

// toast test descriptions
const val IMAGE_CAPTURE_SUCCESS_TOAST_TAG = "ImageCaptureSuccessToast"
const val IMAGE_CAPTURE_FAIL_TOAST_TAG = "ImageCaptureFailureToast"

/**
 * [ViewModel] for [PreviewScreen].
 */
@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val cameraUseCase: CameraUseCase,
    private val settingsRepository: SettingsRepository
    // only reads from settingsRepository. do not push changes to repository from here
) : ViewModel() {
    private val _previewUiState: MutableStateFlow<PreviewUiState> =
        MutableStateFlow(PreviewUiState(currentCameraSettings = DEFAULT_CAMERA_APP_SETTINGS))

    val previewUiState: StateFlow<PreviewUiState> = _previewUiState

    val surfaceRequest: StateFlow<SurfaceRequest?> = cameraUseCase.getSurfaceRequest()

    private var runningCameraJob: Job? = null

    private var recordingJob: Job? = null

    val screenFlash = ScreenFlash(cameraUseCase, viewModelScope)

    // Eagerly initialize the CameraUseCase and encapsulate in a Deferred that can be
    // used to ensure we don't start the camera before initialization is complete.
    private var initializationDeferred: Deferred<Unit> = viewModelScope.async {
        cameraUseCase.initialize(previewUiState.value.currentCameraSettings)
        _previewUiState.emit(
            previewUiState.value.copy(
                cameraState = CameraState.READY
            )
        )
    }

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.cameraAppSettings,
                cameraUseCase.getZoomScale()
            ) { cameraAppSettings, zoomScale ->
                previewUiState.value.copy(
                    currentCameraSettings = cameraAppSettings,
                    zoomScale = zoomScale
                )
            }.collect {
                // TODO: only update settings that were actually changed
                // currently resets all "quick" settings to stored settings
                Log.d(TAG, "UPDATE UI STATE: ${it.zoomScale}")
                _previewUiState.emit(it)
            }
        }
    }

    fun startCamera() {
        Log.d(TAG, "startCamera")
        stopCamera()
        runningCameraJob = viewModelScope.launch {
            // Ensure CameraUseCase is initialized before starting camera
            initializationDeferred.await()
            // TODO(yasith): Handle Exceptions from binding use cases
            cameraUseCase.runCamera(
                previewUiState.value.currentCameraSettings
            )
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
            _previewUiState.emit(
                previewUiState.value.copy(
                    currentCameraSettings =
                    previewUiState.value.currentCameraSettings.copy(
                        flashMode = flashMode
                    )
                )
            )
            // apply to cameraUseCase
            cameraUseCase.setFlashMode(
                previewUiState.value.currentCameraSettings.flashMode,
                previewUiState.value.currentCameraSettings.isFrontCameraFacing
            )
        }
    }

    fun setAspectRatio(aspectRatio: AspectRatio) {
        stopCamera()
        runningCameraJob = viewModelScope.launch {
            _previewUiState.emit(
                previewUiState.value.copy(
                    currentCameraSettings =
                    previewUiState.value.currentCameraSettings.copy(
                        aspectRatio = aspectRatio
                    )
                )
            )
            cameraUseCase.setAspectRatio(
                aspectRatio,
                previewUiState.value
                    .currentCameraSettings.isFrontCameraFacing
            )
        }
    }

    // flips the camera opposite to its current direction
    fun flipCamera() {
        flipCamera(
            !previewUiState.value
                .currentCameraSettings.isFrontCameraFacing
        )
    }

    fun setCaptureMode(captureMode: CaptureMode) {
        stopCamera()
        runningCameraJob = viewModelScope.launch {
            _previewUiState.emit(
                previewUiState.value.copy(
                    currentCameraSettings =
                    previewUiState.value.currentCameraSettings.copy(
                        captureMode = captureMode
                    )
                )
            )
            // apply to cameraUseCase
            cameraUseCase.setCaptureMode(captureMode)
        }
    }

    // sets the camera to a designated direction
    fun flipCamera(isFacingFront: Boolean) {
        // only flip if 2 directions are available
        if (previewUiState.value.currentCameraSettings.isBackCameraAvailable &&
            previewUiState.value.currentCameraSettings.isFrontCameraAvailable
        ) {
            stopCamera()
            runningCameraJob = viewModelScope.launch {
                _previewUiState.emit(
                    previewUiState.value.copy(
                        currentCameraSettings =
                        previewUiState.value.currentCameraSettings.copy(
                            isFrontCameraFacing = isFacingFront
                        )
                    )
                )
                // apply to cameraUseCase
                cameraUseCase.flipCamera(
                    previewUiState.value.currentCameraSettings.isFrontCameraFacing,
                    previewUiState.value.currentCameraSettings.flashMode
                )
            }
        }
    }

    fun captureImage() {
        Log.d(TAG, "captureImage")
        viewModelScope.launch {
            traceAsync(IMAGE_CAPTURE_TRACE, 0) {
                try {
                    cameraUseCase.takePicture()
                    // todo: remove toast after postcapture screen implemented
                    _previewUiState.emit(
                        previewUiState.value.copy(
                            toastMessageToShow = ToastMessage(
                                stringResource = R.string.toast_image_capture_success,
                                testTag = IMAGE_CAPTURE_SUCCESS_TOAST_TAG
                            )
                        )
                    )
                    Log.d(TAG, "cameraUseCase.takePicture success")
                } catch (exception: Exception) {
                    // todo: remove toast after postcapture screen implemented
                    _previewUiState.emit(
                        previewUiState.value.copy(
                            toastMessageToShow = ToastMessage(
                                stringResource = R.string.toast_capture_failure,
                                testTag = IMAGE_CAPTURE_FAIL_TOAST_TAG
                            )
                        )
                    )
                    Log.d(TAG, "cameraUseCase.takePicture error")
                    Log.d(TAG, exception.toString())
                }
            }
        }
    }

    fun captureImageWithUri(
        contentResolver: ContentResolver,
        imageCaptureUri: Uri?,
        onImageCapture: (ImageCaptureEvent) -> Unit
    ) {
        Log.d(TAG, "captureImageWithUri")
        viewModelScope.launch {
            traceAsync(IMAGE_CAPTURE_TRACE, 0) {
                try {
                    cameraUseCase.takePicture(contentResolver, imageCaptureUri)
                    // todo: remove toast after postcapture screen implemented
                    _previewUiState.emit(
                        previewUiState.value.copy(
                            toastMessageToShow = ToastMessage(
                                stringResource = R.string.toast_image_capture_success,
                                testTag = IMAGE_CAPTURE_SUCCESS_TOAST_TAG
                            )
                        )
                    )
                    onImageCapture(ImageCaptureEvent.ImageSaved)
                    Log.d(TAG, "cameraUseCase.takePicture success")
                } catch (exception: Exception) {
                    // todo: remove toast after postcapture screen implemented
                    _previewUiState.emit(
                        previewUiState.value.copy(
                            toastMessageToShow = ToastMessage(
                                stringResource = R.string.toast_capture_failure,
                                testTag = IMAGE_CAPTURE_FAIL_TOAST_TAG
                            )
                        )
                    )
                    Log.d(TAG, "cameraUseCase.takePicture error")
                    Log.d(TAG, exception.toString())
                    onImageCapture(ImageCaptureEvent.ImageCaptureError(exception))
                }
            }
        }
    }

    fun startVideoRecording() {
        Log.d(TAG, "startVideoRecording")
        recordingJob = viewModelScope.launch {
            try {
                cameraUseCase.startVideoRecording()
                _previewUiState.emit(
                    previewUiState.value.copy(
                        videoRecordingState = VideoRecordingState.ACTIVE
                    )
                )
                Log.d(TAG, "cameraUseCase.startRecording success")
            } catch (exception: IllegalStateException) {
                Log.d(TAG, "cameraUseCase.startVideoRecording error")
                Log.d(TAG, exception.toString())
            }
        }
    }

    fun stopVideoRecording() {
        Log.d(TAG, "stopVideoRecording")
        viewModelScope.launch {
            _previewUiState.emit(
                previewUiState.value.copy(
                    videoRecordingState = VideoRecordingState.INACTIVE
                )
            )
        }
        cameraUseCase.stopVideoRecording()
        recordingJob?.cancel()
    }

    fun setZoomScale(scale: Float) {
        cameraUseCase.setZoomScale(scale = scale)
    }

    // modify ui values
    fun toggleQuickSettings() {
        toggleQuickSettings(!previewUiState.value.quickSettingsIsOpen)
    }

    private fun toggleQuickSettings(isOpen: Boolean) {
        viewModelScope.launch {
            _previewUiState.emit(
                previewUiState.value.copy(
                    quickSettingsIsOpen = isOpen
                )
            )
        }
    }

    fun tapToFocus(display: Display, surfaceWidth: Int, surfaceHeight: Int, x: Float, y: Float) {
        cameraUseCase.tapToFocus(
            display = display,
            surfaceWidth = surfaceWidth,
            surfaceHeight = surfaceHeight,
            x = x,
            y = y
        )
    }

    fun onToastShown() {
        viewModelScope.launch {
            // keeps the composable up on screen longer to be detected by UiAutomator
            delay(2.seconds)
            _previewUiState.emit(
                previewUiState.value.copy(
                    toastMessageToShow = null
                )
            )
        }
    }

    sealed interface ImageCaptureEvent {
        object ImageSaved : ImageCaptureEvent

        data class ImageCaptureError(
            val exception: Exception
        ) : ImageCaptureEvent
    }
}
