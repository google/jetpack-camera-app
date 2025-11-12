/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.jetpackcamera.utils

import android.app.Instrumentation
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.BaseColumns
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.transform

private const val TAG = "AppTestUtil"

internal val APP_REQUIRED_PERMISSIONS: List<String> = buildList {
    add(android.Manifest.permission.CAMERA)
    add(android.Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT <= 28) {
        add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

val TEST_REQUIRED_PERMISSIONS: List<String> = buildList {
    addAll(APP_REQUIRED_PERMISSIONS)
    if (Build.VERSION.SDK_INT >= 33) {
        add(android.Manifest.permission.READ_MEDIA_IMAGES)
        add(android.Manifest.permission.READ_MEDIA_VIDEO)
    }
}

internal val PICTURES_DIR_PATH: String = Environment.getExternalStoragePublicDirectory(
    Environment.DIRECTORY_PICTURES
).path

internal val MOVIES_DIR_PATH: String = Environment.getExternalStoragePublicDirectory(
    Environment.DIRECTORY_MOVIES
).path

internal val MEDIA_DIR_PATH: String = Environment.getExternalStoragePublicDirectory(
    Environment.DIRECTORY_DCIM
).path
fun mediaStoreInsertedFlow(
    mediaUri: Uri,
    instrumentation: Instrumentation,
    filePrefix: String = ""
): Flow<Pair<String, Uri>> = with(instrumentation.targetContext.contentResolver) {
    // Creates a map of the display names and corresponding URIs for all files contained within
    // the URI argument. If the URI is a single file, the map will contain a single file.
    // On API 29+, this will also only return files that are not "pending". Pending files
    // have not yet been fully written.
    fun queryWrittenFiles(uri: Uri): Map<String, Uri> {
        return buildMap {
            query(
                uri,
                buildList {
                    add(BaseColumns._ID)
                    add(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (Build.VERSION.SDK_INT >= 29) {
                        add(MediaStore.MediaColumns.IS_PENDING)
                    }
                }.toTypedArray(),
                null,
                null,
                null
            )?.use { cursor: Cursor ->
                cursor.moveToFirst()
                val idCol = cursor.getColumnIndex(BaseColumns._ID)
                val displayNameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)

                while (!cursor.isAfterLast) {
                    val id = cursor.getLong(idCol)
                    val displayName = cursor.getString(displayNameCol)
                    val isPending = if (Build.VERSION.SDK_INT >= 29) {
                        cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns.IS_PENDING))
                    } else {
                        // On devices pre-API 29, we don't have an is_pending column, so never
                        // say that the file is pending
                        0
                    }
                    if (isPending == 0 &&
                        (filePrefix.isEmpty() || displayName.startsWith(filePrefix))
                    ) {
                        // Construct URI for a single file
                        val outputUri = if (uri.lastPathSegment?.equals("$id") == false) {
                            uri.buildUpon().appendPath("$id").build()
                        } else {
                            uri
                        }
                        put(displayName, outputUri)
                    }
                    cursor.moveToNext()
                }
            }
        }
    }

    // Get the full list of initially written files. We'll append files to this as we
    // publish them.
    val existingFiles = queryWrittenFiles(mediaUri).toMutableMap()
    return callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                onChange(selfChange, null)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                onChange(selfChange, uri, 0)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?, flags: Int) {
                onChange(selfChange, uri?.let { setOf(it) } ?: emptySet(), flags)
            }

            override fun onChange(selfChange: Boolean, uris: Collection<Uri>, flags: Int) {
                uris.forEach { uri ->
                    queryWrittenFiles(uri).forEach {
                        val result = trySend(it)
                        if (result.isFailure) {
                            Log.d(TAG, "Media store change failed result: $result")
                        }
                    }
                }
            }
        }

        registerContentObserver(mediaUri, true, observer)

        awaitClose {
            unregisterContentObserver(observer)
        }
    }.transform {
        if (!existingFiles.containsKey(it.key)) {
            existingFiles[it.key] = it.value
            emit(it.toPair())
        }
    }
}
