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
package com.google.jetpackcamera.core.camera

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.Rational
import androidx.camera.core.MeteringPointFactory

/**
 * A [MeteringPointFactory] that converts coordinates from a [android.view.Surface]'s coordinate
 * system to normalized sensor coordinates.
 *
 * @param sensorRect The rectangle representing the sensor's active array.
 * @param sensorToBufferTransform The transform from sensor coordinates to buffer coordinates.
 */
@SuppressLint("RestrictedApi")
class SurfaceToSensorMeteringPointFactory(
    private val sensorRect: Rect,
    private val sensorToBufferTransform: Matrix
) : MeteringPointFactory(Rational(sensorRect.width(), sensorRect.height())) {

    private val surfaceToNormalizedSensorTransform by lazy {
        val fullTransform = Matrix()
        // Map surface coordinates to sensor coordinates
        sensorToBufferTransform.invert(fullTransform)

        // Map surface coordinates to normalized coordinates
        fullTransform.postConcat(
            Matrix().apply {
                setRectToRect(
                    RectF(0f, 0f, sensorRect.width().toFloat(), sensorRect.height().toFloat()),
                    RectF(0f, 0f, 1f, 1f),
                    Matrix.ScaleToFit.FILL
                )
            }
        )

        fullTransform
    }

    override fun convertPoint(x: Float, y: Float): PointF {
//        val point = floatArrayOf(x, y)
//        surfaceToNormalizedSensorTransform.mapPoints(point)
//        return PointF(point[0], point[1])
        return PointF(0.5f, 0.5f)
    }
}
