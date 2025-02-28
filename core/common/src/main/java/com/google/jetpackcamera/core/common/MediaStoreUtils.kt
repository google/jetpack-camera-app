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
package com.google.jetpackcamera.core.common

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore

/**
 * Retrieves the URI for the most recently added image whose filename starts with "JCA".
 *
 * @param context The application context.
 * @return The content URI of the matching image, or null if none is found.
 */
fun getLastImageUri(context: Context): Uri? {
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_ADDED
    )

    // Filter by filenames starting with "JCA"
    val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
    val selectionArgs = arrayOf("JCA%")

    // Sort the results so that the most recently added image appears first.
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    // Perform the query on the MediaStore.
    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val id = cursor.getLong(idColumn)

            return ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )
        }
    }
    return null
}

/**
 * Loads a Bitmap from a given URI and rotates it by the specified degrees.
 *
 * @param context The application context.
 * @param uri The URI of the image to load.
 * @param degrees The number of degrees to rotate the image by.
 */
fun loadAndRotateBitmap(context: Context, uri: Uri?, degrees: Float): Bitmap? {
    uri?.let {
        val bitmap = if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) {
            MediaStore.Images.Media.getBitmap(context.contentResolver, it)
        } else {
            val imageDecoderSource = ImageDecoder.createSource(context.contentResolver, it)
            ImageDecoder.decodeBitmap(imageDecoderSource)
        }

        return bitmap?.let {
            val matrix = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
        }
    }
    return null
}
