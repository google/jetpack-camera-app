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
package com.google.jetpackcamera.feature.quicksettings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.jetpackcamera.quicksettings.R

interface QuickSettingsEnum {
    @DrawableRes
    fun getDrawableResId(): Int

    @StringRes
    fun getTextResId(): Int

    @StringRes
    fun getDescriptionResId(): Int
}

enum class CameraLensFace : QuickSettingsEnum {
    FRONT {
        override fun getDrawableResId(): Int = R.drawable.baseline_cameraswitch_72

        override fun getTextResId(): Int = R.string.quick_settings_front_camera_text

        override fun getDescriptionResId(): Int = R.string.quick_settings_front_camera_description
    },
    BACK {
        override fun getDrawableResId(): Int = R.drawable.baseline_cameraswitch_72

        override fun getTextResId(): Int = R.string.quick_settings_back_camera_text

        override fun getDescriptionResId(): Int = R.string.quick_settings_back_camera_description
    }
}

enum class CameraFlashMode : QuickSettingsEnum {
    OFF {
        override fun getDrawableResId(): Int = R.drawable.baseline_flash_off_72

        override fun getTextResId(): Int = R.string.quick_settings_flash_off

        override fun getDescriptionResId(): Int = R.string.quick_settings_flash_off_description
    },
    AUTO {
        override fun getDrawableResId(): Int = R.drawable.baseline_flash_auto_72

        override fun getTextResId(): Int = R.string.quick_settings_flash_auto

        override fun getDescriptionResId(): Int = R.string.quick_settings_flash_auto_description
    },
    ON {
        override fun getDrawableResId(): Int = R.drawable.baseline_flash_on_72

        override fun getTextResId(): Int = R.string.quick_settings_flash_on

        override fun getDescriptionResId(): Int = R.string.quick_settings_flash_on_description
    }
}

enum class CameraAspectRatio : QuickSettingsEnum {
    THREE_FOUR {
        override fun getDrawableResId(): Int = R.drawable.baseline_aspect_ratio_72

        override fun getTextResId(): Int = R.string.quick_settings_aspect_ratio_3_4

        override fun getDescriptionResId(): Int =
            R.string.quick_settings_aspect_ratio_3_4_description
    },
    NINE_SIXTEEN {
        override fun getDrawableResId(): Int = R.drawable.baseline_aspect_ratio_72

        override fun getTextResId(): Int = R.string.quick_settings_aspect_ratio_9_16

        override fun getDescriptionResId(): Int =
            R.string.quick_settings_aspect_ratio_9_16_description
    },
    ONE_ONE {
        override fun getDrawableResId(): Int = R.drawable.baseline_aspect_ratio_72

        override fun getTextResId(): Int = R.string.quick_settings_aspect_ratio_1_1

        override fun getDescriptionResId(): Int =
            R.string.quick_settings_aspect_ratio_1_1_description
    }
}
