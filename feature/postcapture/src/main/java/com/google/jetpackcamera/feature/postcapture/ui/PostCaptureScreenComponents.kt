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
package com.google.jetpackcamera.feature.postcapture.ui

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import com.google.jetpackcamera.feature.postcapture.R

@Composable
fun ImageFromBitmap(modifier: Modifier, bitmap: Bitmap?) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.post_capture_image_description),
            modifier = modifier
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(modifier: Modifier, player: ExoPlayer?) {
    val presentationState = rememberPresentationState(player)
    ContentFrame(
        modifier = modifier.resizeWithContentScale(
            ContentScale.Fit,
            presentationState.videoSizeDp
        ),
        player = player
    )
}

/**
 * A button to exit post capture.
 *
 * @param onExitPostCapture the action to be performed when the button is clicked.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CancelPostCaptureButton(onExitPostCapture: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        modifier = modifier
            .size(56.dp)
            .shadow(10.dp, CircleShape)
            .testTag(BUTTON_POST_CAPTURE_EXIT),
        colors = IconButtonDefaults
            .iconButtonColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onExitPostCapture
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.button_exit_description)
        )
    }
}

/**
 * A button to save the current media.
 *
 * @param onClick the action to be performed when the button is clicked.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SaveCurrentMediaButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        modifier = modifier
            .size(56.dp)
            .shadow(10.dp, CircleShape)
            .testTag(BUTTON_POST_CAPTURE_SAVE),
        colors = IconButtonDefaults
            .iconButtonColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Icon(
            imageVector = Icons.Default.SaveAlt,
            contentDescription = stringResource(R.string.button_save_media_description)
        )
    }
}
