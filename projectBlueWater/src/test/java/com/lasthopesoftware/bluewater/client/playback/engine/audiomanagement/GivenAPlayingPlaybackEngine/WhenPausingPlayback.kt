package com.lasthopesoftware.bluewater.client.playback.engine.audiomanagement.GivenAPlayingPlaybackEngine

import androidx.media.AudioFocusRequestCompat
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.AudioManagingPlaybackStateChanger
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaybackState
import com.lasthopesoftware.bluewater.shared.android.audiofocus.ControlAudioFocus
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test

class WhenPausingPlayback {

	companion object Setup {

		private var isPaused = false
		private var isAbandoned = false

		private val innerPlaybackState = object : ChangePlaybackState {
			override fun startPlaylist(playlist: MutableList<ServiceFile>, playlistPosition: Int, filePosition: Duration): Promise<Unit> =
				Unit.toPromise()

			override fun resume(): Promise<Unit> = Unit.toPromise()

			override fun pause(): Promise<Unit> {
				isPaused = true
				return Unit.toPromise()
			}
		}

		private val audioFocus = object : ControlAudioFocus {
			override fun promiseAudioFocus(audioFocusRequest: AudioFocusRequestCompat): Promise<AudioFocusRequestCompat> =
				audioFocusRequest.toPromise()

			override fun abandonAudioFocus(audioFocusRequest: AudioFocusRequestCompat) {
				isAbandoned = true
			}
		}

		@JvmStatic
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
		assertThat(isAbandoned).isTrue
	}

	@Test
	fun thenPlaybackIsPaused() {
		assertThat(isPaused).isTrue
	}
}
