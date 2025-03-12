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

package media

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import com.google.jetpackcamera.data.media.LocalMediaRepository
import com.google.jetpackcamera.data.media.Media
import com.google.jetpackcamera.data.media.MediaDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.provider.MediaStore.Images
import android.provider.MediaStore.Video
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.shadows.ShadowMediaStore


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class LocalMediaRepositoryRobolectricTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var shadowContentResolver: ShadowContentResolver
    private lateinit var repository: LocalMediaRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Get a real context from Robolectric
        context = RuntimeEnvironment.getApplication()
        contentResolver = RuntimeEnvironment.getApplication().contentResolver
        shadowContentResolver = shadowOf(contentResolver)
        repository = LocalMediaRepository(context, testDispatcher)

        ShadowMediaStore.setStubBitmapForThumbnails(createMockBitmap())
//        Robolectric.setupContentResolver(FakeContentProvider.class, )
        Robolectric.setupContentProvider(FakeContentProvider::class.java, "com.example.provider")
    }

    class FakeContentProvider : ContentProvider() {
        override fun onCreate(): Boolean {
            TODO("Not yet implemented")
        }

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor? {
            TODO("Not yet implemented")
        }

        override fun getType(uri: Uri): String? {
            TODO("Not yet implemented")
        }

        override fun insert(uri: Uri, values: ContentValues?): Uri? {
            TODO("Not yet implemented")
        }

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
            TODO("Not yet implemented")
        }

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?
        ): Int {
            TODO("Not yet implemented")
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load returns Media None when mediaDescriptor is None`() = runTest {
        // Arrange
        val descriptor = MediaDescriptor.None

        // Act
        val result = repository.load(descriptor)

        // Assert
        assertEquals(Media.None, result)
    }

    @Test
    fun `load returns Media Image when mediaDescriptor is Image`() = runTest {
        // Arrange
        val imageUri = Uri.parse("content://media/external/images/media/123")
        val thumbnail: Bitmap = mock(Bitmap::class.java)
        val descriptor = MediaDescriptor.Image(imageUri, thumbnail)

        // Stub openInputStream to return a dummy stream (we don't actually use it here)
        `when`(contentResolver.openInputStream(imageUri)).thenReturn(mock())

        // Act
        val result = repository.load(descriptor)

        // Assert
        assertTrue(result is Media.Image)
        result as Media.Image
        // Optionally, add assertions to verify bitmap contents
    }

    @Test
    fun `getLastCapturedMedia returns MediaDescriptor None when no video or image is found`() = runTest {
        // Arrange

        // Act
        val result = repository.getLastCapturedMedia()

        // Assert
        assertEquals(MediaDescriptor.None, result)
    }

    @Test
    fun `getLastCapturedMedia returns MediaDescriptor Video when only video is found`() = runTest {
        // Arrange

        // Act
        val result = repository.getLastCapturedMedia()

        // Assert
        val expectedUri = Uri.parse("content://media/external/video/media/123")
        assertTrue(result is MediaDescriptor.Video)
        result as MediaDescriptor.Video
        assertEquals(expectedUri, result.uri)
    }

    @Test
    fun `getLastCapturedMedia returns MediaDescriptor Image when only image is found`() = runTest {
        // Arrange

        // Act
        val result = repository.getLastCapturedMedia()

        // Assert
        val expectedUri = Uri.parse("content://media/external/images/media/456")
        assertTrue(result is MediaDescriptor.Image)
        result as MediaDescriptor.Image
        assertEquals(expectedUri, result.uri)
    }

    @Test
    fun `getLastCapturedMedia returns MediaDescriptor Image when video is older than image`() = runTest {
        // Arrange

        // Act
        val result = repository.getLastCapturedMedia()

        // Assert
        val expectedUri = Uri.parse("content://media/external/images/media/456")
        assertTrue(result is MediaDescriptor.Image)
        result as MediaDescriptor.Image
        assertEquals(expectedUri, result.uri)
    }

    @Test
    fun `getLastCapturedMedia returns MediaDescriptor Video when image is older than video`() = runTest {
        // Arrange
        val videoId = 123L
        val videoDateAdded = 1678886400000L // Newer
        val imageId = 456L
        val imageDateAdded = 1678880000000L // Older

        // Add mock image data.
        val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(imageId.toString()).build()
        ShadowMediaStore.Images.Media.insertImage(contentResolver, "/mock/path/image.jpg", "Mock Image", "Mock Image Description", imageDateAdded)
        shadowContentResolver.registerFileDescriptor(imageUri, makePipeFromBitmap(createMockBitmap()))

        // Add mock video data.
        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(videoId.toString()).build()
        ShadowMediaStore.Video.Media.insertVideo(contentResolver, "/mock/path/video.mp4", "Mock Video", "Mock Video Description", videoDateAdded)
        shadowContentResolver.registerFileDescriptor(videoUri, makePipeFromBitmap(createMockBitmap()))

        // Act
        val result = repository.getLastCapturedMedia()

        // Assert
        val expectedUri = Uri.parse("content://media/external/video/media/$videoId")
        assertTrue(result is MediaDescriptor.Video)
        val videoResult = result as MediaDescriptor.Video
        assertEquals(expectedUri, videoResult.uri)
    }

    private fun createMockBitmap(): Bitmap {
        return Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888) // Create a simple bitmap
    }

    sealed class CursorBehavior {
        object Empty : CursorBehavior()
        data class NotEmpty(val id: Long, val dateAdded: Long) : CursorBehavior()
    }
}