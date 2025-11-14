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
package com.google.jetpackcamera.feature.postcapture

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_CHANGE_MEDIA_ITEMS
import androidx.media3.common.Player.COMMAND_PLAY_PAUSE
import androidx.media3.common.Player.COMMAND_PREPARE
import androidx.media3.common.Player.COMMAND_SET_MEDIA_ITEM
import androidx.media3.exoplayer.ExoPlayer
import com.google.jetpackcamera.core.common.ApplicationScope
import com.google.jetpackcamera.data.media.Media
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.data.media.MediaRepository
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_IMAGE_DELETE_FAILURE
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_IMAGE_SAVE_FAILURE
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_IMAGE_SAVE_SUCCESS
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_VIDEO_DELETE_FAILURE
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_VIDEO_SAVE_FAILURE
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_VIDEO_SAVE_SUCCESS
import com.google.jetpackcamera.ui.uistate.capture.SnackBarUiState
import com.google.jetpackcamera.ui.uistate.capture.SnackbarData
import com.google.jetpackcamera.ui.uistateadapter.capture.from
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList
import java.util.Locale
import javax.inject.Inject
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "PostCaptureViewModel"

@HiltViewModel
class PostCaptureViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {

    private val playerState = MutableStateFlow(
        PlayerState()
    )
    private val playerListener = object : Player.Listener {
        /**
         * This callback is the single source of truth for
         * what the UI is allowed to do.
         */
        override fun onAvailableCommandsChanged(commands: Player.Commands) {
            super.onAvailableCommandsChanged(commands)
            updatePlayerState(commands)
        }
    }

    private val _uiState = MutableStateFlow(
        PostCaptureUiState(
            mediaDescriptor = MediaDescriptor.None,
            media = Media.None,
            snackBarUiState = SnackBarUiState()
        )
    )

    private val snackBarCount = atomic(0)

    var player: ExoPlayer? = null
        private set
    val uiState: StateFlow<PostCaptureUiState> = _uiState

    init {
        viewModelScope.launch {
            mediaRepository.currentMedia.collectLatest { mediaDescriptor ->
                val media = mediaRepository.load(mediaDescriptor)

                // init with player if current media is a video
                if (media is Media.Video) {
                    if (player == null) {
                        initPlayer()
                    }
                }
                _uiState.update {
                    it.copy(mediaDescriptor = mediaDescriptor, media = media)
                }
            }
        }

        // release and remove player when not needed
        viewModelScope.launch {
            uiState.collectLatest {
                when (it.media) {
                    Media.Error,
                    is Media.Image,
                    Media.None -> if (player != null) {
                        releasePlayer()
                        player = null
                    }

                    is Media.Video -> {
                        if (player == null) initPlayer()
                        loadCurrentVideo()
                    }
                }
            }
        }
    }

    // todo(kc): improve cache cleanup strategy
    override fun onCleared() {
        releasePlayer()

        val mediaDescriptor = uiState.value.mediaDescriptor

        // todo(kc): improve cache cleanup strategy
        if ((mediaDescriptor as? MediaDescriptor.Content)?.isCached == true) {
            // use application scope for cleanup
            // coroutine will not be cancelled when ViewModel dies
            applicationScope.launch {
                mediaRepository.deleteMedia(context.contentResolver, mediaDescriptor)
                mediaRepository.setCurrentMedia(MediaDescriptor.None)
            }
        }
        super.onCleared()
    }

    fun updatePlayerState(commands: Player.Commands?) {
        viewModelScope.launch {
            playerState.update {
                if (commands == null) {
                    PlayerState()
                } else {
                    it.copy(
                        canPrepare = commands.contains(COMMAND_PREPARE),
                        canPlayPause = commands.contains(COMMAND_PLAY_PAUSE),
                        canSetMediaItem = commands.contains(COMMAND_SET_MEDIA_ITEM),
                        canChangeMediaItem = commands.contains(COMMAND_CHANGE_MEDIA_ITEMS)
                    )
                }
            }
        }
    }

