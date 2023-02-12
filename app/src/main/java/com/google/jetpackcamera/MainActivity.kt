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

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.jetpackcamera.feature.preview.PreviewScreen
import com.google.jetpackcamera.ui.theme.JetpackCameraTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow


@OptIn(ExperimentalPermissionsApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var permissionStateFlow: MutableStateFlow<PermissionState>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JetpackCameraTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val permissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
                    permissionStateFlow = MutableStateFlow(permissionState)

                    if (permissionState.status.isGranted)  {
                        PreviewScreen()
                    } else {
                        CameraPermission(permissionState)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun CameraPermission(cameraPermissionState : PermissionState) {
    Column {
        val textToShow = if (cameraPermissionState.status.shouldShowRationale) {
            "The camera is important for this app. Please grant the permission."
        } else {
            "Camera not available"
        }

        Text(textToShow)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
            Text("Request permission")
        }
    }
}