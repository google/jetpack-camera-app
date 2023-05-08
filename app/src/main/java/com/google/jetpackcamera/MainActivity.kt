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

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.jetpackcamera.ui.JcaApp
import com.google.jetpackcamera.ui.theme.JetpackCameraTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.google.jetpackcamera.MainActivityUiState.Loading
import com.google.jetpackcamera.MainActivityUiState.Success
import com.google.jetpackcamera.settings.model.DarkModeStatus
import kotlinx.coroutines.flow.collect


/**
 * Activity for the JetpackCameraApp.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var uiState: MainActivityUiState by mutableStateOf(Loading)

        // update viewmodel
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .onEach {
                        uiState = it
                    }
                    .collect()
            }
        }
        setContent {
            when (uiState) {
                Loading -> {/* Todo a loading screen */}
                is Success -> {
                   // Toast.makeText(this, "Success", Toast.LENGTH_LONG).show()
                    JetpackCameraTheme( darkTheme = isInDarkMode(uiState = uiState)) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            JcaApp()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Determines whether the Theme should be in dark, light, or follow system theme
 */
@Composable
private fun isInDarkMode(uiState: MainActivityUiState):Boolean =
    when (uiState) {
        Loading -> isSystemInDarkTheme()
        is Success -> when (uiState.settings.dark_mode_status) {
            DarkModeStatus.DARK -> true
            DarkModeStatus.LIGHT -> false
            DarkModeStatus.SYSTEM -> isSystemInDarkTheme()
        }
    }



