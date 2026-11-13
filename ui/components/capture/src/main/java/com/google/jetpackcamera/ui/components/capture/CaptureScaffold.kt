/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.google.jetpackcamera.ui.components.capture

import android.util.Range
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.camera.core.SurfaceRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.google.jetpackcamera.core.camera.AudioStreamState
import com.google.jetpackcamera.core.camera.InitialRecordingSettings
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.model.CameraError
import com.google.jetpackcamera.model.CameraZoomRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LensToZoom
import com.google.jetpackcamera.ui.components.capture.quicksettings.QuickSettingsScaffoldContent
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.FlashModeIndicator
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.HdrIndicator
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.ToggleQuickSettingsButton
import com.google.jetpackcamera.ui.controller.CameraController
import com.google.jetpackcamera.ui.controller.CaptureController
import com.google.jetpackcamera.ui.controller.ScreenFlashController
import com.google.jetpackcamera.ui.controller.SnackBarController
import com.google.jetpackcamera.ui.controller.ZoomController
import com.google.jetpackcamera.ui.controller.quicksettings.QuickSettingsController
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.SnackBarUiState
import com.google.jetpackcamera.ui.uistate.capture.AudioUiState
import com.google.jetpackcamera.ui.uistate.capture.CameraErrorUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureSubModeUiState
import com.google.jetpackcamera.ui.uistate.capture.ElapsedTimeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Mode-agnostic state holder for [CaptureScaffold] and [CaptureScaffoldLayout], managing Quick
 * Settings visibility (compatible with both bottom-sheet and side-rail / floating-card layouts)
 * and the scaffold [SnackbarHostState].
 *
 * @param isInitiallyExpanded whether the Quick Settings surface starts in the open state.
 * @param snackbarHostState the [SnackbarHostState] used for displaying snackbar messages inside the
 *   scaffold.
 */
@Stable
class CaptureScaffoldState(
    isInitiallyExpanded: Boolean = false,
    val snackbarHostState: SnackbarHostState = SnackbarHostState()
) {
    /**
     * Whether the Quick Settings surface (bottom sheet or floating overlay card) is currently open.
     */
    var isQuickSettingsOpen: Boolean by mutableStateOf(isInitiallyExpanded)
        private set

    /**
     * Opens the Quick Settings surface.
     */
    fun showQuickSettings() {
        isQuickSettingsOpen = true
    }

    /**
     * Dismisses the Quick Settings surface if it is open.
     */
    fun dismissQuickSettings() {
        isQuickSettingsOpen = false
    }

    /**
     * Toggles the Quick Settings surface between open and dismissed.
     */
    fun toggleQuickSettings() {
        isQuickSettingsOpen = !isQuickSettingsOpen
    }

    /**
     * Expands the Quick Settings surface.
     */
    suspend fun expandQuickSettings() {
        isQuickSettingsOpen = true
    }

    /**
     * Hides the Quick Settings surface.
     */
    suspend fun hideQuickSettings() {
        isQuickSettingsOpen = false
    }

    companion object {
        /**
         * [Saver] implementation that persists [isQuickSettingsOpen] across configuration changes
         * and process recreation.
         */
        val Saver: Saver<CaptureScaffoldState, Boolean> = Saver(
            save = { it.isQuickSettingsOpen },
            restore = { CaptureScaffoldState(isInitiallyExpanded = it) }
        )
    }
}

/**
 * Creates and remembers a mode-agnostic [CaptureScaffoldState] saved across configuration changes
 * and process recreation.
 *
 * @param isInitiallyExpanded whether the Quick Settings surface starts expanded.
 */
@Composable
fun rememberCaptureScaffoldState(isInitiallyExpanded: Boolean = false): CaptureScaffoldState =
    rememberSaveable(isInitiallyExpanded, saver = CaptureScaffoldState.Saver) {
        CaptureScaffoldState(isInitiallyExpanded = isInitiallyExpanded)
    }

/**
 * Receiver scope for [CaptureScaffold] slots.
 *
 * Extends [PreviewOverlaySlotScope] with capture session state, Quick Settings controls, and zoom
 * actions coordinated with the scaffold's [ZoomStateManager]. Properties are backed by
 * `derivedStateOf` so that slots only recompose when a property they actually read changes.
 */
@Stable
sealed interface CaptureSlotScope : PreviewOverlaySlotScope {
    /**
     * Whether a video recording is currently active (recording or paused).
     */
    val isRecording: Boolean

    /**
     * Whether the Quick Settings surface is currently open.
     */
    val isQuickSettingsOpen: Boolean

    /**
     * Toggles the Quick Settings surface between open and dismissed.
     */
    fun toggleQuickSettings()

    /**
     * Dismisses the Quick Settings surface if it is open.
     */
    fun dismissQuickSettings()

    /**
     * Increments the current zoom ratio by [zoomIncrement] on [lensToZoom] (for example, during a
     * capture-button drag-to-zoom gesture).
     */
    fun incrementZoom(zoomIncrement: Float, lensToZoom: LensToZoom = LensToZoom.PRIMARY)

    /**
     * Smoothly animates the zoom ratio to [targetZoom] on [lensToZoom] (for example, when a zoom
     * bar button is tapped).
     */
    fun animateZoomTo(targetZoom: Float, lensToZoom: LensToZoom = LensToZoom.PRIMARY)

    /**
     * Immediately sets the zoom ratio to [targetZoom] on [lensToZoom], keeping the scaffold's
     * internal zoom state synchronized with subsequent pinch or drag gestures.
     */
    fun setAbsoluteZoom(targetZoom: Float, lensToZoom: LensToZoom = LensToZoom.PRIMARY)
}

/**
 * Creates a [CaptureSlotScope] with static values for Compose `@Preview`s and unit tests.
 */
fun CaptureSlotScope(
    isOverViewfinder: Boolean = true,
    isRecording: Boolean = false,
    isQuickSettingsOpen: Boolean = false,
    onToggleQuickSettings: () -> Unit = {},
    onDismissQuickSettings: () -> Unit = {},
    onIncrementZoom: (Float, LensToZoom) -> Unit = { _, _ -> },
    onAnimateZoomTo: (Float, LensToZoom) -> Unit = { _, _ -> },
    onSetAbsoluteZoom: (Float, LensToZoom) -> Unit = { _, _ -> }
): CaptureSlotScope = StaticCaptureSlotScope(
    isOverViewfinder = isOverViewfinder,
    isRecording = isRecording,
    isQuickSettingsOpen = isQuickSettingsOpen,
    onToggleQuickSettings = onToggleQuickSettings,
    onDismissQuickSettings = onDismissQuickSettings,
    onIncrementZoom = onIncrementZoom,
    onAnimateZoomTo = onAnimateZoomTo,
    onSetAbsoluteZoom = onSetAbsoluteZoom
)

