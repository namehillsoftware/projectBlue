package com.lasthopesoftware.bluewater.client.playback.service.receivers

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import com.lasthopesoftware.bluewater.client.browsing.ExternalBrowserService
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.IFileListParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.access.LaunchPlaybackFromResult
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class MediaSessionCallbackReceiver(
	private val context: Context,
	private val fileListParameterProvider: IFileListParameterProvider,
	private val fileStringListProvider: FileStringListProvider,
) : MediaSessionCompat.Callback() {
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
		val itemIdParts = mediaId?.split(':', limit = 2)
		if (itemIdParts == null || itemIdParts.size < 2) return

		val type = itemIdParts[0]
		if (type != ExternalBrowserService.itemFileMediaIdPrefix) return

		val id = itemIdParts[1].toIntOrNull() ?: return
		fileStringListProvider
			.promiseFileStringList(
				FileListParameters.Options.None,
				*fileListParameterProvider.getFileListParameters(Item(id)))
			.then(LaunchPlaybackFromResult(context))
	}
}
