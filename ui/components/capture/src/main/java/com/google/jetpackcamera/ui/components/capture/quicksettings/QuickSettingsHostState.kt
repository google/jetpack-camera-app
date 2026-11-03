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
package com.google.jetpackcamera.ui.components.capture.quicksettings

import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * State holder for the Quick Settings overlay container, tracking whether the panel is open
 * across compact and adaptive layouts and surviving configuration or recreation changes via
 * [Saver].
 *
 * @param initialIsOpen Whether Quick Settings starts in the open state.
 */
@Stable
class QuickSettingsHostState(initialIsOpen: Boolean = false) {
    /**
     * Whether the Quick Settings panel is currently open.
     */
    var isOpen: Boolean by mutableStateOf(initialIsOpen)
        private set

    /**
     * Opens the Quick Settings panel.
     */
    fun open() {
        isOpen = true
    }

    /**
     * Dismisses the Quick Settings panel.
     */
    fun dismiss() {
        isOpen = false
    }

    /**
     * Toggles the Quick Settings panel between open and dismissed states.
     */
    fun toggle() {
        isOpen = !isOpen
    }

    companion object {
        /**
         * [Saver] implementation that persists [isOpen] across configuration changes and process
         * recreation.
         */
        val Saver: Saver<QuickSettingsHostState, Boolean> = Saver(
            save = { it.isOpen },
            restore = { QuickSettingsHostState(initialIsOpen = it) }
        )
    }
}

/**
 * Creates and remembers a [QuickSettingsHostState] across recompositions and saves its open/closed
 * state across configuration changes using [rememberSaveable].
 *
 * @param initialIsOpen Whether Quick Settings starts open when first initialized.
 * @return A saveable [QuickSettingsHostState] instance.
 */
@Composable
fun rememberQuickSettingsHostState(initialIsOpen: Boolean = false): QuickSettingsHostState =
    rememberSaveable(saver = QuickSettingsHostState.Saver) {
        QuickSettingsHostState(initialIsOpen = initialIsOpen)
    }

/**
 * Creates and remembers a [BottomSheetScaffoldState] configured for the in-hierarchy Quick Settings
 * bottom sheet, supporting [SheetValue.Hidden] without deprecated sheet state APIs.
 *
 * @param initialValue Initial [SheetValue] of the Quick Settings bottom sheet.
 * @return A remembered [BottomSheetScaffoldState] for the Quick Settings bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberQuickSettingsBottomSheetScaffoldState(
    initialValue: SheetValue = SheetValue.Hidden
): BottomSheetScaffoldState = rememberBottomSheetScaffoldState(
    bottomSheetState = rememberBottomSheetState(initialValue = initialValue)
)
