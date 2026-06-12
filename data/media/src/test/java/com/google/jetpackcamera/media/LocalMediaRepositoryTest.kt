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
            FakeFilePathGenerator()
        ).apply {
            setThumbnailLoader(fakeThumbnailLoader)
        }
    }

    @Test
    fun lastCapturedMedia_initialValueIsLatest() = runTest {
        // Given
        val olderImageTime = 1000L
        val newerVideoTime = 5000L
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

        // When initializing a new repository
        val newRepo = LocalMediaRepository(context, testDispatcher, FakeFilePathGenerator()).apply {
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
        val imageValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DATE_ADDED, 6000L)
            put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Image_New.jpg")
        }
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
        val videoValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DATE_ADDED, 7000L)
            put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Video_New.mp4")
        }
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
    fun setCurrentMedia_updatesStateFlow() = runTest {
        // When
        val newMedia = MediaDescriptor.Content.Image(
            Uri.parse("content://media/external/images/media/1"),
            null,
            false
        )
        repository.setCurrentMedia(newMedia)

        // Then
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

        // When
        val result = repository.load(mediaDescriptor)

        // Then
        assertThat(result).isInstanceOf(Media.Image::class.java)
    }

    @Test
    fun loadVideo_succeeds_returnsVideoMedia() = runTest {
        val sourceFile = File(context.cacheDir, "temp_video.mp4")
        sourceFile.writeText("fake video content")
        val videoUri = Uri.fromFile(sourceFile)
        val mediaDescriptor = MediaDescriptor.Content.Video(videoUri, null, true)

        // When
        val result = repository.load(mediaDescriptor)

        // Then
        assertThat(result).isInstanceOf(Media.Video::class.java)
        assertThat((result as Media.Video).uri).isEqualTo(videoUri)
    }

    @Test
    fun loadImage_fails_returnsError() = runTest {
        val invalidImageUri = Uri.parse("file:///nonexistent/image.jpg")
        val mediaDescriptor = MediaDescriptor.Content.Image(invalidImageUri, null, true)

        // When
        val result = repository.load(mediaDescriptor)

        // Then
        assertThat(result).isEqualTo(Media.Error)
    }

    @Test
    fun loadVideo_fails_returnsError() = runTest {
        val nonExistentUri = Uri.parse("file:///nonexistent/video.mp4")
        val mediaDescriptor = MediaDescriptor.Content.Video(nonExistentUri, null, true)

        // When
        val result = repository.load(mediaDescriptor)

        // Then
        assertThat(result).isEqualTo(Media.Error)
    }

    @Test
    fun load_none_returnsNone() = runTest {
        val result = repository.load(MediaDescriptor.None)
        assertThat(result).isEqualTo(Media.None)
    }

    @Test
    fun deleteMedia_savedMedia_callsContentResolverDelete() = runTest {
        val insertedUri = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_To_Delete.jpg")
            }
        )!!

        val mediaToDelete = MediaDescriptor.Content.Image(insertedUri, null, false)

        // When
        repository.deleteMedia(mediaToDelete)

        // Then
        val cursor = fakeContentProvider.query(insertedUri, null, null, null, null)
        assertThat(cursor.count).isEqualTo(0)
    }

    @Test
    fun deleteMedia_cachedMedia_deletesRealFile() = runTest {
        val tempFile = File(context.cacheDir, "temp_to_delete.mp4")
        tempFile.createNewFile()
        assertThat(tempFile.exists()).isTrue()

        val mediaToDelete = MediaDescriptor.Content.Video(Uri.fromFile(tempFile), null, true)

        // When
        repository.deleteMedia(mediaToDelete)

        // Then
        assertThat(tempFile.exists()).isFalse()
    }

    @Test
    fun deleteMedia_currentMedia_resetsToNone() = runTest {
        val returnedUri = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Active.jpg")
            }
        )!!
        val mediaToDelete = MediaDescriptor.Content.Image(returnedUri, null, false)
        repository.setCurrentMedia(mediaToDelete)

        // When
        repository.deleteMedia(mediaToDelete)

        // Then
        assertThat(repository.currentMedia.value).isEqualTo(MediaDescriptor.None)
    }

    @Test
    fun lastCapturedMedia_videoIsNewer_returnsVideo() = runTest {
        val olderImageTime = 1000L
        val newerVideoTime = 5000L
        fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DATE_ADDED, olderImageTime)
                put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Image.jpg")
            }
        )
        val videoUrl = fakeContentProvider.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DATE_ADDED, newerVideoTime)
                put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Video.mp4")
            }
        )!!

        val newRepo = LocalMediaRepository(context, testDispatcher, FakeFilePathGenerator()).apply {
            setThumbnailLoader(fakeThumbnailLoader)
        }
        val result = newRepo.lastCapturedMedia.value

        assertThat(result).isInstanceOf(MediaDescriptor.Content.Video::class.java)
        assertThat((result as MediaDescriptor.Content.Video).uri).isEqualTo(videoUrl)
    }

    @Test
    fun lastCapturedMedia_imageIsNewer_returnsImage() = runTest {
        val newerImageTime = 9000L
        val olderVideoTime = 2000L
        val imageUrl = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DATE_ADDED, newerImageTime)
                put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Image.jpg")
            }
        )!!
        fakeContentProvider.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DATE_ADDED, olderVideoTime)
                put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Video.mp4")
            }
        )

        val newRepo = LocalMediaRepository(context, testDispatcher, FakeFilePathGenerator()).apply {
            setThumbnailLoader(fakeThumbnailLoader)
        }
        val result = newRepo.lastCapturedMedia.value

        assertThat(result).isInstanceOf(MediaDescriptor.Content.Image::class.java)
        assertThat((result as MediaDescriptor.Content.Image).uri).isEqualTo(imageUrl)
    }

    @Test
    fun lastCapturedMedia_nothingFound_returnsNone() = runTest {
        val newRepo = LocalMediaRepository(context, testDispatcher, FakeFilePathGenerator()).apply {
            setThumbnailLoader(fakeThumbnailLoader)
        }
        assertThat(newRepo.lastCapturedMedia.value).isEqualTo(MediaDescriptor.None)
    }

    @Test
    fun lastCapturedMedia_equalTimestamps_returnsImage() = runTest {
        val sameTime = 9999L
        val imageUrl = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DATE_ADDED, sameTime)
                put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Image.jpg")
            }
        )!!
        fakeContentProvider.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DATE_ADDED, sameTime)
                put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Video.mp4")
            }
        )

        val newRepo = LocalMediaRepository(context, testDispatcher, FakeFilePathGenerator()).apply {
            setThumbnailLoader(fakeThumbnailLoader)
        }
        val result = newRepo.lastCapturedMedia.value

        assertThat(result).isInstanceOf(MediaDescriptor.Content.Image::class.java)
        assertThat((result as MediaDescriptor.Content.Image).uri).isEqualTo(imageUrl)
    }

    @Test
    fun deleteMedia_nonExistentUri_doesNotThrow() = runTest {
        val nonExistentUri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            999L
        )
        val mediaToDelete = MediaDescriptor.Content.Image(nonExistentUri, null, false)
        repository.deleteMedia(mediaToDelete)
    }

    @Test
    fun saveToMediaStore_video_success_returnsNewUri() = runTest {
        val sourceFile = File(context.cacheDir, "temp.mp4")
        sourceFile.writeText("fake video data")
        val mediaDescriptor = MediaDescriptor.Content.Video(Uri.fromFile(sourceFile), null, true)

        val result = repository.saveToMediaStore(mediaDescriptor, "my_video.mp4")

        assertThat(result).isNotNull()
        val values = fakeContentProvider.get(result!!)
        assertThat(values?.get(MediaStore.MediaColumns.DISPLAY_NAME)).isEqualTo("my_video.mp4")
    }

    @Test
    fun saveToMediaStore_success_returnsNewUri() = runTest {
        val sourceFile = File(context.cacheDir, "temp.jpg")
        sourceFile.writeText("fake image data")
        val mediaDescriptor = MediaDescriptor.Content.Image(Uri.fromFile(sourceFile), null, true)

        val result = repository.saveToMediaStore(mediaDescriptor, "my_photo.jpg")

        assertThat(result).isNotNull()
        val values = fakeContentProvider.get(result!!)
        assertThat(values?.get(MediaStore.MediaColumns.DISPLAY_NAME)).isEqualTo("my_photo.jpg")
    }

    @Test
    fun saveToMediaStore_insertFails_returnsNull() = runTest {
        val sourceFile = File(context.cacheDir, "temp.jpg")
        sourceFile.writeText("fake image data")
        val mediaDescriptor = MediaDescriptor.Content.Image(Uri.fromFile(sourceFile), null, true)
        fakeContentProvider.setFailNextInsert(true)

        val result = repository.saveToMediaStore(mediaDescriptor, "my_photo.jpg")

        assertThat(result).isNull()
    }

    @Test
    fun saveToMediaStore_copyFails_returnsNull() = runTest {
        val sourceUri = Uri.parse("file:///nonexistent/file.jpg")
        val mediaDescriptor = MediaDescriptor.Content.Image(sourceUri, null, true)

        val result = repository.saveToMediaStore(mediaDescriptor, "broken.jpg")

        assertThat(result).isNull()
    }

    @Test
    fun lastCapturedMedia_ignoresNonAppMediaStoreChange() = runTest {
        // Given a JCA file is current
        val jcaUrl = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DATE_ADDED, 1000L)
                put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Image.jpg")
                put(MediaStore.MediaColumns.OWNER_PACKAGE_NAME, context.packageName)
            }
        )!!
        contentResolver.notifyChange(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)

        val jcaDescriptor = repository.lastCapturedMedia.value
        assertThat(jcaDescriptor).isInstanceOf(MediaDescriptor.Content::class.java)

        // When a non-JCA file from a different app is inserted and notified
        val otherUrl = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DATE_ADDED, 5000L)
                put(MediaStore.MediaColumns.DISPLAY_NAME, "OTHER_Image.jpg")
                put(MediaStore.MediaColumns.OWNER_PACKAGE_NAME, "com.other.app")
            }
        )!!
        contentResolver.notifyChange(otherUrl, null)

        // Then the flow still points to the JCA file
        assertThat(repository.lastCapturedMedia.value).isEqualTo(jcaDescriptor)
    }

    @Test
    fun lastCapturedMedia_initialLoad_ignoresNonAppFiles() = runTest {
        val jcaUrl = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DATE_ADDED, 1000L)
                put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Image.jpg")
                put(MediaStore.MediaColumns.OWNER_PACKAGE_NAME, context.packageName)
            }
        )!!
        fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DATE_ADDED, 5000L)
                put(MediaStore.MediaColumns.DISPLAY_NAME, "OTHER_Image.jpg")
                put(MediaStore.MediaColumns.OWNER_PACKAGE_NAME, "com.other.app")
            }
        )!!

        val newRepo = LocalMediaRepository(context, testDispatcher, FakeFilePathGenerator()).apply {
            setThumbnailLoader(fakeThumbnailLoader)
        }
        val result = newRepo.lastCapturedMedia.value

        assertThat(result).isInstanceOf(MediaDescriptor.Content::class.java)
        assertThat((result as MediaDescriptor.Content).uri).isEqualTo(jcaUrl)
    }

    @Test
    fun lastCapturedMedia_multipleEventsForSameUri_emitsSameObjectReference() = runTest {
        val jcaUrl = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DATE_ADDED, 1000L)
                put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Image.jpg")
            }
        )!!
        contentResolver.notifyChange(jcaUrl, null)
        val firstEmission = repository.lastCapturedMedia.value

        contentResolver.notifyChange(jcaUrl, null)
        contentResolver.notifyChange(jcaUrl, null)

        assertThat(repository.lastCapturedMedia.value).isSameInstanceAs(firstEmission)
    }

    @Test
    fun lastCapturedMedia_onDeletion_fallsBackToNextLatest() = runTest {
        val urlA = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DATE_ADDED, 1000L)
                put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_A.jpg")
            }
        )!!
        val urlB = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DATE_ADDED, 2000L)
                put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_B.jpg")
            }
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
    fun lastCapturedMedia_onLastItemDeletion_emitsNone() = runTest {
        val jcaUrl = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DATE_ADDED, 1000L)
                put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Image.jpg")
            }
        )!!
        contentResolver.notifyChange(jcaUrl, null)
        assertThat(repository.lastCapturedMedia.value).isNotEqualTo(MediaDescriptor.None)

        fakeContentProvider.delete(jcaUrl, null, null)
        contentResolver.notifyChange(jcaUrl, null)

        assertThat(repository.lastCapturedMedia.value).isEqualTo(MediaDescriptor.None)
    }

    @Test
    fun lastCapturedMedia_newUriWithNullThumbnail_holdsPreviousMedia() = runTest {
        // We use a custom repo here so we can control the thumbnail loader specifically for this test
        var shouldFailThumbnail = false
        val customThumbnailLoader: suspend (Uri, Uri) -> Bitmap? = { _, _ ->
            if (shouldFailThumbnail) null else Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
        val customRepo = LocalMediaRepository(
            context,
            testDispatcher,
            FakeFilePathGenerator()
        ).apply {
            setThumbnailLoader(customThumbnailLoader)
        }

        val oldUrl = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DATE_ADDED, 1000L)
                put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_Old.jpg")
            }
        )!!
        contentResolver.notifyChange(oldUrl, null)
        val initialResult = customRepo.lastCapturedMedia.value
        assertThat((initialResult as MediaDescriptor.Content).uri).isEqualTo(oldUrl)

        val newUrl = fakeContentProvider.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DATE_ADDED, 2000L)
                put(MediaStore.MediaColumns.DISPLAY_NAME, "JCA_New.jpg")
            }
        )!!

        // Trigger thumbnail failure
        shouldFailThumbnail = true
        contentResolver.notifyChange(newUrl, null)

        // Then it holds the previous media
        assertThat(customRepo.lastCapturedMedia.value).isEqualTo(initialResult)

        // Thumbnail becomes ready
        shouldFailThumbnail = false
        contentResolver.notifyChange(newUrl, null)

        // Finally emits new media
        val finalResult = customRepo.lastCapturedMedia.value
        assertThat(finalResult).isInstanceOf(MediaDescriptor.Content::class.java)
        assertThat((finalResult as MediaDescriptor.Content).uri).isEqualTo(newUrl)
    }
}
