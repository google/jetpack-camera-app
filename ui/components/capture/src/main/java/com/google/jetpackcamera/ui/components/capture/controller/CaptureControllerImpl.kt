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
package com.google.jetpackcamera.ui.components.capture.controller

import android.content.ContentResolver
import android.util.Log
import androidx.tracing.traceAsync
import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.core.camera.OnVideoRecordEvent
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.data.media.MediaRepository
import com.google.jetpackcamera.model.CaptureEvent
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.ImageCaptureEvent
import com.google.jetpackcamera.model.IntProgress
import com.google.jetpackcamera.model.SaveLocation
import com.google.jetpackcamera.model.SaveMode
import com.google.jetpackcamera.model.VideoCaptureEvent
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_FAILURE_TAG
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.ui.components.capture.R
import com.google.jetpackcamera.ui.components.capture.SnackBarController
import com.google.jetpackcamera.ui.components.capture.VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
import com.google.jetpackcamera.ui.components.capture.VIDEO_CAPTURE_FAILURE_TAG
import com.google.jetpackcamera.ui.components.capture.VIDEO_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.ui.components.capture.controller.Utils.nextSaveLocation
import com.google.jetpackcamera.ui.components.capture.controller.Utils.postCurrentMediaToMediaRepository
import com.google.jetpackcamera.ui.uistate.SnackbarData
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "CaptureButtonControllerImpl"

private const val IMAGE_CAPTURE_TRACE = "JCA Image Capture"

