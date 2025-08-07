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

import android.Manifest
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LowLightBoostPriority
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.settings.DisabledRationale.DeviceUnsupportedRationale
import com.google.jetpackcamera.settings.DisabledRationale.FpsUnsupportedRationale
import com.google.jetpackcamera.settings.DisabledRationale.StabilizationUnsupportedRationale
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraConstraints
import com.google.jetpackcamera.settings.model.CameraConstraints.Companion.FPS_15
import com.google.jetpackcamera.settings.model.CameraConstraints.Companion.FPS_30
import com.google.jetpackcamera.settings.model.CameraConstraints.Companion.FPS_60
import com.google.jetpackcamera.settings.model.CameraConstraints.Companion.FPS_AUTO
import com.google.jetpackcamera.settings.model.SystemConstraints
import com.google.jetpackcamera.settings.model.forCurrentLens
import com.google.jetpackcamera.settings.model.forDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "SettingsViewModel"
private val fpsOptions = setOf(FPS_15, FPS_30, FPS_60)

/**
 * [ViewModel] for [SettingsScreen].
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    constraintsRepository: ConstraintsRepository
) : ViewModel() {
    private var grantedPermissions = MutableStateFlow<Set<String>>(emptySet())

    val settingsUiState: StateFlow<SettingsUiState> =
        combine(
            settingsRepository.defaultCameraAppSettings,
            constraintsRepository.systemConstraints.filterNotNull(),
            grantedPermissions
        ) { updatedSettings, constraints, grantedPerms ->
            updatedSettings.videoQuality
            SettingsUiState.Enabled(
                aspectRatioUiState = AspectRatioUiState.Enabled(updatedSettings.aspectRatio),
                streamConfigUiState = StreamConfigUiState.Enabled(updatedSettings.streamConfig),
                maxVideoDurationUiState = MaxVideoDurationUiState.Enabled(
                    updatedSettings.maxVideoDurationMillis
                ),
                flashUiState = getFlashUiState(updatedSettings, constraints),
                darkModeUiState = DarkModeUiState.Enabled(updatedSettings.darkMode),
                audioUiState = getAudioUiState(
                    updatedSettings.audioEnabled,
                    grantedPerms.contains(Manifest.permission.RECORD_AUDIO)
                ),
                fpsUiState = getFpsUiState(constraints, updatedSettings),
                lensFlipUiState = getLensFlipUiState(constraints, updatedSettings),
                stabilizationUiState = getStabilizationUiState(constraints, updatedSettings),
                videoQualityUiState = getVideoQualityUiState(constraints, updatedSettings),
                lowLightBoostPriorityUiState = LowLightBoostPriorityUiState.Enabled(
                    updatedSettings.lowLightBoostPriority
                )
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState.Disabled
        )

// ////////////////////////////////////////////////////////////
//
// Get UiStates for components
//
// ////////////////////////////////////////////////////////////

    private fun getFlashUiState(
        cameraAppSettings: CameraAppSettings,
        constraints: SystemConstraints
    ): FlashUiState {
        val currentSupportedFlashModes =
            constraints.forCurrentLens(cameraAppSettings)?.supportedFlashModes ?: emptySet()

        check(currentSupportedFlashModes.isNotEmpty()) {
            "No flash modes supported. Should at least support OFF."
        }
        val deviceSupportedFlashModes: Set<FlashMode> = constraints.forDevice(
            CameraConstraints::supportedFlashModes
        )
        // disable entire setting when:git status
        //  device only supports off... device unsupported rationale
        //  lens only supports off... lens unsupported rationale
        if (deviceSupportedFlashModes == setOf(FlashMode.OFF)) {
            return FlashUiState.Disabled(
                DeviceUnsupportedRationale(R.string.flash_rationale_prefix)
            )
        } else if (deviceSupportedFlashModes == setOf(FlashMode.OFF)) {
            return FlashUiState.Disabled(
                getLensUnsupportedRationale(
                    cameraAppSettings.cameraLensFacing,
                    R.string.flash_rationale_prefix
                )
            )
        }

        // if options besides off are available for this lens...
        val onSelectableState = if (currentSupportedFlashModes.contains(FlashMode.ON)) {
            SingleSelectableState.Selectable
        } else if (deviceSupportedFlashModes.contains(FlashMode.ON)) {
            SingleSelectableState.Disabled(
                getLensUnsupportedRationale(
                    cameraAppSettings.cameraLensFacing,
                    affectedSettingNameResId = R.string.flash_on_rationale_prefix
                )
            )
        } else {
            SingleSelectableState.Disabled(
                DeviceUnsupportedRationale(R.string.flash_on_rationale_prefix)
            )
        }

        val autoSelectableState = if (currentSupportedFlashModes.contains(FlashMode.AUTO)) {
            SingleSelectableState.Selectable
        } else if (deviceSupportedFlashModes.contains(FlashMode.AUTO)) {
            SingleSelectableState.Disabled(
                getLensUnsupportedRationale(
                    cameraAppSettings.cameraLensFacing,
                    affectedSettingNameResId = R.string.flash_auto_rationale_prefix
                )
            )
        } else {
            SingleSelectableState.Disabled(
                DeviceUnsupportedRationale(R.string.flash_auto_rationale_prefix)
            )
        }

        // check if llb constraints:
        // llb must be supported by device
        val llbSelectableState =
            if (!currentSupportedFlashModes.contains(FlashMode.LOW_LIGHT_BOOST)) {
                SingleSelectableState.Disabled(
                    DeviceUnsupportedRationale(R.string.flash_llb_rationale_prefix)
                )
            } // llb unsupported above 30fps
            else if (cameraAppSettings.targetFrameRate > FPS_30) {
                SingleSelectableState.Disabled(
                    FpsUnsupportedRationale(
                        R.string.flash_llb_rationale_prefix,
                        cameraAppSettings.targetFrameRate
                    )
                )
            } else {
                SingleSelectableState.Selectable
            }

        return FlashUiState.Enabled(
            currentFlashMode = cameraAppSettings.flashMode,
            onSelectableState = onSelectableState,
            autoSelectableState = autoSelectableState,
            lowLightSelectableState = llbSelectableState
        )
    }

    private fun getAudioUiState(isAudioEnabled: Boolean, permissionGranted: Boolean): AudioUiState =
        if (permissionGranted) {
            if (isAudioEnabled) {
                AudioUiState.Enabled.On()
            } else {
                AudioUiState.Enabled.Mute()
            }
        } else {
            AudioUiState.Disabled(
                DisabledRationale
                    .PermissionRecordAudioNotGrantedRationale(
                        R.string.mute_audio_rationale_prefix
                    )
            )
        }

    @OptIn(ExperimentalPermissionsApi::class)
    fun setGrantedPermissions(multiplePermissionsState: MultiplePermissionsState) {
        val permissions = mutableSetOf<String>()
        for (permissionState in multiplePermissionsState.permissions) {
            if (permissionState.status.isGranted) {
                permissions.add(permissionState.permission)
            }
        }
        grantedPermissions.update {
            permissions
        }
    }

    fun setGrantedPermissions(permissions: MutableSet<String>) {
        grantedPermissions.update { permissions }
    }

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
        val currentLensConstraints = checkNotNull(
            systemConstraints.forCurrentLens(cameraAppSettings)
        ) {
            "Lens constraints for ${cameraAppSettings.cameraLensFacing} not available."
        }

        with(currentLensConstraints) {
            supportedStabilizationModes.let {
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
            val maxCommonUnsupportedFps = currentLensConstraints.unsupportedStabilizationFpsMap
                .asSequence()
                .filter {
                    it.key != StabilizationMode.AUTO &&
                        it.key != StabilizationMode.OFF &&
                        it.key in currentLensConstraints.supportedStabilizationModes
                }
                .map { it.value }
                .reduceOrNull { acc, additionalUnsupported -> additionalUnsupported intersect acc }
                ?.maxOrNull()

            if (maxCommonUnsupportedFps != null &&
                maxCommonUnsupportedFps <= cameraAppSettings.targetFrameRate
            ) {
                return StabilizationUiState.Disabled(
                    FpsUnsupportedRationale(
                        R.string.stabilization_rationale_prefix,
                        maxCommonUnsupportedFps
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
                    currentLensStabilizations = supportedStabilizationModes,
                    unsupportedFrameRates = StabilizationMode.AUTO.unsupportedFpsSet
                ),
                stabilizationOnState = getSingleStabilizationState(
                    stabilizationMode = StabilizationMode.ON,
                    currentFrameRate = cameraAppSettings.targetFrameRate,
                    defaultLensFacing = cameraAppSettings.cameraLensFacing,
                    deviceStabilizations = deviceStabilizations,
                    currentLensStabilizations = supportedStabilizationModes,
                    unsupportedFrameRates = StabilizationMode.ON.unsupportedFpsSet
                ),
                stabilizationHighQualityState = getSingleStabilizationState(
                    stabilizationMode = StabilizationMode.HIGH_QUALITY,
                    currentFrameRate = cameraAppSettings.targetFrameRate,
                    defaultLensFacing = cameraAppSettings.cameraLensFacing,
                    deviceStabilizations = deviceStabilizations,
                    currentLensStabilizations = supportedStabilizationModes,
                    unsupportedFrameRates = StabilizationMode.HIGH_QUALITY.unsupportedFpsSet
                ),
                stabilizationOpticalState = getSingleStabilizationState(
                    stabilizationMode = StabilizationMode.OPTICAL,
                    currentFrameRate = cameraAppSettings.targetFrameRate,
                    defaultLensFacing = cameraAppSettings.cameraLensFacing,
                    deviceStabilizations = deviceStabilizations,
                    currentLensStabilizations = supportedStabilizationModes,
                    unsupportedFrameRates = StabilizationMode.OPTICAL.unsupportedFpsSet
                )
            )
        }
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

    private fun getVideoQualityUiState(
        systemConstraints: SystemConstraints,
        cameraAppSettings: CameraAppSettings
    ): VideoQualityUiState {
        val cameraConstraints = systemConstraints.forCurrentLens(cameraAppSettings)
        val supportedVideoQualities: List<VideoQuality> =
            cameraConstraints?.supportedVideoQualitiesMap?.get(
                cameraAppSettings.dynamicRange
            ) ?: listOf(VideoQuality.UNSPECIFIED)

        return if (supportedVideoQualities != listOf(VideoQuality.UNSPECIFIED)) {
            VideoQualityUiState.Enabled(
                currentVideoQuality = cameraAppSettings.videoQuality,
                videoQualityAutoState = SingleSelectableState.Selectable,
                videoQualitySDState = getSingleVideoQualityState(
                    VideoQuality.SD,
                    supportedVideoQualities
                ),
                videoQualityHDState = getSingleVideoQualityState(
                    VideoQuality.HD,
                    supportedVideoQualities
                ),
                videoQualityFHDState = getSingleVideoQualityState(
                    VideoQuality.FHD,
                    supportedVideoQualities
                ),
                videoQualityUHDState = getSingleVideoQualityState(
                    VideoQuality.UHD,
                    supportedVideoQualities
                )
            )
        } else {
            VideoQualityUiState.Disabled(
                DisabledRationale.VideoQualityUnsupportedRationale(
                    R.string.video_quality_rationale_prefix
                )
            )
        }
    }

    private fun getSingleVideoQualityState(
        videoQuality: VideoQuality,
        supportedVideQualities: List<VideoQuality>
    ): SingleSelectableState = if (supportedVideQualities.contains(videoQuality)) {
        SingleSelectableState.Selectable
    } else {
        SingleSelectableState.Disabled(
            DisabledRationale.VideoQualityUnsupportedRationale(
                R.string.video_quality_rationale_prefix
            )
        )
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
                size > 1 && contains(LensFacing.FRONT)
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

        // If a non-AUTO stabilization is currently on and the other lens won't support it
        if (currentSettings.stabilizationMode != StabilizationMode.AUTO &&
            currentSettings.stabilizationMode !in newLensConstraints.supportedStabilizationModes
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

        // if other lens doesnt support the video quality
        if (currentSettings.videoQuality != VideoQuality.UNSPECIFIED &&
            newLensConstraints.supportedVideoQualitiesMap[DynamicRange.SDR]?.contains(
                currentSettings.videoQuality
            ) != true
        ) {
            return FlipLensUiState.Disabled(
                currentLensFacing = currentSettings.cameraLensFacing,
                disabledRationale = DisabledRationale.VideoQualityUnsupportedRationale(
                    when (currentSettings.cameraLensFacing) {
                        LensFacing.BACK -> R.string.front_lens_rationale_prefix
                        LensFacing.FRONT -> R.string.rear_lens_rationale_prefix
                    },
                    R.string.video_quality_rationale_suffix_sdr
                )
            )
        }

        return FlipLensUiState.Enabled(currentLensFacing = currentSettings.cameraLensFacing)
    }

    private fun getFpsUiState(
        systemConstraints: SystemConstraints,
        cameraAppSettings: CameraAppSettings
    ): FpsUiState {
        val optionConstraintRationale: MutableMap<Int, SingleSelectableState> = mutableMapOf()

        val deviceSupportedFrameRates = systemConstraints.perLensConstraints
            .asSequence()
            .flatMap { it.value.supportedFixedFrameRates }
            .toSet()

        // if device supports no fixed frame rates, disable
        if (deviceSupportedFrameRates.isEmpty()) {
            return FpsUiState.Disabled(
                DeviceUnsupportedRationale(R.string.no_fixed_fps_rationale_prefix)
            )
        }

        val currentLensConstraints = checkNotNull(
            systemConstraints.forCurrentLens(cameraAppSettings)
        ) {
            "Lens constraints for ${cameraAppSettings.cameraLensFacing} not available."
        }

        with(currentLensConstraints) {
            // provide selectable states for each of the fps options
            fpsOptions.forEach { fpsOption ->
                val fpsUiState = isFpsOptionEnabled(
                    fpsOption = fpsOption,
                    defaultLensFacing = cameraAppSettings.cameraLensFacing,
                    deviceSupportedFrameRates = deviceSupportedFrameRates,
                    stabilizationMode = cameraAppSettings.stabilizationMode
                )
                if (fpsUiState is SingleSelectableState.Disabled) {
                    Log.d(
                        TAG,
                        "fps option $fpsOption disabled. ${fpsUiState.disabledRationale::class}"
                    )
                }
                optionConstraintRationale[fpsOption] = fpsUiState
            }
        }
        return FpsUiState.Enabled(
            currentSelection = cameraAppSettings.targetFrameRate,
            fpsAutoState = SingleSelectableState.Selectable,
            fpsFifteenState = optionConstraintRationale[FPS_15]!!,
            fpsThirtyState = optionConstraintRationale[FPS_30]!!,
            fpsSixtyState = optionConstraintRationale[FPS_60]!!
        )
    }

    /**
     * Auxiliary function to determine if an FPS option should be disabled or not
     */
    private fun CameraConstraints.isFpsOptionEnabled(
        fpsOption: Int,
        defaultLensFacing: LensFacing,
        deviceSupportedFrameRates: Set<Int>,
        stabilizationMode: StabilizationMode
    ): SingleSelectableState {
        // if device doesn't support the fps option, disable
        if (!deviceSupportedFrameRates.contains(fpsOption)) {
            return SingleSelectableState.Disabled(
                disabledRationale = DeviceUnsupportedRationale(R.string.fps_rationale_prefix)
            )
        }
        // if the current lens doesn't support the fps, disable
        if (!supportedFixedFrameRates.contains(fpsOption)) {
            Log.d(TAG, "FPS disabled for current lens")

            return SingleSelectableState.Disabled(
                getLensUnsupportedRationale(defaultLensFacing, R.string.fps_rationale_prefix)
            )
        }

        // if stabilization is on and the option is incompatible, disable
        if (fpsOption in stabilizationMode.unsupportedFpsSet) {
            return SingleSelectableState.Disabled(
                StabilizationUnsupportedRationale(R.string.fps_rationale_prefix)
            )
        }

        return SingleSelectableState.Selectable
    }

// ////////////////////////////////////////////////////////////
//
// Settings Repository functions
// ------------------------------------------------------------
// Note: These do not update the running camera state. Each
// setting should be applied individually (via diff) in
// PreviewViewModel.
//
// ////////////////////////////////////////////////////////////

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

    fun setStreamConfig(streamConfig: StreamConfig) {
        viewModelScope.launch {
            settingsRepository.updateStreamConfig(streamConfig)
            Log.d(TAG, "set default capture mode: $streamConfig")
        }
    }

    fun setLowLightBoostPriority(lowLightBoostPriority: LowLightBoostPriority) {
        viewModelScope.launch {
            settingsRepository.updateLowLightBoostPriority(lowLightBoostPriority)
            Log.d(TAG, "set low light boost priority: $lowLightBoostPriority")
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

    fun setVideoAudio(isAudioEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAudioEnabled(isAudioEnabled)
            Log.d(TAG, "recording audio muted: $isAudioEnabled")
        }
    }
}
