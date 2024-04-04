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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.jetpackcamera.R
import com.google.jetpackcamera.settings.SettingsViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(
    modifier: Modifier = Modifier,
    onClosePermissionsScreen: () -> Unit,
    permissionEnums: List<PermissionsEnum>,
    shouldShowPermissionsRequestRationale: (String) -> Boolean,
    openAppSettings: () -> Unit,
    cameraPermissionState: PermissionState
) {
    val permissionsToShow = remember { mutableStateListOf<PermissionsEnum>() }
    permissionsToShow.addAll(permissionEnums)

    val permissionsViewModel =
        PermissionsViewModel(
            shouldShowRequestPermissionRationale = shouldShowPermissionsRequestRationale,
            openAppSettings = openAppSettings,
            permissionEnums = permissionsToShow
        )

    if (permissionEnums.isEmpty()) {
        onClosePermissionsScreen()
    } else {
        when (permissionEnums.first()) {
            PermissionsEnum.CAMERA -> CameraPermission(
                modifier = modifier,
                cameraPermissionState = cameraPermissionState,
                onRequestPermission = permissionsViewModel::onRequestPermission
            )

        }
    }

}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermission(
    modifier: Modifier = Modifier,
    cameraPermissionState: PermissionState,
    onRequestPermission: (PermissionState) -> Unit
) {
    PermissionTemplate(
        modifier = modifier,
        //permissionState = cameraPermissionState,
        onRequestPermission = { onRequestPermission(cameraPermissionState) },
        painter = painterResource(id = R.drawable.photo_camera),
        iconAccessibilityText = stringResource(id = R.string.camera_permission_accessibility_text),
        title = stringResource(id = R.string.camera_permission_screen_title),
        bodyText = stringResource(id = R.string.camera_permission_required_rationale),
        requestButtonText = stringResource(R.string.request_permission)
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionTemplate(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
    onSkipPermission: (() -> Unit)? = null,
    painter: Painter,
    iconAccessibilityText: String,
    title: String,
    bodyText: String,
    requestButtonText: String
) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.primary),
        verticalArrangement = Arrangement.Bottom
    ) {
        // permission image / top half
        PermissionImage(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .align(Alignment.CenterHorizontally),
            painter = painter,
            accessibilityText = iconAccessibilityText
        )
        Spacer(modifier = Modifier.fillMaxHeight(.1f))
        // bottom half
        Column(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            // text section
            PermissionText(title = title, bodyText = bodyText)
            Spacer(modifier = Modifier.fillMaxHeight(.1f))
            // permission button section
            PermissionButtonSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .height(IntrinsicSize.Min),
                requestButtonText = requestButtonText,
                onRequestPermission = onRequestPermission,
                onSkipPermission = onSkipPermission
            )
        }
        Spacer(modifier = Modifier.fillMaxHeight(.2f))
    }
}

@Composable
fun PermissionImage(modifier: Modifier = Modifier, painter: Painter, accessibilityText: String) {
    Box(modifier = modifier) {
        Icon(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomCenter),
            painter = painter,
            tint = MaterialTheme.colorScheme.onPrimary,
            contentDescription = accessibilityText
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionButtonSection(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
    requestButtonText: String,
    onSkipPermission: (() -> Unit)?
) {
    Box(modifier = modifier) {
        // permission buttons
        Column(
            modifier = Modifier
                .align(Alignment.Center)
        ) {
            PermissionButton(
                onRequestPermission = { onRequestPermission() },
                requestButtonText = requestButtonText
            )
            Spacer(modifier = Modifier.height(20.dp))
            if (onSkipPermission != null) {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { onSkipPermission() },
                    text = "Maybe Later",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.inversePrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionButton(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
    requestButtonText: String
) {
    Button(
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        onClick = { onRequestPermission() }
    ) {
        Text(
            modifier = Modifier.padding(10.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            text = requestButtonText
        )
    }
}

@Composable
fun PermissionText(modifier: Modifier = Modifier, title: String, bodyText: String) {
    Box(
        modifier = modifier
            .height(IntrinsicSize.Min)
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .align(Alignment.Center)
        ) {
            // permission title

            PermissionTitleText(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                text = title,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            // permission body text
            PermissionBodyText(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 50.dp),
                text = bodyText,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun PermissionTitleText(modifier: Modifier = Modifier, text: String, color: Color) {
    Text(
        modifier = modifier,
        color = color,
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun PermissionBodyText(modifier: Modifier = Modifier, text: String, color: Color) {
    Text(
        modifier = modifier,
        color = color,
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
}
