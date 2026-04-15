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
package com.google.jetpackcamera.ui.uistate.postcapture

/**
 * Defines the UI state for the delete button in the post-capture screen.
 *
 * This sealed interface represents the different states of the delete button, which can be either
 * ready for interaction or unavailable.
 */
sealed interface DeleteButtonUiState {
    /**
     * The delete button is ready and can be interacted with.
     */
    data object Ready : DeleteButtonUiState

    /**
     * The delete button is unavailable and should not be displayed or should be disabled.
     * This might be the case if there is no media to delete.
     */
    data object Unavailable : DeleteButtonUiState

    companion object
}
