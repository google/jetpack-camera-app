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
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import com.google.jetpackcamera.data.media.LocalMediaRepository
import com.google.jetpackcamera.data.media.Media
import com.google.jetpackcamera.data.media.MediaDescriptor
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
@OptIn(ExperimentalCoroutinesApi::class)
class LocalMediaRepositoryTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var repository: LocalMediaRepository
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeContentProvider: FakeContentProvider

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        contentResolver = context.contentResolver

        // Set up the FakeContentProvider to handle MediaStore URIs
        fakeContentProvider =
            Robolectric.setupContentProvider(FakeContentProvider::class.java, MediaStore.AUTHORITY)
        ShadowContentResolver.registerProviderInternal(MediaStore.AUTHORITY, fakeContentProvider)

        repository = LocalMediaRepository(context, testDispatcher)
    }

    @Test
    fun setCurrentMedia_updatesStateFlow() = runTest(testDispatcher) {
        // Given
        val initialMedia = repository.currentMedia.value
        assertEquals(MediaDescriptor.None, initialMedia)

        // When
        val newMedia = MediaDescriptor.Content.Image(
            Uri.parse("content://media/external/images/media/1"),
            null,
            false
        )
        repository.setCurrentMedia(newMedia)

        // Then
        assertEquals(newMedia, repository.currentMedia.value)
    }

    @Test
    fun loadImage_succeeds_returnsImageMedia() = runTest(testDispatcher) {
        // 1. Create a real, decodable Bitmap
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        // 2. Given a valid image URI
        val sourceFile = File(context.cacheDir, "temp.jpg")

        // Write the actual Bitmap data as a compressed JPEG
        try {
            FileOutputStream(sourceFile).use { outputStream ->
                // Use compress to write a valid image format
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        } catch (e: Exception) {
            // Handle potential IO exceptions if necessary
            fail("Failed to write mock image data: ${e.message}")
        }

        // Check if the file was created and is non-empty
        assertTrue(
            "Mock file should exist and be non-empty",
            sourceFile.exists() && sourceFile.length() > 0
        )

        val imageUri = Uri.fromFile(sourceFile)
        val mediaDescriptor = MediaDescriptor.Content.Image(imageUri, null, true)

        // When
        val result = repository.load(mediaDescriptor)

        // Then
        assertTrue(
            "Load result should be Media.Image but was ${result::class.java.simpleName}",
            result is Media.Image
        )
    }

    @Test
    fun loadVideo_succeeds_returnsVideoMedia() = runTest(testDispatcher) {
        // 1. Setup: Create a temporary file in the cache directory
        val sourceFile = File(context.cacheDir, "temp_video.mp4")

        // 2. Write a small amount of data to make it non-empty.
        // Unlike images, video content doesn't need to be fully valid to pass the existence check.
        // However, if the repository eventually uses a video decoder for metadata/thumbnail,
        // a small amount of non-zero data ensures the file exists and is readable.
        sourceFile.writeText("fake video content")

        // Ensure the file exists before proceeding
        assertTrue("Setup failed: Temp file must exist", sourceFile.exists())

        // 3. Given a valid video URI (file:// pointing to the real file)
        val videoUri = Uri.fromFile(sourceFile)
        val mediaDescriptor = MediaDescriptor.Content.Video(videoUri, null, true)

        // When
        val result = repository.load(mediaDescriptor)

        // Then
        assertTrue(
            "Result should be Media.Video but was: ${result::class.java.simpleName}",
            result is Media.Video
        )
        assertEquals(videoUri, (result as Media.Video).uri)
    }

    @Test
    fun loadImage_fails_returnsError() = runTest(testDispatcher) {
        // Given an invalid image URI
        val invalidImageUri = Uri.parse("file:///nonexistent/image.jpg")
        val mediaDescriptor = MediaDescriptor.Content.Image(invalidImageUri, null, true)

        // When
        val result = repository.load(mediaDescriptor)

        // Then
        assertEquals(Media.Error, result)
    }

    @Test
    fun loadVideo_fails_returnsError() = runTest(testDispatcher) {
        val nonExistentPath = "/nonexistent/path/video_not_here.mp4"
        val nonExistentUri = Uri.parse("file://$nonExistentPath")

        // Explicitly verify file does not exist (for robust setup assertion)
        assertFalse(File(nonExistentPath).exists())

        val mediaDescriptor = MediaDescriptor.Content.Video(
            uri = nonExistentUri,
            thumbnail = null,
            isCached = true
        )

        // 2. When: The repository attempts to load the non-existent video.
        val result = repository.load(mediaDescriptor)

        // 3. Then: The result should be Media.Error because the existence check failed.
        assertEquals(Media.Error, result)
    }

    @Test
    fun load_none_returnsNone() = runTest(testDispatcher) {
        // When
        val result = repository.load(MediaDescriptor.None)
        // Then
        assertEquals(Media.None, result)
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
        // Add the media to the fake provider so it can be deleted
        fakeContentProvider.insert(mediaUri, ContentValues())

        // When
        repository.deleteMedia(mediaToDelete)

        // Then
        // The fake provider will assert internally if the URI is not found.
        // A successful run of this test means the delete was handled.
        val cursor = fakeContentProvider.query(mediaUri, null, null, null, null)
        assertEquals(0, cursor.count)
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
        repository.deleteMedia(mediaToDelete)

        // 4. Assert: Verify the file is physically gone
        assertFalse("Repository should have deleted the cached file", tempFile.exists())
    }

    @Test
    fun deleteMedia_currentMedia_resetsToNone() = runTest(testDispatcher) {
        // Given a media item that is currently set as the active media
        val returnedUri = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues()
        )!!
        val mediaToDelete = MediaDescriptor.Content.Image(
            returnedUri,
            thumbnail = null,
            isCached = false
        )
        repository.setCurrentMedia(mediaToDelete)
        assertEquals(mediaToDelete, repository.currentMedia.value)

        // When
        repository.deleteMedia(mediaToDelete)

        // Then
        assertEquals(MediaDescriptor.None, repository.currentMedia.value)
    }

    @Test
    fun getLastCapturedMedia_videoIsNewer_returnsVideo() = runTest(testDispatcher) {
        // Given
        val olderImageTime = 1000L
        val newerVideoTime = 5000L

        // Insert mock data into the fake provider
        val imageValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DATE_ADDED, olderImageTime)
            put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Image.jpg")
        }
        val videoValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DATE_ADDED, newerVideoTime)
            put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Video.mp4")
        }
        fakeContentProvider.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageValues)!!
        val videoUrl =
            fakeContentProvider.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoValues)!!

        // When
        val result = repository.getLastCapturedMedia()

        // Then
        assertTrue("Result should be Video", result is MediaDescriptor.Content.Video)
        assertEquals(videoUrl, (result as MediaDescriptor.Content.Video).uri)
    }

    @Test
    fun getLastCapturedMedia_imageIsNewer_returnsImage() = runTest(testDispatcher) {
        // Given
        val newerImageTime = 9000L
        val olderVideoTime = 2000L

        val imageValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DATE_ADDED, newerImageTime)
            put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Image.jpg")
        }
        val videoValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DATE_ADDED, olderVideoTime)
            put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Video.mp4")
        }
        val imageUrl =
            fakeContentProvider.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageValues)!!
        fakeContentProvider.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoValues)!!

        // When
        val result = repository.getLastCapturedMedia()

        // Then
        assertTrue("Result should be Image", result is MediaDescriptor.Content.Image)
        assertEquals(imageUrl, (result as MediaDescriptor.Content.Image).uri)
    }

    @Test
    fun getLastCapturedMedia_nothingFound_returnsNone() = runTest(testDispatcher) {
        // Given an empty provider
        // When
        val result = repository.getLastCapturedMedia()
        // Then
        assertEquals(MediaDescriptor.None, result)
    }

    @Test
    fun getLastCapturedMedia_equalTimestamps_returnsImage() = runTest(testDispatcher) {
        // Given
        val sameTime = 9999L
        val imageValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DATE_ADDED, sameTime)
            put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Image.jpg")
        }
        val videoValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DATE_ADDED, sameTime)
            put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Video.mp4")
        }
        val imageUrl =
            fakeContentProvider.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageValues)!!
        fakeContentProvider.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoValues)!!

        // When
        val result = repository.getLastCapturedMedia()

        // Then
        assertTrue(
            "Result should be Image when timestamps are equal",
            result is MediaDescriptor.Content.Image
        )
        assertEquals(imageUrl, (result as MediaDescriptor.Content.Image).uri)
    }

    @Test
    fun getLastCapturedMedia_onlyImageExists_returnsImage() = runTest(testDispatcher) {
        // Given
        val imageTime = 10000L
        val imageValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DATE_ADDED, imageTime)
            put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Image.jpg")
        }
        val imageUrl =
            fakeContentProvider.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageValues)!!

        // When
        val result = repository.getLastCapturedMedia()

        // Then
        assertTrue(
            "Result should be Image but was ${result::class}",
            result is MediaDescriptor.Content.Image
        )
        assertEquals(imageUrl, (result as MediaDescriptor.Content.Image).uri)
    }

    @Test
    fun getLastCapturedMedia_onlyVideoExists_returnsVideo() = runTest(testDispatcher) {
        // Given
        val videoTime = 11000L
        val videoValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DATE_ADDED, videoTime)
            put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Video.mp4")
        }
        val videoUrl =
            fakeContentProvider.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoValues)!!

        // When
        val result = repository.getLastCapturedMedia()

        // Then
        assertTrue("Result should be Video", result is MediaDescriptor.Content.Video)
        assertEquals(videoUrl, (result as MediaDescriptor.Content.Video).uri)
    }

    @Test
    fun deleteMedia_nonExistentUri_doesNotThrow() = runTest(testDispatcher) {
        // Given a URI that does not exist in the provider
        val nonExistentUri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            999L
        )
        val mediaToDelete = MediaDescriptor.Content.Image(
            nonExistentUri,
            thumbnail = null,
            isCached = false
        )

        // When & Then (The test passes if no exception is thrown)
        repository.deleteMedia(mediaToDelete)
    }

    @Test
    fun saveToMediaStore_video_success_returnsNewUri() = runTest(testDispatcher) {
        // Given
        val sourceFile = File(context.cacheDir, "temp.mp4")
        sourceFile.writeText("fake video data")
        val sourceUri = Uri.fromFile(sourceFile)

        val mediaDescriptor = MediaDescriptor.Content.Video(
            sourceUri,
            thumbnail = null,
            isCached = true
        )

        // When
        val result = repository.saveToMediaStore(

            mediaDescriptor,
            "my_video.mp4"
        )

        // Then
        assertNotNull(result)
        // Check that the media is in the fake provider with the correct name
        val values = fakeContentProvider.get(result!!)
        assertEquals("my_video.mp4", values?.get(MediaStore.MediaColumns.DISPLAY_NAME))
    }

    @Test
    fun saveToMediaStore_success_returnsNewUri() = runTest(testDispatcher) {
        // Given
        val sourceFile = File(context.cacheDir, "temp.jpg")
        sourceFile.writeText("fake image data")
        val sourceUri = Uri.fromFile(sourceFile)
        val mediaDescriptor = MediaDescriptor.Content.Image(
            sourceUri,
            thumbnail = null,
            isCached = true
        )

        // When
        val result = repository.saveToMediaStore(

            mediaDescriptor,
            "my_photo.jpg"
        )

        // Then
        assertNotNull(result)
        val values = fakeContentProvider.get(result!!)
        assertEquals("my_photo.jpg", values?.get(MediaStore.MediaColumns.DISPLAY_NAME))
    }

    @Test
    fun saveToMediaStore_insertFails_returnsNull() = runTest(testDispatcher) {
        // Given
        val sourceFile = File(context.cacheDir, "temp.jpg")
        sourceFile.writeText("fake image data")
        val sourceUri = Uri.fromFile(sourceFile)
        val mediaDescriptor = MediaDescriptor.Content.Image(
            sourceUri,
            thumbnail = null,
            isCached = true
        )
        // Simulate an insert failure
        fakeContentProvider.setFailNextInsert(true)

        // When
        val result = repository.saveToMediaStore(

            mediaDescriptor,
            "my_photo.jpg"
        )

        // Then
        assertEquals(null, result)
    }

    @Test
    fun saveToMediaStore_copyFails_returnsNull() = runTest(testDispatcher) {
        // Given a source URI that points to a non-existent file
        val sourceUri = Uri.parse("file:///nonexistent/file.jpg")
        val mediaDescriptor = MediaDescriptor.Content.Image(
            sourceUri,
            thumbnail = null,
            isCached = true
        )

        // When
        val result = repository.saveToMediaStore(mediaDescriptor, "broken.jpg")

        // Then
        assertEquals(null, result)
    }
}
