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

import android.graphics.Matrix
import android.graphics.Rect
import androidx.camera.core.MeteringPoint
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SurfaceToSensorMeteringPointFactoryTest {

    private val sensorRect = Rect(0, 0, 4000, 3000) // 4:3 aspect ratio
    private var bufferWidth: Int = 0
    private var bufferHeight: Int = 0
    private lateinit var factory: SurfaceToSensorMeteringPointFactory

    @Before
    fun setUp() {
        // Simulate a 16:9 buffer centered within the 4:3 sensor.
        bufferWidth = sensorRect.width()
        bufferHeight = bufferWidth * 9 / 16 // 4000 * 9 / 16 = 2250
        val yOffset = (sensorRect.height() - bufferHeight) / 2f // (3000 - 2250) / 2 = 375

        // The transform from sensor to buffer is a vertical translation.
        val sensorToBufferTransform = Matrix().apply {
            postTranslate(0f, -yOffset)
        }

        factory = SurfaceToSensorMeteringPointFactory(sensorRect, sensorToBufferTransform)
    }

    @Test
    fun createPoint_topLeftTap_returnsCorrectNormalizedPoint() {
        // Act
        val meteringPoint: MeteringPoint = factory.createPoint(0f, 0f)

        // Assert
        val expectedX = 0.0f
        val expectedY = 0.125f // 375 / 3000
        assertThat(meteringPoint.x).isWithin(0.001f).of(expectedX)
        assertThat(meteringPoint.y).isWithin(0.001f).of(expectedY)
    }

    @Test
    fun createPoint_topRightTap_returnsCorrectNormalizedPoint() {
        // Act
        val meteringPoint: MeteringPoint = factory.createPoint(bufferWidth.toFloat(), 0f)

        // Assert
        val expectedX = 1.0f
        val expectedY = 0.125f // 375 / 3000
        assertThat(meteringPoint.x).isWithin(0.001f).of(expectedX)
        assertThat(meteringPoint.y).isWithin(0.001f).of(expectedY)
    }

    @Test
    fun createPoint_bottomLeftTap_returnsCorrectNormalizedPoint() {
        // Act
        val meteringPoint: MeteringPoint = factory.createPoint(0f, bufferHeight.toFloat())

        // Assert
        val expectedX = 0.0f
        val expectedY = 0.875f // (2250 + 375) / 3000
        assertThat(meteringPoint.x).isWithin(0.001f).of(expectedX)
        assertThat(meteringPoint.y).isWithin(0.001f).of(expectedY)
    }

    @Test
    fun createPoint_bottomRightTap_returnsCorrectNormalizedPoint() {
        // Act
        val meteringPoint: MeteringPoint = factory.createPoint(
            bufferWidth.toFloat(),
            bufferHeight.toFloat()
        )

        // Assert
        val expectedX = 1.0f
        val expectedY = 0.875f // (2250 + 375) / 3000
        assertThat(meteringPoint.x).isWithin(0.001f).of(expectedX)
        assertThat(meteringPoint.y).isWithin(0.001f).of(expectedY)
    }

    @Test
    fun createPoint_centerTap_returnsCorrectNormalizedPoint() {
        // Act
        val meteringPoint: MeteringPoint = factory.createPoint(
            bufferWidth / 2f,
            bufferHeight / 2f
        )

        // Assert
        val expectedX = 0.5f
        val expectedY = 0.5f
        assertThat(meteringPoint.x).isWithin(0.001f).of(expectedX)
        assertThat(meteringPoint.y).isWithin(0.001f).of(expectedY)
    }
}
