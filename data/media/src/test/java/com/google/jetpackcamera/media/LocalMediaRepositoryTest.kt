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
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.common.FilePathGenerator
import com.google.jetpackcamera.core.common.testing.FakeFilePathGenerator
import com.google.jetpackcamera.data.media.LocalMediaRepository
import com.google.jetpackcamera.data.media.Media
import com.google.jetpackcamera.data.media.MediaDescriptor
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeContentProvider: FakeContentProvider
    private val filePathGenerator: FilePathGenerator = FakeFilePathGenerator()

    // Reliable fake thumbnail loader for tests
    private val fakeThumbnailLoader: suspend (Uri, Uri) -> Bitmap? = { _, _ ->
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        contentResolver = context.contentResolver

        // Set up the FakeContentProvider to handle MediaStore URIs
        fakeContentProvider =
            Robolectric.setupContentProvider(FakeContentProvider::class.java, MediaStore.AUTHORITY)
        ShadowContentResolver.registerProviderInternal(MediaStore.AUTHORITY, fakeContentProvider)

        repository = LocalMediaRepository(
            context,
            testDispatcher,
            filePathGenerator
        ).apply {
            setThumbnailLoader(fakeThumbnailLoader)
        }
    }

    private fun createContentValues(
        displayName: String = "${filePathGenerator.prefix}_Image.jpg",
        dateAdded: Long = 1000L,
        relativePath: String = filePathGenerator.baseRelativePath,
        ownerPackageName: String = context.packageName
    ) = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.DATE_ADDED, dateAdded)
        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        put(MediaStore.MediaColumns.OWNER_PACKAGE_NAME, ownerPackageName)
    }

    @Test
    fun lastCapturedMedia_initialValueIsLatest() = runTest {
        // Given
        val olderImageTime = 1000L
        val newerVideoTime = 5000L
        val imageValues = createContentValues(
            displayName = "${filePathGenerator.prefix}_Image.jpg",
            dateAdded = olderImageTime
        )
        val videoValues = createContentValues(
            displayName = "${filePathGenerator.prefix}_Video.mp4",
            dateAdded = newerVideoTime
        )
        fakeContentProvider.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageValues)!!
        val videoUrl =
            fakeContentProvider.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoValues)!!

        // When initializing a new repository
        val newRepo = LocalMediaRepository(context, testDispatcher, filePathGenerator).apply {
            setThumbnailLoader(fakeThumbnailLoader)
        }
        val result = newRepo.lastCapturedMedia.value

        // Then
        assertThat(result).isInstanceOf(MediaDescriptor.Content.Video::class.java)
        assertThat((result as MediaDescriptor.Content.Video).uri).isEqualTo(videoUrl)
    }

    @Test
    fun lastCapturedMedia_emitsNewImageOnContentChange() = runTest {
        // When a new image is added
        val imageValues = createContentValues(
            displayName = "${filePathGenerator.prefix}_Image_New.jpg",
            dateAdded = 6000L
        )
        val imageUrl =
            fakeContentProvider.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageValues)!!

        // Notify change and let coroutines process
        contentResolver.notifyChange(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)

        // Then
        val result = repository.lastCapturedMedia.value
        assertThat(result).isInstanceOf(MediaDescriptor.Content.Image::class.java)
        assertThat((result as MediaDescriptor.Content.Image).uri).isEqualTo(imageUrl)
    }

    @Test
    fun lastCapturedMedia_emitsNewVideoOnContentChange() = runTest {
        // When a new video is added
        val videoValues = createContentValues(
            displayName = "${filePathGenerator.prefix}_Video_New.mp4",
            dateAdded = 7000L
        )
        val videoUrl =
            fakeContentProvider.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoValues)!!

        // Notify change and let coroutines process
        contentResolver.notifyChange(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null)

        // Then
        val result = repository.lastCapturedMedia.value
        assertThat(result).isInstanceOf(MediaDescriptor.Content.Video::class.java)
        assertThat((result as MediaDescriptor.Content.Video).uri).isEqualTo(videoUrl)
    }

    @Test
    fun lastCapturedMedia_ignoresNonAppMediaStoreChange() = runTest {
        // Given an app-specific file is current
        val appUrl = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            createContentValues(dateAdded = 1000L)
        )!!
        contentResolver.notifyChange(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)

        val appDescriptor = repository.lastCapturedMedia.value
        assertThat(appDescriptor).isInstanceOf(MediaDescriptor.Content::class.java)

        // When a file from a different app is inserted
        val otherUrl = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            createContentValues(
                displayName = "OTHER_Image.jpg",
                dateAdded = 5000L,
                ownerPackageName = "com.other.app"
            )
        )!!
        contentResolver.notifyChange(otherUrl, null)

        // Then the flow still points to our app's file
        assertThat(repository.lastCapturedMedia.value).isEqualTo(appDescriptor)
    }

    @Test
    fun lastCapturedMedia_ignoresWrongPath() = runTest {
        // Given a file in the wrong directory
        fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            createContentValues(
                displayName = "${filePathGenerator.prefix}_External.jpg",
                dateAdded = 5000L,
                relativePath = "Download/"
            )
        )!!
        contentResolver.notifyChange(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)

        // Then it should be ignored
        assertThat(repository.lastCapturedMedia.value).isEqualTo(MediaDescriptor.None)
    }

    @Test
    fun lastCapturedMedia_initialLoad_usesDynamicPrefixAndPath() = runTest {
        // Given a custom generator with different prefix and path
        val customGenerator = object : FilePathGenerator by filePathGenerator {
            override val prefix: String = "GPH"
            override val baseRelativePath: String = "DCIM/Photos"
        }

        // Add a JCA file (should be ignored by the custom repo)
        fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            createContentValues(
                displayName = "JCA_Image.jpg",
                relativePath = "DCIM/Camera",
                ownerPackageName = context.packageName
            )
        )!!

        // Add a GPH file (should be found by the custom repo)
        val gphUrl = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            createContentValues(
                displayName = "GPH_Image.jpg",
                relativePath = "DCIM/Photos",
                ownerPackageName = context.packageName
            )
        )!!

        val customRepo = LocalMediaRepository(context, testDispatcher, customGenerator).apply {
            setThumbnailLoader(fakeThumbnailLoader)
        }

        val result = customRepo.lastCapturedMedia.value
        assertThat(result).isInstanceOf(MediaDescriptor.Content::class.java)
        assertThat((result as MediaDescriptor.Content).uri).isEqualTo(gphUrl)
    }

    @Test
    fun lastCapturedMedia_onDeletion_fallsBackToNextLatest() = runTest {
        val urlA = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            createContentValues(
                displayName = "${filePathGenerator.prefix}_A.jpg",
                dateAdded = 1000L
            )
        )!!
        val urlB = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            createContentValues(
                displayName = "${filePathGenerator.prefix}_B.jpg",
                dateAdded = 2000L
            )
        )!!

        contentResolver.notifyChange(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)
        assertThat(
            (repository.lastCapturedMedia.value as MediaDescriptor.Content).uri
        ).isEqualTo(urlB)

        fakeContentProvider.delete(urlB, null, null)
        contentResolver.notifyChange(urlB, null)

        val result = repository.lastCapturedMedia.value
        assertThat(result).isInstanceOf(MediaDescriptor.Content::class.java)
        assertThat((result as MediaDescriptor.Content).uri).isEqualTo(urlA)
    }

    @Test
    fun lastCapturedMedia_newUriWithNullThumbnail_holdsPreviousMedia() = runTest {
        var shouldFailThumbnail = false
        val customThumbnailLoader: suspend (Uri, Uri) -> Bitmap? = { _, _ ->
            if (shouldFailThumbnail) null else Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
        val customRepo = LocalMediaRepository(
            context,
            testDispatcher,
            filePathGenerator
        ).apply {
            setThumbnailLoader(customThumbnailLoader)
        }

        val oldUrl = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            createContentValues(
                displayName = "${filePathGenerator.prefix}_Old.jpg",
                dateAdded = 1000L
            )
        )!!
        contentResolver.notifyChange(oldUrl, null)
        val initialResult = customRepo.lastCapturedMedia.value
        assertThat((initialResult as MediaDescriptor.Content).uri).isEqualTo(oldUrl)

        val newUrl = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            createContentValues(
                displayName = "${filePathGenerator.prefix}_New.jpg",
                dateAdded = 2000L
            )
        )!!

        shouldFailThumbnail = true
        contentResolver.notifyChange(newUrl, null)
        assertThat(customRepo.lastCapturedMedia.value).isEqualTo(initialResult)

        shouldFailThumbnail = false
        contentResolver.notifyChange(newUrl, null)
        val finalResult = customRepo.lastCapturedMedia.value
        assertThat((finalResult as MediaDescriptor.Content).uri).isEqualTo(newUrl)
    }

    @Test
    fun setCurrentMedia_updatesStateFlow() = runTest {
        val newMedia = MediaDescriptor.Content.Image(
            Uri.parse("content://media/external/images/media/1"),
            null,
            false
        )
        repository.setCurrentMedia(newMedia)
        assertThat(repository.currentMedia.value).isEqualTo(newMedia)
    }

    @Test
    fun loadImage_succeeds_returnsImageMedia() = runTest {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val sourceFile = File(context.cacheDir, "temp.jpg")
        try {
            FileOutputStream(sourceFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        } catch (e: Exception) {
            fail("Failed to write mock image data: ${e.message}")
        }

        val imageUri = Uri.fromFile(sourceFile)
        val mediaDescriptor = MediaDescriptor.Content.Image(imageUri, null, true)
        val result = repository.load(mediaDescriptor)
        assertThat(result).isInstanceOf(Media.Image::class.java)
    }

    @Test
    fun loadVideo_succeeds_returnsVideoMedia() = runTest {
        val sourceFile = File(context.cacheDir, "temp_video.mp4")
        sourceFile.writeText("fake video content")
        val videoUri = Uri.fromFile(sourceFile)
        val mediaDescriptor = MediaDescriptor.Content.Video(videoUri, null, true)
        val result = repository.load(mediaDescriptor)
        assertThat(result).isInstanceOf(Media.Video::class.java)
        assertThat((result as Media.Video).uri).isEqualTo(videoUri)
    }

    @Test
    fun loadImage_fails_returnsError() = runTest {
        // Given an invalid image URI
        val invalidImageUri = Uri.parse("file:///nonexistent/image.jpg")
        val mediaDescriptor = MediaDescriptor.Content.Image(invalidImageUri, null, true)

        // When
        val result = repository.load(mediaDescriptor)

        // Then
        assertThat(result).isEqualTo(Media.Error)
    }

    @Test
    fun loadVideo_fails_returnsError() = runTest {
        val nonExistentPath = "/nonexistent/path/video_not_here.mp4"
        val nonExistentUri = Uri.parse("file://$nonExistentPath")

        // Explicitly verify file does not exist (for robust setup assertion)
        assertThat(File(nonExistentPath).exists()).isFalse()

        val mediaDescriptor = MediaDescriptor.Content.Video(
            uri = nonExistentUri,
            thumbnail = null,
            isCached = true
        )

        // 2. When: The repository attempts to load the non-existent video.
        val result = repository.load(mediaDescriptor)

        // 3. Then: The result should be Media.Error because the existence check failed.
        assertThat(result).isEqualTo(Media.Error)
    }

    @Test
    fun load_none_returnsNone() = runTest {
        // When
        val result = repository.load(MediaDescriptor.None)
        // Then
        assertThat(result).isEqualTo(Media.None)
    }

    @Test
    fun deleteMedia_savedMedia_callsContentResolverDelete() = runTest {
        val insertedUri = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            createContentValues(displayName = "${filePathGenerator.prefix}_Delete.jpg")
        )!!
        val mediaToDelete = MediaDescriptor.Content.Image(insertedUri, null, false)

        // Verify it exists before deleting
        var cursor = fakeContentProvider.query(
            insertedUri,
            arrayOf(MediaStore.MediaColumns._ID),
            null,
            null,
            null
        )
        assertThat(cursor.count).isEqualTo(1)

        // When
        assertThat(repository.deleteMedia(mediaToDelete)).isTrue()

        // Then
        cursor = fakeContentProvider.query(
            insertedUri,
            arrayOf(MediaStore.MediaColumns._ID),
            null,
            null,
            null
        )
        assertThat(cursor.count).isEqualTo(0)
    }

    @Test
    fun deleteMedia_cachedMedia_deletesRealFile() = runTest {
        // 1. Setup: Create a REAL temporary file in the app's cache directory
        val cacheDir = ApplicationProvider.getApplicationContext<Context>().cacheDir
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val tempFile = File(cacheDir, "temp_test_video.mp4")
        tempFile.createNewFile()

        assertThat(tempFile.exists()).isTrue()

        // 2. Create the descriptor pointing to this real file
        val cachedUri = Uri.fromFile(tempFile)
        val mediaToDelete = MediaDescriptor.Content.Video(
            cachedUri,
            thumbnail = null,
            isCached = true
        )

        // 3. Act: Call deleteMedia
        assertThat(repository.deleteMedia(mediaToDelete)).isTrue()

        // 4. Assert: Verify the file is physically gone
        assertThat(tempFile.exists()).isFalse()
    }

    @Test
    fun deleteMedia_currentMedia_resetsToNone() = runTest {
        // Given a media item that is currently set as the active media
        val returnedUri = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            createContentValues()
        )!!
        val mediaToDelete = MediaDescriptor.Content.Image(
            returnedUri,
            thumbnail = null,
            isCached = false
        )
        repository.setCurrentMedia(mediaToDelete)
        assertThat(repository.currentMedia.value).isEqualTo(mediaToDelete)

        // When
        assertThat(repository.deleteMedia(mediaToDelete)).isTrue()

        // Then
        assertThat(repository.currentMedia.value).isEqualTo(MediaDescriptor.None)
    }

    @Test
    fun lastCapturedMedia_videoIsNewer_returnsVideo() = runTest {
        val imageValues = createContentValues(
            displayName = "${filePathGenerator.prefix}_Image.jpg",
            dateAdded = 1000L
        )
        val videoValues = createContentValues(
            displayName = "${filePathGenerator.prefix}_Video.mp4",
            dateAdded = 5000L
        )
        fakeContentProvider.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageValues)!!
        val videoUrl =
            fakeContentProvider.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoValues)!!

        contentResolver.notifyChange(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null)
        val result = repository.lastCapturedMedia.value

        assertThat(result).isInstanceOf(MediaDescriptor.Content.Video::class.java)
        assertThat((result as MediaDescriptor.Content.Video).uri).isEqualTo(videoUrl)
    }

    @Test
    fun lastCapturedMedia_imageIsNewer_returnsImage() = runTest {
        val imageValues = createContentValues(
            displayName = "${filePathGenerator.prefix}_Image.jpg",
            dateAdded = 9000L
        )
        val videoValues = createContentValues(
            displayName = "${filePathGenerator.prefix}_Video.mp4",
            dateAdded = 2000L
        )
        val imageUrl =
            fakeContentProvider.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageValues)!!
        fakeContentProvider.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoValues)!!

        contentResolver.notifyChange(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)
        val result = repository.lastCapturedMedia.value

        assertThat(result).isInstanceOf(MediaDescriptor.Content.Image::class.java)
        assertThat((result as MediaDescriptor.Content.Image).uri).isEqualTo(imageUrl)
    }

    @Test
    fun lastCapturedMedia_nothingFound_returnsNone() = runTest {
        val newRepo = LocalMediaRepository(context, testDispatcher, filePathGenerator).apply {
            setThumbnailLoader(fakeThumbnailLoader)
        }
        assertThat(newRepo.lastCapturedMedia.value).isEqualTo(MediaDescriptor.None)
    }

    @Test
    fun lastCapturedMedia_equalTimestamps_returnsImage() = runTest {
        val sameTime = 9999L
        val imageValues = createContentValues(
            displayName = "${filePathGenerator.prefix}_Image.jpg",
            dateAdded = sameTime
        )
        val videoValues = createContentValues(
            displayName = "${filePathGenerator.prefix}_Video.mp4",
            dateAdded = sameTime
        )
        val imageUrl =
            fakeContentProvider.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageValues)!!
        fakeContentProvider.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoValues)!!

        contentResolver.notifyChange(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)
        val result = repository.lastCapturedMedia.value

        assertThat(result).isInstanceOf(MediaDescriptor.Content.Image::class.java)
        assertThat((result as MediaDescriptor.Content.Image).uri).isEqualTo(imageUrl)
    }

    @Test
    fun lastCapturedMedia_multipleEventsForSameUri_emitsSameObjectReference() = runTest {
        val jcaUrl = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            createContentValues(
                displayName = "${filePathGenerator.prefix}_Image.jpg",
                dateAdded = 1000L
            )
        )!!
        contentResolver.notifyChange(jcaUrl, null)
        val firstEmission = repository.lastCapturedMedia.value

        contentResolver.notifyChange(jcaUrl, null)
        contentResolver.notifyChange(jcaUrl, null)

        assertThat(repository.lastCapturedMedia.value).isSameInstanceAs(firstEmission)
    }

    @Test
    fun lastCapturedMedia_onLastItemDeletion_emitsNone() = runTest {
        val jcaUrl = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            createContentValues(
                displayName = "${filePathGenerator.prefix}_Image.jpg",
                dateAdded = 1000L
            )
        )!!
        contentResolver.notifyChange(jcaUrl, null)
        assertThat(repository.lastCapturedMedia.value).isNotEqualTo(MediaDescriptor.None)

        fakeContentProvider.delete(jcaUrl, null, null)
        contentResolver.notifyChange(jcaUrl, null)

        assertThat(repository.lastCapturedMedia.value).isEqualTo(MediaDescriptor.None)
    }

    @Test
    fun deleteMedia_nonExistentUri_doesNotThrow() = runTest {
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

        // When & Then (no exception is thrown, and the delete reports no rows removed)
        assertThat(repository.deleteMedia(mediaToDelete)).isFalse()
    }

    @Test
    fun saveToMediaStore_video_success_returnsNewUri() = runTest {
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
        assertThat(result).isNotNull()
        val values = fakeContentProvider.get(result!!)
        assertThat(values?.get(MediaStore.MediaColumns.DISPLAY_NAME)).isEqualTo("my_video.mp4")
    }

    @Test
    fun saveToMediaStore_success_returnsNewUri() = runTest {
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
        assertThat(result).isNotNull()
        val values = fakeContentProvider.get(result!!)
        assertThat(values?.get(MediaStore.MediaColumns.DISPLAY_NAME)).isEqualTo("my_photo.jpg")
    }

    @Test
    fun saveToMediaStore_insertFails_returnsNull() = runTest {
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
        assertThat(result).isNull()
    }

    @Test
    fun saveToMediaStore_copyFails_returnsNull() = runTest {
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
        assertThat(result).isNull()
    }
}