    // exoplayer controls

    /**
     * Initializes a new [ExoPlayer] instance with a [Player.Listener] attached.
     * Releases any pre-existing player.
     */
    fun initPlayer() {
        releasePlayer()
        val exoplayer = ExoPlayer.Builder(context).build()
        updatePlayerState(exoplayer.availableCommands)
        exoplayer.addListener(playerListener)
        player = exoplayer
    }

    /**
     * Releases the current [ExoPlayer] instance. Resets [player] to null.
     */
    fun releasePlayer() {
        player?.let {
            it.release()
            player = null
            updatePlayerState(null)
            Log.d(TAG, "successfully released ExoPlayer")
        }
    }

    /**
     * Loads and prepares the uistate's current video media for playback.
     */
    fun loadVideo() {
        val media = uiState.value.media
        if (media is Media.Video) {
            if (player?.isPlaying == true) {
                player?.stop()
            }
            val mediaItem = MediaItem.fromUri(media.uri)
            if (playerState.value.canChangeMediaItem) {
                player?.clearMediaItems()
            }
            if (playerState.value.canSetMediaItem) {
                player?.setMediaItem(mediaItem)
            }
            player?.prepare()
        }
    }

    /**
     * Loads and plays the uiState's current video media.
     * playback of the video is an infinite loop.
     */
    fun loadCurrentVideo() {
        viewModelScope.launch {
            player?.let {
                loadVideo()
                it.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                if (playerState.value.canPlayPause) {
                    it.playWhenReady = true
                }
            }
        }
    }

    /**
     * Saves the current media to the MediaStore.
     */
    fun saveCurrentMedia() {
        (uiState.value.mediaDescriptor as? MediaDescriptor.Content)?.let {
            applicationScope.launch {
                val cookieInt = snackBarCount.incrementAndGet()
                val cookie = "MediaSave-$cookieInt"
                val result: Uri?

                try {
                    result = saveMedia(it)
                    if (result != null) {
                        val (stringResource, testTag) = when (it) {
                            is MediaDescriptor.Content.Image ->
                                R.string.snackbar_save_image_success to
                                    SNACKBAR_POST_CAPTURE_IMAGE_SAVE_SUCCESS

                            is MediaDescriptor.Content.Video ->
                                R.string.snackbar_save_video_success to
                                    SNACKBAR_POST_CAPTURE_VIDEO_SAVE_SUCCESS
                        }

                        addSnackBarData(
                            SnackbarData(
                                cookie = cookie,
                                stringResource = stringResource,
                                withDismissAction = true,
                                testTag = testTag
                            )
                        )
                    } else {
                        // potential custom behavior for null
                        Log.e(TAG, "null saved media Uri without exception")

                        val (stringResource, testTag) = when (it) {
                            is MediaDescriptor.Content.Image ->
                                R.string.snackbar_save_image_failure to
                                    SNACKBAR_POST_CAPTURE_IMAGE_SAVE_FAILURE

                            is MediaDescriptor.Content.Video ->
                                R.string.snackbar_save_video_failure to
                                    SNACKBAR_POST_CAPTURE_VIDEO_SAVE_FAILURE
                        }

                        addSnackBarData(
                            SnackbarData(
                                cookie = cookie,
                                stringResource = stringResource,
                                withDismissAction = true,
                                testTag = testTag
                            )
                        )
                    }
                } catch (e: Exception) {
                    // todo: custom message depending on failure reason
                    val (stringResource, testTag) = when (it) {
                        is MediaDescriptor.Content.Image ->
                            R.string.snackbar_save_image_failure to
                                SNACKBAR_POST_CAPTURE_IMAGE_SAVE_FAILURE

                        is MediaDescriptor.Content.Video ->
                            R.string.snackbar_save_video_failure to
                                SNACKBAR_POST_CAPTURE_VIDEO_SAVE_FAILURE
                    }

                    addSnackBarData(
                        SnackbarData(
                            cookie = cookie,
                            stringResource = stringResource,
                            withDismissAction = true,
                            testTag = testTag
                        )
                    )
                }
            }
        }
    }

