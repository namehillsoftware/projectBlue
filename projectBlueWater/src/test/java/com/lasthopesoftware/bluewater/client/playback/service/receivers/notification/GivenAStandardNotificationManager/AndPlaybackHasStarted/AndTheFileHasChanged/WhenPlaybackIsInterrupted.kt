package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotifyOfPlaybackEvents
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class WhenPlaybackIsInterrupted : AndroidContext() {

	companion object {
		private val playbackEventsNotifier by lazy { mockk<NotifyOfPlaybackEvents>(relaxed = true, relaxUnitFun = true) }
	}

	override fun before() {
		PlaybackNotificationRouter(playbackEventsNotifier)
			.onReceive(
				ApplicationProvider.getApplicationContext(),
				Intent(PlaylistEvents.onPlaylistInterrupted))
	}

	@Test
	fun `then notify of interrupted events`() {
		verify { playbackEventsNotifier.notifyInterrupted() }
	}
}
