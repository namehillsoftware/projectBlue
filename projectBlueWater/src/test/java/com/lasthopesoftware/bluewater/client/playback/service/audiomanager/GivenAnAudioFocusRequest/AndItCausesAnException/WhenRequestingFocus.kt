package com.lasthopesoftware.bluewater.client.playback.service.audiomanager.GivenAnAudioFocusRequest.AndItCausesAnException

import android.media.AudioManager
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.playback.service.audiomanager.promiseAudioFocus
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.*
import java.util.concurrent.ExecutionException

class WhenRequestingFocus : AndroidContext() {

	companion object {
		private val badException = RuntimeException("Bad!")
		private lateinit var cause: Throwable
	}

	override fun before() {
		val audioManager = mock(AudioManager::class.java)
		`when`(audioManager.requestAudioFocus(any()))
			.thenThrow(badException)

		val request = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
			.setOnAudioFocusChangeListener {  }
			.build()

		try {
			audioManager.promiseAudioFocus(request).toFuture().get()!!
		} catch (e: ExecutionException) {
			cause = e.cause!!
		}
	}

	@Test
	fun thenTheExceptionIsForwarded() {
		assertThat(cause).isSameAs(badException)
	}
}
