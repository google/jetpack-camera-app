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
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider
import org.junit.AssumptionViolatedException

/**
 * Allows use of testRule.onNodeWithText that uses an integer string resource
 * rather than a [String] directly.
 */
fun SemanticsNodeInteractionsProvider.onNodeWithText(
    @StringRes strRes: Int
): SemanticsNodeInteraction = onNodeWithText(
    text = ApplicationProvider.getApplicationContext<Context>().getString(strRes)
)

/**
 * Assumes that the provided [matcher] is satisfied for this node.
 *
 * This is equivalent to [SemanticsNodeInteraction.assert()], but will skip the test rather than
 * fail the test.
 *
 * @param matcher Matcher to verify.
 * @param messagePrefixOnError Prefix to be put in front of an error that gets thrown in case this
 * assert fails. This can be helpful in situations where this assert fails as part of a bigger
 * operation that used this assert as a precondition check.
 *
 * @throws AssumptionViolatedException if the matcher does not match or the node can no
 * longer be found
 */
fun SemanticsNodeInteraction.assume(
    matcher: SemanticsMatcher,
    messagePrefixOnError: (() -> String)? = null
): SemanticsNodeInteraction {
    var errorMessageOnFail = "Failed to assume the following: (${matcher.description})"
    if (messagePrefixOnError != null) {
        errorMessageOnFail = messagePrefixOnError() + "\n" + errorMessageOnFail
    }
    val node = fetchSemanticsNode(errorMessageOnFail)
    if (!matcher.matches(node)) {
        throw AssumptionViolatedException(
            buildGeneralErrorMessage(errorMessageOnFail, this)
        )
    }
    return this
}

internal fun buildGeneralErrorMessage(
    errorMessage: String,
    nodeInteraction: SemanticsNodeInteraction
): String {
    val sb = StringBuilder()

    sb.appendLine(errorMessage)

    sb.appendLine("Semantics of the node:")
    sb.appendLine(nodeInteraction.printToString())

    return sb.toString()
}