    /**
     * returns the Uri of the new saved media location
     */
    suspend fun saveMedia(mediaDescriptor: MediaDescriptor.Content): Uri? =
        mediaRepository.saveToMediaStore(
            context.contentResolver,
            mediaDescriptor,
            createFilename(mediaDescriptor)
        )

    /**
     * Deletes the given media.
     *
     * @param mediaDescriptor the [MediaDescriptor] of the media to be deleted.
     */
    fun deleteMedia(mediaDescriptor: MediaDescriptor.Content) {
        applicationScope.launch {
            try {
                mediaRepository.deleteMedia(context.contentResolver, mediaDescriptor)
                _uiState.update {
                    it.copy(
                        mediaDescriptor = MediaDescriptor.None,
                        media = Media.None
                    )
                }
            } catch (e: Exception) {
                val cookieInt = snackBarCount.incrementAndGet()
                val cookie = "MediaDelete-$cookieInt"
                val (stringResource, testTag) = when (mediaDescriptor) {
                    is MediaDescriptor.Content.Image ->
                        R.string.snackbar_delete_image_failure to
                            SNACKBAR_POST_CAPTURE_IMAGE_DELETE_FAILURE

                    is MediaDescriptor.Content.Video ->
                        R.string.snackbar_delete_video_failure to
                            SNACKBAR_POST_CAPTURE_VIDEO_DELETE_FAILURE
                }
                addSnackBarData(
                    SnackbarData(
                        cookie = cookie,
                        stringResource = stringResource,
                        withDismissAction = true,
                        testTag = testTag
                    )
                )
            }
        }
    }

    private fun addSnackBarData(snackBarData: SnackbarData) {
        viewModelScope.launch {
            _uiState.update { old ->
                val newQueue = LinkedList(old.snackBarUiState.snackBarQueue)

                newQueue.add(snackBarData)
                Log.d(TAG, "SnackBar added. Queue size: ${newQueue.size}")
                old.copy(
                    snackBarUiState = SnackBarUiState.from(newQueue)
                )
            }
        }
    }

    fun onSnackBarResult(cookie: String) {
        viewModelScope.launch {
            _uiState.update { old ->
                val newQueue = LinkedList(old.snackBarUiState.snackBarQueue)
                val snackBarData = newQueue.poll()
                if (snackBarData != null && snackBarData.cookie == cookie) {
                    // If the latest snackBar had a result, then clear snackBarToShow
                    Log.d(TAG, "SnackBar removed. Queue size: ${newQueue.size}")
                    old.copy(
                        snackBarUiState = SnackBarUiState.from(newQueue)
                    )
                } else {
                    old
                }
            }
        }
    }
}

/**
 * Creates a filename for the media descriptor.
 *
 * @param mediaDescriptor the [MediaDescriptor] to create a filename for.
 *
 * @return a filename for the media descriptor.
 */
private fun createFilename(mediaDescriptor: MediaDescriptor.Content): String {
    val timeStamp =
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(Date())

    return when (mediaDescriptor) {
        is MediaDescriptor.Content.Image -> {
            "JCA-photo-$timeStamp.jpg"
        }

        is MediaDescriptor.Content.Video -> {
            "JCA-recording-$timeStamp.mp4"
        }
    }
}

data class PostCaptureUiState(
    val mediaDescriptor: MediaDescriptor,
    val media: Media,
    val snackBarUiState: SnackBarUiState
)

data class PlayerState(
    val canPrepare: Boolean = false,
    val canPlayPause: Boolean = false,
    val canSetMediaItem: Boolean = false,
    val canChangeMediaItem: Boolean = false
)