private class StaticCaptureSlotScope(
    override val isOverViewfinder: Boolean,
    override val isRecording: Boolean,
    override val isQuickSettingsOpen: Boolean,
    private val onToggleQuickSettings: () -> Unit,
    private val onDismissQuickSettings: () -> Unit,
    private val onIncrementZoom: (Float, LensToZoom) -> Unit,
    private val onAnimateZoomTo: (Float, LensToZoom) -> Unit,
    private val onSetAbsoluteZoom: (Float, LensToZoom) -> Unit
) : CaptureSlotScope {
    override val cameraControlContainerColor: Color
        get() = backgroundStyleFor(isOverViewfinder).containerColor

    override fun toggleQuickSettings() {
        onToggleQuickSettings()
    }

    override fun dismissQuickSettings() {
        onDismissQuickSettings()
    }

    override fun incrementZoom(zoomIncrement: Float, lensToZoom: LensToZoom) {
        onIncrementZoom(zoomIncrement, lensToZoom)
    }

    override fun animateZoomTo(targetZoom: Float, lensToZoom: LensToZoom) {
        onAnimateZoomTo(targetZoom, lensToZoom)
    }

    override fun setAbsoluteZoom(targetZoom: Float, lensToZoom: LensToZoom) {
        onSetAbsoluteZoom(targetZoom, lensToZoom)
    }
}

private class CaptureSlotScopeImpl(
    private val overlayScope: PreviewOverlaySlotScope,
    private val isRecordingState: State<Boolean>,
    private val scaffoldState: CaptureScaffoldState,
    private val onToggleQuickSettings: () -> Unit,
    private val onDismissQuickSettings: () -> Unit,
    private val onIncrementZoom: (Float, LensToZoom) -> Unit,
    private val onAnimateZoomTo: (Float, LensToZoom) -> Unit,
    private val onSetAbsoluteZoom: (Float, LensToZoom) -> Unit
) : CaptureSlotScope {
    override val isOverViewfinder: Boolean
        get() = overlayScope.isOverViewfinder

    override val cameraControlContainerColor: Color
        get() = overlayScope.cameraControlContainerColor

    override val isRecording: Boolean
        get() = isRecordingState.value

    override val isQuickSettingsOpen: Boolean
        get() = scaffoldState.isQuickSettingsOpen

    override fun toggleQuickSettings() {
        onToggleQuickSettings()
    }

    override fun dismissQuickSettings() {
        onDismissQuickSettings()
    }

    override fun incrementZoom(zoomIncrement: Float, lensToZoom: LensToZoom) {
        onIncrementZoom(zoomIncrement, lensToZoom)
    }

    override fun animateZoomTo(targetZoom: Float, lensToZoom: LensToZoom) {
        onAnimateZoomTo(targetZoom, lensToZoom)
    }

    override fun setAbsoluteZoom(targetZoom: Float, lensToZoom: LensToZoom) {
        onSetAbsoluteZoom(targetZoom, lensToZoom)
    }
}

@Composable
private fun rememberCaptureSlotScope(
    overlayScope: PreviewOverlaySlotScope,
    isRecordingState: State<Boolean>,
    scaffoldState: CaptureScaffoldState,
    onToggleQuickSettings: () -> Unit,
    onDismissQuickSettings: () -> Unit,
    onIncrementZoom: (Float, LensToZoom) -> Unit,
    onAnimateZoomTo: (Float, LensToZoom) -> Unit,
    onSetAbsoluteZoom: (Float, LensToZoom) -> Unit
): CaptureSlotScope = remember(
    overlayScope,
    isRecordingState,
    scaffoldState,
    onToggleQuickSettings,
    onDismissQuickSettings,
    onIncrementZoom,
    onAnimateZoomTo,
    onSetAbsoluteZoom
) {
    CaptureSlotScopeImpl(
        overlayScope = overlayScope,
        isRecordingState = isRecordingState,
        scaffoldState = scaffoldState,
        onToggleQuickSettings = onToggleQuickSettings,
        onDismissQuickSettings = onDismissQuickSettings,
        onIncrementZoom = onIncrementZoom,
        onAnimateZoomTo = onAnimateZoomTo,
        onSetAbsoluteZoom = onSetAbsoluteZoom
    )
}

/**
 * Localized strings for [CaptureScaffoldDefaults.ErrorDialog].
 *
 * @property title The dialog title.
 * @property body Optional dialog body text.
 * @property confirmButtonText Text for the confirm button.
 */
@Immutable
class CameraErrorStrings(
    val title: String,
    val body: String? = null,
    val confirmButtonText: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CameraErrorStrings) return false
        return title == other.title &&
            body == other.body &&
            confirmButtonText == other.confirmButtonText
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + (body?.hashCode() ?: 0)
        result = 31 * result + confirmButtonText.hashCode()
        return result
    }

    override fun toString(): String =
        "CameraErrorStrings(title=$title, body=$body, confirmButtonText=$confirmButtonText)"
}

/**
 * Resolves the default [CameraErrorStrings] for [error] from this module's string resources, or
 * `null` if [error] does not display a blocking dialog (such as
 * [CameraError.CameraSensorPrivacyDisabled]).
 */
@Composable
fun defaultCameraErrorStrings(error: CameraError): CameraErrorStrings? {
    val resources = error.toDialogResources() ?: return null
    return CameraErrorStrings(
        title = stringResource(resources.titleResId),
        body = stringResource(resources.bodyResId),
        confirmButtonText = stringResource(R.string.camera_error_dialog_ok)
    )
}

/**
 * Public default building blocks for [CaptureScaffold] slots.
 */
object CaptureScaffoldDefaults {
    /**
     * Default shutter / capture button wired to [CaptureController].
     */
    @Composable
    fun CaptureButton(
        uiState: CaptureButtonUiState,
        captureController: CaptureController?,
        modifier: Modifier = Modifier,
        onDismissQuickSettings: () -> Unit = {},
        onIncrementZoom: (Float) -> Unit = {}
    ) {
        com.google.jetpackcamera.ui.components.capture.CaptureButton(
            modifier = modifier,
            captureButtonUiState = uiState,
            onCaptureImage = { contentResolver ->
                onDismissQuickSettings()
                captureController?.captureImage(contentResolver)
            },
            onIncrementZoom = onIncrementZoom,
            onStartVideoRecording = {
                onDismissQuickSettings()
                captureController?.startVideoRecording()
            },
            onStopVideoRecording = { captureController?.stopVideoRecording() },
            onLockVideoRecording = { isLocked ->
                captureController?.setLockedRecording(isLocked)
            }
        )
    }

