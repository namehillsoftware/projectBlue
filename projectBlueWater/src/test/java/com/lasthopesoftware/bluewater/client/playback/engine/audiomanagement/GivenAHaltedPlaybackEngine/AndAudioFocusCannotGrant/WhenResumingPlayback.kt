package com.lasthopesoftware.bluewater.client.playback.engine.audiomanagement.GivenAHaltedPlaybackEngine.AndAudioFocusCannotGrant

import androidx.media.AudioFocusRequestCompat
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.AudioManagingPlaybackStateChanger
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaybackState
import com.lasthopesoftware.bluewater.shared.android.audiofocus.ControlAudioFocus
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenResumingPlayback {

	private var isCancelled = false
	private var isResumed = false
	private var timeoutException: TimeoutException? = null

	private val mut by lazy {
		val innerPlaybackState = object : ChangePlaybackState {
			override fun startPlaylist(
				playlist: List<ServiceFile>,
				playlistPosition: Int,
				filePosition: Duration
			): Promise<Unit> =
				Unit.toPromise()

			override fun resume(): Promise<Unit> {
				isResumed = true
				return Unit.toPromise()
			}

			override fun pause(): Promise<Unit> = Unit.toPromise()
		}

		val audioFocus = object : ControlAudioFocus {
			override fun promiseAudioFocus(audioFocusRequest: AudioFocusRequestCompat): Promise<AudioFocusRequestCompat> =
				Promise { it.cancellationRequested { isCancelled = true } }

			override fun abandonAudioFocus(audioFocusRequest: AudioFocusRequestCompat) {}
		}

		val audioManagingPlaybackStateChanger = AudioManagingPlaybackStateChanger(
			innerPlaybackState,
			mockk(),
			audioFocus,
			mockk(relaxed = true)
		)

		audioManagingPlaybackStateChanger
	}

	@BeforeAll
	fun act() {
		try {
			mut.resume().toExpiringFuture().get(20, TimeUnit.SECONDS)
		} catch (e: ExecutionException) {
			timeoutException = e.cause as? TimeoutException
		}
	}

	@Test
	fun `then a timeout occurs internal to the method`() {
		assertThat(timeoutException?.message).isEqualTo("Unable to gain audio focus in 10s")
	}

	@Test
	fun `then the audio focus request is cancelled`() {
		assertThat(isCancelled).isTrue
	}

	@Test
	fun thenPlaybackIsNotResumed() {
		assertThat(isResumed).isFalse
	}
}
