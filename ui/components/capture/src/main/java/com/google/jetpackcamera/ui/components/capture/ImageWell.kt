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

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState

/**
 * The inset between the edge of the image well container and the thumbnail, which leaves a frame
 * of the container color around the thumbnail.
 */
private val ThumbnailInset = 2.dp

/**
 * The shape of the thumbnail, concentric with the default image well container shape.
 */
private val ThumbnailShape = RoundedCornerShape(10.dp)

/**
 * A composable that displays thumbnail image that can be clicked to open the full media in
 * post-capture.
 *
 * The container color is determined by [LocalCameraControlBackgroundStyle], so that the image well
 * remains legible when drawn over the viewfinder.
 *
 * @param imageWellUiState the [ImageWellUiState] for this component
 * @param onClick the callback for when the image well is clicked
 * @param modifier the modifier for this component
 * @param shape the shape of the image well's outer container
 * @param enabled true if the image well is enabled
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalAnimationApi::class)
@Composable
fun ImageWell(
    imageWellUiState: ImageWellUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    enabled: Boolean = true
) {
    if (imageWellUiState is ImageWellUiState.Content) {
        val disableAnimations = LocalDisableAnimations.current
        val animationSpec: FiniteAnimationSpec<IntOffset> =
            if (disableAnimations) snap() else tween(300)

        // The container keeps its color when disabled so the control does not fade out over the
        // viewfinder.
        val containerColor = LocalCameraControlBackgroundStyle.current.containerColor

        Box(
            modifier = modifier
                .testTag(IMAGE_WELL_TAG)
                .size(IconButtonDefaults.mediumContainerSize())
                .clip(shape)
                .background(containerColor)
                .clickable(onClick = onClick, enabled = enabled)
                .padding(ThumbnailInset)
                .clip(ThumbnailShape)
        ) {
            AnimatedContent(
                targetState = imageWellUiState.mediaDescriptor,
                modifier = Modifier.fillMaxSize(),
                label = "ImageWellAnimation",
                contentKey = { it.uri },
                transitionSpec = {
                    val enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = animationSpec
                    )
                    val exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = animationSpec
                    )
                    enter.togetherWith(exit).apply {
                        targetContentZIndex = 1f
                    }
                }
            ) { contentDesc ->
                contentDesc.thumbnail?.let { bitmap ->
                    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
                    Image(
                        bitmap = imageBitmap,
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = stringResource(
                            id = R.string.image_well_content_description
                        ),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
internal fun PreviewImageWell() {
    val previewBitmap = createBitmap(1000, 1000).apply {
        eraseColor(android.graphics.Color.BLUE)
    }
    val imageWellUiState = ImageWellUiState.Content(
        mediaDescriptor = MediaDescriptor.Content.Image(
            uri = Uri.EMPTY,
            thumbnail = previewBitmap
        )
    )

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            modifier = Modifier
                .background(Brush.verticalGradient(colors = listOf(Color.Gray, Color.DarkGray)))
                .padding(8.dp)
        ) {
            ImageWell(
                imageWellUiState = imageWellUiState,
                onClick = {}
            )
        }
    }
}

@Preview
@Composable
internal fun ImageWellPreview() {
    PreviewImageWell()
}