class CaptureControllerImpl(
    private val trackedCaptureUiState: MutableStateFlow<TrackedCaptureUiState>,
    private val captureUiState: StateFlow<CaptureUiState>,
    private val viewModelScope: CoroutineScope,
    private val cameraSystem: CameraSystem,
    private val mediaRepository: MediaRepository,
    private val saveMode: SaveMode,
    private val externalCaptureMode: ExternalCaptureMode,
    private val externalCapturesCallback: () -> Pair<SaveLocation, IntProgress?>,
    private val captureEvents: Channel<CaptureEvent>,
    private val captureScreenController: CaptureScreenController,
    private val snackBarController: SnackBarController?
) : CaptureController {

    private val traceCookie = atomic(0)
    private val videoCaptureStartedCount = atomic(0)
    private var recordingJob: Job? = null

    override fun captureImage(contentResolver: ContentResolver) {
        if (captureUiState.value is CaptureUiState.Ready &&
            (captureUiState.value as CaptureUiState.Ready).externalCaptureMode ==
            ExternalCaptureMode.VideoCapture
        ) {
            snackBarController?.addSnackBarData(
                SnackbarData(
                    cookie = "Image-ExternalVideoCaptureMode",
                    stringResource = R.string.toast_image_capture_external_unsupported,
                    withDismissAction = true,
                    testTag = IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
                )
            )
            return
        }

        if (captureUiState.value is CaptureUiState.Ready &&
            (captureUiState.value as CaptureUiState.Ready).externalCaptureMode ==
            ExternalCaptureMode.VideoCapture
        ) {
            snackBarController?.addSnackBarData(
                SnackbarData(
                    cookie = "Image-ExternalVideoCaptureMode",
                    stringResource = R.string.toast_image_capture_external_unsupported,
                    withDismissAction = true,
                    testTag = IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
                )
            )
            return
        }
        Log.d(TAG, "captureImage")
        viewModelScope.launch {
            val (saveLocation, progress) = nextSaveLocation(
                saveMode,
                externalCaptureMode,
                externalCapturesCallback
            )
            captureImageInternal(
                saveLocation = saveLocation,
                doTakePicture = {
                    cameraSystem.takePicture(contentResolver, saveLocation) {
                        trackedCaptureUiState.update { old ->
                            old.copy(lastBlinkTimeStamp = System.currentTimeMillis())
                        }
                    }.savedUri
                },
                onSuccess = { savedUri ->
                    val event = if (progress != null) {
                        ImageCaptureEvent.SequentialImageSaved(savedUri, progress)
                    } else {
                        if (saveLocation is SaveLocation.Cache) {
                            ImageCaptureEvent.SingleImageCached(savedUri)
                        } else {
                            ImageCaptureEvent.SingleImageSaved(savedUri)
                        }
                    }
                    if (saveLocation !is SaveLocation.Cache) {
                        captureScreenController.updateLastCapturedMedia()
                    } else {
                        savedUri?.let {
                            postCurrentMediaToMediaRepository(
                                viewModelScope,
                                mediaRepository,
                                MediaDescriptor.Content.Image(it, null, true)
                            )
                        }
                    }
                    captureEvents.trySend(event)
                },
                onFailure = { exception ->
                    val event = if (progress != null) {
                        ImageCaptureEvent.SequentialImageCaptureError(exception, progress)
                    } else {
                        ImageCaptureEvent.SingleImageCaptureError(exception)
                    }

                    captureEvents.trySend(event)
                }
            )
        }
    }

    override fun startVideoRecording() {
        if (captureUiState.value is CaptureUiState.Ready &&
            (captureUiState.value as CaptureUiState.Ready).externalCaptureMode ==
            ExternalCaptureMode.ImageCapture
        ) {
            Log.d(TAG, "externalVideoRecording")
            snackBarController?.addSnackBarData(
                SnackbarData(
                    cookie = "Video-ExternalImageCaptureMode",
                    stringResource = R.string.toast_video_capture_external_unsupported,
                    withDismissAction = true,
                    testTag = VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
                )
            )
            return
        }
        Log.d(TAG, "startVideoRecording")
        recordingJob = viewModelScope.launch {
            val cookie = "Video-${videoCaptureStartedCount.incrementAndGet()}"
            val (saveLocation, _) = nextSaveLocation(
                saveMode,
                externalCaptureMode,
                externalCapturesCallback
            )
            try {
                cameraSystem.startVideoRecording(saveLocation) {
                    var snackbarToShow: SnackbarData?
                    when (it) {
                        is OnVideoRecordEvent.OnVideoRecorded -> {
                            Log.d(TAG, "cameraSystem.startRecording OnVideoRecorded")
                            val event = if (saveLocation is SaveLocation.Cache) {
                                VideoCaptureEvent.VideoCached(it.savedUri)
                            } else {
                                VideoCaptureEvent.VideoSaved(it.savedUri)
                            }

                            if (saveLocation !is SaveLocation.Cache) {
                                captureScreenController.updateLastCapturedMedia()
                            } else {
                                postCurrentMediaToMediaRepository(
                                    viewModelScope,
                                    mediaRepository,
                                    MediaDescriptor.Content.Video(it.savedUri, null, true)
                                )
                            }

                            captureEvents.trySend(event)
                            // don't display snackbar for successful capture
                            snackbarToShow = if (saveLocation is SaveLocation.Cache) {
                                null
                            } else {
                                SnackbarData(
                                    cookie = cookie,
                                    stringResource = R.string.toast_video_capture_success,
                                    withDismissAction = true,
                                    testTag = VIDEO_CAPTURE_SUCCESS_TAG
                                )
                            }
                        }

                        is OnVideoRecordEvent.OnVideoRecordError -> {
                            Log.d(TAG, "cameraSystem.startRecording OnVideoRecordError")
                            captureEvents.trySend(VideoCaptureEvent.VideoCaptureError(it.error))
                            snackbarToShow = SnackbarData(
                                cookie = cookie,
                                stringResource = R.string.toast_video_capture_failure,
                                withDismissAction = true,
                                testTag = VIDEO_CAPTURE_FAILURE_TAG
                            )
                        }
                    }

                    snackbarToShow?.let { data ->
                        snackBarController?.addSnackBarData(data)
                    }
                }
                Log.d(TAG, "cameraSystem.startRecording success")
            } catch (exception: IllegalStateException) {
                Log.d(TAG, "cameraSystem.startVideoRecording error", exception)
            }
        }
    }

    override fun stopVideoRecording() {
        Log.d(TAG, "stopVideoRecording")
        viewModelScope.launch {
            cameraSystem.stopVideoRecording()
            recordingJob?.cancel()
        }
    }

    private suspend fun <T> captureImageInternal(
        saveLocation: SaveLocation,
        doTakePicture: suspend () -> T,
        onSuccess: (T) -> Unit = {},
        onFailure: (exception: Exception) -> Unit = {}
    ) {
        val cookieInt = snackBarController?.incrementAndGetSnackBarCount()
            ?: traceCookie.incrementAndGet()
        val cookie = "Image-$cookieInt"
        val snackBarData = try {
            traceAsync(IMAGE_CAPTURE_TRACE, cookieInt) {
                doTakePicture()
            }.also { result ->
                onSuccess(result)
            }
            Log.d(TAG, "cameraSystem.takePicture success")
            // don't display snackbar for successful capture
            if (saveLocation is SaveLocation.Cache) {
                null
            } else {
                SnackbarData(
                    cookie = cookie,
                    stringResource = R.string.toast_image_capture_success,
                    withDismissAction = true,
                    testTag = IMAGE_CAPTURE_SUCCESS_TAG
                )
            }
        } catch (exception: Exception) {
            onFailure(exception)
            Log.d(TAG, "cameraSystem.takePicture error", exception)
            SnackbarData(
                cookie = cookie,
                stringResource = R.string.toast_capture_failure,
                withDismissAction = true,
                testTag = IMAGE_CAPTURE_FAILURE_TAG
            )
        }
        snackBarData?.let {
            snackBarController?.addSnackBarData(
                it
            )
        }
    }

    override fun setLockedRecording(isLocked: Boolean) {
        trackedCaptureUiState.update { old ->
            old.copy(isRecordingLocked = isLocked)
        }
    }
}
