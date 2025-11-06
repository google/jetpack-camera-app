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

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.core.net.toFile
import com.google.jetpackcamera.core.common.IODispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

private const val TAG = "LocalMediaRepository"
private const val IMAGE_MIME_TYPE = "image/jpeg"
private const val VIDEO_MIME_TYPE = "video/mp4"

class LocalMediaRepository
@Inject constructor(
    @ApplicationContext private val context: Context,
    @IODispatcher private val iODispatcher: CoroutineDispatcher
) : MediaRepository {
    private val _currentMedia = MutableStateFlow<MediaDescriptor>(MediaDescriptor.None)

    override val currentMedia = _currentMedia.asStateFlow()

    override suspend fun setCurrentMedia(pendingMedia: MediaDescriptor) {
        _currentMedia.update { pendingMedia }
        Log.d(TAG, "set new media $pendingMedia")
    }

    override suspend fun load(mediaDescriptor: MediaDescriptor): Media {
        return when (mediaDescriptor) {
            is MediaDescriptor.Content.Image -> {
                val bitmap = loadImage(mediaDescriptor.uri)
                bitmap?.let { Media.Image(bitmap) } ?: Media.Error
            }

            MediaDescriptor.None -> Media.None

            is MediaDescriptor.Content.Video -> Media.Video(mediaDescriptor.uri)
        }
    }

    override suspend fun getLastCapturedMedia(): MediaDescriptor {
        val imagePair =
            getLastMediaUriWithDate(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
        val videoPair =
            getLastMediaUriWithDate(
                context.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )

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

    override suspend fun deleteMedia(
        contentResolver: ContentResolver,
        mediaDescriptor: MediaDescriptor.Content
    ) {
        if (mediaDescriptor.isCached) {
            deleteCachedMedia(mediaDescriptor)
            Log.d(TAG, "deleted cached media")
        } else {
            contentResolver.delete(mediaDescriptor.uri, null, null)
            Log.d(TAG, "deleted saved media")
        }
    }

    private fun deleteCachedMedia(mediaDescriptor: MediaDescriptor.Content) {
        mediaDescriptor.uri.toFile().delete()
    }

    @Throws(IOException::class)
    override suspend fun copyToUri(
        contentResolver: ContentResolver,
        mediaDescriptor: MediaDescriptor.Content,
        destinationUri: Uri
    ) = copyUriToUri(contentResolver, sourceUri = mediaDescriptor.uri, destinationUri)

    @Throws(IOException::class)
    private fun copyUriToUri(
        contentResolver: ContentResolver,
        sourceUri: Uri,
        destinationUri: Uri
    ) {
        contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                inputStream.copyTo(outputStream)
            } ?: throw IOException("Could not open output stream for $destinationUri")
        } ?: throw IOException("Could not open input stream for $sourceUri")
    }

    @Throws(IOException::class)
    override suspend fun saveToMediaStore(
        contentResolver: ContentResolver,
        mediaDescriptor: MediaDescriptor.Content,
        filename: String
    ): Uri? {
        val mimeType: String
        val mediaUrl: Uri
        if (mediaDescriptor is MediaDescriptor.Content.Video) {
            mimeType = VIDEO_MIME_TYPE
            mediaUrl = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            mimeType = IMAGE_MIME_TYPE
            mediaUrl = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        return copyToMediaStore(contentResolver, mediaDescriptor.uri, filename, mimeType, mediaUrl)
    }

    private fun copyToMediaStore(
        contentResolver: ContentResolver,
        sourceUri: Uri,
        outputFilename: String,
        mimeType: String,
        mediaUrl: Uri
    ): Uri? {
        var destinationUri: Uri?

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, outputFilename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DCIM + File.separator + "Camera"
                )
                // Mark as "pending" so the file isn't visible until we're done writing
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        try {
            destinationUri = contentResolver.insert(
                mediaUrl,
                contentValues
            )
            if (destinationUri == null) {
                throw IOException("Failed to create new MediaStore entry")
            }

            // Copy the data from source to destination
            copyUriToUri(contentResolver, sourceUri, destinationUri)

            // (API 29+) Publish the file by marking it as "not pending"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(destinationUri, contentValues, null, null)
            }

            Log.d(TAG, "File saved to MediaStore: $destinationUri")
            return destinationUri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to MediaStore: ${e.message}", e)

            return null
        }
    }

    /**
     * Loads an image from bitmap
     */
    private suspend fun loadImage(uri: Uri): Bitmap? = withContext(iODispatcher) {
        try {
            val loadedBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 (API 29) and above: Use ImageDecoder
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                // Android 9 (API 28) and below: Use BitmapFactory
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            }

            return@withContext loadedBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image: ${e.message}", e)
            return@withContext null
        }
    }

    private suspend fun getVideoMediaDescriptor(uri: Uri): MediaDescriptor {
        val thumbnail = getThumbnail(uri, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        return MediaDescriptor.Content.Video(uri, thumbnail)
    }

    private suspend fun getImageMediaDescriptor(uri: Uri): MediaDescriptor {
        val thumbnail = getThumbnail(uri, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        return MediaDescriptor.Content.Image(uri, thumbnail)
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
                Log.e(TAG, "Error retrieving thumbnail: ${e.message}", e)
                null
            }
        }

    private fun getLastMediaUriWithDate(
        contentResolver: ContentResolver,
        collectionUri: Uri
    ): Pair<Uri, Long>? {
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
        contentResolver.query(
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
