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
package com.google.jetpackcamera.media

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * A fake [ContentProvider] for testing interactions with the MediaStore.
 *
 * This class simulates the basic behavior of the MediaStore's ContentProvider,
 * allowing tests to query, insert, delete, and open file streams for media URIs
 * without relying on a real Android device or emulator. It stores media data
 * in memory.
 *
 * Currently supports:
 * - `query`: Simulates querying for media, primarily for `_ID` and `DATE_ADDED`.
 * - `insert`: Simulates adding a new media item and returns a content URI.
 * - `delete`: Simulates removing a media item.
 * - `openOutputStream`: Provides an in-memory [OutputStream] for writing data.
 *
 * Note: This is a simplified fake and does not implement all features of the
 *       real MediaStore ContentProvider. It is intended for specific test cases
 *       in this project.
 */
class FakeContentProvider : ContentProvider() {

    private val mediaStore: MutableMap<Uri, ContentValues> = mutableMapOf()
    private var nextId = 1L

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor {
        val resolvedProjection = projection ?: arrayOf()
        val cursor = MatrixCursor(resolvedProjection)

        // Case 1: Direct URI lookup (e.g., content://media/external/images/media/123)
        if (mediaStore.containsKey(uri)) {
            mediaStore[uri]?.let { values ->
                cursor.addRow(createRow(resolvedProjection, values, uri))
            }
            return cursor
        }

        // Case 2: Collection URI lookup (e.g., content://media/external/images/media)
        val isImageQuery = uri == MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val isVideoQuery = uri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        if (isImageQuery || isVideoQuery) {
            val relevantMediaStore = mediaStore.entries.filter {
                val keyString = it.key.toString()
                if (isImageQuery) {
                    keyString.contains("images")
                } else {
                    keyString.contains("video")
                }
            }

            val sortedMedia = relevantMediaStore
                .sortedByDescending { it.value.getAsLong(MediaStore.MediaColumns.DATE_ADDED) }

            for ((itemUri, values) in sortedMedia) {
                cursor.addRow(createRow(resolvedProjection, values, itemUri))
            }
        }
        return cursor
    }

    private fun createRow(
        projection: Array<String>,
        values: ContentValues,
        uri: Uri
    ): Array<Any?> {
        return projection.map { proj ->
            when (proj) {
                MediaStore.MediaColumns._ID -> uri.lastPathSegment?.toLong()
                else -> values.get(proj)
            }
        }.toTypedArray()
    }

    override fun getType(uri: Uri): String? {
        return null // Not implemented
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (values == null) return null
        val newUri = Uri.withAppendedPath(uri, nextId.toString())
        mediaStore[newUri] = values
        nextId++
        return newUri
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return if (mediaStore.remove(uri) != null) 1 else 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        if (mediaStore.containsKey(uri) && values != null) {
            mediaStore[uri]?.putAll(values)
            return 1
        }
        return 0
    }

    fun get(uri: Uri): ContentValues? {
        return mediaStore[uri]
    }

    override fun openFile(uri: Uri, mode: String): android.os.ParcelFileDescriptor? {
        val context = context ?: return null
        val file = File(context.cacheDir, uri.lastPathSegment ?: "tempfile")
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
            val accessMode = android.os.ParcelFileDescriptor.parseMode(mode)
            return android.os.ParcelFileDescriptor.open(file, accessMode)
        } catch (e: FileNotFoundException) {
            return null
        }
    }
}
