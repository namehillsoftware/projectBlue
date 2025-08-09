package com.lasthopesoftware.bluewater.client.playback.service.receivers

import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.remote.RemoteBrowserService
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService

class MediaSessionCallbackReceiver(
	private val controlPlaybackService: ControlPlaybackService,
	private val selectedLibraryId: ProvideSelectedLibraryId,
	private val libraryFileProvider: ProvideLibraryFiles,
) : MediaSessionCompat.Callback() {
	override fun onPrepare() {
		withSelectedLibraryId(controlPlaybackService::initialize)
	}

    override fun onPlay() {
		withSelectedLibraryId(controlPlaybackService::play)
	}

	override fun onStop() = controlPlaybackService.pause()

    override fun onPause() = controlPlaybackService.pause()

    override fun onSkipToNext() {
		withSelectedLibraryId(controlPlaybackService::next)
	}

	override fun onSkipToPrevious() {
		withSelectedLibraryId(controlPlaybackService::previous)
	}

	override fun onSetRepeatMode(repeatMode: Int) {
		withSelectedLibraryId { l ->
			when (repeatMode) {
				PlaybackStateCompat.REPEAT_MODE_ALL -> controlPlaybackService.setRepeating(l)
				else -> controlPlaybackService.setCompleting(l)
			}
		}
	}

	override fun onAddQueueItem(description: MediaDescriptionCompat?) {
		val fileId = description?.mediaId ?: return
		withSelectedLibraryId { l -> controlPlaybackService.addToPlaylist(l, ServiceFile(fileId)) }
	}

	override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
		val itemIdParts = mediaId?.split(RemoteBrowserService.mediaIdDelimiter, limit = 3)
		if (itemIdParts == null || itemIdParts.size < 2) return

		if (itemIdParts[0] != RemoteBrowserService.itemFileMediaIdPrefix) return

		val ids = itemIdParts.drop(1)
		val itemId = ids.firstOrNull() ?: return

		withSelectedLibraryId { libraryId ->
			val promisedFiles = libraryFileProvider.promiseFiles(libraryId, ItemId(itemId))

			if (ids.size < 2) {
				promisedFiles.then { sl -> controlPlaybackService.startPlaylist(libraryId, sl) }
			} else {
				promisedFiles.then { sl -> controlPlaybackService.startPlaylist(libraryId, sl, ids[1].toInt()) }
			}
		}
	}

	private fun withSelectedLibraryId(action: (LibraryId) -> Unit) =
		selectedLibraryId.promiseSelectedLibraryId().then { it ->
			it?.also(action)
		}
}
