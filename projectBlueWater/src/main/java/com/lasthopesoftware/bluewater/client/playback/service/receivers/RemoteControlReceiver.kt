package com.lasthopesoftware.bluewater.client.playback.service.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.next
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.pause
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.play
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.previous
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.togglePlayPause
import com.lasthopesoftware.bluewater.shared.android.intents.safelyGetParcelableExtra

@UnstableApi class RemoteControlReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		fun getSelectedLibraryIdProvider(): ProvideSelectedLibraryId {
			return context.getCachedSelectedLibraryIdProvider()
		}

		val event = intent.safelyGetParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)

		if (event?.action != KeyEvent.ACTION_DOWN) return

		when (event.keyCode) {
			KeyEvent.KEYCODE_MEDIA_PLAY -> {
				getSelectedLibraryIdProvider().promiseSelectedLibraryId().then { it?.also { l -> play(context, l) } }
			}
			KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MEDIA_PAUSE -> pause(context)
			KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
				getSelectedLibraryIdProvider().promiseSelectedLibraryId().then { it?.also { l -> togglePlayPause(context, l) } }
			}
			KeyEvent.KEYCODE_MEDIA_NEXT -> {
				getSelectedLibraryIdProvider().promiseSelectedLibraryId().then { it?.also { l -> next(context, l) } }
			}
			KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
				getSelectedLibraryIdProvider().promiseSelectedLibraryId().then { it?.also { l -> previous(context, l) } }
			}
			else -> return
		}

		if (isOrderedBroadcast) abortBroadcast()
	}
}
