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
package com.google.jetpackcamera.viewfinder

import android.annotation.SuppressLint
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.SurfaceRequest.TransformationInfo
import androidx.camera.view.PreviewView.ImplementationMode
import androidx.compose.foundation.AndroidEmbeddedExternalSurface
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.AndroidExternalSurfaceScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.setFrom
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.google.jetpackcamera.viewfinder.surface.SurfaceTransformationUtil
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "CameraPreview"

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    implementationMode: ImplementationMode = ImplementationMode.COMPATIBLE,
    onSurfaceProviderReady: (SurfaceProvider) -> Unit = {}
) {
    Log.d(TAG, "CameraPreview")

    val surfaceRequest by produceState<SurfaceRequest?>(initialValue = null) {
        onSurfaceProviderReady(
            SurfaceProvider { request ->
                Log.d(TAG, "newSurfaceRequest")
                value?.willNotProvideSurface()
                value = request
            }
        )
    }

    val transformationInfo by produceState<TransformationInfo?>(
        key1 = surfaceRequest,
        initialValue = null
    ) {
        surfaceRequest?.let {
            it.setTransformationInfoListener(
                Dispatchers.Main.asExecutor()
            ) { transformationInfo ->
                Log.d(TAG, "TransformationInfo: $it")
                value = transformationInfo
            }
        }

        awaitDispose {
            surfaceRequest?.let { it.clearTransformationInfoListener() }
        }
    }

    surfaceRequest?.let { surfaceRequest ->
        transformationInfo?.let { transformationInfo ->
            Viewfinder(
                surfaceRequest = surfaceRequest,
                implementationMode = implementationMode,
                transformationInfo = transformationInfo,
                modifier = modifier
            )
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun Viewfinder(
    surfaceRequest: SurfaceRequest,
    implementationMode: ImplementationMode,
    transformationInfo: TransformationInfo,
    modifier: Modifier = Modifier
) {
    Log.d(TAG, "PreviewSurface")

    var availableSurface by remember { mutableStateOf<Surface?>(null) }
    var parentViewSize = IntSize.Zero

    val resolution = surfaceRequest.resolution
    val isFrontFacing = surfaceRequest.camera.isFrontFacing

    val cropRectSize =
        IntSize(transformationInfo.cropRect.width(), transformationInfo.cropRect.height())

    Log.d(TAG, "resolution: $resolution")
    Log.d(TAG, "cropRect: ${transformationInfo.cropRect}")
    Log.d(TAG, "cropRectSize: $cropRectSize")

    Box(
        modifier = modifier
            .onSizeChanged {
                parentViewSize = it
            }
            .clipToBounds()
            .wrapContentSize(unbounded = true, align = Alignment.Center)
    ) {
        TransformedSurface(
            resolution = resolution,
            isFrontFacing = isFrontFacing,
            transformationInfo = transformationInfo,
            implementationMode = implementationMode,
            getParentSize = { parentViewSize },
            onSurface = {
                availableSurface = it
            }
        )
    }

    LaunchedEffect(surfaceRequest) {
        availableSurface?.let { surfaceRequest.provideSurface(it) }.also { availableSurface = it }
    }
}

private suspend fun SurfaceRequest.provideSurface(surface: Surface): Surface =
    suspendCancellableCoroutine {
        this.provideSurface(surface, Dispatchers.Main.asExecutor()) { result ->
            Log.d(TAG, "Releasing the available surface")
            it.resume(result.surface)
        }
        it.invokeOnCancellation {
            this.willNotProvideSurface()
        }
    }

@SuppressLint("RestrictedApi")
@Composable
private fun TransformedSurface(
    resolution: Size,
    isFrontFacing: Boolean,
    transformationInfo: TransformationInfo,
    implementationMode: ImplementationMode,
    onSurface: (Surface) -> Unit,
    getParentSize: () -> IntSize
) {
    Log.d(TAG, "Creating TransformedSurface")

    // For TextureView, correct the orientation to match the target rotation.
    val correctionMatrix = Matrix()
    transformationInfo.let {
        correctionMatrix.setFrom(
            SurfaceTransformationUtil.getTextureViewCorrectionMatrix(
                it,
                resolution
            )
        )
    }

    val getSurfaceRectInViewFinder = {
        SurfaceTransformationUtil.getTransformedSurfaceRect(
            resolution,
            transformationInfo,
            getParentSize().toSize(),
            isFrontFacing
        )
    }

    var viewFinderWidth = resolution.width
    var viewFinderHeight = resolution.height

    val getViewFinderScaleX = { getSurfaceRectInViewFinder().width() / resolution.width }
    val getViewFinderScaleY = { getSurfaceRectInViewFinder().height() / resolution.height }

    val heightDp = with(LocalDensity.current) { viewFinderHeight.toDp() }
    val widthDp = with(LocalDensity.current) { viewFinderWidth.toDp() }

    val getModifier: () -> Modifier = {
        Modifier
            .height(heightDp)
            .width(widthDp)
            .scale(getViewFinderScaleX(), getViewFinderScaleY())
    }

    val onInit: AndroidExternalSurfaceScope.() -> Unit = {
        onSurface { newSurface, _, _ ->
            Log.d(TAG, "Providing Surface from AndroidExternalSurface")
            onSurface(newSurface)
        }
    }

    when (implementationMode) {
        ImplementationMode.PERFORMANCE -> {
            AndroidExternalSurface(
                modifier = getModifier(),
                onInit = onInit
            )
        }
        ImplementationMode.COMPATIBLE -> {
            AndroidEmbeddedExternalSurface(
                modifier = getModifier(),
                transform = correctionMatrix,
                onInit = onInit
            )
        }
    }
}

private fun IntSize.toSize() = Size(this.width, this.height)
