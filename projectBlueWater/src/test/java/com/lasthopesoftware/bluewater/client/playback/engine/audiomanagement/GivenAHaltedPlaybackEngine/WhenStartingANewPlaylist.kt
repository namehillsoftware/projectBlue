package com.lasthopesoftware.bluewater.client.playback.engine.audiomanagement.GivenAHaltedPlaybackEngine

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
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.TimeUnit

class WhenStartingANewPlaylist {
	companion object Setup {

		private var isStarted = false
		private var request: AudioFocusRequestCompat? = null

		private val innerPlaybackState = object : ChangePlaybackState {
			override fun startPlaylist(playlist: MutableList<ServiceFile>, playlistPosition: Int, filePosition: Int): Promise<Unit> {
				isStarted = true
				return Unit.toPromise()
			}

			override fun resume(): Promise<Unit> = Unit.toPromise()

			override fun pause(): Promise<Unit> = Unit.toPromise()
		}

		private val audioFocus = object : ControlAudioFocus {
			override fun promiseAudioFocus(audioFocusRequest: AudioFocusRequestCompat): Promise<AudioFocusRequestCompat> {
				request = audioFocusRequest
				return audioFocusRequest.toPromise()
			}

			override fun abandonAudioFocus(audioFocusRequest: AudioFocusRequestCompat) {}
		}

		@JvmStatic
		@BeforeClass
		fun context() {
			val audioManagingPlaybackStateChanger = AudioManagingPlaybackStateChanger(
				innerPlaybackState,
				audioFocus,
				mockk(relaxed = true))

			audioManagingPlaybackStateChanger
				.startPlaylist(ArrayList(), 0, 0)
				.toFuture()
				.get(20, TimeUnit.SECONDS)
		}
	}

	@Test
	fun thenPlaybackIsStarted() {
		assertThat(isStarted).isTrue
	}

	@Test
	fun thenAudioFocusIsGranted() {
		assertThat(request).isNotNull
	}
}
