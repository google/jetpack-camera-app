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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tracing.Trace
import androidx.tracing.traceAsync
import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.core.camera.CameraSystem.Companion.applyDiffs
import com.google.jetpackcamera.core.camera.OnVideoRecordEvent
import com.google.jetpackcamera.core.common.DefaultSaveMode
import com.google.jetpackcamera.core.common.traceFirstFramePreview
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.data.media.MediaRepository
import com.google.jetpackcamera.feature.preview.navigation.getCaptureUris
import com.google.jetpackcamera.feature.preview.navigation.getDebugSettings
import com.google.jetpackcamera.feature.preview.navigation.getExternalCaptureMode
import com.google.jetpackcamera.feature.preview.navigation.getRequestedSaveMode
import com.google.jetpackcamera.model.CaptureEvent
import com.google.jetpackcamera.model.DebugSettings
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.ImageCaptureEvent
import com.google.jetpackcamera.model.IntProgress
import com.google.jetpackcamera.model.LowLightBoostState
import com.google.jetpackcamera.model.SaveLocation
import com.google.jetpackcamera.model.SaveMode
import com.google.jetpackcamera.model.VideoCaptureEvent
import com.google.jetpackcamera.settings.ConstraintsRepository
import com.google.jetpackcamera.settings.SettingsRepository
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.applyExternalCaptureMode
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_FAILURE_TAG
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.ui.components.capture.LOW_LIGHT_BOOST_FAILURE_TAG
import com.google.jetpackcamera.ui.components.capture.R
import com.google.jetpackcamera.ui.components.capture.ScreenFlash
import com.google.jetpackcamera.ui.components.capture.SnackBarController
import com.google.jetpackcamera.ui.components.capture.SnackBarControllerImpl
import com.google.jetpackcamera.ui.components.capture.VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
import com.google.jetpackcamera.ui.components.capture.VIDEO_CAPTURE_FAILURE_TAG
import com.google.jetpackcamera.ui.components.capture.VIDEO_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.ui.components.capture.debug.controller.DebugController
import com.google.jetpackcamera.ui.components.capture.debug.controller.DebugControllerImpl
import com.google.jetpackcamera.ui.components.capture.getCaptureCallbacks
import com.google.jetpackcamera.ui.components.capture.getSnackBarCallbacks
import com.google.jetpackcamera.ui.components.capture.postCurrentMediaToMediaRepository
import com.google.jetpackcamera.ui.components.capture.quicksettings.controller.QuickSettingsController
import com.google.jetpackcamera.ui.components.capture.quicksettings.controller.QuickSettingsControllerImpl
import com.google.jetpackcamera.ui.uistate.capture.DebugUiState
import com.google.jetpackcamera.ui.uistate.capture.SnackBarUiState
import com.google.jetpackcamera.ui.uistate.capture.SnackbarData
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import com.google.jetpackcamera.ui.uistateadapter.capture.compound.captureUiState
import com.google.jetpackcamera.ui.uistateadapter.capture.debugUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.atomicfu.atomic
import javax.inject.Inject
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "PreviewViewModel"
private const val IMAGE_CAPTURE_TRACE = "JCA Image Capture"

/**
 * [ViewModel] for [PreviewScreen].
 */
