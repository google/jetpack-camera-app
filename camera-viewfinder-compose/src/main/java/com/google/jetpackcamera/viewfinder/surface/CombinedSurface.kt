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

import android.graphics.Bitmap
import android.util.Log
import android.view.Surface
import android.view.View
import androidx.compose.runtime.Composable
import com.google.jetpackcamera.viewfinder.surface.SurfaceType.*

private const val TAG = "CombinedSurface"

@Composable
fun CombinedSurface(
    onSurfaceEvent: (CombinedSurfaceEvent) -> Unit,
    onRequestBitmapReady: (() -> Bitmap?) -> Unit = {},
    type: SurfaceType = TEXTURE_VIEW,
    setView: (View) -> Unit
) {
    Log.d(TAG, "PreviewTexture")

    when (type) {
        SURFACE_VIEW -> Surface {
            when (it) {
                is SurfaceHolderEvent.SurfaceCreated -> {
                    onSurfaceEvent(CombinedSurfaceEvent.SurfaceAvailable(it.holder.surface))
                }

                is SurfaceHolderEvent.SurfaceDestroyed -> {
                    onSurfaceEvent(CombinedSurfaceEvent.SurfaceDestroyed)
                }

                is SurfaceHolderEvent.SurfaceChanged -> {
                    // TODO(yasith@)
                }

            }
        }

        TEXTURE_VIEW -> Texture(

            {
                when (it) {
                    is SurfaceTextureEvent.SurfaceTextureAvailable -> {
                        onSurfaceEvent(CombinedSurfaceEvent.SurfaceAvailable(Surface(it.surface)))
                    }

                    is SurfaceTextureEvent.SurfaceTextureDestroyed -> {
                        onSurfaceEvent(CombinedSurfaceEvent.SurfaceDestroyed)
                    }

                    is SurfaceTextureEvent.SurfaceTextureSizeChanged -> {
                        // TODO(yasith@)
                    }

                    is SurfaceTextureEvent.SurfaceTextureUpdated -> {
                        // TODO(yasith@)
                    }
                }
                true
            },
            onRequestBitmapReady,
            setView = setView
        )
    }
}

sealed interface CombinedSurfaceEvent {
    data class SurfaceAvailable(
        val surface: Surface
    ) : CombinedSurfaceEvent

    object SurfaceDestroyed : CombinedSurfaceEvent
}

enum class SurfaceType {
    SURFACE_VIEW, TEXTURE_VIEW
}

