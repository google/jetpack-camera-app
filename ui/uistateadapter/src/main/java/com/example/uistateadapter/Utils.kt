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
package com.example.uistateadapter

import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState

object Utils {

    /**
     *  @param supportedValues a [Set] of options that are selectable in the current device configuration
     *  @param sortedValues a [List] of sorted options that always contains [supportedValues].
     *  @return a new List of [SingleSelectableUiState.SelectableUi] that contains all [supportedValues] ordered in accordance to [sortedValues]
     *  */
    fun <T> getSelectableListFromValues(
        valueSet: Set<T>,
        filteringList: List<T>
    ): List<SingleSelectableUiState<T>> {
        return filteringList.filter {
            it in valueSet
        }.map { value ->
            SingleSelectableUiState.SelectableUi(value)
        }
    }
}
