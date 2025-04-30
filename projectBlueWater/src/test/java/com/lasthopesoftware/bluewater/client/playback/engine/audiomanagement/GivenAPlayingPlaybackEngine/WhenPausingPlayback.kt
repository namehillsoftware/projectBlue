package com.lasthopesoftware.bluewater.client.playback.engine.audiomanagement.GivenAPlayingPlaybackEngine

import androidx.media.AudioFocusRequestCompat
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.AudioManagingPlaybackStateChanger
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaybackState
import com.lasthopesoftware.bluewater.shared.android.audiofocus.ControlAudioFocus
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenPausingPlayback {

	private var isPaused = false
	private var isAbandoned = false

	private val mut by lazy {
		val innerPlaybackState = object : ChangePlaybackState {
			override fun startPlaylist(libraryId: LibraryId, playlist: List<ServiceFile>, playlistPosition: Int): Promise<Unit> =
				Unit.toPromise()

			override fun resume(): Promise<Unit> = Unit.toPromise()

			override fun pause(): Promise<Unit> {
				isPaused = true
				return Unit.toPromise()
			}
		}

		val audioFocus = object : ControlAudioFocus {
			override fun promiseAudioFocus(audioFocusRequest: AudioFocusRequestCompat): Promise<AudioFocusRequestCompat> =
				audioFocusRequest.toPromise()

			override fun abandonAudioFocus(audioFocusRequest: AudioFocusRequestCompat) {
				isAbandoned = true
			}
		}

		val audioManagingPlaybackStateChanger = AudioManagingPlaybackStateChanger(
			innerPlaybackState,
			mockk(),
			audioFocus,
			mockk(relaxed = true))

		audioManagingPlaybackStateChanger
	}

	@BeforeAll
	fun act() {
		with (mut) {
			resume().toExpiringFuture().get()
			pause().toExpiringFuture().get()
		}
	}

	@Test
	fun `then audio focus is released`() {
		assertThat(isAbandoned).isTrue
	}

	@Test
	fun `then playback is paused`() {
		assertThat(isPaused).isTrue
	}
}
