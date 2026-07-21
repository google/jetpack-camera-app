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
package com.google.jetpackcamera.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.google.jetpackcamera.settings.ui.VersionInfo
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun versionInfo_displaysVersionStrings() {
        val mockVersionInfo = VersionInfoHolder(
            primaryVersionString = "1.2.3",
            detailedLabels = listOf(
                "Build Origin" to "MockOrigin",
                "Git SHA" to "mock-sha-123"
            )
        )

        composeTestRule.setContent {
            // Using VersionInfo directly since it isolates the logic under test
            // from the ViewModel requirements of the full SettingsScreen.
            VersionInfo(versionInfo = mockVersionInfo)
        }

        // Verify primary string is displayed
        composeTestRule.onNodeWithText("1.2.3").assertIsDisplayed()

        // Verify distinct detailed rows are displayed
        composeTestRule.onNodeWithText("Build Origin").assertIsDisplayed()
        composeTestRule.onNodeWithText("MockOrigin").assertIsDisplayed()
        composeTestRule.onNodeWithText("Git SHA").assertIsDisplayed()
        composeTestRule.onNodeWithText("mock-sha-123").assertIsDisplayed()
    }
}
