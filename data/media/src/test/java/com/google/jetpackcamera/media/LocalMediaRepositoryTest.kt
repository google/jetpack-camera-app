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

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.test.core.app.ApplicationProvider
import com.google.jetpackcamera.data.media.LocalMediaRepository
import com.google.jetpackcamera.data.media.MediaDescriptor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
@OptIn(ExperimentalCoroutinesApi::class)
class LocalMediaRepositoryTest {

    private lateinit var context: Context

    @Mock
    private lateinit var mockContentResolver: ContentResolver
    private lateinit var repository: LocalMediaRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // We strictly mock the ContentResolver to avoid Robolectric DB flakes
        context = spy(ApplicationProvider.getApplicationContext<Context>())
        doReturn(mockContentResolver).`when`(context).contentResolver

        // stub loadThumbnail
        doReturn(mock(Bitmap::class.java)).`when`(mockContentResolver).loadThumbnail(
            any<Uri>(),
            any<Size>(),
            any()
        )
        repository = LocalMediaRepository(context, testDispatcher)
    }

    @Test
    fun deleteMedia_savedMedia_callsContentResolverDelete() = runTest(testDispatcher) {
        // Given a saved media item (isCached = false)
        val mediaUri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            123L
        )
        val mediaToDelete = MediaDescriptor.Content.Image(
            mediaUri,
            thumbnail = null,
            isCached = false
        )

        // Mock the delete call to return 1 (success)
        doReturn(1).`when`(mockContentResolver).delete(eq(mediaUri), any(), any())

        // When
        repository.deleteMedia(mockContentResolver, mediaToDelete)

        // Then
        verify(mockContentResolver).delete(eq(mediaUri), eq(null), eq(null))
    }

    @Test
    fun deleteMedia_cachedMedia_deletesRealFile() = runTest(testDispatcher) {
        // 1. Setup: Create a REAL temporary file in the app's cache directory
        // ApplicationProvider gives us a working context for file ops in Robolectric
        val cacheDir = ApplicationProvider.getApplicationContext<Context>().cacheDir
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val tempFile = java.io.File(cacheDir, "temp_test_video.mp4")
        tempFile.createNewFile() // Actually creates the empty file on disk

        assertTrue("Setup failed: Temp file should exist before test", tempFile.exists())

        // 2. Create the descriptor pointing to this real file
        // Uri.fromFile() creates a "file://" URI, which is what your app likely uses for cached media
        val cachedUri = Uri.fromFile(tempFile)
        val mediaToDelete = MediaDescriptor.Content.Video(
            cachedUri,
            thumbnail = null,
            isCached = true
        )

        // 3. Act: Call deleteMedia
        repository.deleteMedia(mockContentResolver, mediaToDelete)

        // 4. Assert: Verify the file is physically gone
        assertFalse("Repository should have deleted the cached file", tempFile.exists())

        // Optional: Verify we DID NOT try to use standard ContentResolver delete
        verify(mockContentResolver, never()).delete(any(), any(), any())
    }

    @Test
    fun getLastCapturedMedia_videoIsNewer_returnsVideo() = runTest(testDispatcher) {
        // Given
        val olderImageId = 100L
        val olderImageTime = 1000L
        val newerVideoId = 200L
        val newerVideoTime = 5000L // Newer timestamp

        // Precisely mock what each specific URI query returns
        mockQuery(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            Pair(olderImageId, olderImageTime)
        )
        mockQuery(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            Pair(newerVideoId, newerVideoTime)
        )

        // When
        val result = repository.getLastCapturedMedia()

        // Then
        assertTrue("Result should be Video", result is MediaDescriptor.Content.Video)
        // We can use standard ContentUris to verify the ID matches
        assertEquals(
            ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, newerVideoId),
            (result as MediaDescriptor.Content.Video).uri
        )
    }

    @Test
    fun getLastCapturedMedia_imageIsNewer_returnsImage() = runTest(testDispatcher) {
        // Given
        val newerImageId = 300L
        val newerImageTime = 9000L // Newer
        val olderVideoId = 400L
        val olderVideoTime = 2000L

        mockQuery(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            Pair(newerImageId, newerImageTime)
        )
        mockQuery(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            Pair(olderVideoId, olderVideoTime)
        )

        // When
        val result = repository.getLastCapturedMedia()

        // Then
        assertTrue("Result should be Image", result is MediaDescriptor.Content.Image)
        assertEquals(
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, newerImageId),
            (result as MediaDescriptor.Content.Image).uri
        )
    }

    @Test
    fun getLastCapturedMedia_nothingFound_returnsNone() = runTest(testDispatcher) {
        // Given both queries return empty cursors (null data in helper)
        mockQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)
        mockQuery(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null)

        // When
        val result = repository.getLastCapturedMedia()

        // Then
        assertEquals(MediaDescriptor.None, result)
    }

    @Test
    fun getLastCapturedMedia_equalTimestamps_returnsImage() = runTest(testDispatcher) {
        // Given
        val imageId = 500L
        val videoId = 600L
        val sameTime = 9999L

        mockQuery(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            Pair(imageId, sameTime)
        )
        mockQuery(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            Pair(videoId, sameTime)
        )

        // When
        val result = repository.getLastCapturedMedia()

        // Then
        assertTrue(
            "Result should be Image when timestamps are equal",
            result is MediaDescriptor.Content.Image
        )
        assertEquals(
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId),
            (result as MediaDescriptor.Content.Image).uri
        )
    }

    @Test
    fun getLastCapturedMedia_onlyImageExists_returnsImage() = runTest(testDispatcher) {
        // Given
        val imageId = 700L
        val imageTime = 10000L

        mockQuery(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            Pair(imageId, imageTime)
        )
        mockQuery(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null) // No video

        // When
        val result = repository.getLastCapturedMedia()

        // Then
        assertTrue("Result should be Image", result is MediaDescriptor.Content.Image)
        assertEquals(
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId),
            (result as MediaDescriptor.Content.Image).uri
        )
    }

    @Test
    fun getLastCapturedMedia_onlyVideoExists_returnsVideo() = runTest(testDispatcher) {
        // Given
        val videoId = 800L
        val videoTime = 11000L

        mockQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null) // No image
        mockQuery(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            Pair(videoId, videoTime)
        )

        // When
        val result = repository.getLastCapturedMedia()

        // Then
        assertTrue("Result should be Video", result is MediaDescriptor.Content.Video)
        assertEquals(
            ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId),
            (result as MediaDescriptor.Content.Video).uri
        )
    }

    @Test
    fun deleteMedia_nonExistentUri_doesNotThrow() = runTest(testDispatcher) {
        // Given a URI that looks valid but won't be found by the ContentResolver
        val nonExistentUri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            999L
        )
        val mediaToDelete = MediaDescriptor.Content.Image(
            nonExistentUri,
            thumbnail = null,
            isCached = false
        )

        // Mock the delete call to return 0 (no rows affected)
        doReturn(0).`when`(mockContentResolver).delete(eq(nonExistentUri), any(), any())

        // When & Then (The test passes if no exception is thrown)
        repository.deleteMedia(mockContentResolver, mediaToDelete)

        // Verify delete was still called
        verify(mockContentResolver).delete(eq(nonExistentUri), eq(null), eq(null))
    }

    @Test
    fun saveToMediaStore_video_success_returnsNewUri() = runTest(testDispatcher) {
        // Given
        val sourceUri = Uri.parse("file://cache/temp.mp4")
        val mediaDescriptor = MediaDescriptor.Content.Video(
            sourceUri,
            thumbnail = null,
            isCached = true
        )
        val newMediaStoreUri = Uri.parse("content://media/external/video/media/102")

        // Mock successful insertion, stream opening, and update
        doReturn(newMediaStoreUri).`when`(mockContentResolver).insert(any(), any())
        val mockInputStream = java.io.ByteArrayInputStream("fake video".toByteArray())
        val mockOutputStream = java.io.ByteArrayOutputStream()
        doReturn(mockInputStream).`when`(mockContentResolver).openInputStream(sourceUri)
        doReturn(mockOutputStream).`when`(mockContentResolver).openOutputStream(newMediaStoreUri)
        doReturn(1).`when`(mockContentResolver).update(eq(newMediaStoreUri), any(), any(), any())

        // When
        val result = repository.saveToMediaStore(
            mockContentResolver,
            mediaDescriptor,
            "my_video.mp4"
        )

        // Then
        assertEquals(newMediaStoreUri, result)
        assertEquals("fake video", mockOutputStream.toString())
        verify(mockContentResolver).update(eq(newMediaStoreUri), any(), eq(null), eq(null))
    }

    @Test
    fun saveToMediaStore_success_returnsNewUri() = runTest(testDispatcher) {
        // Given
        val sourceUri = Uri.parse("file://cache/temp.jpg")
        val mediaDescriptor = MediaDescriptor.Content.Image(
            sourceUri,
            thumbnail = null,
            isCached = true
        )
        val newMediaStoreUri = Uri.parse("content://media/external/images/media/101")

        // 1. Mock successful insertion
        doReturn(newMediaStoreUri).`when`(mockContentResolver).insert(any(), any())

        // 2. Mock successful stream opening
        val mockInputStream = java.io.ByteArrayInputStream("fake data".toByteArray())
        val mockOutputStream = java.io.ByteArrayOutputStream()
        doReturn(mockInputStream).`when`(mockContentResolver).openInputStream(sourceUri)
        doReturn(mockOutputStream).`when`(mockContentResolver).openOutputStream(newMediaStoreUri)

        // 3. Mock successful update (needed for API 29+ IS_PENDING toggle)
        doReturn(1).`when`(mockContentResolver).update(eq(newMediaStoreUri), any(), any(), any())

        // When
        val result = repository.saveToMediaStore(
            mockContentResolver,
            mediaDescriptor,
            "my_photo.jpg"
        )

        // Then
        assertEquals(newMediaStoreUri, result)

        // Verify we actually wrote data (our fake "fake data")
        assertEquals("fake data", mockOutputStream.toString())

        // Verify IS_PENDING was flipped back to 0 (visible)
        verify(mockContentResolver).update(eq(newMediaStoreUri), any(), eq(null), eq(null))
    }

    @Test
    fun saveToMediaStore_insertFails_returnsNull() = runTest(testDispatcher) {
        // Given
        val sourceUri = Uri.parse("file://cache/temp.jpg")
        val mediaDescriptor = MediaDescriptor.Content.Image(
            sourceUri,
            thumbnail = null,
            isCached = true
        )

        // Mock insert returning null (e.g., database error)
        doReturn(null).`when`(mockContentResolver).insert(any(), any())

        // When
        val result = repository.saveToMediaStore(mockContentResolver, mediaDescriptor, "fail.jpg")

        // Then
        assertEquals(null, result)
    }

    @Test
    fun saveToMediaStore_copyFails_returnsNull() = runTest(testDispatcher) {
        // Given
        val sourceUri = Uri.parse("file://cache/temp.jpg")
        val mediaDescriptor = MediaDescriptor.Content.Image(
            sourceUri,
            thumbnail = null,
            isCached = true
        )
        val newMediaStoreUri = Uri.parse("content://media/external/images/media/101")

        // Insert succeeds...
        doReturn(newMediaStoreUri).`when`(mockContentResolver).insert(any(), any())

        // ...but Input Stream fails to open (e.g. file deleted before save)
        doThrow(java.io.FileNotFoundException("Source file missing"))
            .`when`(mockContentResolver).openInputStream(sourceUri)

        // When
        val result = repository.saveToMediaStore(mockContentResolver, mediaDescriptor, "broken.jpg")

        // Then
        assertEquals(null, result)
    }

    // --- Helpers ---

    private fun mockQuery(collectionUri: Uri, data: Pair<Long, Long>?) {
        val cursor = if (data != null) {
            MatrixCursor(
                arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_ADDED)
            ).apply {
                addRow(arrayOf(data.first, data.second))
            }
        } else {
            MatrixCursor(arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_ADDED))
        }

        // Important: Use doReturn().when() for spies/complex mocks
        // We ONLY match strictly on the collectionUri to differentiate calls.
        // Everything else is any() to be robust against minor code changes.
        doReturn(cursor).`when`(mockContentResolver).query(
            eq(collectionUri),
            any(), // projection
            anyString(), // selection (using anyString() is safer than contains() sometimes)
            any(), // selectionArgs
            anyString() // sortOrder
        )
    }
}