    /**
     * Default camera lens flip button wired to [QuickSettingsController].
     */
    @Composable
    fun FlipCameraButton(
        uiState: FlipLensUiState,
        quickSettingsController: QuickSettingsController?,
        modifier: Modifier = Modifier
    ) {
        com.google.jetpackcamera.ui.components.capture.FlipCameraButton(
            modifier = modifier.testTag(FLIP_CAMERA_BUTTON),
            onClick = {
                if (uiState is FlipLensUiState.Available) {
                    quickSettingsController?.setLensFacing(uiState.selectedLensFacing.flip())
                }
            },
            flipLensUiState = uiState,
            enabled = uiState is FlipLensUiState.Available &&
                uiState.availableLensFacings.size > 1
        )
    }

    /**
     * Default media thumbnail image well. Renders only when [uiState] is
     * [ImageWellUiState.Content].
     */
    @Composable
    fun ImageWell(uiState: ImageWellUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
        if (uiState is ImageWellUiState.Content) {
            com.google.jetpackcamera.ui.components.capture.ImageWell(
                modifier = modifier,
                imageWellUiState = uiState,
                onClick = onClick
            )
        }
    }

    /**
     * Default zoom controls bar.
     */
    @Composable
    fun ZoomControls(
        uiState: ZoomControlUiState,
        onChangeZoom: (Float) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            ZoomBar(
                zoomControlUiState = uiState,
                onChangeZoom = onChangeZoom
            )
        }
    }

    /**
     * Default capture mode toggle button. Hidden while [isRecording] is `true` or when [uiState]
     * is not [CaptureModeToggleUiState.Available].
     */
    @Composable
    fun CaptureModeToggle(
        uiState: CaptureModeToggleUiState,
        isRecording: Boolean,
        quickSettingsController: QuickSettingsController?,
        snackBarController: SnackBarController?,
        modifier: Modifier = Modifier
    ) {
        if (!isRecording && uiState is CaptureModeToggleUiState.Available) {
            CaptureModeToggleButton(
                uiState = uiState,
                onChangeCaptureMode = { newCaptureMode ->
                    quickSettingsController?.setCaptureMode(newCaptureMode)
                },
                onToggleWhenDisabled = { disableRationale ->
                    snackBarController?.enqueueDisabledHdrToggleSnackBar(disableRationale)
                },
                modifier = modifier.testTag(CAPTURE_MODE_TOGGLE_BUTTON)
            )
        }
    }

    /**
     * Default capture sub-mode carousel wired to [QuickSettingsController].
     */
    @Composable
    fun CaptureModeCarousel(
        uiState: CaptureSubModeUiState,
        quickSettingsController: QuickSettingsController?,
        modifier: Modifier = Modifier
    ) {
        com.google.jetpackcamera.ui.components.capture.CaptureModeCarousel(
            uiState = uiState,
            onSelectSubMode = { subModeId ->
                quickSettingsController?.setCaptureSubMode(subModeId)
            },
            modifier = modifier
        )
    }

    /**
     * Default Quick Settings toggle button, hidden while [isRecording] is `true`.
     */
    @Composable
    fun QuickSettingsButton(
        uiState: QuickSettingsUiState,
        isRecording: Boolean,
        isQuickSettingsOpen: Boolean,
        onToggleQuickSettings: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val isQuickSettingsVisible = !isRecording
        val isQuickSettingsEnabled = uiState is QuickSettingsUiState.Available
        val disableAnimations = LocalDisableAnimations.current
        AnimatedVisibility(
            visible = isQuickSettingsVisible,
            enter = if (disableAnimations) EnterTransition.None else fadeIn(),
            exit = if (disableAnimations) {
                ExitTransition.None
            } else {
                fadeOut(animationSpec = tween(delayMillis = 1_500))
            }
        ) {
            ToggleQuickSettingsButton(
                isOpen = isQuickSettingsOpen,
                enabled = isQuickSettingsEnabled,
                onClick = onToggleQuickSettings,
                modifier = modifier
            )
        }
    }

    /**
     * Default recording elapsed-time readout, shown while [isRecording] is `true`.
     */
    @Composable
    fun ElapsedTimeDisplay(
        isRecording: Boolean,
        elapsedTimeUiStateProvider: () -> ElapsedTimeUiState,
        modifier: Modifier = Modifier
    ) {
        val disableAnimations = LocalDisableAnimations.current
        AnimatedVisibility(
            visible = isRecording,
            enter = if (disableAnimations) EnterTransition.None else fadeIn(),
            exit = if (disableAnimations) {
                ExitTransition.None
            } else {
                fadeOut(animationSpec = tween(delayMillis = 1_500))
            }
        ) {
            val elapsedTimeModifier =
                remember(modifier) { modifier.testTag(ELAPSED_TIME_TAG) }
            ElapsedTimeText(
                modifier = elapsedTimeModifier,
                elapsedTimeUiStateProvider = elapsedTimeUiStateProvider
            )
        }
    }

    /**
     * Default top status indicator pill reading slices from a deferred [uiState] provider.
     */
    @Composable
    fun TopBar(uiState: () -> CaptureUiState.Ready, modifier: Modifier = Modifier) {
        val currentUiState by rememberUpdatedState(uiState)
        val flashModeState = remember { derivedStateOf { currentUiState().flashModeUiState } }
        val hdrState = remember { derivedStateOf { currentUiState().hdrUiState } }
        val videoQualityState = remember { derivedStateOf { currentUiState().videoQuality } }
        val stabilizationState =
            remember { derivedStateOf { currentUiState().stabilizationUiState } }

        IndicatorPill(modifier = modifier.fillMaxWidth()) {
            FlashModeIndicator(flashModeUiState = flashModeState.value)
            HdrIndicator(hdrUiState = hdrState.value)
            VideoQualityIcon(
                videoQuality = videoQualityState.value,
                modifier = Modifier.testTag(VIDEO_QUALITY_TAG)
            )
            StabilizationIcon(stabilizationUiState = stabilizationState.value)
        }
    }

    /**
     * Default Quick Settings content.
     */
    @Composable
    fun QuickSettings(
        uiState: QuickSettingsUiState,
        controller: QuickSettingsController?,
        onNavigateToSettings: () -> Unit,
        modifier: Modifier = Modifier,
        showMoreSettingsButton: Boolean = true
    ) {
        if (controller != null) {
            QuickSettingsScaffoldContent(
                quickSettingsUiState = uiState,
                onNavigateToSettings = onNavigateToSettings,
                quickSettingsController = controller,
                modifier = modifier,
                showMoreSettingsButton = showMoreSettingsButton
            )
        }
    }

    /**
     * Default camera error dialog.
     *
     * @param error the active [CameraError].
     * @param onDismiss callback invoked when the user confirms/dismisses the dialog.
     * @param modifier the [Modifier] to apply to the dialog container.
     * @param strings maps [error] to localized [CameraErrorStrings], or `null` if no dialog should
     *   be rendered for [error]. Hosts that require localized strings can supply their own mapper.
     */
    @Composable
    fun ErrorDialog(
        error: CameraError,
        onDismiss: () -> Unit,
        modifier: Modifier = Modifier,
        strings: @Composable (CameraError) -> CameraErrorStrings? = {
            defaultCameraErrorStrings(it)
        }
    ) {
        val resolvedStrings = strings(error) ?: return
        CameraErrorDialog(
            title = resolvedStrings.title,
            body = resolvedStrings.body,
            confirmButtonText = resolvedStrings.confirmButtonText,
            onConfirm = onDismiss,
            modifier = modifier
        )
    }
}

