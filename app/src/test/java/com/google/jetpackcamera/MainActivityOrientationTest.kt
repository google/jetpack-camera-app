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
package com.google.jetpackcamera

import android.content.pm.ActivityInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for [resolveRequestedOrientation].
 */
@RunWith(JUnit4::class)
class MainActivityOrientationTest {

    @Test
    fun resolveRequestedOrientation_fullScreenCompactPhone_locksToNoSensor() {
        assertThat(
            resolveRequestedOrientation(
                isInMultiWindowMode = false,
                smallestScreenWidthDp = 411
            )
        ).isEqualTo(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR)

        assertThat(
            resolveRequestedOrientation(
                isInMultiWindowMode = false,
                smallestScreenWidthDp = 599
            )
        ).isEqualTo(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR)
    }

    @Test
    fun resolveRequestedOrientation_multiWindowCompact_allowsUnspecifiedOrientation() {
        assertThat(
            resolveRequestedOrientation(
                isInMultiWindowMode = true,
                smallestScreenWidthDp = 411
            )
        ).isEqualTo(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
    }

    @Test
    fun resolveRequestedOrientation_largeScreenOrFoldable_allowsUnspecifiedOrientation() {
        assertThat(
            resolveRequestedOrientation(
                isInMultiWindowMode = false,
                smallestScreenWidthDp = 600
            )
        ).isEqualTo(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)

        assertThat(
            resolveRequestedOrientation(
                isInMultiWindowMode = true,
                smallestScreenWidthDp = 673
            )
        ).isEqualTo(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
    }
}
