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
package com.google.jetpackcamera.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetpackcamera.settings.DisabledRationale.DeviceUnsupportedRationale
import com.google.jetpackcamera.settings.DisabledRationale.FpsUnsupportedRationale
import com.google.jetpackcamera.settings.DisabledRationale.StabilizationUnsupportedRationale
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.DarkMode
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.StabilizationMode
import com.google.jetpackcamera.settings.model.SystemConstraints
import com.google.jetpackcamera.settings.model.VideoQuality
import com.google.jetpackcamera.settings.model.forCurrentLens
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "SettingsViewModel"
val fpsOptions = setOf(FPS_15, FPS_30, FPS_60)
val previewStabilizationUnsupportedFps = setOf(FPS_15, FPS_60)
val videoStabilizationUnsupportedFps = setOf(FPS_60)

/**
 * [ViewModel] for [SettingsScreen].
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    constraintsRepository: ConstraintsRepository
) : ViewModel() {

    val settingsUiState: StateFlow<SettingsUiState> =
        combine(
            settingsRepository.defaultCameraAppSettings,
            constraintsRepository.systemConstraints.filterNotNull()
        ) { updatedSettings, constraints ->
            updatedSettings.videoQuality
            SettingsUiState.Enabled(
                aspectRatioUiState = AspectRatioUiState.Enabled(updatedSettings.aspectRatio),
                captureModeUiState = CaptureModeUiState.Enabled(updatedSettings.captureMode),
                maxVideoDurationUiState = MaxVideoDurationUiState.Enabled(
                    updatedSettings.maxVideoDurationMillis
                ),
                flashUiState = FlashUiState.Enabled(updatedSettings.flashMode),
                darkModeUiState = DarkModeUiState.Enabled(updatedSettings.darkMode),
                fpsUiState = getFpsUiState(constraints, updatedSettings),
                lensFlipUiState = getLensFlipUiState(constraints, updatedSettings),
                stabilizationUiState = getStabilizationUiState(constraints, updatedSettings),
                videoQualityUiState = getVideoQualityUiState(constraints, updatedSettings)
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState.Disabled
        )

    private fun getStabilizationUiState(
        systemConstraints: SystemConstraints,
        cameraAppSettings: CameraAppSettings
    ): StabilizationUiState {
        val deviceStabilizations: Set<StabilizationMode> =
            systemConstraints
                .perLensConstraints.values
                .asSequence()
                .flatMap { it.supportedStabilizationModes }
                .toSet()

        fun supportsStabilization(stabilizationModes: Collection<StabilizationMode>): Boolean =
            stabilizationModes.isNotEmpty() &&
                stabilizationModes.toSet() != setOf(StabilizationMode.OFF)

        // if no lens supports stabilization
        if (!supportsStabilization(deviceStabilizations)) {
            return StabilizationUiState.Disabled(
                DeviceUnsupportedRationale(
                    R.string.stabilization_rationale_prefix
                )
            )
        }

        // if a lens supports any stabilization but it isn't the current
        val lensSupportedStabilizations = systemConstraints.forCurrentLens(
            cameraAppSettings
        )?.supportedStabilizationModes
        lensSupportedStabilizations?.let {
            if (!supportsStabilization(it)) {
                return StabilizationUiState.Disabled(
                    getLensUnsupportedRationale(
                        cameraAppSettings.cameraLensFacing,
                        R.string.stabilization_rationale_prefix
                    )
                )
            }
        }

        // if fps is too high for any stabilization
        if (cameraAppSettings.targetFrameRate >= FPS_60) {
            return StabilizationUiState.Disabled(
                FpsUnsupportedRationale(
                    R.string.stabilization_rationale_prefix,
                    FPS_60
                )
            )
        }

        return StabilizationUiState.Enabled(
            currentStabilizationMode = cameraAppSettings.stabilizationMode,
            stabilizationAutoState = getSingleStabilizationState(
                stabilizationMode = StabilizationMode.AUTO,
                currentFrameRate = cameraAppSettings.targetFrameRate,
                defaultLensFacing = cameraAppSettings.cameraLensFacing,
                deviceStabilizations = deviceStabilizations,
                currentLensStabilizations = lensSupportedStabilizations,
                unsupportedFrameRates = emptySet()
            ),
            stabilizationOnState = getSingleStabilizationState(
                stabilizationMode = StabilizationMode.ON,
                currentFrameRate = cameraAppSettings.targetFrameRate,
                defaultLensFacing = cameraAppSettings.cameraLensFacing,
                deviceStabilizations = deviceStabilizations,
                currentLensStabilizations = lensSupportedStabilizations,
                unsupportedFrameRates = previewStabilizationUnsupportedFps
            ),
            stabilizationHighQualityState = getSingleStabilizationState(
                stabilizationMode = StabilizationMode.HIGH_QUALITY,
                currentFrameRate = cameraAppSettings.targetFrameRate,
                defaultLensFacing = cameraAppSettings.cameraLensFacing,
                deviceStabilizations = deviceStabilizations,
                currentLensStabilizations = lensSupportedStabilizations,
                unsupportedFrameRates = videoStabilizationUnsupportedFps
            )
        )
    }

    private fun getSingleStabilizationState(
        stabilizationMode: StabilizationMode,
        currentFrameRate: Int,
        defaultLensFacing: LensFacing,
        deviceStabilizations: Set<StabilizationMode>,
        currentLensStabilizations: Set<StabilizationMode>?,
        unsupportedFrameRates: Set<Int>
    ): SingleSelectableState {
        // if unsupported by device
        if (!deviceStabilizations.contains(stabilizationMode)) {
            return SingleSelectableState.Disabled(
                disabledRationale =
                DeviceUnsupportedRationale(R.string.stabilization_rationale_prefix)
            )
        }

        // if unsupported by by current lens
        if (currentLensStabilizations?.contains(stabilizationMode) == false) {
            return SingleSelectableState.Disabled(
                getLensUnsupportedRationale(
                    defaultLensFacing,
                    R.string.stabilization_rationale_prefix
                )
            )
        }

        // if fps is unsupported by preview stabilization
        if (currentFrameRate in unsupportedFrameRates) {
            return SingleSelectableState.Disabled(
                FpsUnsupportedRationale(
                    R.string.stabilization_rationale_prefix,
                    currentFrameRate
                )
            )
        }

        return SingleSelectableState.Selectable
    }

    /**
     * Enables or disables default camera switch based on:
     * - number of cameras available
     * - if there is a front and rear camera, the camera that the setting would switch to must also
     * support the other settings
     * */
    private fun getLensFlipUiState(
        systemConstraints: SystemConstraints,
        currentSettings: CameraAppSettings
    ): FlipLensUiState {
        // if there is only one lens, stop here
        if (!with(systemConstraints.availableLenses) {
                size > 1 && contains(com.google.jetpackcamera.settings.model.LensFacing.FRONT)
            }
        ) {
            return FlipLensUiState.Disabled(
                currentLensFacing = currentSettings.cameraLensFacing,
                disabledRationale =
                DeviceUnsupportedRationale(
                    // display the lens that isnt supported
                    when (currentSettings.cameraLensFacing) {
                        LensFacing.BACK -> R.string.front_lens_rationale_prefix
                        LensFacing.FRONT -> R.string.rear_lens_rationale_prefix
                    }
                )
            )
        }

        // If multiple lens available, continue
        val newLensFacing = if (currentSettings.cameraLensFacing == LensFacing.FRONT) {
            LensFacing.BACK
        } else {
            LensFacing.FRONT
        }
        val newLensConstraints = systemConstraints.perLensConstraints[newLensFacing]!!
        // make sure all current settings wont break constraint when changing new default lens

        // if new lens won't support current fps
        if (currentSettings.targetFrameRate != FPS_AUTO &&
            !newLensConstraints.supportedFixedFrameRates
                .contains(currentSettings.targetFrameRate)
        ) {
            return FlipLensUiState.Disabled(
                currentLensFacing = currentSettings.cameraLensFacing,
                disabledRationale = FpsUnsupportedRationale(
                    when (currentSettings.cameraLensFacing) {
                        LensFacing.BACK -> R.string.front_lens_rationale_prefix
                        LensFacing.FRONT -> R.string.rear_lens_rationale_prefix
                    },
                    currentSettings.targetFrameRate
                )
            )
        }

        // if preview stabilization is currently on and the other lens won't support it
        if (currentSettings.stabilizationMode == StabilizationMode.ON) {
            if (!newLensConstraints.supportedStabilizationModes.contains(StabilizationMode.ON)) {
                return FlipLensUiState.Disabled(
                    currentLensFacing = currentSettings.cameraLensFacing,
                    disabledRationale = StabilizationUnsupportedRationale(
                        when (currentSettings.cameraLensFacing) {
                            LensFacing.BACK -> R.string.front_lens_rationale_prefix
                            LensFacing.FRONT -> R.string.rear_lens_rationale_prefix
                        }
                    )
                )
            }
        }
        // if video stabilization is currently on and the other lens won't support it
        if (currentSettings.stabilizationMode == StabilizationMode.HIGH_QUALITY) {
            if (!newLensConstraints.supportedStabilizationModes
                    .contains(StabilizationMode.HIGH_QUALITY)
            ) {
                return FlipLensUiState.Disabled(
                    currentLensFacing = currentSettings.cameraLensFacing,
                    disabledRationale = StabilizationUnsupportedRationale(
                        when (currentSettings.cameraLensFacing) {
                            LensFacing.BACK -> R.string.front_lens_rationale_prefix
                            LensFacing.FRONT -> R.string.rear_lens_rationale_prefix
                        }
                    )
                )
            }
        }

        return FlipLensUiState.Enabled(currentLensFacing = currentSettings.cameraLensFacing)
    }

    private fun getFpsUiState(
        systemConstraints: SystemConstraints,
        cameraAppSettings: CameraAppSettings
    ): FpsUiState {
        val optionConstraintRationale: MutableMap<Int, SingleSelectableState> = mutableMapOf()

        val currentLensFrameRates: Set<Int> = systemConstraints
            .perLensConstraints[cameraAppSettings.cameraLensFacing]
            ?.supportedFixedFrameRates ?: emptySet()

        // if device supports no fixed frame rates, disable
        if (currentLensFrameRates.isEmpty()) {
            return FpsUiState.Disabled(
                DeviceUnsupportedRationale(R.string.no_fixed_fps_rationale_prefix)
            )
        }

        // provide selectable states for each of the fps options
        fpsOptions.forEach { fpsOption ->
            val fpsUiState = isFpsOptionEnabled(
                fpsOption,
                cameraAppSettings.cameraLensFacing,
                currentLensFrameRates,
                systemConstraints.perLensConstraints[cameraAppSettings.cameraLensFacing]
                    ?.supportedFixedFrameRates ?: emptySet(),
                cameraAppSettings.stabilizationMode
            )
            if (fpsUiState is SingleSelectableState.Disabled) {
                Log.d(TAG, "fps option $fpsOption disabled. ${fpsUiState.disabledRationale::class}")
            }
            optionConstraintRationale[fpsOption] = fpsUiState
        }
        return FpsUiState.Enabled(
            currentSelection = cameraAppSettings.targetFrameRate,
            fpsAutoState = SingleSelectableState.Selectable,
            fpsFifteenState = optionConstraintRationale[FPS_15]!!,
            fpsThirtyState = optionConstraintRationale[FPS_30]!!,
            fpsSixtyState = optionConstraintRationale[FPS_60]!!
        )
    }

    private fun getVideoQualityUiState(
        systemConstraints: SystemConstraints,
        cameraAppSettings: CameraAppSettings
    ): VideoQualityUiState {
        val cameraConstraints = systemConstraints.forCurrentLens(cameraAppSettings)
        val supportedVideQualities: List<VideoQuality>? =
            cameraConstraints?.supportedVideoQualitiesMap?.get(cameraAppSettings.dynamicRange)
        return if (!supportedVideQualities.isNullOrEmpty()) {
             VideoQualityUiState.Enabled(
                cameraAppSettings.videoQuality,
                supportedVideQualities
            )
        } else {
            VideoQualityUiState.Disabled
        }
    }

    /**
     * Auxiliary function to determine if an FPS option should be disabled or not
     */
    private fun isFpsOptionEnabled(
        fpsOption: Int,
        defaultLensFacing: LensFacing,
        deviceFrameRates: Set<Int>,
        lensFrameRates: Set<Int>,
        stabilizationMode: StabilizationMode
    ): SingleSelectableState {
        // if device doesn't support the fps option, disable
        if (!deviceFrameRates.contains(fpsOption)) {
            return SingleSelectableState.Disabled(
                disabledRationale = DeviceUnsupportedRationale(R.string.fps_rationale_prefix)
            )
        }
        // if the current lens doesn't support the fps, disable
        if (!lensFrameRates.contains(fpsOption)) {
            Log.d(TAG, "FPS disabled for current lens")

            return SingleSelectableState.Disabled(
                getLensUnsupportedRationale(defaultLensFacing, R.string.fps_rationale_prefix)
            )
        }

        // if stabilization is on and the option is incompatible, disable
        val unsupportedFpsForStabilization = when (stabilizationMode) {
            StabilizationMode.ON -> previewStabilizationUnsupportedFps
            StabilizationMode.HIGH_QUALITY -> videoStabilizationUnsupportedFps
            else -> emptySet()
        }

        if (fpsOption in unsupportedFpsForStabilization) {
            return SingleSelectableState.Disabled(
                StabilizationUnsupportedRationale(R.string.fps_rationale_prefix)
            )
        }

        return SingleSelectableState.Selectable
    }

    fun setDefaultLensFacing(lensFacing: LensFacing) {
        viewModelScope.launch {
            settingsRepository.updateDefaultLensFacing(lensFacing)
            Log.d(TAG, "set camera default facing: $lensFacing")
        }
    }

    fun setDarkMode(darkMode: DarkMode) {
        viewModelScope.launch {
            settingsRepository.updateDarkModeStatus(darkMode)
            Log.d(TAG, "set dark mode theme: $darkMode")
        }
    }

    fun setFlashMode(flashMode: FlashMode) {
        viewModelScope.launch {
            settingsRepository.updateFlashModeStatus(flashMode)
            Log.d(TAG, "set flash mode: $flashMode")
        }
    }

    fun setTargetFrameRate(targetFrameRate: Int) {
        viewModelScope.launch {
            settingsRepository.updateTargetFrameRate(targetFrameRate)
            Log.d(TAG, "set target frame rate: $targetFrameRate")
        }
    }

    fun setAspectRatio(aspectRatio: AspectRatio) {
        viewModelScope.launch {
            settingsRepository.updateAspectRatio(aspectRatio)
            Log.d(TAG, "set aspect ratio: $aspectRatio")
        }
    }

    fun setCaptureMode(captureMode: CaptureMode) {
        viewModelScope.launch {
            settingsRepository.updateCaptureMode(captureMode)
            Log.d(TAG, "set default capture mode: $captureMode")
        }
    }

    fun setStabilizationMode(stabilizationMode: StabilizationMode) {
        viewModelScope.launch {
            settingsRepository.updateStabilizationMode(stabilizationMode)
            Log.d(TAG, "set stabilization mode: $stabilizationMode")
        }
    }

    fun setMaxVideoDuration(durationMillis: Long) {
        viewModelScope.launch {
            settingsRepository.updateMaxVideoDuration(durationMillis)
            Log.d(TAG, "set video duration: $durationMillis ms")
        }
    }

    fun setVideoQuality(videoQuality: VideoQuality) {
        viewModelScope.launch {
            settingsRepository.updateVideoQuality(videoQuality)
            Log.d(TAG, "set video quality: $videoQuality ms")
        }
    }
}
