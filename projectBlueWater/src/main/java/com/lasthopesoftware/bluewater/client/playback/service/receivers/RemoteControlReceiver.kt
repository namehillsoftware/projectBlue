package com.lasthopesoftware.bluewater.client.playback.service.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.next
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.pause
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.play
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.previous
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.togglePlayPause
import com.lasthopesoftware.bluewater.shared.cls

class RemoteControlReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		val event = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, cls<KeyEvent>())
		} else {
			@Suppress("DEPRECATION")
			intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
		}
		if (event?.action != KeyEvent.ACTION_UP) return
		when (event.keyCode) {
			KeyEvent.KEYCODE_MEDIA_PLAY -> play(context)
			KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MEDIA_PAUSE -> pause(context)
			KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> togglePlayPause(context)
			KeyEvent.KEYCODE_MEDIA_NEXT -> next(context)
			KeyEvent.KEYCODE_MEDIA_PREVIOUS -> previous(context)
		}
	}
}