@Composable
private fun rememberZoomStateManager(
    captureUiStateProvider: () -> CaptureUiState.Ready,
    zoomController: ZoomController?
): ZoomStateManager {
    val currentCaptureUiStateProvider by rememberUpdatedState(captureUiStateProvider)
    val flipLensState =
        remember { derivedStateOf { currentCaptureUiStateProvider().flipLensUiState } }
    val zoomControlState = remember {
        derivedStateOf { currentCaptureUiStateProvider().zoomControlUiState }
    }
    val zoomUiState = remember { derivedStateOf { currentCaptureUiStateProvider().zoomUiState } }

    val zoomStateManager = remember(zoomController) {
        val safeZoomController = zoomController ?: object : ZoomController {
            override fun setZoomRatio(zoomRatio: CameraZoomRatio) {}
            override fun setZoomAnimationState(targetValue: Float?) {}
        }
        ZoomStateManager(
            initialZoomLevel =
            (zoomControlState.value as? ZoomControlUiState.Enabled)?.initialZoomRatio ?: 1f,
            zoomRange = (zoomUiState.value as? ZoomUiState.Enabled)
                ?.primaryZoomRange ?: Range(1f, 1f),
            zoomController = safeZoomController
        )
    }

    val selectedLensFacing = (flipLensState.value as? FlipLensUiState.Available)?.selectedLensFacing
    LaunchedEffect(selectedLensFacing, zoomStateManager) {
        // Before the camera is ready, zoom is unavailable; initialize once this lens reports its
        // range.
        val zoomRange =
            snapshotFlow { (zoomUiState.value as? ZoomUiState.Enabled)?.primaryZoomRange }
                .filterNotNull()
                .first()
        zoomStateManager.onChangeLens(
            newInitialZoomLevel =
            (zoomControlState.value as? ZoomControlUiState.Enabled)?.initialZoomRatio ?: 1f,
            newZoomRange = zoomRange
        )
    }

    return zoomStateManager
}

/**
 * Opinionated camera capture scaffold exposing individual control and chrome slots ([topBar],
 * [captureButton], [flipCameraButton], [zoomControls], [captureModeToggle], [captureModeCarousel],
 * [quickSettingsButton], [elapsedTimeDisplay], opt-in [imageWell], [quickSettings], [errorDialog],
 * [topStartContent], [viewfinderTopEndContent], [topBarTrailingContent], and [overlay]).
 *
 * Host screens remain in control of their own screen policy (permissions, camera session start/stop
 * via [CameraSessionEffect], capture event navigation, and benchmarking), and pass a deferred
 * [captureUiState] provider (falling back to [LOADING_CAPTURE_UI_STATE_SKELETON] while loading).
 *
 * @param captureUiState deferred provider of the current [CaptureUiState.Ready] state.
 * @param surfaceRequest the active CameraX [SurfaceRequest] rendered in the viewfinder, or `null`
 *   before a surface is available.
 * @param modifier the [Modifier] to apply to the scaffold root.
 * @param state the mode-agnostic [CaptureScaffoldState] managing Quick Settings visibility and the
 *   snackbar host.
 * @param onNavigateToSettings callback invoked when the user taps the full settings button inside
 *   the default [quickSettings] surface.
 * @param onRequestWindowColorMode callback invoked when the viewfinder requests a window color mode
 *   change (such as wide color gamut or HDR).
 * @param onRequiresImmersiveChange invoked with `true` when the window is too short for the
 *   tallest viewfinder to clear a persistent navigation bar, and `false` otherwise or on dispose.
 * @param controlsVisible whether the camera control slots should be composed and visible when no
 *   blocking camera error is active.
 * @param snackBarUiState state of the snackbar queue to display via [snackBarController].
 * @param cameraController optional [CameraController]; when non-null, enables tap-to-focus on the
 *   viewfinder and dismissing active camera errors via the `onDismiss` callback passed to
 *   [errorDialog].
 * @param captureController optional [CaptureController]; when non-null, enables still image
 *   capture, starting/stopping/locking video recording in the default [captureButton], and
 *   restoring audio state after a video recording finishes.
 * @param quickSettingsController optional [QuickSettingsController]; when non-null, enables lens
 *   flipping (via the flip button and double-tap on the viewfinder), capture mode/sub-mode
 *   selection, Quick Settings actions, and restoring the primary lens after a video recording
 *   finishes.
 * @param zoomController optional [ZoomController]; when non-null, enables viewfinder pinch-to-zoom,
 *   animated zoom bar selections, capture-button drag-to-zoom, [CaptureSlotScope] zoom actions, and
 *   restoring zoom ratios after a video recording finishes.
 * @param snackBarController optional [SnackBarController]; when non-null, enables displaying queued
 *   snackbars from [snackBarUiState] and enqueuing disabled-HDR toggle rationale snackbars.
 * @param screenFlashController optional [ScreenFlashController]; when non-null, enables reporting
 *   the initial screen brightness for front-camera screen flash.
 * @param topBar composable slot for the top status indicator bar.
 * @param captureButton composable slot for the primary shutter / capture button.
 * @param flipCameraButton composable slot for the camera lens flip button.
 * @param zoomControls composable slot for the zoom controls bar.
 * @param captureModeToggle composable slot for the capture mode toggle switch.
 * @param captureModeCarousel composable slot for the capture sub-mode carousel.
 * @param quickSettingsButton composable slot for the Quick Settings toggle button.
 * @param elapsedTimeDisplay composable slot for the recording elapsed-time readout.
 * @param imageWell optional composable slot for the post-capture media thumbnail well; omitted when
 *   `null` so only hosts with post-capture media review opt in.
 * @param quickSettings composable slot for the Quick Settings content.
 * @param errorDialog optional composable slot rendered when [CameraErrorUiState.Showing] is active.
 *   Passing `null` disables the built-in error dialog (for example, when the host renders its own
 *   error UI), while [CameraErrorUiState.Showing.blocksPreview] continues to control whether the
 *   viewfinder, top bar, and controls are hidden.
 * @param topStartContent optional composable slot placed at the top-start of the layout directly
 *   below the top bar.
 * @param viewfinderTopEndContent optional composable slot anchored to the top-end corner of the
 *   viewfinder.
 * @param topBarTrailingContent optional composable slot aligned to the end of the top bar.
 * @param overlay optional overlay composable slot drawn above the viewfinder and controls.
 */
