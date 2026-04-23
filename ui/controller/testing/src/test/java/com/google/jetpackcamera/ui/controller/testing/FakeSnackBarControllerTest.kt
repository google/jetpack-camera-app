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
package com.google.jetpackcamera.ui.controller.testing

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.ui.uistate.DisableRationale
import com.google.jetpackcamera.ui.uistate.SnackbarData
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeSnackBarControllerTest {
    private val testDisableRationale = object : DisableRationale {
        override val testTag: String = "test-tag"
        override val reasonTextResId: Int = 123
    }

    @Test
    fun enqueueDisabledHdrToggleSnackBar_invokesAddSnackBarData() {
        var calledValue: SnackbarData? = null
        val controller = FakeSnackBarController(
            addSnackBarDataAction = { calledValue = it }
        )
        controller.enqueueDisabledHdrToggleSnackBar(testDisableRationale)

        assertThat(calledValue).isNotNull()
        assertThat(calledValue?.cookie).isEqualTo("DisabledHdrToggle-1")
        assertThat(calledValue?.stringResource).isEqualTo(testDisableRationale.reasonTextResId)
        assertThat(calledValue?.testTag).isEqualTo(testDisableRationale.testTag)
    }

    @Test
    fun onSnackBarResult_invokesAction() {
        var calledValue: String? = null
        val controller = FakeSnackBarController(onSnackBarResultAction = { calledValue = it })
        controller.onSnackBarResult("test-cookie")
        assertThat(calledValue).isEqualTo("test-cookie")
    }

    @Test
    fun incrementAndGetSnackBarCount_usesInternalCounterByDefault() {
        val controller = FakeSnackBarController()
        assertThat(controller.incrementAndGetSnackBarCount()).isEqualTo(1)
        assertThat(controller.incrementAndGetSnackBarCount()).isEqualTo(2)
    }

    @Test
    fun incrementAndGetSnackBarCount_invokesActionIfProvided() {
        val controller = FakeSnackBarController(incrementAndGetSnackBarCountAction = { 42 })
        assertThat(controller.incrementAndGetSnackBarCount()).isEqualTo(42)
    }

    @Test
    fun addSnackBarData_invokesAction() {
        var calledValue: SnackbarData? = null
        val controller = FakeSnackBarController(addSnackBarDataAction = { calledValue = it })
        val data = SnackbarData(cookie = "test-cookie", stringResource = 123)
        controller.addSnackBarData(data)
        assertThat(calledValue).isEqualTo(data)
    }
}
