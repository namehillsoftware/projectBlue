package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.GivenAStandardNotificationBroadcaster

import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WhenRegisteringForActions {

	companion object {
		private val registeredIntents by lazy {
			val playbackNotificationRouter = PlaybackNotificationRouter(mockk(), mockk(relaxed = true))
			playbackNotificationRouter.registerForIntents()
		}
	}

	@Test
	fun thenTheRegisteredActionsAreCorrect() {
		assertThat(registeredIntents).isSubsetOf(
			PlaylistEvents.onPlaylistStop
		)
	}
}
