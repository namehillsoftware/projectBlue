package com.lasthopesoftware.bluewater.client.playback.service.receivers

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.RequiresApi
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class MediaSessionCallbackReceiver(private val context: Context) : MediaSessionCompat.Callback() {
    override fun onPlay() {
		PlaybackService.play(context)
    }

    override fun onStop() {
		PlaybackService.pause(context)
    }

    override fun onPause() {
		PlaybackService.pause(context)
    }

    override fun onSkipToNext() {
		PlaybackService.next(context)
    }

    override fun onSkipToPrevious() {
		PlaybackService.previous(context)
    }

	override fun onAddQueueItem(description: MediaDescriptionCompat?) {
		val fileId = description?.mediaId?.toIntOrNull() ?: return
		PlaybackService.addFileToPlaylist(context, fileId)
	}

	override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
		super.onPlayFromMediaId(mediaId, extras)
	}
}
