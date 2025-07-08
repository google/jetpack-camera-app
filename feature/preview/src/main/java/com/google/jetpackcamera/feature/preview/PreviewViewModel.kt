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
import com.google.jetpackcamera.data.media.MediaRepository
import com.google.jetpackcamera.settings.ConstraintsRepository
import com.google.jetpackcamera.settings.SettingsRepository
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraZoomRatio
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.ConcurrentCameraMode
import com.google.jetpackcamera.settings.model.DebugSettings
import com.google.jetpackcamera.settings.model.DeviceRotation
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.ExternalCaptureMode
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.StreamConfig
import com.google.jetpackcamera.settings.model.SystemConstraints
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_FAILURE_TAG
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.ui.components.capture.VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
import com.google.jetpackcamera.ui.components.capture.VIDEO_CAPTURE_FAILURE_TAG
import com.google.jetpackcamera.ui.components.capture.VIDEO_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.ui.uistate.DisableRationale
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.AudioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.ConcurrentCameraUiState
import com.google.jetpackcamera.ui.uistate.capture.DebugUiState
import com.google.jetpackcamera.ui.uistate.capture.ElapsedTimeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState
import com.google.jetpackcamera.ui.uistate.capture.SnackBarUiState
import com.google.jetpackcamera.ui.uistate.capture.SnackbarData
import com.google.jetpackcamera.ui.uistate.capture.StabilizationUiState
import com.google.jetpackcamera.ui.uistate.capture.StreamConfigUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.PreviewDisplayUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import com.google.jetpackcamera.ui.uistateadapter.capture.from
import com.google.jetpackcamera.ui.uistateadapter.capture.getCaptureModeUiState
import com.google.jetpackcamera.ui.uistateadapter.capture.getCaptureToggleUiState
import com.google.jetpackcamera.ui.uistateadapter.capture.toggleDebugOverlay
import com.google.jetpackcamera.ui.uistateadapter.capture.updateFrom
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.LinkedList
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
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
    @Assisted val externalCaptureMode: ExternalCaptureMode,
    @Assisted val debugSettings: DebugSettings,
    private val cameraUseCase: CameraUseCase,
    private val settingsRepository: SettingsRepository,
    private val constraintsRepository: ConstraintsRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {
    private val _viewFinderUiState: MutableStateFlow<CaptureUiState> =
        MutableStateFlow(CaptureUiState.NotReady)
    private val lockedRecordingState: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val viewFinderUiState: StateFlow<CaptureUiState> =
        _viewFinderUiState.asStateFlow()

    val surfaceRequest: StateFlow<SurfaceRequest?> = cameraUseCase.getSurfaceRequest()

    private var runningCameraJob: Job? = null

    private var recordingJob: Job? = null

    private var externalUriIndex: Int = 0

    private var cameraPropertiesJSON = ""

    val screenFlash = ScreenFlash(cameraUseCase, viewModelScope)

    private val snackBarCount = atomic(0)
    private val videoCaptureStartedCount = atomic(0)

    // Eagerly initialize the CameraUseCase and encapsulate in a Deferred that can be
    // used to ensure we don't start the camera before initialization is complete.
    private var initializationDeferred: Deferred<Unit> = viewModelScope.async {
        cameraUseCase.initialize(
            cameraAppSettings = settingsRepository.defaultCameraAppSettings.first()
                .applyExternalCaptureMode(externalCaptureMode),
            debugSettings = debugSettings
        ) { cameraPropertiesJSON = it }
    }

    /**
     * updates the capture mode based on the preview mode
     */
    private fun CameraAppSettings.applyExternalCaptureMode(
        externalCaptureMode: ExternalCaptureMode
    ): CameraAppSettings {
        val captureMode = externalCaptureMode.toCaptureMode()
        return if (captureMode == this.captureMode) {
            this
        } else {
            this.copy(captureMode = captureMode)
        }
    }

    init {
        viewModelScope.launch {
            launch {
                var oldCameraAppSettings: CameraAppSettings? = null
                settingsRepository.defaultCameraAppSettings
                    .collect { new ->
                        oldCameraAppSettings?.apply {
                            applyDiffs(new)
                        }
                        oldCameraAppSettings = new
                    }
            }
            combine(
                cameraUseCase.getCurrentSettings().filterNotNull(),
                constraintsRepository.systemConstraints.filterNotNull(),
                cameraUseCase.getCurrentCameraState(),
                lockedRecordingState.filterNotNull().distinctUntilChanged()
            ) { cameraAppSettings, systemConstraints, cameraState, lockedState ->

                var flashModeUiState: FlashModeUiState
                val captureModeUiState = CaptureModeUiState.from(
                    systemConstraints,
                    cameraAppSettings,
                    externalCaptureMode
                )
                val flipLensUiState = FlipLensUiState.from(
                    cameraAppSettings,
                    systemConstraints
                )
                val aspectRatioUiState = AspectRatioUiState.from(cameraAppSettings)
                var quickSettingsIsOpen: Boolean
                _viewFinderUiState.update { old ->
                    when (old) {
                        is CaptureUiState.NotReady -> {
                            flashModeUiState = FlashModeUiState.from(
                                cameraAppSettings,
                                systemConstraints
                            )
                            quickSettingsIsOpen = false
                            // This is the first PreviewUiState.Ready. Create the initial
                            // PreviewUiState.Ready from defaults and initialize it below.
                            CaptureUiState.Ready()
                        }

                        is CaptureUiState.Ready -> {
                            flashModeUiState = old.flashModeUiState.updateFrom(
                                cameraAppSettings = cameraAppSettings,
                                systemConstraints = systemConstraints,
                                cameraState = cameraState
                            )
                            quickSettingsIsOpen = when (old.quickSettingsUiState) {
                                is QuickSettingsUiState.Available -> {
                                    (old.quickSettingsUiState as QuickSettingsUiState.Available)
                                        .quickSettingsIsOpen
                                }

                                is QuickSettingsUiState.Unavailable -> {
                                    false
                                }
                            }

                            // We have a previous `PreviewUiState.Ready`, return it here and
                            // update it below.
                            old
                        }
                    }.copy(
                        // Update or initialize PreviewUiState.Ready
                        externalCaptureMode = externalCaptureMode,
                        videoRecordingState = cameraState.videoRecordingState,
                        flipLensUiState = flipLensUiState,
                        aspectRatioUiState = aspectRatioUiState,
                        quickSettingsUiState = getQuickSettingsUiState(
                            captureModeUiState,
                            flashModeUiState,
                            flipLensUiState,
                            cameraAppSettings,
                            systemConstraints,
                            aspectRatioUiState,
                            quickSettingsIsOpen
                        ),
                        sessionFirstFrameTimestamp = cameraState.sessionFirstFrameTimestamp,
                        debugUiState = DebugUiState.from(
                            cameraPropertiesJSON,
                            cameraState,
                            debugSettings.isDebugModeEnabled
                        ),
                        stabilizationUiState = StabilizationUiState.Companion.from(
                            cameraAppSettings,
                            cameraState
                        ),
                        flashModeUiState = flashModeUiState,
                        videoQuality = cameraState.videoQualityInfo.quality,
                        audioUiState = AudioUiState.Companion.from(
                            cameraAppSettings,
                            cameraState
                        ),
                        elapsedTimeUiState = ElapsedTimeUiState.Companion.from(cameraState),
                        captureButtonUiState = CaptureButtonUiState.Companion.from(
                            cameraAppSettings,
                            cameraState,
                            lockedState
                        ),
                        zoomUiState = ZoomUiState.Companion.from(
                            systemConstraints,
                            cameraAppSettings.cameraLensFacing,
                            cameraState
                        ),
                        captureModeToggleUiState = CaptureModeToggleUiState.from(
                            systemConstraints,
                            cameraAppSettings,
                            cameraState,
                            externalCaptureMode
                        )
                    )
                }
            }.collect {}
        }
    }

    private fun getQuickSettingsUiState(
        captureModeUiState: CaptureModeUiState,
        flashModeUiState: FlashModeUiState,
        flipLensUiState: FlipLensUiState,
        cameraAppSettings: CameraAppSettings,
        systemConstraints: SystemConstraints,
        aspectRatioUiState: AspectRatioUiState,
        quickSettingsIsOpen: Boolean
    ): QuickSettingsUiState {
        val streamConfigUiState = StreamConfigUiState.from(cameraAppSettings)
        return QuickSettingsUiState.Available(
            aspectRatioUiState = aspectRatioUiState,
            captureModeUiState = captureModeUiState,
            concurrentCameraUiState = ConcurrentCameraUiState.from(
                cameraAppSettings,
                systemConstraints,
                    externalCaptureMode,
                captureModeUiState,
                streamConfigUiState
            ),
            flashModeUiState = flashModeUiState,
            flipLensUiState = flipLensUiState,
            hdrUiState = HdrUiState.Companion.from(
                cameraAppSettings,
                systemConstraints,
                externalCaptureMode
            ),
            streamConfigUiState = streamConfigUiState,
            quickSettingsIsOpen = quickSettingsIsOpen
        )
    }

    fun updateLastCapturedMedia() {
        viewModelScope.launch {
            val lastCapturedMediaDescriptor = mediaRepository.getLastCapturedMedia()
            _viewFinderUiState.update { old ->
                (old as? CaptureUiState.Ready)?.copy(
                    imageWellUiState =
                    ImageWellUiState.Companion.from(lastCapturedMediaDescriptor)
                ) ?: old
            }
        }
    }

    private fun ExternalCaptureMode.toCaptureMode() = when (this) {
        is ExternalCaptureMode.ExternalImageCaptureMode -> CaptureMode.IMAGE_ONLY
        is ExternalCaptureMode.ExternalMultipleImageCaptureMode -> CaptureMode.IMAGE_ONLY
        is ExternalCaptureMode.ExternalVideoCaptureMode -> CaptureMode.VIDEO_ONLY
        is ExternalCaptureMode.StandardMode -> CaptureMode.STANDARD
    }

    /**
     * Applies an individual camera app setting with the given [settingExtractor] and
     * [settingApplicator] if the new setting differs from the old setting.
     */
    private suspend inline fun <R> CameraAppSettings.applyDiff(
        new: CameraAppSettings,
        settingExtractor: CameraAppSettings.() -> R,
        crossinline settingApplicator: suspend (R) -> Unit
    ) {
        val oldSetting = settingExtractor.invoke(this)
        val newSetting = settingExtractor.invoke(new)
        if (oldSetting != newSetting) {
            settingApplicator(newSetting)
        }
    }

    /**
     * Checks whether each actionable individual setting has changed and applies them to
     * [CameraUseCase].
     */
    private suspend fun CameraAppSettings.applyDiffs(new: CameraAppSettings) {
        applyDiff(new, CameraAppSettings::cameraLensFacing, cameraUseCase::setLensFacing)
        applyDiff(new, CameraAppSettings::flashMode, cameraUseCase::setFlashMode)
        applyDiff(new, CameraAppSettings::streamConfig, cameraUseCase::setStreamConfig)
        applyDiff(new, CameraAppSettings::aspectRatio, cameraUseCase::setAspectRatio)
        applyDiff(new, CameraAppSettings::stabilizationMode, cameraUseCase::setStabilizationMode)
        applyDiff(new, CameraAppSettings::targetFrameRate, cameraUseCase::setTargetFrameRate)
        applyDiff(
            new,
            CameraAppSettings::maxVideoDurationMillis,
            cameraUseCase::setMaxVideoDuration
        )
        applyDiff(new, CameraAppSettings::videoQuality, cameraUseCase::setVideoQuality)
        applyDiff(new, CameraAppSettings::audioEnabled, cameraUseCase::setAudioEnabled)
    }

    fun startCamera() {
        Log.d(TAG, "startCamera")
        stopCamera()
        runningCameraJob = viewModelScope.launch {
            if (Trace.isEnabled()) {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    val startTraceTimestamp: Long = SystemClock.elapsedRealtimeNanos()
                    traceFirstFramePreview(cookie = 1) {
                        _viewFinderUiState.transformWhile {
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

    fun setStreamConfig(streamConfig: StreamConfig) {
        viewModelScope.launch {
            cameraUseCase.setStreamConfig(streamConfig)
        }
    }

    /** Sets the camera to a designated lens facing */
    fun setLensFacing(newLensFacing: LensFacing) {
        viewModelScope.launch {
            // apply to cameraUseCase
            cameraUseCase.setLensFacing(newLensFacing)
        }
    }

    fun setAudioEnabled(shouldEnableAudio: Boolean) {
        viewModelScope.launch {
            cameraUseCase.setAudioEnabled(shouldEnableAudio)
        }

        Log.d(
            TAG,
            "Toggle Audio: $shouldEnableAudio"
        )
    }

    fun setPaused(shouldBePaused: Boolean) {
        viewModelScope.launch {
            if (shouldBePaused) {
                cameraUseCase.pauseVideoRecording()
            } else {
                cameraUseCase.resumeVideoRecording()
            }
        }
    }

    private fun addSnackBarData(snackBarData: SnackbarData) {
        viewModelScope.launch {
            _viewFinderUiState.update { old ->
                val newQueue = LinkedList(
                    (old as? CaptureUiState.Ready)
                        ?.snackBarUiState?.snackBarQueue!!
                )
                newQueue.add(snackBarData)
                Log.d(TAG, "SnackBar added. Queue size: ${newQueue.size}")
                old.copy(
                    snackBarUiState = SnackBarUiState.Companion.from(newQueue)
                )
            }
        }
    }

    private fun enqueueExternalImageCaptureUnsupportedSnackBar() {
        addSnackBarData(
            SnackbarData(
                cookie = "Image-ExternalVideoCaptureMode",
                stringResource = R.string.toast_image_capture_external_unsupported,
                withDismissAction = true,
                testTag = IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
            )
        )
    }

    fun captureImageWithUri(
        contentResolver: ContentResolver,
        imageCaptureUri: Uri?,
        ignoreUri: Boolean = false,
        onImageCapture: (ImageCaptureEvent, Int) -> Unit
    ) {
        if (viewFinderUiState.value is CaptureUiState.Ready &&
            (viewFinderUiState.value as CaptureUiState.Ready).externalCaptureMode is
                ExternalCaptureMode.ExternalVideoCaptureMode
        ) {
            enqueueExternalImageCaptureUnsupportedSnackBar()
            return
        }

        if (viewFinderUiState.value is CaptureUiState.Ready &&
            (viewFinderUiState.value as CaptureUiState.Ready).externalCaptureMode is
                ExternalCaptureMode.ExternalVideoCaptureMode
        ) {
            addSnackBarData(
                SnackbarData(
                    cookie = "Image-ExternalVideoCaptureMode",
                    stringResource = R.string.toast_image_capture_external_unsupported,
                    withDismissAction = true,
                    testTag = IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
                )
            )
            return
        }
        Log.d(TAG, "captureImageWithUri")
        viewModelScope.launch {
            val (uriIndex: Int, finalImageUri: Uri?) =
                (
                    (viewFinderUiState.value as? CaptureUiState.Ready)?.externalCaptureMode as?
                        ExternalCaptureMode.ExternalMultipleImageCaptureMode
                    )?.let {
                    val uri = if (ignoreUri || it.imageCaptureUris.isNullOrEmpty()) {
                        null
                    } else {
                        it.imageCaptureUris!![externalUriIndex]
                    }
                    Pair(externalUriIndex, uri)
                } ?: Pair(-1, imageCaptureUri)
            captureImageInternal(
                doTakePicture = {
                    cameraUseCase.takePicture({
                        _viewFinderUiState.update { old ->
                            (old as? CaptureUiState.Ready)?.copy(
                                previewDisplayUiState = PreviewDisplayUiState(
                                    lastBlinkTimeStamp = System.currentTimeMillis(),
                                    aspectRatioUiState = old.aspectRatioUiState
                                )
                            ) ?: old
                        }
                    }, contentResolver, finalImageUri, ignoreUri).savedUri
                },
                onSuccess = { savedUri ->
                    updateLastCapturedMedia()
                    onImageCapture(ImageCaptureEvent.ImageSaved(savedUri), uriIndex)
                },
                onFailure = { exception ->
                    onImageCapture(ImageCaptureEvent.ImageCaptureError(exception), uriIndex)
                }
            )
            incrementExternalMultipleImageCaptureModeUriIndexIfNeeded()
        }
    }

    private fun incrementExternalMultipleImageCaptureModeUriIndexIfNeeded() {
        (
            (viewFinderUiState.value as? CaptureUiState.Ready)
                ?.externalCaptureMode as? ExternalCaptureMode.ExternalMultipleImageCaptureMode
            )?.let {
            if (!it.imageCaptureUris.isNullOrEmpty()) {
                externalUriIndex++
                Log.d(TAG, "Uri index for multiple image capture at $externalUriIndex")
            }
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
            addSnackBarData(snackBarData)
        }
    }

    fun enqueueDisabledHdrToggleSnackBar(disabledReason: DisableRationale) {
        val cookieInt = snackBarCount.incrementAndGet()
        val cookie = "DisabledHdrToggle-$cookieInt"
        addSnackBarData(
            SnackbarData(
                cookie = cookie,
                stringResource = disabledReason.reasonTextResId,
                withDismissAction = true,
                testTag = disabledReason.testTag
            )
        )
    }

    fun startVideoRecording(
        videoCaptureUri: Uri?,
        shouldUseUri: Boolean,
        onVideoCapture: (VideoCaptureEvent) -> Unit
    ) {
        if (viewFinderUiState.value is CaptureUiState.Ready &&
            (viewFinderUiState.value as CaptureUiState.Ready).externalCaptureMode is
                ExternalCaptureMode.ExternalImageCaptureMode
        ) {
            Log.d(TAG, "externalVideoRecording")
            addSnackBarData(
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
            try {
                cameraUseCase.startVideoRecording(videoCaptureUri, shouldUseUri) {
                    var snackbarToShow: SnackbarData? = null
                    when (it) {
                        is CameraUseCase.OnVideoRecordEvent.OnVideoRecorded -> {
                            Log.d(TAG, "cameraUseCase.startRecording OnVideoRecorded")
                            onVideoCapture(VideoCaptureEvent.VideoSaved(it.savedUri))
                            snackbarToShow = SnackbarData(
                                cookie = cookie,
                                stringResource = R.string.toast_video_capture_success,
                                withDismissAction = true,
                                testTag = VIDEO_CAPTURE_SUCCESS_TAG
                            )
                            updateLastCapturedMedia()
                        }

                        is CameraUseCase.OnVideoRecordEvent.OnVideoRecordError -> {
                            Log.d(TAG, "cameraUseCase.startRecording OnVideoRecordError")
                            onVideoCapture(VideoCaptureEvent.VideoCaptureError(it.error))
                            snackbarToShow = SnackbarData(
                                cookie = cookie,
                                stringResource = R.string.toast_video_capture_failure,
                                withDismissAction = true,
                                testTag = VIDEO_CAPTURE_FAILURE_TAG
                            )
                        }
                    }

                    addSnackBarData(snackbarToShow)
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
            cameraUseCase.stopVideoRecording()
            recordingJob?.cancel()
        }
    }

    /**
     "Locks" the video recording such that the user no longer needs to keep their finger pressed on the capture button
     */
    fun setLockedRecording(isLocked: Boolean) {
        viewModelScope.launch {
            lockedRecordingState.update {
                isLocked
            }
        }
    }

    fun changeZoomRatio(newZoomState: CameraZoomRatio) {
        cameraUseCase.changeZoomRatio(newZoomState = newZoomState)
    }

    fun setDynamicRange(dynamicRange: DynamicRange) {
        if (externalCaptureMode !is ExternalCaptureMode.ExternalImageCaptureMode &&
            externalCaptureMode !is ExternalCaptureMode.ExternalMultipleImageCaptureMode
        ) {
            viewModelScope.launch {
                cameraUseCase.setDynamicRange(dynamicRange)
            }
        }
    }

    fun setConcurrentCameraMode(concurrentCameraMode: ConcurrentCameraMode) {
        viewModelScope.launch {
            cameraUseCase.setConcurrentCameraMode(concurrentCameraMode)
        }
    }

    fun setImageFormat(imageFormat: ImageOutputFormat) {
        if (externalCaptureMode !is ExternalCaptureMode.ExternalVideoCaptureMode) {
            viewModelScope.launch {
                cameraUseCase.setImageFormat(imageFormat)
            }
        }
    }

    fun setCaptureMode(captureMode: CaptureMode) {
        viewModelScope.launch {
            cameraUseCase.setCaptureMode(captureMode)
        }
    }

    // modify ui values
    fun toggleQuickSettings() {
        viewModelScope.launch {
            _viewFinderUiState.update { old ->
                (old as? CaptureUiState.Ready)?.copy(
                    quickSettingsUiState = QuickSettingsUiState.Available(
                        (
                            old.quickSettingsUiState
                                as QuickSettingsUiState.Available
                            ).aspectRatioUiState,
                        (
                            old.quickSettingsUiState
                                as QuickSettingsUiState.Available
                            ).captureModeUiState,
                        (
                            old.quickSettingsUiState
                                as QuickSettingsUiState.Available
                            ).concurrentCameraUiState,
                        (
                            old.quickSettingsUiState
                                as QuickSettingsUiState.Available
                            ).flashModeUiState,
                        (
                            old.quickSettingsUiState
                                as QuickSettingsUiState.Available
                            ).flipLensUiState,
                        (
                            old.quickSettingsUiState
                                as QuickSettingsUiState.Available
                            ).hdrUiState,
                        (
                            old.quickSettingsUiState
                                as QuickSettingsUiState.Available
                            ).streamConfigUiState,
                        !(
                            old.quickSettingsUiState
                                as QuickSettingsUiState.Available
                            ).quickSettingsIsOpen
                    )
                ) ?: old
            }
        }
    }

    fun toggleDebugOverlay() {
        viewModelScope.launch {
            _viewFinderUiState.update { old ->
                (old as? CaptureUiState.Ready)?.copy(
                    debugUiState = old.debugUiState.toggleDebugOverlay()
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

    fun onSnackBarResult(cookie: String) {
        viewModelScope.launch {
            _viewFinderUiState.update { old ->
                (old as? CaptureUiState.Ready)?.snackBarUiState?.snackBarQueue!!.let {
                    val newQueue = LinkedList(it)
                    val snackBarData = newQueue.remove()
                    if (snackBarData != null && snackBarData.cookie == cookie) {
                        // If the latest snackBar had a result, then clear snackBarToShow
                        Log.d(TAG, "SnackBar removed. Queue size: ${newQueue.size}")
                        old.copy(
                            snackBarUiState = SnackBarUiState.Companion.from(newQueue)
                        )
                    } else {
                        old
                    }
                }
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
        fun create(externalCaptureMode: ExternalCaptureMode, debugSettings: DebugSettings): PreviewViewModel
    }

    sealed interface ImageCaptureEvent {
        data class ImageSaved(val savedUri: Uri? = null) : ImageCaptureEvent

        data class ImageCaptureError(val exception: Exception) : ImageCaptureEvent
    }

    sealed interface VideoCaptureEvent {
        data class VideoSaved(val savedUri: Uri) : VideoCaptureEvent

        data class VideoCaptureError(val error: Throwable?) : VideoCaptureEvent
    }
}
