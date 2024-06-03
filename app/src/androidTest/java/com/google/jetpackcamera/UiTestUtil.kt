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
package com.google.jetpackcamera

import android.app.Activity
import android.os.Build
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.google.jetpackcamera.feature.preview.R
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.settings.model.LensFacing
import org.junit.Assert.fail
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

const val APP_START_TIMEOUT_MILLIS = 10_000L
const val IMAGE_CAPTURE_TIMEOUT_MILLIS = 5_000L

inline fun <reified T : Activity> runScenarioTest(
    crossinline block: ActivityScenario<T>.() -> Unit
) {
    ActivityScenario.launch(T::class.java).use { scenario ->
        scenario.apply(block)
    }
}

context(ActivityScenario<MainActivity>)
fun ComposeTestRule.getCurrentLensFacing(): LensFacing {
    var needReturnFromQuickSettings = false
    onNodeWithContentDescription(R.string.quick_settings_dropdown_closed_description).apply {
        if (isDisplayed()) {
            performClick()
            needReturnFromQuickSettings = true
        }
    }

    onNodeWithContentDescription(R.string.quick_settings_dropdown_open_description).assertExists(
        "LensFacing can only be retrieved from PreviewScreen or QuickSettings screen"
    )

    try {
        return onNodeWithTag(QUICK_SETTINGS_FLIP_CAMERA_BUTTON).fetchSemanticsNode(
            "Flip camera button is not visible when expected."
        ).let { node ->
            node.config[SemanticsProperties.ContentDescription].any { description ->
                when (description) {
                    getResString(R.string.quick_settings_front_camera_description) ->
                        return@let LensFacing.FRONT

                    getResString(R.string.quick_settings_back_camera_description) ->
                        return@let LensFacing.BACK

                    else -> false
                }
            }
            throw AssertionError("Unable to determine lens facing from quick settings")
        }
    } finally {
        if (needReturnFromQuickSettings) {
            onNodeWithContentDescription(R.string.quick_settings_dropdown_open_description)
                .assertExists()
                .performClick()
        }
    }
}

/**
 * Rule that you to specify test methods that will have permissions granted prior to starting
 *
 * @param permissions the permissions to be granted
 * @param targetTestNames the names of the tests that this rule will apply to
 */
class IndividualTestGrantPermissionRule(
    private vararg val permissions: String,
    private val targetTestNames: List<String>
) : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                if (targetTestNames.contains(description.methodName)) {
                    GrantPermissionRule.grant(*permissions)
                } else {
                    base.evaluate() // Run the test without granting permissions
                }
            }
        }
    }
}

// functions for interacting with system permission dialog
@SdkSuppress(minSdkVersion = 30)
fun askEveryTimeDialog(uiDevice: UiDevice) {
    if (Build.VERSION.SDK_INT >= 30) {
        val askPermission = findObjectByText(uiDevice = uiDevice, text = "Only this time")
        askPermission!!.click()
    }
}

/**
 *  Clicks ALLOW option on an open permission dialog
 */
fun grantPermissionDialog(uiDevice: UiDevice) {
    if (Build.VERSION.SDK_INT >= 23) {
        val allowPermission = findObjectByText(
            uiDevice = uiDevice,
            text = when {
                Build.VERSION.SDK_INT == 23 -> "Allow"
                Build.VERSION.SDK_INT <= 28 -> "ALLOW"
                Build.VERSION.SDK_INT == 29 -> "Allow only while using the app"
                else -> "While using the app"
            }
        )
        allowPermission?.click()
    }
}

/**
 * Clicks the DENY option on an open permission dialog
 */
fun denyPermissionDialog(uiDevice: UiDevice) {
    if (Build.VERSION.SDK_INT >= 23) {
        println("sdk version ${Build.VERSION.SDK_INT}")
        val denyPermission = findObjectByText(
            uiDevice = uiDevice,
            text = when {
                // todo fix this to be "Don't allow"... lol
                // for some reason.... cannot find when the string has an apostrophe...
                // i tried using \'...

                Build.VERSION.SDK_INT >= 30 -> "Don"
                Build.VERSION.SDK_INT in 24..29 -> "DENY"
                else -> "Deny"
            }
        )

        denyPermission!!.click()
    }
}

/**
 * Finds a system button by its string value.
 * fails if not found
 */
fun findObjectByText(
    uiDevice: UiDevice,
    text: String,
    timeout: Long = 2_500,
    shouldFailIfNotFound: Boolean = true
): UiObject2? {
    val selector = By.textContains(text)
    return if (!uiDevice.wait(Until.hasObject(selector), timeout)) {
        if (shouldFailIfNotFound) {
            fail("Could not find object with text $text")
        }
        null
    } else {
        uiDevice.findObject(selector)
    }
}
