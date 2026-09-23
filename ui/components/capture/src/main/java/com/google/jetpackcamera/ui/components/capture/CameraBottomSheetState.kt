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

import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * State holder for managing the camera screen's quick settings bottom sheet.
 *
 * @param scaffoldState The underlying [BottomSheetScaffoldState].
 * @param coroutineScope The [CoroutineScope] used to launch animated sheet transitions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
class CameraBottomSheetState(
    val scaffoldState: BottomSheetScaffoldState,
    private val coroutineScope: CoroutineScope
) {
    /**
     * Whether the bottom sheet is currently expanding or expanded.
     */
    val isOpen: Boolean
        get() = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded

    /**
     * Whether the bottom sheet is currently visible on screen.
     */
    val isVisible: Boolean
        get() = scaffoldState.bottomSheetState.isVisible

    /**
     * Animates the bottom sheet to the expanded state.
     */
    fun expand() {
        coroutineScope.launch {
            scaffoldState.bottomSheetState.expand()
        }
    }

    /**
     * Animates the bottom sheet to the hidden state.
     */
    fun hide() {
        coroutineScope.launch {
            scaffoldState.bottomSheetState.hide()
        }
    }

    /**
     * Toggles the bottom sheet between expanded and hidden states.
     */
    fun toggle() {
        if (isOpen) {
            hide()
        } else {
            expand()
        }
    }
}

/**
 * CompositionLocal providing access to the current [CameraBottomSheetState] within the [PreviewLayout] tree.
 */
internal val LocalCameraBottomSheetState = compositionLocalOf<CameraBottomSheetState?> { null }

/**
 * Creates and remembers a default [BottomSheetScaffoldState] pre-configured for the camera bottom sheet
 * with `initialValue = SheetValue.Hidden`.
 *
 * @param initialValue the initial [SheetValue] of the bottom sheet.
 * @param confirmValueChange optional callback to confirm or veto state changes.
 * @param snackbarHostState host state for snackbars within the scaffold.
 * @return a remembered [BottomSheetScaffoldState] instance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberCameraBottomSheetScaffoldState(
    initialValue: SheetValue = SheetValue.Hidden,
    confirmValueChange: (SheetValue) -> Boolean = { true },
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
): BottomSheetScaffoldState = rememberBottomSheetScaffoldState(
    bottomSheetState = rememberBottomSheetState(
        initialValue = initialValue,
        confirmValueChange = confirmValueChange
    ),
    snackbarHostState = snackbarHostState
)

/**
 * Creates and remembers a default [CameraBottomSheetState] for controlling the camera's quick settings bottom sheet.
 *
 * @param coroutineScope the scope used to launch sheet animation coroutines.
 * @return a remembered [CameraBottomSheetState] instance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberCameraBottomSheetState(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): CameraBottomSheetState {
    val scaffoldState = rememberCameraBottomSheetScaffoldState()
    return remember(scaffoldState, coroutineScope) {
        CameraBottomSheetState(scaffoldState, coroutineScope)
    }
}

/**
 * Creates and remembers a [CameraBottomSheetState] backed by an existing [BottomSheetScaffoldState].
 *
 * @param scaffoldState the underlying [BottomSheetScaffoldState] controlling the bottom sheet.
 * @param coroutineScope the scope used to launch sheet animation coroutines.
 * @return a remembered [CameraBottomSheetState] instance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberCameraBottomSheetState(
    scaffoldState: BottomSheetScaffoldState,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): CameraBottomSheetState = remember(scaffoldState, coroutineScope) {
    CameraBottomSheetState(scaffoldState, coroutineScope)
}

/**
 * Creates and remembers a [CameraBottomSheetState] for controlling the camera's quick settings bottom sheet
 * with a specified [initialValue].
 *
 * @param initialValue the initial [SheetValue] of the bottom sheet.
 * @param confirmValueChange optional callback to confirm or veto state changes.
 * @param coroutineScope the scope used to launch sheet animation coroutines.
 * @return a remembered [CameraBottomSheetState] instance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberCameraBottomSheetState(
    initialValue: SheetValue,
    confirmValueChange: (SheetValue) -> Boolean = { true },
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): CameraBottomSheetState {
    val scaffoldState = rememberCameraBottomSheetScaffoldState(
        initialValue = initialValue,
        confirmValueChange = confirmValueChange
    )
    return rememberCameraBottomSheetState(
        scaffoldState = scaffoldState,
        coroutineScope = coroutineScope
    )
}
