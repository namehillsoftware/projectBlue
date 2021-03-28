package com.lasthopesoftware.bluewater.client.playback.engine.audiomanagement.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.playback.engine.AudioManagingPlaybackStateChanger
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaybackState
import com.lasthopesoftware.bluewater.shared.android.audiofocus.ControlAudioFocus
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import io.mockk.mockk
import io.mockk.verify
import org.junit.BeforeClass
import org.junit.Test

class WhenPausingPlayback {

	companion object Setup {

		private val innerPlaybackState = mockk<ChangePlaybackState>(relaxed = true)
		private val audioFocus = mockk<ControlAudioFocus>(relaxed = true)

		@BeforeClass
		fun context() {
			val audioManagingPlaybackStateChanger = AudioManagingPlaybackStateChanger(
				innerPlaybackState,
				audioFocus,
				mockk(relaxed = true))
			audioManagingPlaybackStateChanger.resume().toFuture().get()
			audioManagingPlaybackStateChanger.pause().toFuture().get()
		}
	}

	@Test
	fun thenAudioFocusIsReleased() {
		verify { audioFocus.abandonAudioFocus(any()) }
	}

	@Test
	fun thenPlaybackIsPaused() {
		verify { innerPlaybackState.pause() }
	}
}
