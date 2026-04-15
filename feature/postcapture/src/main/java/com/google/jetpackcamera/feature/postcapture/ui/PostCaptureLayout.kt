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
package com.google.jetpackcamera.feature.postcapture.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun PostCaptureLayout(
    modifier: Modifier = Modifier,
    mediaSurface: @Composable (Modifier) -> Unit,
    exitButton: @Composable (Modifier) -> Unit,
    saveButton: @Composable (Modifier) -> Unit,
    shareButton: @Composable (Modifier) -> Unit,
    deleteButton: @Composable (Modifier) -> Unit,
    snackBar: @Composable (Modifier, SnackbarHostState) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Layer 1: Media Surface
            // Occupies the full screen background
            mediaSurface(
                Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            )

            // Layer 2: PostCapture Controls
            // Uses SpaceBetween to push two rows to the absolute edges
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeContentPadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar Area
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    exitButton(Modifier)
                }

                // Bottom Bar Area
                // Using a Row with SpaceBetween to separate negative (left) from positive (right) actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Negative actions on the left
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        deleteButton(Modifier)
                    }

                    // Positive actions on the right
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End
                    ) {
                        saveButton(Modifier)
                        shareButton(Modifier)
                    }
                }
            }
            snackBar(Modifier, snackbarHostState)
        }
    }
}
