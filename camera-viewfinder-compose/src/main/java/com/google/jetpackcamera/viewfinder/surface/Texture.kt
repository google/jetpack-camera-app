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

package com.google.jetpackcamera.viewfinder.surface

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.util.Log
import android.util.Size
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.utils.TransformUtils.getRectToRect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor


private const val TAG = "Texture"

private fun surfaceRotationToRotationDegrees(rotationValue: Int): Int {
    return when (rotationValue) {
        android.view.Surface.ROTATION_90 -> 90
        android.view.Surface.ROTATION_180 -> 180
        android.view.Surface.ROTATION_270 -> 270
        else -> 0
    }
}

@SuppressLint("RestrictedApi")
@Composable
fun Texture(
    onSurfaceTextureEvent: (SurfaceTextureEvent) -> Boolean = { _ -> true },
    onRequestBitmapReady: (() -> Bitmap?) -> Unit,
    setView: (View) -> Unit,
    surfaceRequest: SurfaceRequest?,
    parentSize: Size
) {
    Log.d(TAG, "Texture")


    val resolution = surfaceRequest?.resolution
    var textureView: TextureView? by remember { mutableStateOf(null) }
    if (textureView != null && surfaceRequest != null) {
        surfaceRequest.setTransformationInfoListener(Dispatchers.Main.asExecutor()) {
            val targetRotation = it.targetRotation
            // Check if ready for transformation
            if (resolution != null) {
                val surfaceRect =
                    RectF(0F, 0F, resolution.width.toFloat(), resolution.height.toFloat())
                val correctionMatrix = getRectToRect(
                    surfaceRect,
                    surfaceRect,
                    -surfaceRotationToRotationDegrees(targetRotation)
                )
                textureView!!.setTransform(correctionMatrix)

                textureView!!.pivotX = (parentSize.width / 2).toFloat()
                textureView!!.pivotY = (parentSize.height / 2).toFloat()
                textureView!!.scaleX = parentSize.height.toFloat() / parentSize.width.toFloat()
            }
        }
    }

    if (resolution != null) {
        AndroidView(
            modifier = Modifier.clipToBounds(),
            factory = { context ->
                TextureView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            onSurfaceTextureEvent(
                                SurfaceTextureEvent.SurfaceTextureAvailable(
                                    surface,
                                    width,
                                    height
                                )
                            )
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            onSurfaceTextureEvent(
                                SurfaceTextureEvent.SurfaceTextureSizeChanged(
                                    surface,
                                    width,
                                    height
                                )
                            )
                        }

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            return onSurfaceTextureEvent(
                                SurfaceTextureEvent.SurfaceTextureDestroyed(
                                    surface
                                )
                            )
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                            onSurfaceTextureEvent(SurfaceTextureEvent.SurfaceTextureUpdated(surface))
                        }
                    }
                }
            }, update = {
                textureView = it
                setView(it)
                onRequestBitmapReady { -> it.bitmap }
            }
        )
    }

}

sealed interface SurfaceTextureEvent {
    data class SurfaceTextureAvailable(
        val surface: SurfaceTexture,
        val width: Int,
        val height: Int
    ) : SurfaceTextureEvent

    data class SurfaceTextureSizeChanged(
        val surface: SurfaceTexture,
        val width: Int,
        val height: Int
    ) : SurfaceTextureEvent

    data class SurfaceTextureDestroyed(
        val surface: SurfaceTexture
    ) : SurfaceTextureEvent

    data class SurfaceTextureUpdated(
        val surface: SurfaceTexture
    ) : SurfaceTextureEvent
}