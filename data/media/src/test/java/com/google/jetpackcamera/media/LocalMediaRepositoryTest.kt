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
    fun deleteMedia_savedMedia_callsContentResolverDelete() = runTest {
        val insertedUri = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            createContentValues(displayName = "${filePathGenerator.prefix}_Delete.jpg")
        )!!
        val mediaToDelete = MediaDescriptor.Content.Image(insertedUri, null, false)
        repository.deleteMedia(mediaToDelete)
        val cursor = fakeContentProvider.query(insertedUri, null, null, null, null)
        assertThat(cursor.count).isEqualTo(0)
    }

    @Test
    fun lastCapturedMedia_nothingFound_returnsNone() = runTest {
        val newRepo = LocalMediaRepository(context, testDispatcher, filePathGenerator).apply {
            setThumbnailLoader(fakeThumbnailLoader)
        }
        assertThat(newRepo.lastCapturedMedia.value).isEqualTo(MediaDescriptor.None)
    }
}
