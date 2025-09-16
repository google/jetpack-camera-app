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
package com.google.jetpackcamera.ui.uistate

import android.content.Context

/**
 * Represents the UI state for an item that can be selected, but might also be
 * disabled for specific reasons.
 *
 * This sealed interface is used to model UI elements where an option (of type [T])
 * can either be actively selectable or presented as disabled with a rationale.
 *
 * @param T The type of the value that can be selected or is disabled.
 */
sealed interface SingleSelectableUiState<T> {
    val value: T

    /**
     * Represents an item that is currently available for selection.
     * @property value The underlying value of the selectable item.
     */
    data class SelectableUi<T>(override val value: T) : SingleSelectableUiState<T>

    /**
     * Represents an item that is currently disabled and cannot be selected.
     * Includes a reason why the item is disabled.
     * @property value The underlying value of the item, even though it's disabled.
     * @property disabledReason The rationale explaining why this item is disabled.
     */
    data class Disabled<T>(override val value: T, val disabledReason: DisableRationale) :
        SingleSelectableUiState<T>
}

/**
 * Defines the reason why a [SingleSelectableUiState.Disabled] item is not available.
 * Provides a way to get a human-readable message and a test tag for UI testing.
 */
interface DisableRationale {
    val testTag: String
    val reasonTextResId: Int
    fun getDisplayMessage(context: Context): String {
        return context.getString(reasonTextResId)
    }
}
