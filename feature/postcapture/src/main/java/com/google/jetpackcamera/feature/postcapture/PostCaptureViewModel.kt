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

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
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
import com.google.jetpackcamera.ui.uistate.postcapture.DeleteButtonUiState
import com.google.jetpackcamera.ui.uistate.postcapture.MediaViewerUiState
import com.google.jetpackcamera.ui.uistate.postcapture.PostCaptureUiState
import com.google.jetpackcamera.ui.uistate.postcapture.ShareButtonUiState
import com.google.jetpackcamera.ui.uistateadapter.capture.from
import com.google.jetpackcamera.ui.uistateadapter.postcapture.from
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList
import java.util.Locale
import javax.inject.Inject

private const val TAG = "PostCaptureViewModel"

@HiltViewModel
class PostCaptureViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {

    private val loadedMediaFlow: StateFlow<Pair<MediaDescriptor, Media>> =
        mediaRepository.currentMedia
            .map { mediaDescriptor -> mediaDescriptor to mediaRepository.load(mediaDescriptor) }
            .distinctUntilChanged().stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = (MediaDescriptor.None to Media.None)
            )
    private val _postCaptureUiState =
        MutableStateFlow<PostCaptureUiState>(PostCaptureUiState.Loading)

    val postCaptureUiState: StateFlow<PostCaptureUiState> = _postCaptureUiState

    private var player: ExoPlayer? = null

    private val playerState = MutableStateFlow<PlayerState>(PlayerState.Unavailable)
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

    private val snackBarCount = atomic(0)

    init {
        // coroutine to update viewmodel
        viewModelScope.launch {
            combine(
                loadedMediaFlow,
                playerState.map { it is PlayerState.Available }.distinctUntilChanged()
            ) { mediaPair, playerstate ->
                _postCaptureUiState.update { old ->
                    when (old) {
                        PostCaptureUiState.Loading -> PostCaptureUiState.Ready()
                        is PostCaptureUiState.Ready -> {
                            old
                        }
                    }.copy(
                        viewerUiState = MediaViewerUiState.from(
                            mediaPair.first,
                            mediaPair.second,
                            player,
                            playerstate
                        ),
                        shareButtonUiState = ShareButtonUiState.from(mediaPair.first),
                        deleteButtonUiState = DeleteButtonUiState.from(mediaPair.first)

                    )
                }
            }.collect { }
        }

        // release and remove player when no videos are loaded
        viewModelScope.launch {
            loadedMediaFlow.map { it.second is Media.Video }
                .distinctUntilChanged()
                .collectLatest { isVideoMedia ->
                    if (isVideoMedia) {
                        if (player == null) {
                            initPlayer()
                        }
                    } else if (player != null) {
                        releasePlayer()
                        player = null
                    }
                }
        }
    }

    // todo(kc): improve cache cleanup strategy
    override fun onCleared() {
        releasePlayer()
        val mediaDescriptor = loadedMediaFlow.value.first

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

    private fun updatePlayerState(commands: Player.Commands?) {
        viewModelScope.launch {
            playerState.update {
                if (commands == null) {
                    PlayerState.Unavailable
                } else {
                    PlayerState.Available(
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
    private fun initPlayer() {
        releasePlayer()
        val exoplayer = ExoPlayer.Builder(context).build()
        updatePlayerState(exoplayer.availableCommands)
        exoplayer.addListener(playerListener)
        player = exoplayer
    }

    /**
     * Releases the current [ExoPlayer] instance. Resets [player] to null.
     */
    private fun releasePlayer() {
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
    private fun loadVideo(media: Media) {
        if (media is Media.Video) {
            if (player?.isPlaying == true) {
                player?.stop()
            }
            val mediaItem = MediaItem.fromUri(media.uri)
            val availablePlayerState = playerState.value as? PlayerState.Available

            if (availablePlayerState?.canChangeMediaItem == true) {
                player?.clearMediaItems()
            }
            if (availablePlayerState?.canSetMediaItem == true) {
                player?.setMediaItem(mediaItem)
            }
            player?.prepare()
        }
    }

    // controls exposed to UI
    /**
     * Loads and plays the uiState's current video media.
     * playback of the video is an infinite loop.
     *
     * no-op if the current media is not a video
     */
    fun loadCurrentVideo() {
        viewModelScope.launch {
            val currentMedia = loadedMediaFlow.value.second
            if (currentMedia is Media.Video) {
                player?.let {
                    loadVideo(currentMedia)
                    it.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                    if ((playerState.value as? PlayerState.Available)?.canPlayPause == true) {
                        it.playWhenReady = true
                    }
                }
            }
        }
    }

    /**
     * Saves the current media to the MediaStore.
     */
    fun saveCurrentMedia() {
        (loadedMediaFlow.value.first as? MediaDescriptor.Content)?.let {
            applicationScope.launch {
                val cookieInt = snackBarCount.incrementAndGet()
                val cookie = "MediaSave-$cookieInt"
                val result: Uri?
                try {
                    result = saveMedia(it)
                    if (result != null) {
                        addSnackBarData(
                            SnackbarData(
                                cookie = cookie,
                                stringResource = when (it) {
                                    is MediaDescriptor.Content.Image ->
                                        R.string.snackbar_save_image_success

                                    is MediaDescriptor.Content.Video ->
                                        R.string.snackbar_save_video_success
                                },
                                withDismissAction = true,
                                testTag = when (it) {
                                    is MediaDescriptor.Content.Image ->
                                        SNACKBAR_POST_CAPTURE_IMAGE_SAVE_SUCCESS

                                    is MediaDescriptor.Content.Video ->
                                        SNACKBAR_POST_CAPTURE_VIDEO_SAVE_SUCCESS
                                }
                            )
                        )
                    }
                } catch (e: Exception) {
                    addSnackBarData(
                        SnackbarData(
                            cookie = cookie,
                            stringResource = when (it) {
                                is MediaDescriptor.Content.Image ->
                                    R.string.snackbar_save_image_failure

                                is MediaDescriptor.Content.Video ->
                                    R.string.snackbar_save_video_failure
                            },
                            withDismissAction = true,
                            testTag = when (it) {
                                is MediaDescriptor.Content.Image ->
                                    SNACKBAR_POST_CAPTURE_IMAGE_SAVE_FAILURE

                                is MediaDescriptor.Content.Video ->
                                    SNACKBAR_POST_CAPTURE_VIDEO_SAVE_FAILURE
                            }
                        )
                    )
                }
            }
        }
    }

    fun deleteCurrentMedia() {
        val currentMediaDescriptor = loadedMediaFlow.value.first
        (currentMediaDescriptor as? MediaDescriptor.Content)?.let {
            deleteMedia(mediaDescriptor = it)
        }
    }

    fun shareCurrentMedia() {
        val currentMediaDescriptor = loadedMediaFlow.value.first
        (currentMediaDescriptor as? MediaDescriptor.Content)?.let {
            shareMedia(
                context,
                mediaDescriptor = it
            )
        }
    }

    /**
     * returns the Uri of the new saved media location
     */
    private suspend fun saveMedia(mediaDescriptor: MediaDescriptor.Content): Uri? =
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
    private fun deleteMedia(mediaDescriptor: MediaDescriptor.Content) {
        applicationScope.launch {
            try {
                mediaRepository.deleteMedia(context.contentResolver, mediaDescriptor)
            } catch (e: Exception) {
                val cookieInt = snackBarCount.incrementAndGet()
                val cookie = "MediaDelete-$cookieInt"
                addSnackBarData(
                    SnackbarData(
                        cookie = cookie,
                        stringResource = when (mediaDescriptor) {
                            is MediaDescriptor.Content.Image ->
                                R.string.snackbar_delete_image_failure

                            is MediaDescriptor.Content.Video ->
                                R.string.snackbar_delete_video_failure
                        },
                        withDismissAction = true,
                        testTag = when (mediaDescriptor) {
                            is MediaDescriptor.Content.Image ->
                                SNACKBAR_POST_CAPTURE_IMAGE_DELETE_FAILURE

                            is MediaDescriptor.Content.Video ->
                                SNACKBAR_POST_CAPTURE_VIDEO_DELETE_FAILURE
                        }
                    )
                )
            }
        }
    }

    private fun addSnackBarData(snackBarData: SnackbarData) {
        viewModelScope.launch {
            _postCaptureUiState.update { old ->
                if (old is PostCaptureUiState.Ready) {
                    val newQueue = LinkedList(old.snackBarUiState.snackBarQueue)

                    newQueue.add(snackBarData)
                    Log.d(TAG, "SnackBar added. Queue size: ${newQueue.size}")
                    old.copy(
                        snackBarUiState = SnackBarUiState.from(newQueue)
                    )
                } else {
                    old
                }
            }
        }
    }

    fun onSnackBarResult(cookie: String) {
        _postCaptureUiState.update { state ->
            if (state is PostCaptureUiState.Ready) {
                val newQueue = LinkedList(state.snackBarUiState.snackBarQueue)
                val snackBarData = newQueue.remove()
                if (snackBarData != null && snackBarData.cookie == cookie) {
                    // If the latest snackBar had a result, then clear snackBarToShow
                    Log.d(TAG, "SnackBar removed. Queue size: ${newQueue.size}")
                    state.copy(
                        snackBarUiState = SnackBarUiState.from(newQueue)
                    )
                } else {
                    state
                }
            } else {
                state
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

sealed interface PlayerState {
    data object Unavailable : PlayerState
    data class Available(
        val canPrepare: Boolean = false,
        val canPlayPause: Boolean = false,
        val canSetMediaItem: Boolean = false,
        val canChangeMediaItem: Boolean = false
    ) : PlayerState
}

/**
 * Starts an intent to share media.
 *
 * @param context the context of the calling component.
 * @param mediaDescriptor the [MediaDescriptor] of the media to be shared.
 */
private fun shareMedia(context: Context, mediaDescriptor: MediaDescriptor.Content) {
    // todo(kc): support sharing multiple media
    val uri = mediaDescriptor.uri
    val mimeType: String = when (mediaDescriptor) {
        is MediaDescriptor.Content.Image -> "image/jpeg"
        is MediaDescriptor.Content.Video -> "video/mp4"
    }

    // if the uri isn't already managed by a content provider, we will need
    val contentUri: Uri =
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) uri else getShareableUri(context, uri)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, contentUri)
    }
    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    // todo(kc): prevent "edit image" from appearing in the ShareSheet.
    context.startActivity(Intent.createChooser(intent, "Share Media"))
}

/**
 * Creates a content Uri for a given file Uri.
 *
 * @param context the context of the calling component.
 * @param uri the Uri of the file.
 *
 * @return a content Uri to be used for sharing.
 */
private fun getShareableUri(context: Context, uri: Uri): Uri {
    val authority = "${context.packageName}.fileprovider"
    val file =
        uri.path
            ?.let { File(it) }
            ?: throw FileNotFoundException("path does not exist")

    return FileProvider.getUriForFile(context, authority, file)
}
