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
package com.google.jetpackcamera.feature.postcapture

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.jetpackcamera.feature.postcapture.ui.BUTTON_POST_CAPTURE_DELETE
import com.google.jetpackcamera.feature.postcapture.ui.BUTTON_POST_CAPTURE_EXIT
import com.google.jetpackcamera.feature.postcapture.ui.BUTTON_POST_CAPTURE_SAVE
import com.google.jetpackcamera.feature.postcapture.ui.BUTTON_POST_CAPTURE_SHARE
import com.google.jetpackcamera.ui.uistate.postcapture.DeleteButtonUiState
import com.google.jetpackcamera.ui.uistate.postcapture.MediaViewerUiState
import com.google.jetpackcamera.ui.uistate.postcapture.PostCaptureUiState
import com.google.jetpackcamera.ui.uistate.postcapture.ShareButtonUiState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostCaptureAccessibilityTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(
            AccessibilityValidator()
                .setRunChecksFromRootView(true)
                .setThrowExceptionFor(AccessibilityCheckResultType.ERROR)
        )
    }

    @Test
    fun postCaptureScreen_readyWithAllActionButtons_meetsAccessibilityChecks() {
        val sampleBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.DKGRAY)
        }

        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                PostCaptureComponent(
                    uiState = PostCaptureUiState.Ready(
                        viewerUiState = MediaViewerUiState.Content.Image(sampleBitmap),
                        deleteButtonUiState = DeleteButtonUiState.Ready,
                        shareButtonUiState = ShareButtonUiState.Ready
                    ),
                    onNavigateBack = {},
                    onSaveMedia = {},
                    onShareCurrentMedia = {},
                    onDeleteMedia = {},
                    onLoadVideo = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_EXIT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SAVE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SHARE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_DELETE).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}
