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
package com.google.jetpackcamera.permissions

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.google.jetpackcamera.permissions.ui.CAMERA_PERMISSION_BUTTON
import com.google.jetpackcamera.permissions.ui.RECORD_AUDIO_PERMISSION_BUTTON

const val CAMERA_PERMISSION = "android.permission.CAMERA"
const val AUDIO_RECORD_PERMISSION = "android.permission.RECORD_AUDIO"

/**
 * Helper class storing a permission's relevant UI information
 */
sealed interface PermissionInfoProvider {
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

    /**
     * @return the String reference for the permission
     */
    fun getPermission(): String

    fun isOptional(): Boolean

    fun getTestTag(): String

    @DrawableRes
    fun getDrawableResId(): Int?

    fun getImageVector(): ImageVector?

    @StringRes
    fun getPermissionTitleResId(): Int

    @StringRes
    fun getPermissionBodyTextResId(): Int

    @StringRes
    fun getRationaleBodyTextResId(): Int?

    @StringRes
    fun getIconAccessibilityTextResId(): Int
}

/**
 * Implementation of [PermissionInfoProvider]
 * Supplies the information needed for a permission's UI screen
 */
enum class PermissionEnum : PermissionInfoProvider {

    CAMERA {
        override fun getPermission(): String = CAMERA_PERMISSION

        override fun isOptional(): Boolean = false

        override fun getTestTag(): String = CAMERA_PERMISSION_BUTTON

        override fun getDrawableResId(): Int? = null

        override fun getImageVector(): ImageVector = Icons.Outlined.CameraAlt

        override fun getPermissionTitleResId(): Int = R.string.camera_permission_screen_title

        override fun getPermissionBodyTextResId(): Int =
            R.string.camera_permission_required_rationale

        override fun getRationaleBodyTextResId(): Int =
            R.string.camera_permission_declined_rationale

        override fun getIconAccessibilityTextResId(): Int =
            R.string.camera_permission_accessibility_text
    },

    RECORD_AUDIO {
        override fun getPermission(): String = AUDIO_RECORD_PERMISSION

        override fun isOptional(): Boolean = true

        override fun getTestTag(): String = RECORD_AUDIO_PERMISSION_BUTTON

        override fun getDrawableResId(): Int? = null

        override fun getImageVector(): ImageVector = Icons.Outlined.Mic

        override fun getPermissionTitleResId(): Int = R.string.microphone_permission_screen_title

        override fun getPermissionBodyTextResId(): Int =
            R.string.microphone_permission_required_rationale

        override fun getRationaleBodyTextResId(): Int? = null

        override fun getIconAccessibilityTextResId(): Int =
            R.string.microphone_permission_accessibility_text
    }
}
