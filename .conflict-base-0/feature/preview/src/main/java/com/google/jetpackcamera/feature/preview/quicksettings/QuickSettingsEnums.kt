/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.jetpackcamera.feature.preview.quicksettings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HdrOff
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.google.jetpackcamera.feature.preview.R

interface QuickSettingsEnum {
    @Composable
    fun getPainter(): Painter {
        val iconResId = getDrawableResId()
        val iconVector = getImageVector()
        require((iconResId == null).xor(iconVector == null)) {
            "UI Item should have exactly one of iconResId or iconVector set."
        }
        return iconResId?.let { painterResource(it) }
            ?: iconVector?.let {
                rememberVectorPainter(
                    it
                )
            }!! // !! allowed because we've checked null
    }

    @DrawableRes
    fun getDrawableResId(): Int?

    fun getImageVector(): ImageVector?

    @StringRes
    fun getTextResId(): Int

    @StringRes
    fun getDescriptionResId(): Int
}

enum class CameraLensFace : QuickSettingsEnum {
    FRONT {
        override fun getDrawableResId() = null
        override fun getImageVector() = Icons.Filled.Cameraswitch
        override fun getTextResId() = R.string.quick_settings_front_camera_text
        override fun getDescriptionResId() = R.string.quick_settings_front_camera_description
    },
    BACK {
        override fun getDrawableResId() = null
        override fun getImageVector() = Icons.Filled.Cameraswitch
        override fun getTextResId() = R.string.quick_settings_back_camera_text
        override fun getDescriptionResId() = R.string.quick_settings_back_camera_description
    }
}

enum class CameraFlashMode : QuickSettingsEnum {
    OFF {
        override fun getDrawableResId() = null
        override fun getImageVector() = Icons.Filled.FlashOff
        override fun getTextResId() = R.string.quick_settings_flash_off
        override fun getDescriptionResId() = R.string.quick_settings_flash_off_description
    },
    AUTO {
        override fun getDrawableResId() = null
        override fun getImageVector() = Icons.Filled.FlashAuto
        override fun getTextResId() = R.string.quick_settings_flash_auto
        override fun getDescriptionResId() = R.string.quick_settings_flash_auto_description
    },
    ON {
        override fun getDrawableResId() = null
        override fun getImageVector() = Icons.Filled.FlashOn
        override fun getTextResId() = R.string.quick_settings_flash_on
        override fun getDescriptionResId() = R.string.quick_settings_flash_on_description
    }
}

enum class CameraAspectRatio : QuickSettingsEnum {
    THREE_FOUR {
        override fun getDrawableResId() = null
        override fun getImageVector() = Icons.Filled.AspectRatio
        override fun getTextResId() = R.string.quick_settings_aspect_ratio_3_4
        override fun getDescriptionResId() = R.string.quick_settings_aspect_ratio_3_4_description
    },
    NINE_SIXTEEN {
        override fun getDrawableResId() = null
        override fun getImageVector() = Icons.Filled.AspectRatio
        override fun getTextResId() = R.string.quick_settings_aspect_ratio_9_16
        override fun getDescriptionResId() = R.string.quick_settings_aspect_ratio_9_16_description
    },
    ONE_ONE {
        override fun getDrawableResId() = null
        override fun getImageVector() = Icons.Filled.AspectRatio
        override fun getTextResId() = R.string.quick_settings_aspect_ratio_1_1
        override fun getDescriptionResId() = R.string.quick_settings_aspect_ratio_1_1_description
    }
}

enum class CameraCaptureMode : QuickSettingsEnum {
    MULTI_STREAM {
        override fun getDrawableResId() = R.drawable.multi_stream_icon
        override fun getImageVector() = null // this icon is not available
        override fun getTextResId() = R.string.quick_settings_capture_mode_multi
        override fun getDescriptionResId() = R.string.quick_settings_capture_mode_multi_description
    },
    SINGLE_STREAM {
        override fun getDrawableResId() = R.drawable.single_stream_capture_icon
        override fun getImageVector() = null // this icon is not available
        override fun getTextResId() = R.string.quick_settings_capture_mode_single
        override fun getDescriptionResId() = R.string.quick_settings_capture_mode_single_description
    }
}

enum class CameraDynamicRange : QuickSettingsEnum {
    SDR {
        override fun getDrawableResId() = null
        override fun getImageVector() = Icons.Filled.HdrOff
        override fun getTextResId() = R.string.quick_settings_dynamic_range_sdr
        override fun getDescriptionResId() = R.string.quick_settings_dynamic_range_sdr_description
    },
    HDR {
        override fun getDrawableResId() = null
        override fun getImageVector() = Icons.Filled.HdrOn
        override fun getTextResId() = R.string.quick_settings_dynamic_range_hdr
        override fun getDescriptionResId() = R.string.quick_settings_dynamic_range_hdr_description
    }
}

enum class CameraLowLightBoost : QuickSettingsEnum {

    ENABLED {
        override fun getDrawableResId() = null
        override fun getImageVector() = Icons.Filled.Nightlight
        override fun getTextResId() = R.string.quick_settings_lowlightboost_enabled
        override fun getDescriptionResId() =
            R.string.quick_settings_lowlightboost_enabled_description
    },

    DISABLED {
        override fun getDrawableResId() = null
        override fun getImageVector() = Icons.Outlined.Nightlight
        override fun getTextResId() = R.string.quick_settings_lowlightboost_disabled
        override fun getDescriptionResId() =
            R.string.quick_settings_lowlightboost_disabled_description
    }
}

enum class CameraConcurrentCameraMode : QuickSettingsEnum {
    OFF {
        override fun getDrawableResId() = R.drawable.picture_in_picture_off_icon
        override fun getImageVector() = null
        override fun getTextResId() = R.string.quick_settings_concurrent_camera_off
        override fun getDescriptionResId() =
            R.string.quick_settings_concurrent_camera_off_description
    },
    DUAL {
        override fun getDrawableResId() = null
        override fun getImageVector() = Icons.Filled.PictureInPicture
        override fun getTextResId() = R.string.quick_settings_concurrent_camera_dual
        override fun getDescriptionResId() =
            R.string.quick_settings_concurrent_camera_dual_description
    }
}
