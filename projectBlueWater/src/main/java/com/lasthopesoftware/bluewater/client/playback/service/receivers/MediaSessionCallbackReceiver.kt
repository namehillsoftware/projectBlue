package com.lasthopesoftware.bluewater.client.playback.service.receivers

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ProvideFileStringListForItem
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.access.LaunchPlaybackFromResult
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.remote.RemoteBrowserService
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService

class MediaSessionCallbackReceiver(
	private val context: Context,
	private val selectedLibraryId: ProvideSelectedLibraryId,
	private val fileStringListProvider: ProvideFileStringListForItem,
) : MediaSessionCompat.Callback() {
	override fun onPrepare() {
		withSelectedLibraryId { PlaybackService.initialize(context, it) }
	}

    override fun onPlay() {
		withSelectedLibraryId { l -> PlaybackService.play(context, l) }
	}

	override fun onStop() = PlaybackService.pause(context)

    override fun onPause() = PlaybackService.pause(context)

    override fun onSkipToNext() {
		withSelectedLibraryId { l -> PlaybackService.next(context, l) }
	}

	override fun onSkipToPrevious() {
		withSelectedLibraryId { l -> PlaybackService.previous(context, l) }
	}

	override fun onSetRepeatMode(repeatMode: Int) {
		withSelectedLibraryId { l ->
			when (repeatMode) {
				PlaybackStateCompat.REPEAT_MODE_ALL -> PlaybackService.setRepeating(context, l)
				else -> PlaybackService.setCompleting(context, l)
			}
		}
	}

	override fun onAddQueueItem(description: MediaDescriptionCompat?) {
		val fileId = description?.mediaId?.toIntOrNull() ?: return
		withSelectedLibraryId { l -> PlaybackService.addFileToPlaylist(context, l, ServiceFile(fileId)) }
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
				promisedFileStringList.then(LaunchPlaybackFromResult(context, libraryId))
			} else {
				promisedFileStringList.then { sl -> PlaybackService.launchMusicService(context, libraryId, ids[1], sl) }
			}
		}
	}

	private fun withSelectedLibraryId(action: (LibraryId) -> Unit) =
		selectedLibraryId.promiseSelectedLibraryId().then {
			it?.also(action)
		}
}
