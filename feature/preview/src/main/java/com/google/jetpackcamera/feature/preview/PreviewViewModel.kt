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

import android.net.Uri
import android.util.Log
import androidx.camera.core.SurfaceRequest
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.core.camera.CameraSystem.Companion.applyDiffs
import com.google.jetpackcamera.core.common.DefaultSaveMode
import com.google.jetpackcamera.data.media.MediaRepository
import com.google.jetpackcamera.feature.preview.navigation.getCaptureUris
import com.google.jetpackcamera.feature.preview.navigation.getDebugSettings
import com.google.jetpackcamera.feature.preview.navigation.getExternalCaptureMode
import com.google.jetpackcamera.feature.preview.navigation.getRequestedSaveMode
import com.google.jetpackcamera.model.CaptureEvent
import com.google.jetpackcamera.model.DebugSettings
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.IntProgress
import com.google.jetpackcamera.model.LowLightBoostState
import com.google.jetpackcamera.model.SaveLocation
import com.google.jetpackcamera.model.SaveMode
import com.google.jetpackcamera.settings.ConstraintsRepository
import com.google.jetpackcamera.settings.SettingsRepository
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.applyExternalCaptureMode
import com.google.jetpackcamera.ui.components.capture.LOW_LIGHT_BOOST_FAILURE_TAG
import com.google.jetpackcamera.ui.components.capture.R
import com.google.jetpackcamera.ui.components.capture.ScreenFlash
import com.google.jetpackcamera.ui.components.capture.SnackBarController
import com.google.jetpackcamera.ui.components.capture.SnackBarControllerImpl
import com.google.jetpackcamera.ui.components.capture.controller.CameraController
import com.google.jetpackcamera.ui.components.capture.controller.CameraControllerImpl
import com.google.jetpackcamera.ui.components.capture.controller.CaptureController
import com.google.jetpackcamera.ui.components.capture.controller.CaptureControllerImpl
import com.google.jetpackcamera.ui.components.capture.controller.CaptureScreenController
import com.google.jetpackcamera.ui.components.capture.controller.CaptureScreenControllerImpl
import com.google.jetpackcamera.ui.components.capture.controller.ZoomController
import com.google.jetpackcamera.ui.components.capture.controller.ZoomControllerImpl
import com.google.jetpackcamera.ui.components.capture.debug.controller.DebugController
import com.google.jetpackcamera.ui.components.capture.debug.controller.DebugControllerImpl
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
import javax.inject.Inject
import kotlinx.coroutines.Deferred
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
import kotlinx.coroutines.launch

private const val TAG = "PreviewViewModel"

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
    private val _trackedCaptureUiState: MutableStateFlow<TrackedCaptureUiState> =
        MutableStateFlow(TrackedCaptureUiState())
    private val _snackBarUiState: MutableStateFlow<SnackBarUiState.Enabled> =
        MutableStateFlow(SnackBarUiState.Enabled())
    val snackBarUiState: StateFlow<SnackBarUiState.Enabled> =
        _snackBarUiState.asStateFlow()

    val surfaceRequest: StateFlow<SurfaceRequest?> = cameraSystem.getSurfaceRequest()

    private val _captureEvents = Channel<CaptureEvent>()
    val captureEvents: ReceiveChannel<CaptureEvent> = _captureEvents

    private val externalCaptureMode: ExternalCaptureMode = savedStateHandle.getExternalCaptureMode()
    private val externalUris: List<Uri> = savedStateHandle.getCaptureUris()
    private lateinit var externalUriProgress: IntProgress

    private val debugSettings: DebugSettings = savedStateHandle.getDebugSettings()

    private var cameraPropertiesJSON = ""

    val screenFlash = ScreenFlash(cameraSystem, viewModelScope)

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
        _trackedCaptureUiState,
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
        _trackedCaptureUiState
    )
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DebugUiState.Disabled
        )

    val quickSettingsController: QuickSettingsController = QuickSettingsControllerImpl(
        trackedCaptureUiState = _trackedCaptureUiState,
        viewModelScope = viewModelScope,
        cameraSystem = cameraSystem,
        externalCaptureMode = externalCaptureMode
    )

    val debugController: DebugController = DebugControllerImpl(
        cameraSystem = cameraSystem,
        trackedCaptureUiState = _trackedCaptureUiState
    )

    val snackBarController: SnackBarController = SnackBarControllerImpl(
        viewModelScope = viewModelScope,
        snackBarUiState = _snackBarUiState
    )

    val captureScreenController: CaptureScreenController = CaptureScreenControllerImpl(
        viewModelScope = viewModelScope,
        cameraSystem = cameraSystem,
        trackedCaptureUiState = _trackedCaptureUiState,
        mediaRepository = mediaRepository,
        captureUiState = captureUiState
    )

    val zoomController: ZoomController = ZoomControllerImpl(
        cameraSystem = cameraSystem,
        trackedCaptureUiState = _trackedCaptureUiState
    )

    val cameraController: CameraController = CameraControllerImpl(
        initializationDeferred = initializationDeferred,
        captureUiState = captureUiState,
        viewModelScope = viewModelScope,
        cameraSystem = cameraSystem
    )

    val captureController: CaptureController = CaptureControllerImpl(
        trackedCaptureUiState = _trackedCaptureUiState,
        captureUiState = captureUiState,
        viewModelScope = viewModelScope,
        cameraSystem = cameraSystem,
        mediaRepository = mediaRepository,
        saveMode = saveMode,
        externalCaptureMode = externalCaptureMode,
        externalCapturesCallback = {
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
        },
        captureEvents = _captureEvents,
        captureScreenController = captureScreenController,
        snackBarController = snackBarController
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
}