@Composable
fun CaptureScaffold(
    captureUiState: () -> CaptureUiState.Ready,
    surfaceRequest: SurfaceRequest?,
    modifier: Modifier = Modifier,
    state: CaptureScaffoldState = rememberCaptureScaffoldState(),
    onNavigateToSettings: () -> Unit = {},
    onRequestWindowColorMode: (Int) -> Unit = {},
    onRequiresImmersiveChange: (Boolean) -> Unit = {},
    controlsVisible: Boolean = true,
    snackBarUiState: SnackBarUiState = SnackBarUiState.Disabled,
    cameraController: CameraController? = null,
    captureController: CaptureController? = null,
    quickSettingsController: QuickSettingsController? = null,
    zoomController: ZoomController? = null,
    snackBarController: SnackBarController? = null,
    screenFlashController: ScreenFlashController? = null,
    topBar: @Composable CaptureSlotScope.(Modifier) -> Unit = { slotModifier ->
        CaptureScaffoldDefaults.TopBar(uiState = captureUiState, modifier = slotModifier)
    },
    captureButton: @Composable CaptureSlotScope.(Modifier) -> Unit = { slotModifier ->
        val currentUiState by rememberUpdatedState(captureUiState)
        val captureButtonState = remember {
            derivedStateOf { currentUiState().captureButtonUiState }
        }
        CaptureScaffoldDefaults.CaptureButton(
            uiState = captureButtonState.value,
            captureController = captureController,
            modifier = slotModifier,
            onDismissQuickSettings = ::dismissQuickSettings,
            onIncrementZoom = ::incrementZoom
        )
    },
    flipCameraButton: @Composable CaptureSlotScope.(Modifier) -> Unit = { slotModifier ->
        val currentUiState by rememberUpdatedState(captureUiState)
        val flipLensState = remember { derivedStateOf { currentUiState().flipLensUiState } }
        CaptureScaffoldDefaults.FlipCameraButton(
            uiState = flipLensState.value,
            quickSettingsController = quickSettingsController,
            modifier = slotModifier
        )
    },
    zoomControls: @Composable CaptureSlotScope.(Modifier) -> Unit = { slotModifier ->
        val currentUiState by rememberUpdatedState(captureUiState)
        val zoomControlState = remember {
            derivedStateOf { currentUiState().zoomControlUiState }
        }
        CaptureScaffoldDefaults.ZoomControls(
            uiState = zoomControlState.value,
            onChangeZoom = ::animateZoomTo,
            modifier = slotModifier
        )
    },
    captureModeToggle: @Composable CaptureSlotScope.(Modifier) -> Unit = { slotModifier ->
        val currentUiState by rememberUpdatedState(captureUiState)
        val captureModeToggleState = remember {
            derivedStateOf { currentUiState().captureModeToggleUiState }
        }
        CaptureScaffoldDefaults.CaptureModeToggle(
            uiState = captureModeToggleState.value,
            isRecording = isRecording,
            quickSettingsController = quickSettingsController,
            snackBarController = snackBarController,
            modifier = slotModifier
        )
    },
    captureModeCarousel: @Composable CaptureSlotScope.(Modifier) -> Unit = { slotModifier ->
        val currentUiState by rememberUpdatedState(captureUiState)
        val captureSubModeState = remember {
            derivedStateOf { currentUiState().captureSubModeUiState }
        }
        CaptureScaffoldDefaults.CaptureModeCarousel(
            uiState = captureSubModeState.value,
            quickSettingsController = quickSettingsController,
            modifier = slotModifier
        )
    },
    quickSettingsButton: @Composable CaptureSlotScope.(Modifier) -> Unit = { slotModifier ->
        val currentUiState by rememberUpdatedState(captureUiState)
        val quickSettingsState = remember {
            derivedStateOf { currentUiState().quickSettingsUiState }
        }
        CaptureScaffoldDefaults.QuickSettingsButton(
            uiState = quickSettingsState.value,
            isRecording = isRecording,
            isQuickSettingsOpen = isQuickSettingsOpen,
            onToggleQuickSettings = ::toggleQuickSettings,
            modifier = slotModifier
        )
    },
    elapsedTimeDisplay: @Composable CaptureSlotScope.(Modifier) -> Unit = { slotModifier ->
        val currentUiState by rememberUpdatedState(captureUiState)
        CaptureScaffoldDefaults.ElapsedTimeDisplay(
            isRecording = isRecording,
            elapsedTimeUiStateProvider = { currentUiState().elapsedTimeUiState },
            modifier = slotModifier
        )
    },
    imageWell: (@Composable CaptureSlotScope.(Modifier) -> Unit)? = null,
    quickSettings: @Composable CaptureSlotScope.(Modifier) -> Unit = { slotModifier ->
        val currentUiState by rememberUpdatedState(captureUiState)
        val quickSettingsState = remember {
            derivedStateOf { currentUiState().quickSettingsUiState }
        }
        CaptureScaffoldDefaults.QuickSettings(
            uiState = quickSettingsState.value,
            controller = quickSettingsController,
            onNavigateToSettings = {
                dismissQuickSettings()
                onNavigateToSettings()
            },
            modifier = slotModifier
        )
    },
    errorDialog: (@Composable (error: CameraError, onDismiss: () -> Unit) -> Unit)? =
        { error, onDismiss ->
            CaptureScaffoldDefaults.ErrorDialog(error = error, onDismiss = onDismiss)
        },
    topStartContent: (@Composable CaptureSlotScope.(Modifier) -> Unit)? = null,
    viewfinderTopEndContent: (@Composable CaptureSlotScope.(Modifier) -> Unit)? = null,
    topBarTrailingContent: (@Composable CaptureSlotScope.(Modifier) -> Unit)? = null,
    overlay: (@Composable CaptureSlotScope.(Modifier) -> Unit)? = null
) {
    val currentCaptureUiStateProvider by rememberUpdatedState(captureUiState)
    val flipLensState =
        remember { derivedStateOf { currentCaptureUiStateProvider().flipLensUiState } }
    val isVideoRecordingStarting = remember {
        derivedStateOf {
            currentCaptureUiStateProvider().videoRecordingState is VideoRecordingState.Starting
        }
    }
    val isVideoRecordingInactive = remember {
        derivedStateOf {
            currentCaptureUiStateProvider().videoRecordingState is VideoRecordingState.Inactive
        }
    }
    val isVideoRecordingActive = remember {
        derivedStateOf {
            currentCaptureUiStateProvider().videoRecordingState is VideoRecordingState.Active
        }
    }

    val scope = rememberCoroutineScope()
    val zoomStateManager = rememberZoomStateManager(
        captureUiStateProvider = { currentCaptureUiStateProvider() },
        zoomController = zoomController
    )

    // Intercept back navigation only while Quick Settings is actively open.
    BackHandler(enabled = state.isQuickSettingsOpen) {
        state.dismissQuickSettings()
    }

    val onToggleQuickSettings: () -> Unit = remember(state) {
        state::toggleQuickSettings
    }
    val onDismissQuickSettings: () -> Unit = remember(state) {
        state::dismissQuickSettings
    }
    val onIncrementZoom: (Float, LensToZoom) -> Unit = remember(scope, zoomStateManager) {
        { zoomIncrement, lensToZoom ->
            scope.launch {
                zoomStateManager.incrementZoom(zoomIncrement, lensToZoom)
            }
        }
    }
    val onAnimateZoomTo: (Float, LensToZoom) -> Unit = remember(scope, zoomStateManager) {
        { targetZoom, lensToZoom ->
            scope.launch {
                zoomStateManager.animatedZoom(
                    targetZoomLevel = targetZoom,
                    lensToZoom = lensToZoom
                )
            }
        }
    }
    val onSetAbsoluteZoom: (Float, LensToZoom) -> Unit = remember(scope, zoomStateManager) {
        { targetZoom, lensToZoom ->
            scope.launch {
                zoomStateManager.absoluteZoom(
                    targetZoomLevel = targetZoom,
                    lensToZoom = lensToZoom
                )
            }
        }
    }

    var initialRecordingSettings by remember { mutableStateOf<InitialRecordingSettings?>(null) }
    LaunchedEffect(isVideoRecordingStarting.value, isVideoRecordingInactive.value) {
        when (val recordingState = currentCaptureUiStateProvider().videoRecordingState) {
            is VideoRecordingState.Starting -> {
                initialRecordingSettings = recordingState.initialRecordingSettings
            }

            is VideoRecordingState.Inactive -> {
                initialRecordingSettings?.let {
                    val oldPrimaryLensFacing = it.lensFacing
                    val oldZoomRatios = it.zoomRatios
                    val oldAudioEnabled = it.isAudioEnabled
                    captureController?.setAudioEnabled(oldAudioEnabled)
                    quickSettingsController?.setLensFacing(oldPrimaryLensFacing)
                    zoomStateManager.apply {
                        absoluteZoom(
                            targetZoomLevel = oldZoomRatios[oldPrimaryLensFacing] ?: 1f,
                            lensToZoom = LensToZoom.PRIMARY
                        )
                        absoluteZoom(
                            targetZoomLevel = oldZoomRatios[oldPrimaryLensFacing.flip()] ?: 1f,
                            lensToZoom = LensToZoom.SECONDARY
                        )
                    }
                }
                initialRecordingSettings = null
            }

            is VideoRecordingState.Active -> {}
        }
    }

    val onFlipCamera = remember(quickSettingsController) {
        {
            val currentFlipLens = flipLensState.value
            if (currentFlipLens is FlipLensUiState.Available) {
                quickSettingsController?.setLensFacing(
                    currentFlipLens.selectedLensFacing.flip()
                )
            }
        }
    }

    val onTapToFocusLambda = cameraController?.let { it::tapToFocus }
        ?: remember { { _: Float, _: Float -> } }
    val onScaleZoomLambda = remember(scope, zoomStateManager) {
        { zoomRatio: Float ->
            scope.launch { zoomStateManager.scaleZoom(zoomRatio, LensToZoom.PRIMARY) }
        }
    }

    val cameraErrorState = remember {
        derivedStateOf { currentCaptureUiStateProvider().cameraErrorUiState }
    }
    val blocksPreview by remember {
        derivedStateOf {
            (cameraErrorState.value as? CameraErrorUiState.Showing)?.blocksPreview == true
        }
    }
    LaunchedEffect(blocksPreview) {
        if (blocksPreview && state.isQuickSettingsOpen) {
            state.dismissQuickSettings()
        }
    }

    val previewDisplayState = remember {
        derivedStateOf {
            currentCaptureUiStateProvider().previewDisplayUiState
        }
    }
    val focusMeteringState = remember {
        derivedStateOf {
            currentCaptureUiStateProvider().focusMeteringUiState
        }
    }
    val viewfinderLambda = remember(
        onFlipCamera,
        onTapToFocusLambda,
        onScaleZoomLambda,
        surfaceRequest,
        onRequestWindowColorMode
    ) {
        @Composable { slotModifier: Modifier ->
            if (!blocksPreview) {
                PreviewDisplay(
                    modifier = slotModifier,
                    previewDisplayUiState = previewDisplayState.value,
                    onFlipCamera = onFlipCamera,
                    onTapToFocus = onTapToFocusLambda,
                    onScaleZoom = { zoomRatio -> onScaleZoomLambda(zoomRatio) },
                    surfaceRequest = surfaceRequest,
                    onRequestWindowColorMode = onRequestWindowColorMode,
                    focusMeteringUiState = focusMeteringState.value,
                    useLandscapeAspectRatio = true
                )
            }
        }
    }

    val screenFlashState = remember {
        derivedStateOf {
            currentCaptureUiStateProvider().screenFlashUiState
        }
    }
    val screenFlashOverlayLambda = remember(screenFlashController) {
        @Composable { _: Modifier ->
            ScreenFlashScreen(
                screenFlashUiState = screenFlashState.value,
                onInitialBrightnessCalculated = {
                    screenFlashController?.setClearUiScreenBrightness(it)
                }
            )
        }
    }

    val snackBarLambda = remember(snackBarController, snackBarUiState) {
        @Composable { slotModifier: Modifier, snackbarHostState: SnackbarHostState ->
            if (snackBarUiState is SnackBarUiState.Enabled) {
                val snackBarData = snackBarUiState.snackBarQueue.peek()
                if (snackBarData != null && snackBarController != null) {
                    TestableSnackbar(
                        modifier = slotModifier,
                        snackbarToShow = snackBarData,
                        snackbarHostState = snackbarHostState,
                        snackBarController = snackBarController
                    )
                }
            }
        }
    }

    val sheetSlotScope = remember(
        isVideoRecordingActive,
        state,
        onToggleQuickSettings,
        onDismissQuickSettings,
        onIncrementZoom,
        onAnimateZoomTo,
        onSetAbsoluteZoom
    ) {
        object : PreviewOverlaySlotScope {
            override val isOverViewfinder: Boolean = false
            override val cameraControlContainerColor: Color =
                backgroundStyleFor(false).containerColor
        }.let { overlayScope ->
            CaptureSlotScopeImpl(
                overlayScope = overlayScope,
                isRecordingState = isVideoRecordingActive,
                scaffoldState = state,
                onToggleQuickSettings = onToggleQuickSettings,
                onDismissQuickSettings = onDismissQuickSettings,
                onIncrementZoom = onIncrementZoom,
                onAnimateZoomTo = onAnimateZoomTo,
                onSetAbsoluteZoom = onSetAbsoluteZoom
            )
        }
    }

    CaptureScaffoldLayout(
        modifier = modifier,
        state = state,
        onDismissQuickSettings = onDismissQuickSettings,
        viewfinder = viewfinderLambda,
        captureButton = { slotModifier ->
            val slotScope = rememberCaptureSlotScope(
                overlayScope = this,
                isRecordingState = isVideoRecordingActive,
                scaffoldState = state,
                onToggleQuickSettings = onToggleQuickSettings,
                onDismissQuickSettings = onDismissQuickSettings,
                onIncrementZoom = onIncrementZoom,
                onAnimateZoomTo = onAnimateZoomTo,
                onSetAbsoluteZoom = onSetAbsoluteZoom
            )
            slotScope.captureButton(slotModifier)
        },
        imageWell = { slotModifier ->
            if (imageWell != null) {
                val slotScope = rememberCaptureSlotScope(
                    overlayScope = this,
                    isRecordingState = isVideoRecordingActive,
                    scaffoldState = state,
                    onToggleQuickSettings = onToggleQuickSettings,
                    onDismissQuickSettings = onDismissQuickSettings,
                    onIncrementZoom = onIncrementZoom,
                    onAnimateZoomTo = onAnimateZoomTo,
                    onSetAbsoluteZoom = onSetAbsoluteZoom
                )
                slotScope.imageWell(slotModifier)
            }
        },
        flipCameraButton = { slotModifier ->
            val slotScope = rememberCaptureSlotScope(
                overlayScope = this,
                isRecordingState = isVideoRecordingActive,
                scaffoldState = state,
                onToggleQuickSettings = onToggleQuickSettings,
                onDismissQuickSettings = onDismissQuickSettings,
                onIncrementZoom = onIncrementZoom,
                onAnimateZoomTo = onAnimateZoomTo,
                onSetAbsoluteZoom = onSetAbsoluteZoom
            )
            slotScope.flipCameraButton(slotModifier)
        },
        zoomLevelDisplay = { slotModifier ->
            val slotScope = rememberCaptureSlotScope(
                overlayScope = this,
                isRecordingState = isVideoRecordingActive,
                scaffoldState = state,
                onToggleQuickSettings = onToggleQuickSettings,
                onDismissQuickSettings = onDismissQuickSettings,
                onIncrementZoom = onIncrementZoom,
                onAnimateZoomTo = onAnimateZoomTo,
                onSetAbsoluteZoom = onSetAbsoluteZoom
            )
            slotScope.zoomControls(slotModifier)
        },
        elapsedTimeDisplay = { slotModifier ->
            val slotScope = rememberCaptureSlotScope(
                overlayScope = this,
                isRecordingState = isVideoRecordingActive,
                scaffoldState = state,
                onToggleQuickSettings = onToggleQuickSettings,
                onDismissQuickSettings = onDismissQuickSettings,
                onIncrementZoom = onIncrementZoom,
                onAnimateZoomTo = onAnimateZoomTo,
                onSetAbsoluteZoom = onSetAbsoluteZoom
            )
            slotScope.elapsedTimeDisplay(slotModifier)
        },
        quickSettingsButton = { slotModifier ->
            val slotScope = rememberCaptureSlotScope(
                overlayScope = this,
                isRecordingState = isVideoRecordingActive,
                scaffoldState = state,
                onToggleQuickSettings = onToggleQuickSettings,
                onDismissQuickSettings = onDismissQuickSettings,
                onIncrementZoom = onIncrementZoom,
                onAnimateZoomTo = onAnimateZoomTo,
                onSetAbsoluteZoom = onSetAbsoluteZoom
            )
            slotScope.quickSettingsButton(slotModifier)
        },
        indicatorRow = { slotModifier ->
            if (!blocksPreview) {
                val slotScope = rememberCaptureSlotScope(
                    overlayScope = this,
                    isRecordingState = isVideoRecordingActive,
                    scaffoldState = state,
                    onToggleQuickSettings = onToggleQuickSettings,
                    onDismissQuickSettings = onDismissQuickSettings,
                    onIncrementZoom = onIncrementZoom,
                    onAnimateZoomTo = onAnimateZoomTo,
                    onSetAbsoluteZoom = onSetAbsoluteZoom
                )
                slotScope.topBar(slotModifier)
            }
        },
        captureModeToggle = { slotModifier ->
            val slotScope = rememberCaptureSlotScope(
                overlayScope = this,
                isRecordingState = isVideoRecordingActive,
                scaffoldState = state,
                onToggleQuickSettings = onToggleQuickSettings,
                onDismissQuickSettings = onDismissQuickSettings,
                onIncrementZoom = onIncrementZoom,
                onAnimateZoomTo = onAnimateZoomTo,
                onSetAbsoluteZoom = onSetAbsoluteZoom
            )
            slotScope.captureModeToggle(slotModifier)
        },
        captureModeCarousel = { slotModifier ->
            val slotScope = rememberCaptureSlotScope(
                overlayScope = this,
                isRecordingState = isVideoRecordingActive,
                scaffoldState = state,
                onToggleQuickSettings = onToggleQuickSettings,
                onDismissQuickSettings = onDismissQuickSettings,
                onIncrementZoom = onIncrementZoom,
                onAnimateZoomTo = onAnimateZoomTo,
                onSetAbsoluteZoom = onSetAbsoluteZoom
            )
            slotScope.captureModeCarousel(slotModifier)
        },
        controlsVisible = controlsVisible && !blocksPreview,
        quickSettingsOverlay = { slotModifier ->
            sheetSlotScope.quickSettings(slotModifier)
        },
        overlay = { slotModifier ->
            if (overlay != null) {
                val slotScope = rememberCaptureSlotScope(
                    overlayScope = this,
                    isRecordingState = isVideoRecordingActive,
                    scaffoldState = state,
                    onToggleQuickSettings = onToggleQuickSettings,
                    onDismissQuickSettings = onDismissQuickSettings,
                    onIncrementZoom = onIncrementZoom,
                    onAnimateZoomTo = onAnimateZoomTo,
                    onSetAbsoluteZoom = onSetAbsoluteZoom
                )
                slotScope.overlay(slotModifier)
            }
        },
        screenFlashOverlay = screenFlashOverlayLambda,
        snackBar = snackBarLambda,
        topStartContent = { slotModifier ->
            if (topStartContent != null) {
                val slotScope = rememberCaptureSlotScope(
                    overlayScope = this,
                    isRecordingState = isVideoRecordingActive,
                    scaffoldState = state,
                    onToggleQuickSettings = onToggleQuickSettings,
                    onDismissQuickSettings = onDismissQuickSettings,
                    onIncrementZoom = onIncrementZoom,
                    onAnimateZoomTo = onAnimateZoomTo,
                    onSetAbsoluteZoom = onSetAbsoluteZoom
                )
                slotScope.topStartContent(slotModifier)
            }
        },
        viewfinderTopEndContent = { slotModifier ->
            if (viewfinderTopEndContent != null) {
                val slotScope = rememberCaptureSlotScope(
                    overlayScope = this,
                    isRecordingState = isVideoRecordingActive,
                    scaffoldState = state,
                    onToggleQuickSettings = onToggleQuickSettings,
                    onDismissQuickSettings = onDismissQuickSettings,
                    onIncrementZoom = onIncrementZoom,
                    onAnimateZoomTo = onAnimateZoomTo,
                    onSetAbsoluteZoom = onSetAbsoluteZoom
                )
                slotScope.viewfinderTopEndContent(slotModifier)
            }
        },
        topBarTrailingContent = { slotModifier ->
            if (topBarTrailingContent != null) {
                val slotScope = rememberCaptureSlotScope(
                    overlayScope = this,
                    isRecordingState = isVideoRecordingActive,
                    scaffoldState = state,
                    onToggleQuickSettings = onToggleQuickSettings,
                    onDismissQuickSettings = onDismissQuickSettings,
                    onIncrementZoom = onIncrementZoom,
                    onAnimateZoomTo = onAnimateZoomTo,
                    onSetAbsoluteZoom = onSetAbsoluteZoom
                )
                slotScope.topBarTrailingContent(slotModifier)
            }
        },
        onRequiresImmersiveChange = onRequiresImmersiveChange,
        errorDialog = { _ ->
            val showingState = cameraErrorState.value as? CameraErrorUiState.Showing
            if (showingState != null && errorDialog != null) {
                errorDialog(
                    showingState.error,
                    { cameraController?.dismissCameraError() }
                )
            }
        }
    )
}

