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

    private val mediaStore: MutableMap<String, ContentValues> = mutableMapOf()
    private val thumbnailFailures = mutableSetOf<String>()
    private var nextId = 1L
    private var failNextInsert = false

    fun setFailNextInsert(fail: Boolean) {
        failNextInsert = fail
    }

    /**
     * Toggles whether thumbnail generation (via openFile) should fail for a specific URI.
     */
    fun setThumbnailFail(uri: Uri, fail: Boolean) {
        if (fail) {
            thumbnailFailures.add(
                uri.toString()
            )
        } else {
            thumbnailFailures.remove(uri.toString())
        }
    }

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
        val uriString = uri.toString()

        // Case 1: Direct URI lookup
        if (mediaStore.containsKey(uriString)) {
            mediaStore[uriString]?.let { values ->
                cursor.addRow(createRow(resolvedProjection, values, uri))
            }
            return cursor
        }

        // Case 2: Collection URI lookup
        val segments = uri.pathSegments
        val isImageCollection = segments.contains("images") && segments.last() == "media"
        val isVideoCollection = segments.contains("video") && segments.last() == "media"

        if (isImageCollection || isVideoCollection) {
            var filteredMedia = mediaStore.entries.filter {
                val keyUri = Uri.parse(it.key)
                if (isImageCollection) {
                    keyUri.pathSegments.contains("images")
                } else {
                    keyUri.pathSegments.contains("video")
                }
            }

            // Simple support for RELATIVE_PATH LIKE ? AND OWNER_PACKAGE_NAME = ?
            // or DISPLAY_NAME LIKE ?
            if (selection != null) {
                if (selection.contains(MediaStore.MediaColumns.RELATIVE_PATH) && selectionArgs != null && selectionArgs.size >= 2) {
                    val pathPattern = selectionArgs[0].replace("%", ".*").replace("_", ".")
                    val ownerPattern = selectionArgs[1]
                    val pathRegex = Regex(pathPattern)
                    filteredMedia = filteredMedia.filter {
                        val path = it.value.getAsString(MediaStore.MediaColumns.RELATIVE_PATH) ?: ""
                        val owner = it.value.getAsString(MediaStore.MediaColumns.OWNER_PACKAGE_NAME) ?: context?.packageName
                        pathRegex.matches(path) && owner == ownerPattern
                    }
                } else if (selection.contains(MediaStore.MediaColumns.DISPLAY_NAME) && selectionArgs != null) {
                    val pattern = selectionArgs[0].replace("%", ".*").replace("_", ".")
                    val regex = Regex(pattern)
                    filteredMedia = filteredMedia.filter {
                        val name = it.value.getAsString(MediaStore.MediaColumns.DISPLAY_NAME) ?: ""
                        regex.matches(name)
                    }
                }
            }

            val sortedMedia = filteredMedia
                .sortedByDescending {
                    it.value.getAsLong(MediaStore.MediaColumns.DATE_ADDED) ?: 0L
                }

            for ((itemUriString, values) in sortedMedia) {
                cursor.addRow(createRow(resolvedProjection, values, Uri.parse(itemUriString)))
            }
            return cursor
        }

        // If it's a specific URI that wasn't found in Case 1, return empty cursor
        return cursor
    }

    private fun createRow(projection: Array<String>, values: ContentValues, uri: Uri): Array<Any?> {
        val packageName = context?.packageName
        return projection.map { proj ->
            when (proj) {
                MediaStore.MediaColumns._ID -> uri.lastPathSegment?.toLong()
                MediaStore.MediaColumns.OWNER_PACKAGE_NAME -> {
                    values.getAsString(
                        proj
                    ) ?: if (values.containsKey(MediaStore.MediaColumns.DISPLAY_NAME)) {
                        val name = values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)
                        if (name?.startsWith("JCA") == true) packageName else "com.other.app"
                    } else {
                        packageName
                    }
                }
                else -> values.get(proj)
            }
        }.toTypedArray()
    }

    override fun getType(uri: Uri): String? {
        return null // Not implemented
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (failNextInsert) {
            failNextInsert = false // Reset after one failure
            return null
        }
        if (values == null) return null
        val newUri = Uri.withAppendedPath(uri, nextId.toString())
        mediaStore[newUri.toString()] = values

        // Proactively create the file and write a dummy bitmap so loadThumbnail succeeds
        context?.let { ctx ->
            val file = File(ctx.cacheDir, newUri.lastPathSegment ?: "tempfile")
            if (!file.exists() || file.length() == 0L) {
                file.createNewFile()
                val bitmap = android.graphics.Bitmap.createBitmap(
                    1,
                    1,
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                file.outputStream().use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
                }
            }
        }

        nextId++
        return newUri
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val uriString = uri.toString()
        context?.let { ctx ->
            val file = File(ctx.cacheDir, uri.lastPathSegment ?: "tempfile")
            if (file.exists()) file.delete()
        }
        return if (mediaStore.remove(uriString) != null) 1 else 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        val uriString = uri.toString()
        if (mediaStore.containsKey(uriString) && values != null) {
            mediaStore[uriString]?.putAll(values)
            return 1
        }
        return 0
    }

    fun get(uri: Uri): ContentValues? {
        return mediaStore[uri.toString()]
    }

    override fun openTypedAssetFile(
        uri: Uri,
        mimeTypeFilter: String,
        opts: android.os.Bundle?,
        signal: android.os.CancellationSignal?
    ): android.content.res.AssetFileDescriptor? {
        val pfd = openFile(uri, "r")
        val file = File(context?.cacheDir, uri.lastPathSegment ?: "tempfile")
        return pfd?.let { android.content.res.AssetFileDescriptor(it, 0, file.length()) }
    }

    override fun openFile(uri: Uri, mode: String): android.os.ParcelFileDescriptor? {
        val context = context ?: return null
        val file = File(context.cacheDir, uri.lastPathSegment ?: "tempfile")
        try {
            if (thumbnailFailures.contains(uri.toString())) return null

            if (!file.exists()) {
                file.createNewFile()
                if (mode == "r") {
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        1,
                        1,
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    file.outputStream().use { out ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
                    }
                }
            }

            val accessMode = android.os.ParcelFileDescriptor.parseMode(mode)
            return android.os.ParcelFileDescriptor.open(file, accessMode)
        } catch (e: FileNotFoundException) {
            return null
        }
    }
}
