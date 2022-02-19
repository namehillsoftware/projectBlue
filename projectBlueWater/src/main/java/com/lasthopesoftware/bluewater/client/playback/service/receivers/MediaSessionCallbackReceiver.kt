package com.lasthopesoftware.bluewater.client.playback.service.receivers

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.IFileListParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.access.LaunchPlaybackFromResult
import com.lasthopesoftware.bluewater.client.browsing.remote.RemoteBrowserService
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class MediaSessionCallbackReceiver(
	private val context: Context,
	private val fileListParameterProvider: IFileListParameterProvider,
	private val fileStringListProvider: FileStringListProvider,
) : MediaSessionCompat.Callback() {
	override fun onPrepare() = PlaybackService.initialize(context)

    override fun onPlay() = PlaybackService.play(context)

    override fun onStop() = PlaybackService.pause(context)

    override fun onPause() = PlaybackService.pause(context)

    override fun onSkipToNext() = PlaybackService.next(context)

    override fun onSkipToPrevious() = PlaybackService.previous(context)

	override fun onSetRepeatMode(repeatMode: Int) =
		when (repeatMode) {
			PlaybackStateCompat.REPEAT_MODE_ALL -> PlaybackService.setRepeating(context)
			else -> PlaybackService.setCompleting(context)
		}

	override fun onAddQueueItem(description: MediaDescriptionCompat?) {
		val fileId = description?.mediaId?.toIntOrNull() ?: return
		PlaybackService.addFileToPlaylist(context, fileId)
	}

	override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
		val itemIdParts = mediaId?.split(RemoteBrowserService.mediaIdDelimiter, limit = 3)
		if (itemIdParts == null || itemIdParts.size < 2) return

		if (itemIdParts[0] != RemoteBrowserService.itemFileMediaIdPrefix) return

		val ids = itemIdParts.drop(1).mapNotNull { id -> id.toIntOrNull() }
		val id = ids.firstOrNull() ?: return

		val promisedFileStringList = fileStringListProvider
			.promiseFileStringList(
				FileListParameters.Options.None,
				*fileListParameterProvider.getFileListParameters(ItemId(id))
			)

		if (ids.size < 2) {
			promisedFileStringList.then(LaunchPlaybackFromResult(context))
		} else {
			promisedFileStringList.then { sl -> PlaybackService.launchMusicService(context, ids[1], sl) }
		}
	}
}
