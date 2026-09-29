/*
 * Copyright (C) 2026 The Android Open Source Project
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
package com.google.jetpackcamera.core.camera.concurrent

import android.Manifest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit.assume
import com.google.jetpackcamera.core.camera.appLensFacing
import com.google.jetpackcamera.model.LensFacing
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ConcurrentCameraSessionTest {

    @get:Rule
    val permissionsRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun isSupported_matchesAvailableConcurrentCameraInfos(): Unit = runBlocking {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        val firstCameraInfo = cameraProvider.availableCameraInfos.firstOrNull()
        assume()
            .withMessage("No cameras available on device, skip the test.")
            .that(firstCameraInfo != null)
            .isTrue()

        val expectedSupported = cameraProvider.availableConcurrentCameraInfos.any { group ->
            group.map { it.appLensFacing }.toSet() == setOf(LensFacing.FRONT, LensFacing.BACK)
        }

        val provider = ConcurrentCameraSubModeProvider()
        val actualSupported = provider.isSupported(
            context = context,
            cameraProvider = cameraProvider,
            cameraInfo = firstCameraInfo!!
        )

        assertThat(actualSupported).isEqualTo(expectedSupported)
    }
}
