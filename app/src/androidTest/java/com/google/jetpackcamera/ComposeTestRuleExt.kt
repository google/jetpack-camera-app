/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider

/**
 * Allows use of testRule.onNodeWithText that uses an integer string resource
 * rather than a [String] directly.
 */
fun SemanticsNodeInteractionsProvider.onNodeWithText(
    @StringRes strRes: Int
): SemanticsNodeInteraction = onNodeWithText(
    text = ApplicationProvider.getApplicationContext<Context>().getString(strRes)
)
