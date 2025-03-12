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
package com.google.jetpackcamera.data.media

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import com.google.jetpackcamera.core.common.IODispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class LocalMediaRepository
@Inject constructor(
    @ApplicationContext private val context: Context,
    @IODispatcher private val iODispatcher: CoroutineDispatcher
) : MediaRepository {

    override suspend fun load(mediaDescriptor: MediaDescriptor): Media {
        return when (mediaDescriptor) {
            is MediaDescriptor.Image -> loadImage(mediaDescriptor.uri)
            MediaDescriptor.None -> Media.None
            is MediaDescriptor.Video -> Media.Video(mediaDescriptor.uri)
        }
    }

    override suspend fun getLastCapturedMedia(): MediaDescriptor {
        val imagePair =
            getLastMediaUriWithDate(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val videoPair =
            getLastMediaUriWithDate(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)

        return when {
            imagePair == null && videoPair == null -> MediaDescriptor.None
            imagePair == null && videoPair != null -> getVideoMediaDescriptor(videoPair.first)
            videoPair == null && imagePair != null -> getImageMediaDescriptor(imagePair.first)
            imagePair != null && videoPair != null -> {
                if (imagePair.second > videoPair.second) {
                    getImageMediaDescriptor(imagePair.first)
                } else {
                    getVideoMediaDescriptor(videoPair.first)
                }
            }

            else -> MediaDescriptor.None // Should not happen
        }
    }

    private suspend fun loadImage(uri: Uri): Media = withContext(iODispatcher) {
        try {
            val loadedBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 (API 29) and above: Use ImageDecoder
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } else {
                // Android 9 (API 28) and below: Use BitmapFactory
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            }

            return@withContext if(loadedBitmap != null) {
                 Media.Image(loadedBitmap)
            } else {
                 Media.Error
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Media.Error
        }
    }

    private suspend fun getVideoMediaDescriptor(uri: Uri): MediaDescriptor {
        val thumbnail = getThumbnail(uri, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        return MediaDescriptor.Video(uri, thumbnail)
    }

    private suspend fun getImageMediaDescriptor(uri: Uri): MediaDescriptor {
        val thumbnail = getThumbnail(uri, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        return MediaDescriptor.Image(uri, thumbnail)
    }

    private suspend fun getThumbnail(uri: Uri, collectionUri: Uri): Bitmap? =
        withContext(iODispatcher) {
            return@withContext try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(uri, Size(640, 480), null)
                } else {
                    if (collectionUri == MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {
                        MediaStore.Images.Thumbnails.getThumbnail(
                            context.contentResolver,
                            ContentUris.parseId(uri),
                            MediaStore.Images.Thumbnails.MINI_KIND,
                            null
                        )
                    } else { // Video
                        MediaStore.Video.Thumbnails.getThumbnail(
                            context.contentResolver,
                            ContentUris.parseId(uri),
                            MediaStore.Video.Thumbnails.MINI_KIND,
                            null
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    private fun getLastMediaUriWithDate(context: Context, collectionUri: Uri): Pair<Uri, Long>? {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATE_ADDED
        )

        // Filter by filenames starting with "JCA"
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("JCA%")

        // Sort the results so that the most recently added media appears first.
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        // Perform the query on the MediaStore.
        context.contentResolver.query(
            collectionUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.DATE_ADDED
                )

                val id = cursor.getLong(idColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)

                val uri = ContentUris.withAppendedId(collectionUri, id)
                return Pair(uri, dateAdded)
            }
        }
        return null
    }
}
