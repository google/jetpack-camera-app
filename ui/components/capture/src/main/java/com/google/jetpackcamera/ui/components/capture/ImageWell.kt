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

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState

@Composable
fun ImageWell(
    imageWellUiState: ImageWellUiState.LastCapture,
    shape: Shape = RoundedCornerShape(16.dp),
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val lastCapture = imageWellUiState.mediaDescriptor

    Box(
        modifier = modifier
            .testTag(IMAGE_WELL_TAG)
            .size(120.dp)
            .padding(18.dp)
            .border(2.dp, Color.White, shape)
            .clip(shape)
            .clickable(onClick = onClick, enabled = enabled)
    ) {
        AnimatedContent(targetState = lastCapture, label = "ImageWellAnimation") { contentDesc ->
            contentDesc.thumbnail?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = stringResource(
                        id = R.string.image_well_content_description
                    ),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(110.dp)
                )
            }
        }
    }
}