@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val cameraSystem: CameraSystem,
    private val savedStateHandle: SavedStateHandle,
    @DefaultSaveMode private val defaultSaveMode: SaveMode,
    private val settingsRepository: SettingsRepository,
    private val constraintsRepository: ConstraintsRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {
    private val saveMode: SaveMode = savedStateHandle.getRequestedSaveMode() ?: defaultSaveMode
    private val trackedCaptureUiState: MutableStateFlow<TrackedCaptureUiState> =
        MutableStateFlow(TrackedCaptureUiState())
    private val _snackBarUiState: MutableStateFlow<SnackBarUiState.Enabled> =
        MutableStateFlow(SnackBarUiState.Enabled())
    val snackBarUiState: StateFlow<SnackBarUiState.Enabled> =
        _snackBarUiState.asStateFlow()

    val surfaceRequest: StateFlow<SurfaceRequest?> = cameraSystem.getSurfaceRequest()

    private val _captureEvents = Channel<CaptureEvent>()
    val captureEvents: ReceiveChannel<CaptureEvent> = _captureEvents

    private var runningCameraJob: Job? = null

    private var recordingJob: Job? = null

    private val externalCaptureMode: ExternalCaptureMode = savedStateHandle.getExternalCaptureMode()
    private val externalUris: List<Uri> = savedStateHandle.getCaptureUris()
    private lateinit var externalUriProgress: IntProgress

    private val debugSettings: DebugSettings = savedStateHandle.getDebugSettings()

    private var cameraPropertiesJSON = ""

    val screenFlash = ScreenFlash(cameraSystem, viewModelScope)

    private val videoCaptureStartedCount = atomic(0)

    // Eagerly initialize the CameraSystem and encapsulate in a Deferred that can be
    // used to ensure we don't start the camera before initialization is complete.
    private var initializationDeferred: Deferred<Unit> = viewModelScope.async {
        cameraSystem.initialize(
            cameraAppSettings = settingsRepository.defaultCameraAppSettings.first()
                .applyExternalCaptureMode(externalCaptureMode)
                .copy(debugSettings = debugSettings)
        ) { cameraPropertiesJSON = it }
    }

    val captureUiState: StateFlow<CaptureUiState> = captureUiState(
        cameraSystem,
        constraintsRepository,
        trackedCaptureUiState,
        externalCaptureMode
    )
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CaptureUiState.NotReady
        )
    val debugUiState: StateFlow<DebugUiState> = debugUiState(
        cameraSystem,
        constraintsRepository,
        debugSettings,
        cameraPropertiesJSON,
        trackedCaptureUiState
    )
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DebugUiState.Disabled
        )

    val quickSettingsController: QuickSettingsController = QuickSettingsControllerImpl(
        trackedCaptureUiState = trackedCaptureUiState,
        viewModelScope = viewModelScope,
        cameraSystem = cameraSystem,
        externalCaptureMode = externalCaptureMode
    )

    val debugController: DebugController = DebugControllerImpl(
        cameraSystem = cameraSystem,
        trackedCaptureUiState = trackedCaptureUiState
    )

    val snackBarCallbacks = getSnackBarCallbacks(
        viewModelScope = viewModelScope,
        snackBarUiState = _snackBarUiState
    )

    val snackBarController: SnackBarController = SnackBarControllerImpl(
        viewModelScope = viewModelScope,
        snackBarUiState = _snackBarUiState
    )

    val captureCallbacks = getCaptureCallbacks(
        viewModelScope = viewModelScope,
        cameraSystem = cameraSystem,
        trackedCaptureUiState = trackedCaptureUiState,
        mediaRepository = mediaRepository,
        captureUiState = captureUiState
    )

    init {
        viewModelScope.launch {
            launch {
                var oldCameraAppSettings: CameraAppSettings? = null
                settingsRepository.defaultCameraAppSettings
                    .collect { new ->
                        oldCameraAppSettings?.apply {
                            applyDiffs(new, cameraSystem)
                        }
                        oldCameraAppSettings = new
                    }
            }

            launch {
                cameraSystem.getCurrentCameraState()
                    .map { it.lowLightBoostState }
                    .distinctUntilChanged()
                    .collect { state ->
                        if (state is LowLightBoostState.Error) {
                            val cookieInt = snackBarController.incrementAndGetSnackBarCount()
                            Log.d(TAG, "LowLightBoostState changed to Error #$cookieInt")
                            snackBarController.addSnackBarData(
                                SnackbarData(
                                    cookie = "LowLightBoost-$cookieInt",
                                    stringResource = R.string.low_light_boost_error_toast_message,
                                    withDismissAction = true,
                                    testTag = LOW_LIGHT_BOOST_FAILURE_TAG
                                )
                            )
                        }
                    }
            }
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
                        captureUiState.transformWhile {
                            var continueCollecting = true
                            (it as? CaptureUiState.Ready)?.let { uiState ->
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
            // Ensure CameraSystem is initialized before starting camera
            initializationDeferred.await()
            // TODO(yasith): Handle Exceptions from binding use cases
            cameraSystem.runCamera()
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

    private fun nextSaveLocation(saveMode: SaveMode): Pair<SaveLocation, IntProgress?> {
        return when (externalCaptureMode) {
            ExternalCaptureMode.ImageCapture,
            ExternalCaptureMode.MultipleImageCapture,
            ExternalCaptureMode.VideoCapture -> {
                if (externalUris.isNotEmpty()) {
                    if (!this::externalUriProgress.isInitialized) {
                        externalUriProgress = IntProgress(1, 1..externalUris.size)
                    }
                    val progress = externalUriProgress
                    if (progress.currentValue < progress.range.endInclusive) externalUriProgress++
                    Pair(
                        SaveLocation.Explicit(externalUris[progress.currentValue - 1]),
                        progress
                    )
                } else {
                    Pair(SaveLocation.Default, null)
                }
            }

            ExternalCaptureMode.Standard -> {
                val defaultSaveLocation =
                    if (saveMode is SaveMode.CacheAndReview) {
                        SaveLocation.Cache(saveMode.cacheDir)
                    } else {
                        SaveLocation.Default
                    }
                Pair(defaultSaveLocation, null)
            }
        }
    }

    fun captureImage(contentResolver: ContentResolver) {
        if (captureUiState.value is CaptureUiState.Ready &&
            (captureUiState.value as CaptureUiState.Ready).externalCaptureMode ==
            ExternalCaptureMode.VideoCapture
        ) {
            snackBarController.addSnackBarData(
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
            snackBarController.addSnackBarData(
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
            val (saveLocation, progress) = nextSaveLocation(saveMode)
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
                        captureCallbacks.updateLastCapturedMedia()
                    } else {
                        savedUri?.let {
                            postCurrentMediaToMediaRepository(
                                viewModelScope,
                                mediaRepository,
                                MediaDescriptor.Content.Image(it, null, true)
                            )
                        }
                    }
                    _captureEvents.trySend(event)
                },
                onFailure = { exception ->
                    val event = if (progress != null) {
                        ImageCaptureEvent.SequentialImageCaptureError(exception, progress)
                    } else {
                        ImageCaptureEvent.SingleImageCaptureError(exception)
                    }

                    _captureEvents.trySend(event)
                }
            )
        }
    }

    private suspend fun <T> captureImageInternal(
        saveLocation: SaveLocation,
        doTakePicture: suspend () -> T,
        onSuccess: (T) -> Unit = {},
        onFailure: (exception: Exception) -> Unit = {}
    ) {
        val cookieInt = snackBarController.incrementAndGetSnackBarCount()
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
            snackBarController.addSnackBarData(
                it
            )
        }
    }

    fun startVideoRecording() {
        if (captureUiState.value is CaptureUiState.Ready &&
            (captureUiState.value as CaptureUiState.Ready).externalCaptureMode ==
            ExternalCaptureMode.ImageCapture
        ) {
            Log.d(TAG, "externalVideoRecording")
            snackBarController.addSnackBarData(
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
            val saveMode = saveMode
            val (saveLocation, _) = nextSaveLocation(saveMode)
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
                                captureCallbacks.updateLastCapturedMedia()
                            } else {
                                postCurrentMediaToMediaRepository(
                                    viewModelScope,
                                    mediaRepository,
                                    MediaDescriptor.Content.Video(it.savedUri, null, true)
                                )
                            }

                            _captureEvents.trySend(event)
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
                            _captureEvents.trySend(VideoCaptureEvent.VideoCaptureError(it.error))
                            snackbarToShow = SnackbarData(
                                cookie = cookie,
                                stringResource = R.string.toast_video_capture_failure,
                                withDismissAction = true,
                                testTag = VIDEO_CAPTURE_FAILURE_TAG
                            )
                        }
                    }

                    snackbarToShow?.let { data ->
                        snackBarController.addSnackBarData(data)
                    }
                }
                Log.d(TAG, "cameraSystem.startRecording success")
            } catch (exception: IllegalStateException) {
                Log.d(TAG, "cameraSystem.startVideoRecording error", exception)
            }
        }
    }

    fun stopVideoRecording() {
        Log.d(TAG, "stopVideoRecording")
        viewModelScope.launch {
            cameraSystem.stopVideoRecording()
            recordingJob?.cancel()
        }
    }
}
