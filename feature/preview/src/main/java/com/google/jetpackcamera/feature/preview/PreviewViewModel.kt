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
import android.util.Range
import android.util.Size
import androidx.camera.core.SurfaceRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tracing.Trace
import androidx.tracing.traceAsync
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.core.camera.CameraUseCase
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.core.common.traceFirstFramePreview
import com.google.jetpackcamera.data.media.MediaRepository
import com.google.jetpackcamera.ui.uistate.viewfinder.IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
import com.google.jetpackcamera.ui.uistate.viewfinder.IMAGE_CAPTURE_FAILURE_TAG
import com.google.jetpackcamera.ui.uistate.viewfinder.IMAGE_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.feature.preview.ui.ImageWellUiState
import com.google.jetpackcamera.feature.preview.ui.SnackbarData
import com.google.jetpackcamera.ui.uistate.viewfinder.VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
import com.google.jetpackcamera.ui.uistate.viewfinder.VIDEO_CAPTURE_FAILURE_TAG
import com.google.jetpackcamera.settings.ConstraintsRepository
import com.google.jetpackcamera.settings.SettingsRepository
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraZoomRatio
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.ConcurrentCameraMode
import com.google.jetpackcamera.settings.model.DeviceRotation
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.StabilizationMode
import com.google.jetpackcamera.settings.model.StreamConfig
import com.google.jetpackcamera.settings.model.SystemConstraints
import com.google.jetpackcamera.ui.uistate.viewfinder.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.ReasonDisplayable
import com.google.jetpackcamera.ui.uistate.viewfinder.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.VIDEO_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.ui.uistate.viewfinder.compound.QuickSettingsUiState
import com.google.jetpackcamera.ui.uistateadapter.viewfinder.AspectRatioUiStateAdapter
import com.google.jetpackcamera.ui.uistateadapter.viewfinder.CaptureModeUiStateAdapter
import com.google.jetpackcamera.ui.uistateadapter.viewfinder.CaptureModeUiStateAdapter.getCaptureToggleUiState
import com.google.jetpackcamera.ui.uistateadapter.viewfinder.ConcurrentCameraUiStateAdapter
import com.google.jetpackcamera.ui.uistateadapter.viewfinder.FlashModeUiStateAdapter
import com.google.jetpackcamera.ui.uistateadapter.viewfinder.FlashModeUiStateAdapter.updateFrom
import com.google.jetpackcamera.ui.uistateadapter.viewfinder.FlipLensUiStateAdapter
import com.google.jetpackcamera.ui.uistateadapter.viewfinder.HdrUiStateAdapter
import com.google.jetpackcamera.ui.uistateadapter.viewfinder.StreamConfigsUiStateAdapter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.LinkedList
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
    @Assisted val previewMode: PreviewMode,
    @Assisted val isDebugMode: Boolean,
    private val cameraUseCase: CameraUseCase,
    private val settingsRepository: SettingsRepository,
    private val constraintsRepository: ConstraintsRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {
    private val _previewUiState: MutableStateFlow<PreviewUiState> =
        MutableStateFlow(PreviewUiState.NotReady)
    private val lockedRecordingState: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val previewUiState: StateFlow<PreviewUiState> =
        _previewUiState.asStateFlow()

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
                .applyPreviewMode(previewMode),
            isDebugMode = isDebugMode
        ) { cameraPropertiesJSON = it }
    }

    /**
     * updates the capture mode based on the preview mode
     */
    private fun CameraAppSettings.applyPreviewMode(previewMode: PreviewMode): CameraAppSettings {
        val captureMode = previewMode.toCaptureMode()
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
                val captureModeUiState = CaptureModeUiStateAdapter.getCaptureModeUiState(
                    systemConstraints,
                    cameraAppSettings,
                    previewMode.convertForUiState()
                )
                val flipLensUiState = FlipLensUiStateAdapter.getUiState(
                    cameraAppSettings,
                    systemConstraints
                )
                var quickSettingsIsOpen: Boolean
                _previewUiState.update { old ->
                    when (old) {
                        is PreviewUiState.NotReady -> {
                            flashModeUiState = FlashModeUiStateAdapter.getUiState(
                                cameraAppSettings,
                                systemConstraints
                            )
                            quickSettingsIsOpen = false
                            // This is the first PreviewUiState.Ready. Create the initial
                            // PreviewUiState.Ready from defaults and initialize it below.
                            PreviewUiState.Ready()
                        }

                        is PreviewUiState.Ready -> {
                            flashModeUiState = old.flashModeUiState.updateFrom(
                                currentCameraSettings = cameraAppSettings,
                                currentConstraints = systemConstraints,
                                cameraState = cameraState
                            )
                            quickSettingsIsOpen = when (old.quickSettingsUiState) {
                                is QuickSettingsUiState.Available -> {
                                    old.quickSettingsUiState.quickSettingsIsOpen
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
                        previewMode = previewMode,
                        currentCameraSettings = cameraAppSettings.applyPreviewMode(previewMode),
                        systemConstraints = systemConstraints,
                        videoRecordingState = cameraState.videoRecordingState,
                        flipLensUiState = flipLensUiState,
                        quickSettingsUiState = getQuickSettingsUiState(
                            captureModeUiState,
                            flashModeUiState,
                            flipLensUiState,
                            cameraAppSettings,
                            systemConstraints,
                            quickSettingsIsOpen
                        ),
                        sessionFirstFrameTimestamp = cameraState.sessionFirstFrameTimestamp,
                        currentLogicalCameraId = cameraState.debugInfo.logicalCameraId,
                        currentPhysicalCameraId = cameraState.debugInfo.physicalCameraId,
                        debugUiState = DebugUiState(
                            cameraPropertiesJSON = cameraPropertiesJSON,
                            videoResolution = Size(
                                cameraState.videoQualityInfo.width,
                                cameraState.videoQualityInfo.height
                            ),
                            isDebugMode = isDebugMode
                        ),
                        stabilizationUiState = stabilizationUiStateFrom(
                            cameraAppSettings,
                            cameraState
                        ),
                        flashModeUiState = flashModeUiState,
                        videoQuality = cameraState.videoQualityInfo.quality,
                        audioUiState = getAudioUiState(
                            cameraAppSettings.audioEnabled,
                            cameraState.videoRecordingState
                        ),
                        elapsedTimeUiState = getElapsedTimeUiState(
                            cameraState.videoRecordingState
                        ),
                        captureButtonUiState = getCaptureButtonUiState(
                            cameraAppSettings,
                            cameraState,
                            lockedState
                        ),
                        zoomUiState = getZoomUiState(
                            systemConstraints,
                            cameraAppSettings.cameraLensFacing,
                            cameraState
                        ),
                        captureModeToggleUiState = getCaptureToggleUiState(
                            systemConstraints,
                            cameraAppSettings,
                            cameraState,
                            previewMode.convertForUiState()
                        ),
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
        quickSettingsIsOpen: Boolean
    ): QuickSettingsUiState {
        return QuickSettingsUiState.Available(
            aspectRatioUiState = AspectRatioUiStateAdapter.getUiState(cameraAppSettings),
            captureModeUiState = captureModeUiState,
            concurrentCameraUiState = ConcurrentCameraUiStateAdapter.getUiState(
                cameraAppSettings,
                systemConstraints,
                previewMode.convertForUiState(),
                captureModeUiState
            ),
            flashModeUiState = flashModeUiState,
            flipLensUiState = flipLensUiState,
            hdrUiState = HdrUiStateAdapter.getUiState(
                cameraAppSettings,
                systemConstraints,
                previewMode.convertForUiState()
            ),
            streamConfigUiState = StreamConfigsUiStateAdapter.getUiState(cameraAppSettings),
            quickSettingsIsOpen = quickSettingsIsOpen
        )
    }

    fun updateLastCapturedMedia() {
        viewModelScope.launch {
            val lastCapturedMediaDescriptor = mediaRepository.getLastCapturedMedia()
            _previewUiState.update { old ->
                (old as? PreviewUiState.Ready)?.copy(
                    imageWellUiState = ImageWellUiState.LastCapture(
                        mediaDescriptor = lastCapturedMediaDescriptor
                    )
                ) ?: old
            }
        }
    }

    private fun getElapsedTimeUiState(
        videoRecordingState: VideoRecordingState
    ): ElapsedTimeUiState = when (videoRecordingState) {
        is VideoRecordingState.Active ->
            ElapsedTimeUiState.Enabled(videoRecordingState.elapsedTimeNanos)

        is VideoRecordingState.Inactive ->
            ElapsedTimeUiState.Enabled(videoRecordingState.finalElapsedTimeNanos)

        VideoRecordingState.Starting -> ElapsedTimeUiState.Enabled(0L)
    }

    /**
     * Updates the FlashModeUiState based on the changes in flash mode or constraints
     */

    private fun getAudioUiState(
        isAudioEnabled: Boolean,
        videoRecordingState: VideoRecordingState
    ): AudioUiState = if (isAudioEnabled) {
        if (videoRecordingState is VideoRecordingState.Active) {
            AudioUiState.Enabled.On(videoRecordingState.audioAmplitude)
        } else {
            AudioUiState.Enabled.On(0.0)
        }
    } else {
        AudioUiState.Enabled.Mute
    }

    private fun stabilizationUiStateFrom(
        cameraAppSettings: CameraAppSettings,
        cameraState: CameraState
    ): StabilizationUiState {
        val expectedMode = cameraAppSettings.stabilizationMode
        val actualMode = cameraState.stabilizationMode
        check(actualMode != StabilizationMode.AUTO) {
            "CameraState should never resolve to AUTO stabilization mode"
        }
        return when (expectedMode) {
            StabilizationMode.OFF -> StabilizationUiState.Disabled
            StabilizationMode.AUTO -> {
                if (actualMode !in setOf(StabilizationMode.ON, StabilizationMode.OPTICAL)) {
                    StabilizationUiState.Disabled
                } else {
                    StabilizationUiState.Auto(actualMode)
                }
            }

            StabilizationMode.ON,
            StabilizationMode.HIGH_QUALITY,
            StabilizationMode.OPTICAL ->
                StabilizationUiState.Specific(
                    stabilizationMode = expectedMode,
                    active = expectedMode == actualMode
                )
        }
    }

    private fun PreviewMode.toCaptureMode() = when (this) {
        is PreviewMode.ExternalImageCaptureMode -> CaptureMode.IMAGE_ONLY
        is PreviewMode.ExternalMultipleImageCaptureMode -> CaptureMode.IMAGE_ONLY
        is PreviewMode.ExternalVideoCaptureMode -> CaptureMode.VIDEO_ONLY
        is PreviewMode.StandardMode -> CaptureMode.STANDARD
    }

    private fun PreviewMode.convertForUiState() = when (this) {
        is PreviewMode.ExternalImageCaptureMode ->
            com.google.jetpackcamera.ui.uistateadapter.viewfinder.PreviewMode.EXTERNAL_IMAGE_CAPTURE

        is PreviewMode.ExternalMultipleImageCaptureMode ->
            com.google.jetpackcamera.ui.uistateadapter.viewfinder.PreviewMode
                .EXTERNAL_MULTIPLE_IMAGE_CAPTURE

        is PreviewMode.ExternalVideoCaptureMode ->
            com.google.jetpackcamera.ui.uistateadapter.viewfinder.PreviewMode.EXTERNAL_VIDEO_CAPTURE

        is PreviewMode.StandardMode ->
            com.google.jetpackcamera.ui.uistateadapter.viewfinder.PreviewMode.STANDARD
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

    fun getCaptureButtonUiState(
        cameraAppSettings: CameraAppSettings,
        cameraState: CameraState,
        lockedState: Boolean
    ): CaptureButtonUiState = when (cameraState.videoRecordingState) {
        // if not currently recording, check capturemode to determine idle capture button UI
        is VideoRecordingState.Inactive ->
            CaptureButtonUiState
                .Enabled.Idle(captureMode = cameraAppSettings.captureMode)

        // display different capture button UI depending on if recording is pressed or locked
        is VideoRecordingState.Active.Recording, is VideoRecordingState.Active.Paused ->
            if (lockedState) {
                CaptureButtonUiState.Enabled.Recording.LockedRecording
            } else {
                CaptureButtonUiState.Enabled.Recording.PressedRecording
            }

        VideoRecordingState.Starting ->
            CaptureButtonUiState
                .Enabled.Idle(captureMode = cameraAppSettings.captureMode)
    }

    private fun getZoomUiState(
        systemConstraints: SystemConstraints,
        lensFacing: LensFacing,
        cameraState: CameraState
    ): ZoomUiState = ZoomUiState.Enabled(
        primaryZoomRange =
        systemConstraints.perLensConstraints[lensFacing]?.supportedZoomRange
            ?: Range<Float>(1f, 1f),
        primaryZoomRatio = cameraState.zoomRatios[lensFacing],
        primaryLinearZoom = cameraState.linearZoomScales[lensFacing]
    )

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
            _previewUiState.update { old ->
                val newQueue = LinkedList((old as? PreviewUiState.Ready)?.snackBarQueue!!)
                newQueue.add(snackBarData)
                Log.d(TAG, "SnackBar added. Queue size: ${newQueue.size}")
                (old as? PreviewUiState.Ready)?.copy(
                    snackBarQueue = newQueue
                ) ?: old
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
        if (previewUiState.value is PreviewUiState.Ready &&
            (previewUiState.value as PreviewUiState.Ready).previewMode is
                PreviewMode.ExternalVideoCaptureMode
        ) {
            enqueueExternalImageCaptureUnsupportedSnackBar()
            return
        }

        if (previewUiState.value is PreviewUiState.Ready &&
            (previewUiState.value as PreviewUiState.Ready).previewMode is
                PreviewMode.ExternalVideoCaptureMode
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
                    (previewUiState.value as? PreviewUiState.Ready)?.previewMode as?
                        PreviewMode.ExternalMultipleImageCaptureMode
                    )?.let {
                    val uri = if (ignoreUri || it.imageCaptureUris.isNullOrEmpty()) {
                        null
                    } else {
                        it.imageCaptureUris[externalUriIndex]
                    }
                    Pair(externalUriIndex, uri)
                } ?: Pair(-1, imageCaptureUri)
            captureImageInternal(
                doTakePicture = {
                    cameraUseCase.takePicture({
                        _previewUiState.update { old ->
                            (old as? PreviewUiState.Ready)?.copy(
                                lastBlinkTimeStamp = System.currentTimeMillis()
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
            (previewUiState.value as? PreviewUiState.Ready)
                ?.previewMode as? PreviewMode.ExternalMultipleImageCaptureMode
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

    fun enqueueDisabledHdrToggleSnackBar(disabledReason: ReasonDisplayable) {
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
        if (previewUiState.value is PreviewUiState.Ready &&
            (previewUiState.value as PreviewUiState.Ready).previewMode is
                PreviewMode.ExternalImageCaptureMode
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
        if (previewMode !is PreviewMode.ExternalImageCaptureMode &&
            previewMode !is PreviewMode.ExternalMultipleImageCaptureMode
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
        if (previewMode !is PreviewMode.ExternalVideoCaptureMode) {
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
            _previewUiState.update { old ->
                (old as? PreviewUiState.Ready)?.copy(
                    quickSettingsUiState = QuickSettingsUiState.Available(
                        (old.quickSettingsUiState as QuickSettingsUiState.Available).aspectRatioUiState,
                        old.quickSettingsUiState.captureModeUiState,
                        old.quickSettingsUiState.concurrentCameraUiState,
                        old.quickSettingsUiState.flashModeUiState,
                        old.quickSettingsUiState.flipLensUiState,
                        old.quickSettingsUiState.hdrUiState,
                        old.quickSettingsUiState.streamConfigUiState,
                        !old.quickSettingsUiState.quickSettingsIsOpen
                    ),
                ) ?: old
            }
        }
    }

    fun toggleDebugOverlay() {
        viewModelScope.launch {
            _previewUiState.update { old ->
                (old as? PreviewUiState.Ready)?.copy(
                    debugUiState = DebugUiState(
                        old.debugUiState.cameraPropertiesJSON,
                        old.debugUiState.videoResolution,
                        old.debugUiState.isDebugMode,
                        !old.debugUiState.isDebugOverlayOpen
                    )
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
                (old as? PreviewUiState.Ready)?.snackBarQueue!!.let {
                    val newQueue = LinkedList(it)
                    val snackBarData = newQueue.remove()
                    if (snackBarData != null && snackBarData.cookie == cookie) {
                        // If the latest snackBar had a result, then clear snackBarToShow
                        Log.d(TAG, "SnackBar removed. Queue size: ${newQueue.size}")
                        old.copy(snackBarQueue = newQueue)
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
        fun create(previewMode: PreviewMode, isDebugMode: Boolean): PreviewViewModel
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
