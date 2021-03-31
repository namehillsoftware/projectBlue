package com.lasthopesoftware.bluewater.client.playback.engine.audiomanagement.GivenAHaltedPlaybackEngine.AndAudioFocusCannotGrant

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
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenResumingPlayback {

	companion object Setup {

		private var isCancelled = false
		private var isResumed = false
		private var timeoutException: TimeoutException? = null

		private val innerPlaybackState = object : ChangePlaybackState {
			override fun startPlaylist(playlist: MutableList<ServiceFile>, playlistPosition: Int, filePosition: Int): Promise<Unit> =
				Unit.toPromise()

			override fun resume(): Promise<Unit> {
				isResumed = true
				return Unit.toPromise()
			}

			override fun pause(): Promise<Unit> = Unit.toPromise()
		}

		private val audioFocus = object : ControlAudioFocus {
			override fun promiseAudioFocus(audioFocusRequest: AudioFocusRequestCompat): Promise<AudioFocusRequestCompat> =
				Promise { it.cancellationRequested { isCancelled = true } }

			override fun abandonAudioFocus(audioFocusRequest: AudioFocusRequestCompat) {}
		}

		@JvmStatic
		@BeforeClass
		fun context() {
			val audioManagingPlaybackStateChanger = AudioManagingPlaybackStateChanger(
                    innerPlaybackState,
                    audioFocus,
                    mockk(relaxed = true))

			try {
				audioManagingPlaybackStateChanger.resume().toFuture().get(20, TimeUnit.SECONDS)
			} catch (e: ExecutionException) {
				timeoutException = e.cause as? TimeoutException
			}
		}
	}

	@Test
	fun thenATimeoutOccursInternalToTheMethod() {
		assertThat(timeoutException?.message).isEqualTo("Unable to gain audio focus in 10s")
	}

	@Test
	fun thenTheAudioFocusRequestIsCancelled() {
		assertThat(isCancelled).isTrue
	}

	@Test
	fun thenPlaybackIsNotResumed() {
		assertThat(isResumed).isFalse
	}
}