/**
 * String resource IDs for displaying a [CameraErrorDialog].
 *
 * @property titleResId The string resource ID for the dialog title.
 * @property bodyResId The string resource ID for the dialog body.
 */
internal data class CameraErrorDialogResources(
    @StringRes val titleResId: Int,
    @StringRes val bodyResId: Int
)

/**
 * Maps a [CameraError] to its [CameraErrorDialogResources], or `null` if no dialog should be shown.
 */
internal fun CameraError.toDialogResources(): CameraErrorDialogResources? = when (this) {
    CameraError.CameraInUse -> CameraErrorDialogResources(
        titleResId = R.string.camera_error_in_use_title,
        bodyResId = R.string.camera_error_in_use_body
    )
    CameraError.MaxCamerasInUse -> CameraErrorDialogResources(
        titleResId = R.string.camera_error_max_in_use_title,
        bodyResId = R.string.camera_error_max_in_use_body
    )
    CameraError.OtherRecoverableError -> CameraErrorDialogResources(
        titleResId = R.string.camera_error_recoverable_title,
        bodyResId = R.string.camera_error_recoverable_body
    )
    CameraError.StreamConfigError -> CameraErrorDialogResources(
        titleResId = R.string.camera_error_stream_config_title,
        bodyResId = R.string.camera_error_stream_config_body
    )
    CameraError.CameraDisabledByPolicy -> CameraErrorDialogResources(
        titleResId = R.string.camera_error_disabled_title,
        bodyResId = R.string.camera_error_disabled_body
    )
    CameraError.CameraSensorPrivacyDisabled -> null
    CameraError.FatalCameraError -> CameraErrorDialogResources(
        titleResId = R.string.camera_error_fatal_title,
        bodyResId = R.string.camera_error_fatal_body
    )
    CameraError.DoNotDisturbEnabled -> CameraErrorDialogResources(
        titleResId = R.string.camera_error_dnd_title,
        bodyResId = R.string.camera_error_dnd_body
    )
    CameraError.CameraRemoved -> CameraErrorDialogResources(
        titleResId = R.string.camera_error_removed_title,
        bodyResId = R.string.camera_error_removed_body
    )
}

