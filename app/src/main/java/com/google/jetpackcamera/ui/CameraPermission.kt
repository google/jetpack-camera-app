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

package com.google.jetpackcamera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.jetpackcamera.R

@OptIn(ExperimentalPermissionsApi::class)
@Composable

fun CameraPermission(
    modifier: Modifier = Modifier,
    cameraPermissionState: PermissionState
) {
    /*
    half image, bottom half permission: title, subtext, request
     */
    Column(modifier = modifier.background(MaterialTheme.colorScheme.primary)) {
        // permission image / top half
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxSize()
                .weight(1f)
        ) {
            Icon(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.BottomCenter),
                painter = painterResource(id = R.drawable.photo_camera),
                tint = MaterialTheme.colorScheme.onPrimary,
                contentDescription = stringResource(id = R.string.image_accessibility_text)
            )
        }

        // bottom half
        Column(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .weight(1f)
            // .background(Color.Yellow)
        ) {
            // text section
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                    //  .background(Color.Green)
                ) {
                    // permission title
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.onPrimary,
                        text = "Enable Camera",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    // permission subtext
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(horizontal = 50.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        text = stringResource(id = R.string.camera_permission_required_rationale),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
            // permission button section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .weight(1f)
            ) {
                // permission button
                Button(
                    modifier = Modifier.align(Alignment.Center),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    onClick = { cameraPermissionState.launchPermissionRequest() }
                ) {
                    Text(
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        text = stringResource(R.string.request_permission)
                    )
                }
                // maybe later
            }
        }
    }
}