package com.lasthopesoftware.bluewater.client.playback.engine.audiomanagement.GivenAPlayingPlaybackEngine.AndAudioFocusIsTemporarilyLost.AndRegained

import android.media.AudioManager
import androidx.media.AudioFocusRequestCompat
import com.lasthopesoftware.bluewater.client.playback.engine.AudioManagingPlaybackStateChanger
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaybackState
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaybackStateForSystem
import com.lasthopesoftware.bluewater.shared.android.audiofocus.ControlAudioFocus
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenAudioFocusIsLost {

	companion object Setup {

		private val audioFocusRequests: MutableList<AudioFocusRequestCompat> = ArrayList()
		private var isInterrupted = false
		private var isAbandoned = false

		private val playbackStateForSystem by lazy {
			mockk<ChangePlaybackStateForSystem>().apply {
				every { interrupt() } answers {
					isInterrupted = true
					Unit.toPromise()
				}
			}
		}

		private val innerPlaybackState by lazy {
			mockk<ChangePlaybackState>().apply {
				every { resume() } answers {
					isInterrupted = false
					Unit.toPromise()
				}
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
				playbackStateForSystem,
				audioFocus,
				mockk(relaxed = true))
			audioManagingPlaybackStateChanger.resume().toFuture().get()
			audioManagingPlaybackStateChanger.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
			audioManagingPlaybackStateChanger.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
			audioManagingPlaybackStateChanger.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
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
		assertThat(isInterrupted).isTrue
	}
}
