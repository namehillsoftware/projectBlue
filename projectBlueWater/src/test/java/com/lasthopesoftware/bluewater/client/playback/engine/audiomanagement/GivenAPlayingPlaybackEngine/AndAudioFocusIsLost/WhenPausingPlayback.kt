package com.lasthopesoftware.bluewater.client.playback.engine.audiomanagement.GivenAPlayingPlaybackEngine.AndAudioFocusIsLost

import android.media.AudioManager
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

class WhenPausingPlayback {

	companion object Setup {

		private val audioFocusRequests: MutableList<AudioFocusRequestCompat> = ArrayList()
		private var isPaused = false
		private var isAbandoned = false

		private val innerPlaybackState = object : ChangePlaybackState {
			override fun startPlaylist(playlist: MutableList<ServiceFile>, playlistPosition: Int, filePosition: Int): Promise<Unit> =
				Unit.toPromise()

			override fun resume(): Promise<Unit> = Unit.toPromise()

			override fun pause(): Promise<Unit> {
				isPaused = true
				return Unit.toPromise()
			}
		}

		private val audioFocus = object : ControlAudioFocus {
			override fun promiseAudioFocus(audioFocusRequest: AudioFocusRequestCompat): Promise<AudioFocusRequestCompat> {
				audioFocusRequests.add(audioFocusRequest)
				return audioFocusRequest.toPromise()
			}

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
			audioManagingPlaybackStateChanger.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
			audioManagingPlaybackStateChanger.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
		}
	}

	@Test
	fun thenAudioFocusIsNotReleasedBecauseFocusWillBeNeededAgain() {
		assertThat(isAbandoned).isFalse
	}

	@Test
	fun thenAudioFocusIsOnlyRequestedOnce() {
		assertThat(audioFocusRequests).hasSize(1)
	}

	@Test
	fun thenPlaybackIsPaused() {
		assertThat(isPaused).isTrue
	}
}
