package com.lasthopesoftware.bluewater.client.playback.service.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.next
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.pause
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.play
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.previous
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.togglePlayPause
import com.lasthopesoftware.bluewater.shared.android.intents.safelyGetParcelableExtra
import com.lasthopesoftware.bluewater.shared.lazyLogger

private val logger by lazyLogger<RemoteControlReceiver>()

@UnstableApi class RemoteControlReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		fun withSelectedLibraryId(action: (LibraryId) -> Unit) =
			context.getCachedSelectedLibraryIdProvider()
				.promiseSelectedLibraryId()
				.then { it?.also(action) }

		logger.debug("Received intent: {}.", intent)

		val event = intent.safelyGetParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)

		if (event?.action != KeyEvent.ACTION_DOWN) return

		when (event.keyCode) {
			KeyEvent.KEYCODE_MEDIA_PLAY -> withSelectedLibraryId { play(context, it) }
			KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MEDIA_PAUSE -> pause(context)
			KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> withSelectedLibraryId { togglePlayPause(context, it) }
			KeyEvent.KEYCODE_MEDIA_NEXT -> withSelectedLibraryId { next(context, it) }
			KeyEvent.KEYCODE_MEDIA_PREVIOUS -> withSelectedLibraryId { previous(context, it) }
			else -> return
		}

		if (isOrderedBroadcast) abortBroadcast()
	}
}
