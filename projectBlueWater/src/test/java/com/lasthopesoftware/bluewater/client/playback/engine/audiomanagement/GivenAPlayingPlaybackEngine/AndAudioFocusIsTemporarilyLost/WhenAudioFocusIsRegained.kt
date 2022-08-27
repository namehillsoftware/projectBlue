package com.lasthopesoftware.bluewater.client.playback.engine.audiomanagement.GivenAPlayingPlaybackEngine.AndAudioFocusIsTemporarilyLost

import android.media.AudioManager
import androidx.media.AudioFocusRequestCompat
import com.lasthopesoftware.bluewater.client.playback.engine.AudioManagingPlaybackStateChanger
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaybackState
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaybackStateForSystem
import com.lasthopesoftware.bluewater.shared.android.audiofocus.ControlAudioFocus
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenAudioFocusIsRegained {

	private val audioFocusRequests: MutableList<AudioFocusRequestCompat> = ArrayList()
	private var isInterrupted = false
	private var isAbandoned = false

	private val mut by lazy {
		val playbackStateForSystem = mockk<ChangePlaybackStateForSystem>().apply {
			every { interrupt() } answers {
				isInterrupted = true
				Unit.toPromise()
			}

			every { resume() } answers {
				isInterrupted = false
				Unit.toPromise()
			}
		}

		val innerPlaybackState = mockk<ChangePlaybackState>().apply {
			every { resume() } answers {
				isInterrupted = false
				Unit.toPromise()
			}
		}

		val audioFocus = object : ControlAudioFocus {
			override fun promiseAudioFocus(audioFocusRequest: AudioFocusRequestCompat): Promise<AudioFocusRequestCompat> {
				audioFocusRequests.add(audioFocusRequest)
				return audioFocusRequest.toPromise()
			}

			override fun abandonAudioFocus(audioFocusRequest: AudioFocusRequestCompat) {
				isAbandoned = true
			}
		}

		val audioManagingPlaybackStateChanger = AudioManagingPlaybackStateChanger(
			innerPlaybackState,
			playbackStateForSystem,
			audioFocus,
			mockk(relaxed = true)
		)

		audioManagingPlaybackStateChanger
	}

	@BeforeAll
	fun act() {
		with(mut) {
			resume().toExpiringFuture().get()
			onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
			onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
		}
	}

	@Test
	fun `then audio focus is not released because focus will be needed again`() {
		assertThat(isAbandoned).isFalse
	}

	@Test
	fun thenAudioFocusIsOnlyRequestedOnce() {
		assertThat(audioFocusRequests).hasSize(1)
	}

	@Test
	fun `then playback is not paused`() {
		assertThat(isInterrupted).isFalse
	}
}
