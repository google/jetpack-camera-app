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

import android.graphics.RectF
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState
import kotlin.math.min

@Composable
fun ImageWell(
    modifier: Modifier = Modifier,
    imageWellUiState: ImageWellUiState = ImageWellUiState.Unavailable,
    onClick: () -> Unit
) {
    when (imageWellUiState) {
        is ImageWellUiState.LastCapture -> {
            val bitmap = when (val mediaDescriptor = imageWellUiState.mediaDescriptor) {
                is MediaDescriptor.Content.Image ->
                    mediaDescriptor.thumbnail

                is MediaDescriptor.Content.Video ->
                    mediaDescriptor.thumbnail

                is MediaDescriptor.None -> null
            }

            bitmap?.let {
                Box(
                    modifier = modifier
                        .testTag(IMAGE_WELL_TAG)
                        .size(120.dp)
                        .padding(18.dp)
                        .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onClick)
                ) {
                    AnimatedContent(
                        targetState = bitmap
                    ) { targetBitmap ->
                        Canvas(
                            modifier = Modifier
                                .size(110.dp)
                        ) {
                            drawIntoCanvas { canvas ->
                                val canvasSize = min(size.width, size.height)

                                val scale = canvasSize / min(
                                    targetBitmap.width,
                                    targetBitmap.height
                                )

                                val imageWidth = targetBitmap.width * scale
                                val imageHeight = targetBitmap.height * scale

                                val offsetX = (canvasSize - imageWidth) / 2f
                                val offsetY = (canvasSize - imageHeight) / 2f

                                canvas.nativeCanvas.drawBitmap(
                                    targetBitmap,
                                    null,
                                    RectF(
                                        offsetX,
                                        offsetY,
                                        offsetX + imageWidth,
                                        offsetY + imageHeight
                                    ),
                                    null
                                )
                            }
                        }
                    }
                }
            }
        }

        is ImageWellUiState.Unavailable -> {
        }
    }
}
