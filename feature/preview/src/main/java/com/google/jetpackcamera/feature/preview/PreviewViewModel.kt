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
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.core.camera.OnVideoRecordEvent
import com.google.jetpackcamera.core.common.traceFirstFramePreview
import com.google.jetpackcamera.data.media.MediaRepository
import com.google.jetpackcamera.feature.preview.navigation.getCaptureUris
import com.google.jetpackcamera.feature.preview.navigation.getDebugSettings
import com.google.jetpackcamera.feature.preview.navigation.getExternalCaptureMode
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CameraZoomRatio
import com.google.jetpackcamera.model.CaptureEvent
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DebugSettings
import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageCaptureEvent
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.IntProgress
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.SaveLocation
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.model.TestPattern
import com.google.jetpackcamera.model.VideoCaptureEvent
import com.google.jetpackcamera.settings.ConstraintsRepository
import com.google.jetpackcamera.settings.SettingsRepository
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_FAILURE_TAG
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.ui.components.capture.R
import com.google.jetpackcamera.ui.components.capture.ScreenFlash
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
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.PreviewDisplayUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import com.google.jetpackcamera.ui.uistateadapter.capture.from
import com.google.jetpackcamera.ui.uistateadapter.capture.updateFrom
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.LinkedList
import javax.inject.Inject
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val cameraSystem: CameraSystem,
    private val savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    private val constraintsRepository: ConstraintsRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {
    private val _captureUiState: MutableStateFlow<CaptureUiState> =
        MutableStateFlow(CaptureUiState.NotReady)
    private val trackedPreviewUiState: MutableStateFlow<TrackedPreviewUiState> =
        MutableStateFlow(TrackedPreviewUiState())

    val captureUiState: StateFlow<CaptureUiState> =
        _captureUiState.asStateFlow()

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

    private val snackBarCount = atomic(0)
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
                cameraSystem.getCurrentSettings().filterNotNull(),
                constraintsRepository.systemConstraints.filterNotNull(),
                cameraSystem.getCurrentCameraState(),
                trackedPreviewUiState
            ) { cameraAppSettings, systemConstraints, cameraState, trackedUiState ->

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
                val hdrUiState = HdrUiState.from(
                    cameraAppSettings,
                    systemConstraints,
                    externalCaptureMode
                )
                _captureUiState.update { old ->
                    when (old) {
                        is CaptureUiState.NotReady -> {
                            flashModeUiState = FlashModeUiState.from(
                                cameraAppSettings,
                                systemConstraints
                            )
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
                        previewDisplayUiState = PreviewDisplayUiState(0, aspectRatioUiState),
                        quickSettingsUiState = getQuickSettingsUiState(
                            captureModeUiState,
                            flashModeUiState,
                            flipLensUiState,
                            cameraAppSettings,
                            systemConstraints,
                            aspectRatioUiState,
                            hdrUiState,
                            trackedUiState.isQuickSettingsOpen
                        ),
                        sessionFirstFrameTimestamp = cameraState.sessionFirstFrameTimestamp,
                        debugUiState = getDebugUiState(
                            systemConstraints,
                            cameraAppSettings,
                            cameraState,
                            trackedUiState.isDebugOverlayOpen
                        ),
                        stabilizationUiState = StabilizationUiState.from(
                            cameraAppSettings,
                            cameraState
                        ),
                        flashModeUiState = flashModeUiState,
                        videoQuality = cameraState.videoQualityInfo.quality,
                        audioUiState = AudioUiState.from(
                            cameraAppSettings,
                            cameraState
                        ),
                        elapsedTimeUiState = ElapsedTimeUiState.from(cameraState),
                        captureButtonUiState = CaptureButtonUiState.from(
                            cameraAppSettings,
                            cameraState,
                            trackedUiState.isRecordingLocked
                        ),
                        zoomUiState = ZoomUiState.from(
                            systemConstraints,
                            cameraAppSettings.cameraLensFacing,
                            cameraState
                        ),
                        zoomControlUiState = ZoomControlUiState.from(
                            trackedUiState.zoomAnimationTarget,
                            systemConstraints,
                            cameraAppSettings,
                            cameraState
                        ),
                        captureModeToggleUiState = CaptureModeToggleUiState.from(
                            systemConstraints,
                            cameraAppSettings,
                            cameraState,
                            externalCaptureMode
                        ),
                        hdrUiState = hdrUiState
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
        systemConstraints: CameraSystemConstraints,
        aspectRatioUiState: AspectRatioUiState,
        hdrUiState: HdrUiState,
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
            hdrUiState = hdrUiState,
            streamConfigUiState = streamConfigUiState,
            quickSettingsIsOpen = quickSettingsIsOpen
        )
    }

    private fun getDebugUiState(
        systemConstraints: CameraSystemConstraints,
        cameraAppSettings: CameraAppSettings,
        cameraState: CameraState,
        isDebugOverlayOpen: Boolean
    ): DebugUiState = if (debugSettings.isDebugModeEnabled) {
        if (isDebugOverlayOpen) {
            DebugUiState.Enabled.Open.from(
                systemConstraints,
                cameraAppSettings,
                cameraState,
                cameraPropertiesJSON
            )
        } else {
            DebugUiState.Enabled.Closed.from(cameraState, cameraAppSettings.cameraLensFacing)
        }
    } else {
        DebugUiState.Disabled
    }

    fun updateLastCapturedMedia() {
        viewModelScope.launch {
            val lastCapturedMediaDescriptor = mediaRepository.getLastCapturedMedia()
            _captureUiState.update { old ->
                (old as? CaptureUiState.Ready)?.copy(
                    imageWellUiState =
                    ImageWellUiState.from(lastCapturedMediaDescriptor)
                ) ?: old
            }
        }
    }

    private fun ExternalCaptureMode.toCaptureMode() = when (this) {
        ExternalCaptureMode.ImageCapture -> CaptureMode.IMAGE_ONLY
        ExternalCaptureMode.MultipleImageCapture -> CaptureMode.IMAGE_ONLY
        ExternalCaptureMode.VideoCapture -> CaptureMode.VIDEO_ONLY
        ExternalCaptureMode.Standard -> CaptureMode.STANDARD
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
     * [CameraSystem].
     */
    private suspend fun CameraAppSettings.applyDiffs(new: CameraAppSettings) {
        applyDiff(new, CameraAppSettings::cameraLensFacing, cameraSystem::setLensFacing)
        applyDiff(new, CameraAppSettings::flashMode, cameraSystem::setFlashMode)
        applyDiff(new, CameraAppSettings::streamConfig, cameraSystem::setStreamConfig)
        applyDiff(new, CameraAppSettings::aspectRatio, cameraSystem::setAspectRatio)
        applyDiff(new, CameraAppSettings::stabilizationMode, cameraSystem::setStabilizationMode)
        applyDiff(new, CameraAppSettings::targetFrameRate, cameraSystem::setTargetFrameRate)
        applyDiff(
            new,
            CameraAppSettings::maxVideoDurationMillis,
            cameraSystem::setMaxVideoDuration
        )
        applyDiff(new, CameraAppSettings::videoQuality, cameraSystem::setVideoQuality)
        applyDiff(new, CameraAppSettings::audioEnabled, cameraSystem::setAudioEnabled)
    }

    fun startCamera() {
        Log.d(TAG, "startCamera")
        stopCamera()
        runningCameraJob = viewModelScope.launch {
            if (Trace.isEnabled()) {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    val startTraceTimestamp: Long = SystemClock.elapsedRealtimeNanos()
                    traceFirstFramePreview(cookie = 1) {
                        _captureUiState.transformWhile {
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

    fun setFlash(flashMode: FlashMode) {
        viewModelScope.launch {
            // apply to cameraSystem
            cameraSystem.setFlashMode(flashMode)
        }
    }

    fun setAspectRatio(aspectRatio: AspectRatio) {
        viewModelScope.launch {
            cameraSystem.setAspectRatio(aspectRatio)
        }
    }

    fun setStreamConfig(streamConfig: StreamConfig) {
        viewModelScope.launch {
            cameraSystem.setStreamConfig(streamConfig)
        }
    }

    /** Sets the camera to a designated lens facing */
    fun setLensFacing(newLensFacing: LensFacing) {
        viewModelScope.launch {
            // apply to cameraSystem
            cameraSystem.setLensFacing(newLensFacing)
        }
    }

    fun setAudioEnabled(shouldEnableAudio: Boolean) {
        viewModelScope.launch {
            cameraSystem.setAudioEnabled(shouldEnableAudio)
        }

        Log.d(
            TAG,
            "Toggle Audio: $shouldEnableAudio"
        )
    }

    fun setPaused(shouldBePaused: Boolean) {
        viewModelScope.launch {
            if (shouldBePaused) {
                cameraSystem.pauseVideoRecording()
            } else {
                cameraSystem.resumeVideoRecording()
            }
        }
    }

    private fun addSnackBarData(snackBarData: SnackbarData) {
        viewModelScope.launch {
            _captureUiState.update { old ->
                if (old !is CaptureUiState.Ready) return@update old
                val newQueue = LinkedList(old.snackBarUiState.snackBarQueue)
                newQueue.add(snackBarData)
                Log.d(TAG, "SnackBar added. Queue size: ${newQueue.size}")
                old.copy(
                    snackBarUiState = SnackBarUiState.from(newQueue)
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

    private fun nextSaveLocation(): Pair<SaveLocation, IntProgress?> = when (externalCaptureMode) {
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
        ExternalCaptureMode.Standard ->
            Pair(SaveLocation.Default, null)
    }

    fun captureImage(contentResolver: ContentResolver) {
        if (captureUiState.value is CaptureUiState.Ready &&
            (captureUiState.value as CaptureUiState.Ready).externalCaptureMode ==
            ExternalCaptureMode.VideoCapture
        ) {
            enqueueExternalImageCaptureUnsupportedSnackBar()
            return
        }

        if (captureUiState.value is CaptureUiState.Ready &&
            (captureUiState.value as CaptureUiState.Ready).externalCaptureMode ==
            ExternalCaptureMode.VideoCapture
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
        Log.d(TAG, "captureImage")
        viewModelScope.launch {
            val (saveLocation, progress) = nextSaveLocation()
            captureImageInternal(
                doTakePicture = {
                    cameraSystem.takePicture(contentResolver, saveLocation) {
                        _captureUiState.update { old ->
                            (old as? CaptureUiState.Ready)?.copy(
                                previewDisplayUiState = PreviewDisplayUiState(
                                    lastBlinkTimeStamp = System.currentTimeMillis(),
                                    aspectRatioUiState = old.aspectRatioUiState
                                )
                            ) ?: old
                        }
                    }.savedUri
                },
                onSuccess = { savedUri ->
                    updateLastCapturedMedia()
                    val event = if (progress != null) {
                        ImageCaptureEvent.SequentialImageSaved(savedUri, progress)
                    } else {
                        ImageCaptureEvent.SingleImageSaved(savedUri)
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
            Log.d(TAG, "cameraSystem.takePicture success")
            SnackbarData(
                cookie = cookie,
                stringResource = R.string.toast_image_capture_success,
                withDismissAction = true,
                testTag = IMAGE_CAPTURE_SUCCESS_TAG
            )
        } catch (exception: Exception) {
            onFailure(exception)
            Log.d(TAG, "cameraSystem.takePicture error", exception)
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

    fun startVideoRecording() {
        if (captureUiState.value is CaptureUiState.Ready &&
            (captureUiState.value as CaptureUiState.Ready).externalCaptureMode ==
            ExternalCaptureMode.ImageCapture
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
            val (saveLocation, _) = nextSaveLocation()
            try {
                cameraSystem.startVideoRecording(saveLocation) {
                    var snackbarToShow: SnackbarData?
                    when (it) {
                        is OnVideoRecordEvent.OnVideoRecorded -> {
                            Log.d(TAG, "cameraSystem.startRecording OnVideoRecorded")
                            _captureEvents.trySend(VideoCaptureEvent.VideoSaved(it.savedUri))
                            snackbarToShow = SnackbarData(
                                cookie = cookie,
                                stringResource = R.string.toast_video_capture_success,
                                withDismissAction = true,
                                testTag = VIDEO_CAPTURE_SUCCESS_TAG
                            )
                            updateLastCapturedMedia()
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

                    addSnackBarData(snackbarToShow)
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

    /**
     "Locks" the video recording such that the user no longer needs to keep their finger pressed on the capture button
     */
    fun setLockedRecording(isLocked: Boolean) {
        trackedPreviewUiState.update { old ->
            old.copy(isRecordingLocked = isLocked)
        }
    }
    fun setZoomAnimationState(targetValue: Float?) {
        trackedPreviewUiState.update { old ->
            old.copy(zoomAnimationTarget = targetValue)
        }
    }

    fun changeZoomRatio(newZoomState: CameraZoomRatio) {
        cameraSystem.changeZoomRatio(newZoomState = newZoomState)
    }

    fun setTestPattern(newTestPattern: TestPattern) {
        cameraSystem.setTestPattern(newTestPattern = newTestPattern)
    }

    fun setDynamicRange(dynamicRange: DynamicRange) {
        if (externalCaptureMode != ExternalCaptureMode.ImageCapture &&
            externalCaptureMode != ExternalCaptureMode.MultipleImageCapture
        ) {
            viewModelScope.launch {
                cameraSystem.setDynamicRange(dynamicRange)
            }
        }
    }

    fun setConcurrentCameraMode(concurrentCameraMode: ConcurrentCameraMode) {
        viewModelScope.launch {
            cameraSystem.setConcurrentCameraMode(concurrentCameraMode)
        }
    }

    fun setImageFormat(imageFormat: ImageOutputFormat) {
        if (externalCaptureMode != ExternalCaptureMode.VideoCapture) {
            viewModelScope.launch {
                cameraSystem.setImageFormat(imageFormat)
            }
        }
    }

    fun setCaptureMode(captureMode: CaptureMode) {
        viewModelScope.launch {
            cameraSystem.setCaptureMode(captureMode)
        }
    }

    fun toggleQuickSettings() {
        trackedPreviewUiState.update { old ->
            old.copy(isQuickSettingsOpen = !old.isQuickSettingsOpen)
        }
    }

    fun toggleDebugOverlay() {
        trackedPreviewUiState.update { old ->
            old.copy(isDebugOverlayOpen = !old.isDebugOverlayOpen)
        }
    }

    fun tapToFocus(x: Float, y: Float) {
        Log.d(TAG, "tapToFocus")
        viewModelScope.launch {
            cameraSystem.tapToFocus(x, y)
        }
    }

    fun onSnackBarResult(cookie: String) {
        viewModelScope.launch {
            _captureUiState.update { old ->
                (old as? CaptureUiState.Ready)?.let { readyState ->
                    val newQueue = LinkedList(readyState.snackBarUiState.snackBarQueue)
                    val snackBarData = newQueue.remove()
                    if (snackBarData != null && snackBarData.cookie == cookie) {
                        // If the latest snackBar had a result, then clear snackBarToShow
                        Log.d(TAG, "SnackBar removed. Queue size: ${newQueue.size}")
                        readyState.copy(
                            snackBarUiState = SnackBarUiState.from(newQueue)
                        )
                    } else {
                        readyState
                    }
                } ?: old
            }
        }
    }
    fun setClearUiScreenBrightness(brightness: Float) {
        screenFlash.setClearUiScreenBrightness(brightness)
    }

    fun setDisplayRotation(deviceRotation: DeviceRotation) {
        viewModelScope.launch {
            cameraSystem.setDeviceRotation(deviceRotation)
        }
    }

    /**
     * Data class to track UI-specific states within the PreviewViewModel.
     *
     * This state is managed by the ViewModel and can be thought of as UI configuration
     * or interaction states that might otherwise have been handled by Compose's
     * `remember` if not hoisted to the ViewModel for broader logic integration
     * or persistence. It is then transformed into the `PreviewUiState` that the UI
     * directly observes.
     */
    data class TrackedPreviewUiState(
        val isQuickSettingsOpen: Boolean = false,
        val isDebugOverlayOpen: Boolean = false,
        val isRecordingLocked: Boolean = false,
        val zoomAnimationTarget: Float? = null
    )
}
