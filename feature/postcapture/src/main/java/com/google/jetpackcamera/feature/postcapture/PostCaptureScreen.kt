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

package com.google.jetpackcamera.feature.postcapture

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.Image
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File


@Composable
fun PostCaptureScreen(
    viewModel: PostCaptureViewModel = hiltViewModel()
) {

    val uiState : PostCaptureUiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        uiState.lastCapturedImagePath?.let { path ->
//            val file = File(path)
//            if (file.exists()) {
//                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.fromFile(file))
//                Image(
//                    bitmap = bitmap.asImageBitmap(),
//                    contentDescription = "Captured Image",
//                    modifier = Modifier.fillMaxSize()
//                )
//            }
            val bitmap = remember(path) {
                loadAndRotateBitmap(context, path, 90f) // Load image with 90° rotation
            }

            if (bitmap != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawIntoCanvas { canvas ->
                        val scale = maxOf(
                            size.width / bitmap.width,
                            size.height / bitmap.height
                        )
                        val imageSize = Size(bitmap.width * scale, bitmap.height * scale)
                        canvas.nativeCanvas.drawBitmap(bitmap, null, android.graphics.RectF(
                            0f, 0f, imageSize.width, imageSize.height
                        ), null)
                    }
                }
            }
        } ?: Text(
            text = "No Image Captured",
            modifier = Modifier.align(Alignment.Center)
        )

        // Buttons Overlay at Bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
//            Button(
//                onClick = { viewModel.deleteImage() },
//                modifier = Modifier
//                    .weight(1f)
//                    .padding(end = 8.dp)
//                    .shadow(8.dp, RoundedCornerShape(12.dp))
//            ) {
//                Text(text = "Delete")
//            }
//
//            Button(
//                onClick = {
//                    uiState.lastCapturedImagePath?.let { path ->
//                        shareImage(context, path)
//                    }
//                },
//                modifier = Modifier
//                    .weight(1f)
//                    .padding(start = 8.dp)
//                    .shadow(8.dp, RoundedCornerShape(12.dp))
//            ) {
//                Text(text = "Share")
//            }
            IconButton(
                onClick = { viewModel.deleteImage() },
                modifier = Modifier
                    .size(56.dp)
                    .shadow(10.dp, CircleShape),
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Spacer to push buttons to the sides
            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    uiState.lastCapturedImagePath?.let { path ->
                        shareImage(context, path)
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .shadow(10.dp, CircleShape),
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
//    Column(
//        modifier = Modifier.fillMaxSize()
//    .padding(16.dp),
//    horizontalAlignment = Alignment.CenterHorizontally,
//    verticalArrangement = Arrangement.Center
//    ) {
//        uiState.lastCapturedImagePath?.let { path ->
//            val bitmap = MediaStore.Images.Media.getBitmap(
//                LocalContext.current.contentResolver,
//                Uri.fromFile(File(path))
//            )
//            Image(
//                bitmap = bitmap.asImageBitmap(),
//                contentDescription = "Captured Image",
//                modifier = Modifier
//                    .fillMaxSize()
//                    .height(300.dp),
//                contentScale = ContentScale.Crop
//            )
//        } ?: Text(text = "No Image Captured")
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Delete button
//        Button(
//            onClick = {
//                viewModel.deleteImage()
//                      },
//            enabled = uiState.lastCapturedImagePath != null
//        ) {
//            Text(text = "Delete Image")
//        }
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        // Share button
//        Button(
//            onClick = {
//                uiState.lastCapturedImagePath?.let { path ->
//                    shareImage(context, path)
//                }
//            },
//            enabled = uiState.lastCapturedImagePath != null
//        ) {
//            Text(text = "Share Image")
//        }
//    }
}

private fun shareImage(context: Context, imagePath: String) {
    val file = File(imagePath)
//    val uri = Uri.fromFile(file)
    val uri = FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", file);

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
    }
    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    context.startActivity(Intent.createChooser(intent, "Share Image"))
}

fun loadAndRotateBitmap(context: Context, path: String, degrees: Float): Bitmap? {
    val uri = Uri.fromFile(File(path))
    val originalBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)

    return originalBitmap?.let {
        val matrix = Matrix().apply { postRotate(degrees) }
        Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
    }
}