@PreviewPortraitDevices
@Preview
@Composable
private fun CaptureScaffoldPreview() {
    MaterialTheme {
        CaptureScaffold(
            captureUiState = { FAKE_PREVIEW_UI_STATE_READY },
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun CaptureScaffold_Standard_Idle() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CaptureScaffold(
            captureUiState = { FAKE_PREVIEW_UI_STATE_READY.copy() },
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun CaptureScaffold_ImageOnly_Idle() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CaptureScaffold(
            captureUiState = {
                FAKE_PREVIEW_UI_STATE_READY.copy(
                    captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.IMAGE_ONLY)
                )
            },
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun CaptureScaffold_VideoOnly_Idle() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CaptureScaffold(
            captureUiState = {
                FAKE_PREVIEW_UI_STATE_READY.copy(
                    captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.VIDEO_ONLY)
                )
            },
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun CaptureScaffold_Standard_Recording() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CaptureScaffold(
            captureUiState = { FAKE_PREVIEW_UI_STATE_PRESSED_RECORDING },
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun CaptureScaffold_Locked_Recording() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CaptureScaffold(
            captureUiState = { FAKE_PREVIEW_UI_STATE_LOCKED_RECORDING },
            surfaceRequest = null
        )
    }
}

/**
 * Placeholder [CaptureUiState.Ready] with disabled controls used by host screens while the camera
 * UI state is still [CaptureUiState.NotReady], allowing [CaptureScaffold] to render its layout
 * immediately without waiting for the camera session to finish initializing.
 */
val LOADING_CAPTURE_UI_STATE_SKELETON = CaptureUiState.Ready(
    videoRecordingState = VideoRecordingState.Inactive(),
    externalCaptureMode = ExternalCaptureMode.Standard,
    captureModeToggleUiState = CaptureModeToggleUiState.Unavailable,
    captureButtonUiState = CaptureButtonUiState.Enabled.Idle(
        captureMode = CaptureMode.STANDARD,
        isEnabled = false
    ),
    flipLensUiState = FlipLensUiState.Available(
        selectedLensFacing = LensFacing.BACK,
        availableLensFacings = listOf(SingleSelectableUiState.SelectableUi(LensFacing.BACK))
    )
)

private val FAKE_PREVIEW_UI_STATE_READY = CaptureUiState.Ready(
    videoRecordingState = VideoRecordingState.Inactive(),
    externalCaptureMode = ExternalCaptureMode.Standard,
    captureModeToggleUiState = CaptureModeToggleUiState.Unavailable
)

private val FAKE_PREVIEW_UI_STATE_PRESSED_RECORDING = FAKE_PREVIEW_UI_STATE_READY.copy(
    videoRecordingState = VideoRecordingState.Active.Recording(0, AudioStreamState.Active(0.0), 0),
    captureButtonUiState = CaptureButtonUiState.Enabled.Recording.PressedRecording,
    audioUiState = AudioUiState.Enabled.On(1.0, true)
)

private val FAKE_PREVIEW_UI_STATE_LOCKED_RECORDING = FAKE_PREVIEW_UI_STATE_READY.copy(
    videoRecordingState = VideoRecordingState.Active.Recording(0, AudioStreamState.Active(0.0), 0),
    captureButtonUiState = CaptureButtonUiState.Enabled.Recording.LockedRecording,
    audioUiState = AudioUiState.Enabled.On(1.0, true)
)
