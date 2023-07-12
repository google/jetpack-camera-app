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
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview.SurfaceProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetpackcamera.domain.camera.CameraUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.jetpackcamera.settings.SettingsRepository
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.FlashModeStatus

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
    var runningCameraJob: Job? = null

    private var recordingJob : Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.cameraAppSettings.collect {
                //TODO: only update settings that were actually changed
                // currently resets all "quick" settings to stored settings
                    settings -> _previewUiState
                .emit(previewUiState.value.copy(currentCameraSettings = settings))
            }
        }
        initializeCamera()
    }

    private fun initializeCamera() {
        // TODO(yasith): Handle CameraUnavailableException
        Log.d(TAG, "initializeCamera")
        viewModelScope.launch {
            cameraUseCase.initialize(previewUiState.value.currentCameraSettings)
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
            cameraUseCase.runCamera(
                surfaceProvider,
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

    fun setFlash(flashModeStatus: FlashModeStatus){
        //update viewmodel value
        viewModelScope.launch {
            _previewUiState.emit(
                previewUiState.value.copy(
                    currentCameraSettings =
                        previewUiState.value.currentCameraSettings.copy(
                            flash_mode_status = flashModeStatus
                        )
                )
            )
            // apply to cameraUseCase
            cameraUseCase.setFlashMode(previewUiState.value.currentCameraSettings.flash_mode_status)
        }
    }

    // flips the camera opposite to its current direction
    fun flipCamera() {
        flipCamera(!previewUiState.value
            .currentCameraSettings.default_front_camera)
    }
    // sets the camera to a designated direction
    fun flipCamera(isFacingFront: Boolean) {
        //update viewmodel value
         viewModelScope.launch {
             _previewUiState.emit(
                 previewUiState.value.copy(
                     currentCameraSettings =
                     previewUiState.value.currentCameraSettings.copy(
                         default_front_camera = isFacingFront
                     )
                 )
             )
             // apply to cameraUseCase
             cameraUseCase.flipCamera(previewUiState.value.currentCameraSettings.default_front_camera)
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

    fun setZoomScale(scale: Float) {
        cameraUseCase.setZoomScale(scale = scale)
    }
}