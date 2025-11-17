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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
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
    private val repositoryScope = CoroutineScope(iODispatcher + SupervisorJob())
    private val _currentMedia = MutableStateFlow<MediaDescriptor>(MediaDescriptor.None)

    override val currentMedia = _currentMedia.asStateFlow()

    /**
     * Sets the current media descriptor.
     *
     * @param pendingMedia The [MediaDescriptor] to set as current.
     */
    override suspend fun setCurrentMedia(pendingMedia: MediaDescriptor) {
        _currentMedia.update { pendingMedia }
        Log.d(TAG, "set new media $pendingMedia")
    }

    /**
     * Loads the media for the given [MediaDescriptor].
     *
     * @return The loaded [Media] object, or [Media.Error] if loading fails.
     */
    override suspend fun load(mediaDescriptor: MediaDescriptor): Media {
        return when (mediaDescriptor) {
            is MediaDescriptor.Content.Image -> {
                // 💡 Check existence before loading
                if (!exists(mediaDescriptor.uri)) {
                    return Media.Error
                }

                try {
                    val bitmap = loadImage(mediaDescriptor.uri)
                    bitmap?.let { Media.Image(bitmap) } ?: Media.Error
                } catch (e: Exception) {
                    Media.Error
                }
            }

            is MediaDescriptor.Content.Video -> {
                if (!exists(mediaDescriptor.uri)) {
                    return Media.Error
                }
                Media.Video(mediaDescriptor.uri)
            }

            MediaDescriptor.None -> Media.None
        }
    }

    private suspend fun exists(uri: Uri): Boolean = withContext(iODispatcher) {
        // 1. File URI check (for cached media)
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            val path = uri.path ?: return@withContext false
            return@withContext File(path).exists()
        }

        // 2. Content URI check (for saved MediaStore media)
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            // Only query for the ID column.
            val projection = arrayOf(MediaStore.MediaColumns._ID)

            // Using a try-catch for contentResolver operations is best practice
            return@withContext try {
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    // If the cursor is not null and has at least one row, the URI exists.
                    cursor.moveToFirst() && cursor.count > 0
                } ?: false
            } catch (e: Exception) {
                false
            }
        }

        // 3. Fallback for unexpected schemes
        return@withContext false
    }

    /**
     * Returns the most recent captured media (image or video) from the MediaStore.
     *
     * @return The [MediaDescriptor] of the last captured media, or [MediaDescriptor.None] if no media is found.
     */
    override suspend fun getLastCapturedMedia(): MediaDescriptor {
        val imagePair =
            getLastSavedMediaUriWithDate(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
        val videoPair =
            getLastSavedMediaUriWithDate(
                context.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )

        return if (imagePair != null && videoPair != null) {
            // Case 1: BOTH exist. Compare dates.
            if (imagePair.second >= videoPair.second) {
                getImageMediaDescriptor(imagePair.first)
            } else {
                getVideoMediaDescriptor(videoPair.first)
            }
        } else if (imagePair != null) {
            // Case 2: Only image exists
            getImageMediaDescriptor(imagePair.first)
        } else if (videoPair != null) {
            // Case 3: Only video exists
            getVideoMediaDescriptor(videoPair.first)
        } else {
            // Case 4: Neither exist
            MediaDescriptor.None
        }
    }

    /**
     * Deletes the specified media from either the cache or the MediaStore.
     */
    override suspend fun deleteMedia(mediaDescriptor: MediaDescriptor.Content): Boolean {
        val finalResult = withContext(repositoryScope.coroutineContext) {
            val result = if (mediaDescriptor.isCached) {
                deleteCachedMedia(mediaDescriptor)
            } else {
                context.contentResolver.delete(mediaDescriptor.uri, null, null) >= 1
            }
            if (result && !mediaDescriptor.isCached) {
                Log.d(TAG, "deleted saved media")
            }
            result
        }
        if (finalResult && currentMedia.value == mediaDescriptor) {
            setCurrentMedia(MediaDescriptor.None)
        }
        return finalResult
    }

    /**
     * Deletes a cached media file.
     */
    private fun deleteCachedMedia(mediaDescriptor: MediaDescriptor.Content): Boolean {
        val result = mediaDescriptor.uri.toFile().delete()
        if (result) {
            Log.d(TAG, "deleted cached media")
        }
        return result
    }

    /**
     * Copies the content of a media descriptor to a specified destination URI.
     * @throws IOException if an I/O error occurs during the copy operation.
     */
    @Throws(IOException::class)
    override suspend fun copyToUri(mediaDescriptor: MediaDescriptor.Content, destinationUri: Uri) =
        copyUriToUri(context.contentResolver, sourceUri = mediaDescriptor.uri, destinationUri)

    /**
     * Copies the content from a source URI to a destination URI.
     *
     * @throws IOException if an I/O error occurs during the copy operation.
     */
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

    /**
     * Saves the specified media to the MediaStore.
     *
     * @param filename The desired filename for the media (including file extension e.g. ".mp4" or ".jpg").
     *
     * @return The [Uri] of the saved media, or `null` if the save attempt fails.
     * @throws IOException if an I/O error occurs during the save operation.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Throws(IOException::class)
    override suspend fun saveToMediaStore(
        mediaDescriptor: MediaDescriptor.Content,
        filename: String
    ): Uri? = withContext(repositoryScope.coroutineContext) {
        val mimeType: String
        val mediaUrl: Uri
        if (mediaDescriptor is MediaDescriptor.Content.Video) {
            mimeType = VIDEO_MIME_TYPE
            mediaUrl = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            mimeType = IMAGE_MIME_TYPE
            mediaUrl = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        copyToMediaStore(context.contentResolver, mediaDescriptor.uri, filename, mimeType, mediaUrl)
    }

    /**
     * Copies content from a source URI to the MediaStore.
     *
     * @param contentResolver The [ContentResolver] used for MediaStore operations.
     * @param sourceUri The [Uri] of the source content.
     * @param outputFilename The desired filename for the new MediaStore entry (including file extension e.g. ".mp4" or ".jpg").
     * @param mimeType The MIME type of the content (e.g., "image/jpeg", "video/mp4").
     * @param mediaUrl The base [Uri] for the MediaStore collection (e.g., [MediaStore.Images.Media.EXTERNAL_CONTENT_URI]).
     *
     * @return The [Uri] of the newly created MediaStore entry, or `null` if the operation fails.
     */
    private fun copyToMediaStore(
        contentResolver: ContentResolver,
        sourceUri: Uri,
        outputFilename: String,
        mimeType: String,
        mediaUrl: Uri
    ): Uri? {
        val destinationUri: Uri?

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
    @Throws(IOException::class)
    private suspend fun loadImage(uri: Uri): Bitmap? = withContext(iODispatcher) {
        try {
            val loadedBitmap = if (uri.scheme == ContentResolver.SCHEME_FILE) {
                BitmapFactory.decodeFile(uri.path)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
            throw e
        }
    }

    /**
     * Creates a [MediaDescriptor.Content.Video] for the given video URI.
     *
     * @return A [MediaDescriptor.Content.Video] object.
     */
    private suspend fun getVideoMediaDescriptor(uri: Uri): MediaDescriptor {
        val thumbnail = getThumbnail(uri, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        return MediaDescriptor.Content.Video(uri, thumbnail)
    }

    /**
     * Creates a [MediaDescriptor.Content.Image] for the given image URI.
     *
     * @return A [MediaDescriptor.Content.Image] object.
     */
    private suspend fun getImageMediaDescriptor(uri: Uri): MediaDescriptor {
        val thumbnail = getThumbnail(uri, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        return MediaDescriptor.Content.Image(uri, thumbnail)
    }

    /**
     * Retrieves a thumbnail for the given media URI.
     *
     * @param uri The [Uri] of the media.
     * @param collectionUri The collection [Uri] (e.g., [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] or [MediaStore.Video.Media.EXTERNAL_CONTENT_URI]).
     * @return The [Bitmap] thumbnail, or `null` if retrieval fails.
     */
    private suspend fun getThumbnail(uri: Uri, collectionUri: Uri): Bitmap? =
        withContext(iODispatcher) {
            if (uri.scheme != ContentResolver.SCHEME_CONTENT) {
                Log.e(TAG, "URI is not managed by a content provider")
                return@withContext null
            } else {
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
        }

    /**
     * This function queries the MediaStore for media files that have a display name starting with
     * "JCA". It returns the URI and date added for the most recently added file.
     *
     * @param contentResolver The [ContentResolver] to query the MediaStore.
     * @param collectionUri The [Uri] of the media collection to query (e.g., [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] or [MediaStore.Video.Media.EXTERNAL_CONTENT_URI]).
     * @return A [Pair] containing the [Uri] and date added (in milliseconds) of the last media item, or `null` if no media is found.
     */
    private fun getLastSavedMediaUriWithDate(
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
