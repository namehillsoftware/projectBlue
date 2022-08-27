package com.lasthopesoftware.bluewater.shared.android.audiofocus.GivenAnAudioFocusRequest.AndItCausesAnException

import android.media.AudioManager
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.shared.android.audiofocus.AudioFocusManagement
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.ExecutionException

class WhenRequestingFocus : AndroidContext() {

	companion object {
		private val badException = RuntimeException("Bad!")
		private lateinit var cause: Throwable
	}

	override fun before() {
		val audioManager = mockk<AudioManager> {
			every { requestAudioFocus(any()) } throws badException
		}

		val request = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
			.setOnAudioFocusChangeListener {  }
			.build()

		val audioFocusManagement = AudioFocusManagement(audioManager)

		try {
			audioFocusManagement.promiseAudioFocus(request).toExpiringFuture().get()!!
		} catch (e: ExecutionException) {
			cause = e.cause!!
		}
	}

	@Test
	fun thenTheExceptionIsForwarded() {
		assertThat(cause).isSameAs(badException)
	}
}
