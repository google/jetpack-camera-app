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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.jetpackcamera.R

/**
 * Permission prompts screen.
 * Camera permission will always prompt when disabled, and the app cannot be used otherwise
 * if optional settings have not yet been declined by the user, then they will be prompted as well
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(
    modifier: Modifier = Modifier,
    permissionEnums: Set<PermissionEnum>,
    openAppSettings: () -> Unit
) {
    val permissionsViewModel =
        PermissionsViewModel(
            permissionEnums = permissionEnums
        )

    if (!permissionsViewModel.visiblePermissionDialogQueue.isEmpty()) {
        val permissionEnum = permissionsViewModel.visiblePermissionDialogQueue.first()
        val currentPermissionState =
            rememberPermissionState(permission = permissionEnum.getPermission())

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { permissionGranted ->
                if (permissionGranted) {
                    // remove from list
                    permissionsViewModel.dismissPermission()
                } else if (permissionEnum.isOptional()) {
                    permissionsViewModel.dismissPermission()
                }
            }
        )

        PermissionTemplate(
            modifier = modifier,
            permissionEnum = permissionEnum,
            permissionState = currentPermissionState,
            onSkipPermission = when (permissionEnum) {
                // todo: a prettier navigation to app settings.
                PermissionEnum.CAMERA -> null
                // todo: skip permission button functionality. currently need to go through the prompt to skip
                else -> null // permissionsViewModel::dismissPermission
            },
            onRequestPermission = { permissionLauncher.launch(permissionEnum.getPermission()) },
            onOpenAppSettings = openAppSettings
        )
    }
}

/**
 * Template for a permission Screen page that uses a [permissionEnum]
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionTemplate(
    modifier: Modifier = Modifier,
    permissionEnum: PermissionEnum,
    permissionState: PermissionState,
    onRequestPermission: () -> Unit,
    onSkipPermission: (() -> Unit)? = null,
    onOpenAppSettings: () -> Unit
) {
    PermissionTemplate(
        modifier = modifier,
        onRequestPermission = {
            // if declined by user, must navigate to system app settings to enable permission
            // todo: open a dialog that tells user they must go to device's app settings to enable permissions
            if (permissionState.status.shouldShowRationale) {
                onOpenAppSettings()
            } else {
                onRequestPermission()
            }
        },
        onSkipPermission = onSkipPermission,
        painter = permissionEnum.getPainter(),
        iconAccessibilityText = stringResource(permissionEnum.getIconAccessiblityTextResId()),
        title = stringResource(permissionEnum.getPermissionTitleResId()),
        bodyText = stringResource(id = permissionEnum.getPermissionBodyTextResId()),
        requestButtonText = stringResource(id = R.string.request_permission)
    )
}

/**
 * Template for a Permission Screen page
 */
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

/*
Permission UI Subcomponents
 */
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
