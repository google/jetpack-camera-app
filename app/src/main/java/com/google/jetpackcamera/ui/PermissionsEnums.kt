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

package com.google.jetpackcamera.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.google.jetpackcamera.R

val CAMERA_PERMISSION = "android.permission.CAMERA"

// val  AUDIO_RECORD_PERMISSION = "android.permission.CAMERA"

/**
 * Helper class storing a permission's relevant UI information
 */
interface PermissionInfoProvider {
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

    fun getPermission(): String

    @DrawableRes
    fun getDrawableResId(): Int?

    fun getImageVector(): ImageVector?

    @StringRes
    fun getPermissionTitleResId(): Int

    @StringRes
    fun getPermissionBodyTextResId(): Int

    @StringRes
    fun getIconAccessiblityTextResId(): Int
}


enum class PermissionEnum : PermissionInfoProvider {

    CAMERA {
        override fun getPermission(): String = CAMERA_PERMISSION

        override fun getDrawableResId(): Int? = R.drawable.photo_camera

        override fun getImageVector(): ImageVector? = null

        override fun getPermissionTitleResId(): Int = R.string.camera_permission_screen_title

        override fun getPermissionBodyTextResId(): Int =
            R.string.camera_permission_required_rationale

        override fun getIconAccessiblityTextResId(): Int =
            R.string.camera_permission_accessibility_text
    },
}

