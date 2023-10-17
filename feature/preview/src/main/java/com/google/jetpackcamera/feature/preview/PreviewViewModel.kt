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

import android.util.Log
import android.view.Display
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview.SurfaceProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetpackcamera.domain.camera.CameraUseCase
import com.google.jetpackcamera.settings.SettingsRepository
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.FlashModeStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings


private const val TAG = "PreviewViewModel"

/**
 * [ViewModel] for [PreviewScreen].
 */
@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val cameraUseCase: CameraUseCase,
    private val settingsRepository: SettingsRepository // only reads from settingsRepository. do not push changes to repository from here
) : ViewModel() {

    private val _previewUiState: MutableStateFlow<PreviewUiState> =
        MutableStateFlow(PreviewUiState(currentCameraSettings = DEFAULT_CAMERA_APP_SETTINGS))

    val previewUiState: StateFlow<PreviewUiState> = _previewUiState
    private var runningCameraJob: Job? = null

    private var recordingJob: Job? = null

    init {
        lateinit var initialCameraAppSettings : CameraAppSettings
        viewModelScope.launch {
            settingsRepository.cameraAppSettings.collect {
                //TODO: only update settings that were actually changed
                // currently resets all "quick" settings to stored settings
                    settings ->
                _previewUiState
                    .emit(previewUiState.value.copy(currentCameraSettings = settings))
                initialCameraAppSettings = settings
            }
        }
        initializeCamera(initialCameraAppSettings)

        viewModelScope.launch {
            cameraUseCase.config.collect {
                _previewUiState.emit(
                    it.toUiStateWith(previewUiState.value)
                )
            }
        }
    }

    private fun initializeCamera(cameraAppSettings: CameraAppSettings) {
        // TODO(yasith): Handle CameraUnavailableException
        Log.d(TAG, "initializeCamera")

        val lensFacing = if (cameraAppSettings.isFrontCameraFacing) {
            CameraUseCase.LensFacing.FRONT
        } else {
            CameraUseCase.LensFacing.BACK
        }

        viewModelScope.launch {
            cameraUseCase.initialize(
                CameraUseCase.Config(lensFacing = lensFacing)
            )
            _previewUiState.emit(
                previewUiState.value.copy(
                    cameraState = CameraState.READY
                )
            )
        }
    }

    fun runCamera(surfaceProvider: SurfaceProvider) {
        Log.d(TAG, "runCamera")
        stopCamera()
        runningCameraJob = viewModelScope.launch {
            // TODO(yasith): Handle Exceptions from binding use cases
            cameraUseCase.runCamera(surfaceProvider)
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

    /**
     * Updates the flash mode used by the camera.
     *
     * @param flashModeStatus new flash mode to be used.
     */
    fun setFlash(flashModeStatus: FlashModeStatus) {
        viewModelScope.launch {
            cameraUseCase.setFlashMode(flashModeStatus.toCameraUseCaseFlashMode())
        }
    }

    /**
     * Updates the aspect ratio for the camera.
     *
     * @param aspectRatio new aspect ratio to be used.
     */
    fun setAspectRatio(aspectRatio: AspectRatio) {
        viewModelScope.launch {
            cameraUseCase.setAspectRatio(aspectRatio.toCameraUseCaseAspectRatio())
        }
    }

    /**
     * Flips camera to use the lens that's the opposite of the current lens.
     */
    fun flipCamera() {
        viewModelScope.launch {
            cameraUseCase.setLensFacing(
                cameraUseCase.config.value.lensFacing.next()
            )
        }
    }

    /**
     * Toggles capture mode between single stream and multi stream capture.
     */
    fun toggleCaptureMode() {
        viewModelScope.launch {
            cameraUseCase.setCaptureMode(
                cameraUseCase.config.value.captureMode.next()
            )
        }
    }

    fun captureImage() {
        Log.d(TAG, "captureImage")
        viewModelScope.launch {
            try {
                cameraUseCase.takePicture()
                Log.d(TAG, "cameraUseCase.takePicture success")
            } catch (exception: ImageCaptureException) {
                Log.d(TAG, "cameraUseCase.takePicture error")
                Log.d(TAG, exception.toString())
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

    fun setZoomScale(scale: Float): Float {
        return cameraUseCase.setZoomScale(scale = scale)
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
}

/**
 * Converts [CameraUseCase.Config] to [PreviewUiState], taking current UiState into account.
 */
private fun CameraUseCase.Config.toUiStateWith(
    currentUiState: PreviewUiState
) = currentUiState.copy(
    // TODO(yasith): Remove dependency on CameraX CameraSelector
    lensFacing = this.lensFacing.toCameraSelector(),
    captureMode = this.captureMode.toUiState(),
    currentCameraSettings = currentUiState.currentCameraSettings.copy(
        isFrontCameraFacing = this.lensFacing == CameraUseCase.LensFacing.FRONT,
        flashMode = this.flashMode.toUiState(),
        aspectRatio = this.aspectRatio.toUiState(),
    )
)

/**
 * Picks the next value in an Enum, rotates through values.
 */
private inline fun <reified T: Enum<T>> T.next(): T {
    val values = enumValues<T>()
    val nextOrdinal = (ordinal + 1) % values.size
    return values[nextOrdinal]
}

private fun AspectRatio.toCameraUseCaseAspectRatio() = when(this) {
    AspectRatio.THREE_FOUR -> CameraUseCase.AspectRatio.ASPECT_RATIO_4_3
    AspectRatio.NINE_SIXTEEN -> CameraUseCase.AspectRatio.ASPECT_RATIO_16_9
    AspectRatio.ONE_ONE -> CameraUseCase.AspectRatio.ASPECT_RATIO_1_1
}

private fun FlashModeStatus.toCameraUseCaseFlashMode() = when(this) {
    FlashModeStatus.OFF -> CameraUseCase.FlashMode.OFF
    FlashModeStatus.ON -> CameraUseCase.FlashMode.ON
    FlashModeStatus.AUTO -> CameraUseCase.FlashMode.AUTO
}

private fun CameraUseCase.LensFacing.toCameraSelector() = when(this) {
    CameraUseCase.LensFacing.FRONT -> CameraSelector.LENS_FACING_FRONT
    CameraUseCase.LensFacing.BACK -> CameraSelector.LENS_FACING_BACK
}

private fun CameraUseCase.FlashMode.toUiState() = when(this) {
    CameraUseCase.FlashMode.OFF -> FlashModeStatus.OFF
    CameraUseCase.FlashMode.ON -> FlashModeStatus.ON
    CameraUseCase.FlashMode.AUTO -> FlashModeStatus.AUTO
}

private fun CameraUseCase.AspectRatio.toUiState() = when(this) {
    CameraUseCase.AspectRatio.ASPECT_RATIO_4_3 -> AspectRatio.THREE_FOUR
    CameraUseCase.AspectRatio.ASPECT_RATIO_16_9 -> AspectRatio.NINE_SIXTEEN
    CameraUseCase.AspectRatio.ASPECT_RATIO_1_1 -> AspectRatio.ONE_ONE
}

private fun CameraUseCase.CaptureMode.toUiState() = when(this) {
    CameraUseCase.CaptureMode.MULTI_STREAM -> CaptureMode.MULTI_STREAM
    CameraUseCase.CaptureMode.SINGLE_STREAM -> CaptureMode.SINGLE_STREAM
}
