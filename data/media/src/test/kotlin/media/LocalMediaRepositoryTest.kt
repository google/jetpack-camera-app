package com.google.jetpackcamera.data.media

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*


@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class LocalMediaRepositoryTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var repository: LocalMediaRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mock()
        contentResolver = mock()
        whenever(context.contentResolver).thenReturn(contentResolver)
        // Now using iODispatcher
        repository = LocalMediaRepository(context, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getLastCapturedMedia returns MediaDescriptor None when no media is found`() = runTest {
        // Arrange
        val imageCursor: Cursor = mock()
        val videoCursor: Cursor = mock()
        whenever(imageCursor.moveToFirst()).thenReturn(false)
        whenever(videoCursor.moveToFirst()).thenReturn(false)

        whenever(contentResolver.query(
            eq(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            any(), any(), any(), any()
        )).thenReturn(imageCursor)

        whenever(contentResolver.query(
            eq(MediaStore.Video.Media.EXTERNAL_CONTENT_URI),
            any(), any(), any(), any()
        )).thenReturn(videoCursor)

        // Act
        val result = repository.getLastCapturedMedia()

        // Assert
        assertEquals(MediaDescriptor.None, result)
    }

    @Test
    fun `getLastCapturedMedia returns MediaDescriptor Image when an image is found`() = runTest {
        // Arrange
        val imageCursor: Cursor = mock()
        val videoCursor: Cursor = mock()
        whenever(imageCursor.moveToFirst()).thenReturn(true)
        whenever(videoCursor.moveToFirst()).thenReturn(false)

        whenever(imageCursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)).thenReturn(0)
        whenever(imageCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)).thenReturn(1)

        val imageId = 123L
        val dateAdded = 1678886400000L
        val imageUri = Uri.parse("content://media/external/images/media/$imageId")

        whenever(imageCursor.getLong(0)).thenReturn(imageId)
        whenever(imageCursor.getLong(1)).thenReturn(dateAdded)

        whenever(contentResolver.query(
            eq(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            any(), any(), any(), any()
        )).thenReturn(imageCursor)

        whenever(contentResolver.query(
            eq(MediaStore.Video.Media.EXTERNAL_CONTENT_URI),
            any(), any(), any(), any()
        )).thenReturn(videoCursor)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val thumbnail: Bitmap = mock()
            whenever(contentResolver.loadThumbnail(
                any(),
                any(),
                any()
            )).thenReturn(thumbnail)
        } else {
            val thumbnail: Bitmap = mock()
            whenever(MediaStore.Images.Thumbnails.getThumbnail(
                any(),
                any(),
                any(),
                any()
            )).thenReturn(thumbnail)
        }

        // Act
        val result = repository.getLastCapturedMedia()

        // Assert
        assertTrue(result is MediaDescriptor.Image)
        result as MediaDescriptor.Image
        assertEquals(imageUri, result.uri)
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
        val thumbnail: Bitmap = mock()
        val descriptor = MediaDescriptor.Image(imageUri, thumbnail)

        val bitmap: Bitmap = mock()
        whenever(contentResolver.openInputStream(imageUri)).thenReturn(mock())

        // Act
        val result = repository.load(descriptor)

        // Assert
        assertTrue(result is Media.Image)
        result as Media.Image
        // You might want to add more assertions here to check the bitmap
    }
}