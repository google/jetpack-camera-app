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
import android.graphics.RectF
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.camera.core.SurfaceRequest
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

private const val TAG = "Surface"

// Suppress warnings for TransformationInfo.hasCameraTransform() and SurfaceRequest.getCamera()
@SuppressLint("RestrictedApi")
@Composable
fun Surface(
    modifier: Modifier,
    setView: (View) -> Unit,
    surfaceRequest: SurfaceRequest?,
    onSurfaceHolderEvent: (SurfaceHolderEvent) -> Unit = { _ -> }
) {
    Log.d(TAG, "Surface")

    val resolution = surfaceRequest?.resolution
    var surfaceView: SurfaceView? by remember { mutableStateOf(null) }
    var parentView: FrameLayout? by remember { mutableStateOf(null) }
    if (parentView != null && surfaceRequest != null && resolution != null) {
        surfaceRequest.setTransformationInfoListener(
            Dispatchers.Main.asExecutor()
        ) { transformationInfo ->
            val parentViewSize = Size(parentView!!.width, parentView!!.height)
            if (parentViewSize.height == 0 || parentViewSize.width == 0) {
                return@setTransformationInfoListener
            }
            val viewFinder = surfaceView!!
            val surfaceRectInViewFinder: RectF =
                SurfaceTransformationUtil.getTransformedSurfaceRect(
                    resolution,
                    transformationInfo,
                    parentViewSize,
                    surfaceRequest.camera.isFrontFacing
                )
            if (!transformationInfo.hasCameraTransform()) {
                viewFinder.layoutParams =
                    FrameLayout.LayoutParams(
                        surfaceRectInViewFinder.width().toInt(),
                        surfaceRectInViewFinder.height().toInt()
                    )
            } else {
                viewFinder.layoutParams =
                    FrameLayout.LayoutParams(
                        resolution.width,
                        resolution.height
                    )
            }

            viewFinder.pivotX = 0f
            viewFinder.pivotY = 0f
            viewFinder.scaleX = surfaceRectInViewFinder.width() / resolution.width
            viewFinder.scaleY = surfaceRectInViewFinder.height() / resolution.height
            viewFinder.translationX = surfaceRectInViewFinder.left - viewFinder.left
            viewFinder.translationY = surfaceRectInViewFinder.top - viewFinder.top
        }
    }

    if (resolution != null) {
        AndroidView(
            modifier = modifier.clipToBounds(),
            factory = { context ->
                FrameLayout(context).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    addView(
                        SurfaceView(context).apply {
                            layoutParams =
                                LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            holder.addCallback(
                                object : SurfaceHolder.Callback {
                                    override fun surfaceCreated(holder: SurfaceHolder) {
                                        onSurfaceHolderEvent(
                                            SurfaceHolderEvent.SurfaceCreated(
                                                holder
                                            )
                                        )
                                    }

                                    override fun surfaceChanged(
                                        holder: SurfaceHolder,
                                        format: Int,
                                        width: Int,
                                        height: Int
                                    ) {
                                        onSurfaceHolderEvent(
                                            SurfaceHolderEvent.SurfaceChanged(
                                                holder,
                                                width,
                                                height
                                            )
                                        )
                                    }

                                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                                        onSurfaceHolderEvent(
                                            SurfaceHolderEvent.SurfaceDestroyed(
                                                holder
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    )
                }
            },
            update = {
                parentView = it
                surfaceView = it.getChildAt(0) as SurfaceView?
                setView(it)
            }
        )
    }
}

sealed interface SurfaceHolderEvent {
    data class SurfaceCreated(
        val holder: SurfaceHolder
    ) : SurfaceHolderEvent

    data class SurfaceChanged(
        val holder: SurfaceHolder,
        val width: Int,
        val height: Int
    ) : SurfaceHolderEvent

    data class SurfaceDestroyed(
        val holder: SurfaceHolder
    ) : SurfaceHolderEvent
}
