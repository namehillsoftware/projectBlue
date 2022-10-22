package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.GivenAStandardNotificationManager

import android.support.v4.media.session.PlaybackStateCompat
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.MediaSessionBroadcaster
import com.lasthopesoftware.bluewater.shared.android.MediaSession.ControlMediaSession
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class WhenPlaybackIsPaused : AndroidContext() {
	companion object {
		private val mediaSessionCompat = mockk<ControlMediaSession>(relaxUnitFun = true)
	}

    override fun before() {
		val playbackNotificationBroadcaster = MediaSessionBroadcaster(
                mockk(),
                mockk(),
                mockk(),
                mediaSessionCompat,
			)
        playbackNotificationBroadcaster.notifyPaused()
    }

	@Test
	fun `then state is set to paused`() {
		verify { mediaSessionCompat.setPlaybackState(match { c -> c.state == PlaybackStateCompat.STATE_PAUSED }) }
	}
}
