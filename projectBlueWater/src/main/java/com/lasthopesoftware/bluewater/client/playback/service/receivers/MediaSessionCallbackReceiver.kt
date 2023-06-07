package com.lasthopesoftware.bluewater.client.playback.service.receivers

import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ProvideFileStringListForItem
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.remote.RemoteBrowserService
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService

class MediaSessionCallbackReceiver(
	private val controlPlaybackService: ControlPlaybackService,
	private val selectedLibraryId: ProvideSelectedLibraryId,
	private val fileStringListProvider: ProvideFileStringListForItem,
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
		val fileId = description?.mediaId?.toIntOrNull() ?: return
		withSelectedLibraryId { l -> controlPlaybackService.addToPlaylist(l, ServiceFile(fileId)) }
	}

	override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
		val itemIdParts = mediaId?.split(RemoteBrowserService.mediaIdDelimiter, limit = 3)
		if (itemIdParts == null || itemIdParts.size < 2) return

		if (itemIdParts[0] != RemoteBrowserService.itemFileMediaIdPrefix) return

		val ids = itemIdParts.drop(1).mapNotNull { id -> id.toIntOrNull() }
		val itemId = ids.firstOrNull() ?: return

		withSelectedLibraryId { libraryId ->
			val promisedFileStringList = fileStringListProvider
				.promiseFileStringList(libraryId, ItemId(itemId), FileListParameters.Options.None)

			if (ids.size < 2) {
				promisedFileStringList.then { sl -> controlPlaybackService.startPlaylist(libraryId, sl) }
			} else {
				promisedFileStringList.then { sl -> controlPlaybackService.startPlaylist(libraryId, sl, ids[1]) }
			}
		}
	}

	private fun withSelectedLibraryId(action: (LibraryId) -> Unit) =
		selectedLibraryId.promiseSelectedLibraryId().then {
			it?.also(action)
		}
}
