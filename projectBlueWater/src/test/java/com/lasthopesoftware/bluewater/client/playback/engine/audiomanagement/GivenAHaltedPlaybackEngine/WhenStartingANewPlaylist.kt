package com.lasthopesoftware.bluewater.client.playback.engine.audiomanagement.GivenAHaltedPlaybackEngine

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
import java.util.concurrent.TimeUnit

class WhenStartingANewPlaylist {
	private var isStarted = false
	private var request: AudioFocusRequestCompat? = null

	private val mut by lazy {
		val innerPlaybackState = object : ChangePlaybackState {
			override fun startPlaylist(libraryId: LibraryId, playlist: List<ServiceFile>, playlistPosition: Int): Promise<Unit> {
				isStarted = true
				return Unit.toPromise()
			}

			override fun resume(): Promise<Unit> = Unit.toPromise()

			override fun pause(): Promise<Unit> = Unit.toPromise()
		}

		val audioFocus = object : ControlAudioFocus {
			override fun promiseAudioFocus(audioFocusRequest: AudioFocusRequestCompat): Promise<AudioFocusRequestCompat> {
				request = audioFocusRequest
				return audioFocusRequest.toPromise()
			}

			override fun abandonAudioFocus(audioFocusRequest: AudioFocusRequestCompat) {}
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
		mut.startPlaylist(LibraryId(215), ArrayList(), 0)
			.toExpiringFuture()
			.get(20, TimeUnit.SECONDS)
	}

	@Test
	fun thenPlaybackIsStarted() {
		assertThat(isStarted).isTrue
	}

	@Test
	fun thenAudioFocusIsGranted() {
		assertThat(request).isNotNull
	}

	@Test
	fun `then it will not pause when ducked`() {
		assertThat(request?.willPauseWhenDucked()).isFalse
	}
}
