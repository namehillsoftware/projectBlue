package com.lasthopesoftware.bluewater.shared.android.audiofocus.GivenAnAudioFocusRequest.AndItIsImmediatelyRejected

import android.media.AudioManager
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.shared.android.audiofocus.AudioFocusManagement
import com.lasthopesoftware.bluewater.shared.android.audiofocus.UnableToGrantAudioFocusException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.ExecutionException

class WhenRequestingFocus : AndroidContext() {

	companion object {
		private lateinit var unableToGrantException: UnableToGrantAudioFocusException
	}

	override fun before() {
		val audioManager = mockk<AudioManager> {
			every { requestAudioFocus(any()) } returns AudioManager.AUDIOFOCUS_REQUEST_FAILED
		}

		val request = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
			.setOnAudioFocusChangeListener {  }
			.build()

		val audioFocusManagement = AudioFocusManagement(audioManager)
		try {
			audioFocusManagement.promiseAudioFocus(request).toExpiringFuture().get()!!
		} catch (e: ExecutionException) {
			val cause = e.cause
			if (cause is UnableToGrantAudioFocusException)
				unableToGrantException = cause
		}
	}

	@Test
	fun `then audio focus is not granted`() {
		assertThat(unableToGrantException).isNotNull
	}
}
