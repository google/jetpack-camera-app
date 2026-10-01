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
package com.google.jetpackcamera.ui.components.capture

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CameraErrorDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun cameraErrorDialog_displaysTitleBodyAndConfirmButton_andInvokesOnConfirm() {
        var confirmed = false
        composeTestRule.setContent {
            MaterialTheme {
                CameraErrorDialog(
                    title = "Can't use camera",
                    body = "The camera is being used by another app",
                    confirmButtonText = "OK",
                    onConfirm = { confirmed = true }
                )
            }
        }

        composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_TITLE_TAG)
            .assertIsDisplayed()
            .assertTextEquals("Can't use camera")
        composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_BODY_TAG)
            .assertIsDisplayed()
            .assertTextEquals("The camera is being used by another app")
        composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_CONFIRM_BUTTON_TAG)
            .assertIsDisplayed()
            .performClick()

        assertThat(confirmed).isTrue()
    }

    @Test
    fun cameraErrorDialog_whenBodyIsNull_doesNotDisplayBody() {
        composeTestRule.setContent {
            MaterialTheme {
                CameraErrorDialog(
                    title = "Something went wrong",
                    body = null,
                    confirmButtonText = "OK",
                    onConfirm = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_TITLE_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_BODY_TAG).assertDoesNotExist()
    }
}
