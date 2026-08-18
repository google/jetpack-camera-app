package com.google.jetpackcamera.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.jetpackcamera.core.camera.effects.SingleStreamEffectKey
import com.google.jetpackcamera.model.NONE_EFFECT_ID
import com.google.jetpackcamera.settings.SettingsUiState
import com.google.jetpackcamera.settings.SettingsViewModel
import com.google.jetpackcamera.settings.CameraEffectUiState
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_STREAM_CONFIG_OPTION_MULTI_STREAM_CAPTURE_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_STREAM_CONFIG_OPTION_SINGLE_STREAM_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_STREAM_CONFIG_TAG
import com.google.jetpackcamera.settings.ui.BasicPopupSetting
import com.google.jetpackcamera.settings.ui.SingleChoiceSelector
import com.google.jetpackcamera.settings.ui.disabledRationaleString
import com.google.jetpackcamera.settings.R

@Composable
fun JcaEffectsSetting(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.settingsUiState.collectAsState()
    if (uiState !is SettingsUiState.Enabled) return
    val enabledState = uiState as SettingsUiState.Enabled
    val cameraEffectUiState = enabledState.cameraEffectUiState

    BasicPopupSetting(
        modifier = Modifier.testTag(BTN_OPEN_DIALOG_SETTING_STREAM_CONFIG_TAG),
        title = stringResource(id = R.string.stream_config_title),
        leadingIcon = null,
        enabled = cameraEffectUiState is CameraEffectUiState.Enabled,
        description = when (cameraEffectUiState) {
            is CameraEffectUiState.Enabled -> {
                if (cameraEffectUiState.currentCameraEffect == NONE_EFFECT_ID) {
                    stringResource(id = R.string.stream_config_description_multi_stream)
                } else {
                    stringResource(id = R.string.stream_config_description_single_stream)
                }
            }

            is CameraEffectUiState.Disabled -> {
                disabledRationaleString(disabledRationale = cameraEffectUiState.disabledRationale)
            }
        },
        popupContents = {
            Column(Modifier.selectableGroup()) {
                if (cameraEffectUiState is CameraEffectUiState.Enabled) {
                    if (cameraEffectUiState.supportedEffects.contains(SingleStreamEffectKey.id)) {
                        SingleChoiceSelector(
                            modifier = Modifier.testTag(
                                BTN_DIALOG_STREAM_CONFIG_OPTION_SINGLE_STREAM_TAG
                            ),
                            text = stringResource(
                                id = R.string.stream_config_selector_single_stream
                            ),
                            selected = cameraEffectUiState.currentCameraEffect == SingleStreamEffectKey.id,
                            enabled = true,
                            onClick = { viewModel.setCameraEffect(SingleStreamEffectKey.id) }
                        )
                    }
                    SingleChoiceSelector(
                        modifier = Modifier.testTag(
                            BTN_DIALOG_STREAM_CONFIG_OPTION_MULTI_STREAM_CAPTURE_TAG
                        ),
                        text = stringResource(
                            id = R.string.stream_config_selector_multi_stream
                        ),
                        selected = cameraEffectUiState.currentCameraEffect == NONE_EFFECT_ID,
                        enabled = true,
                        onClick = { viewModel.setCameraEffect(NONE_EFFECT_ID) }
                    )
                }
            }
        }
    )
}
