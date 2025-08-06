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
package com.google.jetpackcamera.permissions.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.jetpackcamera.permissions.PermissionEnum
import com.google.jetpackcamera.permissions.R

/**
 * Template for a single page for the permissions Screen
 *
 * @param permissionEnum a [PermissionEnum] representing the target permission
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionTemplate(
    modifier: Modifier = Modifier,
    permissionEnum: PermissionEnum,
    onDismissPermission: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val permissionState = rememberPermissionState(permissionEnum.getPermission())

    // LaunchedEffect will skip permission enum if already granted.
    LaunchedEffect(permissionState.status) {
        if (permissionState.status.isGranted ||
            (permissionState.status.shouldShowRationale && permissionEnum.isOptional())
        ) {
            onDismissPermission()
        }
    }

    PermissionTemplate(
        modifier = modifier,
        testTag = permissionEnum.getTestTag(),
        onRequestPermission = {
            if (permissionState.status.shouldShowRationale) {
                onOpenAppSettings()
            } else {
                permissionState.launchPermissionRequest()
            }
        },
        imageVector = permissionEnum.getImageVector()!!,
        iconAccessibilityText = stringResource(permissionEnum.getIconAccessibilityTextResId()),
        title = stringResource(permissionEnum.getPermissionTitleResId()),

        // if declined by user, must navigate to system app settings to enable permission
        bodyText =
        if (!permissionState.status.shouldShowRationale || permissionEnum.isOptional()) {
            stringResource(id = permissionEnum.getPermissionBodyTextResId())
        } else {
            stringResource(id = permissionEnum.getRationaleBodyTextResId()!!)
        },
        requestButtonText =
        if (!permissionState.status.shouldShowRationale || permissionEnum.isOptional()) {
            stringResource(id = R.string.request_permission)
        } else {
            stringResource(id = R.string.navigate_to_settings)
        }
    )
}

/**
 * Template for a Permission Screen page
 */
@Composable
fun PermissionTemplate(
    modifier: Modifier = Modifier,
    testTag: String,
    onRequestPermission: () -> Unit,
    imageVector: ImageVector,
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
                .align(Alignment.CenterHorizontally)
                .testTag(testTag),
            imageVector = imageVector,
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
                    .testTag(REQUEST_PERMISSION_BUTTON)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .height(IntrinsicSize.Min),
                requestButtonText = requestButtonText,
                onRequestPermission = onRequestPermission
            )
        }
        Spacer(modifier = Modifier.fillMaxHeight(.2f))
    }
}

/*
Permission UI Previews
 */
@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_Camera_Permission_Page() {
    PermissionTemplate(
        onRequestPermission = { /*TODO*/ },
        testTag = "",
        imageVector = PermissionEnum.CAMERA.getImageVector()!!,
        iconAccessibilityText = "",
        title = stringResource(id = PermissionEnum.CAMERA.getPermissionTitleResId()),
        bodyText = stringResource(id = PermissionEnum.CAMERA.getPermissionBodyTextResId()),
        requestButtonText = stringResource(id = R.string.request_permission)
    )
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_Audio_Permission_Page() {
    PermissionTemplate(
        onRequestPermission = { /*TODO*/ },
        testTag = "",
        imageVector = PermissionEnum.RECORD_AUDIO.getImageVector()!!,
        iconAccessibilityText = "",
        title = stringResource(id = PermissionEnum.RECORD_AUDIO.getPermissionTitleResId()),
        bodyText = stringResource(id = PermissionEnum.RECORD_AUDIO.getPermissionBodyTextResId()),
        requestButtonText = stringResource(id = R.string.request_permission)
    )
}

/*
Permission UI Subcomponents
 */
@Composable
fun PermissionImage(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    accessibilityText: String
) {
    Box(modifier = modifier) {
        Icon(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomCenter),
            imageVector = imageVector,
            tint = MaterialTheme.colorScheme.onPrimary,
            contentDescription = accessibilityText
        )
    }
}

@Composable
fun PermissionButtonSection(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
    requestButtonText: String
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
            modifier = Modifier
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
