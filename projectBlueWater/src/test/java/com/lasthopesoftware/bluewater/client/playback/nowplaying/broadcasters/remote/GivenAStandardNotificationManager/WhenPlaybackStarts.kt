package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.GivenAStandardNotificationManager

import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.MediaSessionBroadcaster
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test

class WhenPlaybackStarts : AndroidContext() {

	companion object {
		private val mediaSessionCompat = spyk(
			MediaSessionCompat(
				ApplicationProvider.getApplicationContext(),
				"test"
			)
		)
	}

	override fun before() {
		val playbackNotificationBroadcaster =
            MediaSessionBroadcaster(
				mockk(),
				mockk(),
				mockk(),
				mediaSessionCompat
            )
		playbackNotificationBroadcaster.notifyPlaying()
	}

	@Test
	fun `then the state is set to playing`() {
		verify { mediaSessionCompat.setPlaybackState(match { c -> c.state == PlaybackStateCompat.STATE_PLAYING }) }
	}
}