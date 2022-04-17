package com.lasthopesoftware.bluewater.shared.android.audiofocus.GivenAnAudioFocusRequest.AndItIsEventuallyRejected

import android.media.AudioManager
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.shared.android.audiofocus.AudioFocusManagement
import com.lasthopesoftware.bluewater.shared.android.audiofocus.UnableToGrantAudioFocusException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.*
import java.util.concurrent.ExecutionException

class WhenRequestingFocus : AndroidContext() {

	companion object {
		private lateinit var unableToGrantException: UnableToGrantAudioFocusException
	}

	override fun before() {
		val audioManager = mock(AudioManager::class.java)
		`when`(audioManager.requestAudioFocus(any()))
			.thenReturn(AudioManager.AUDIOFOCUS_REQUEST_DELAYED)

		val request = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
			.setOnAudioFocusChangeListener {  }
			.build()

		val audioFocusManagement = AudioFocusManagement(audioManager)
		val promisedAudioFocus = audioFocusManagement.promiseAudioFocus(request)

		try {
			val internalListener = promisedAudioFocus as AudioManager.OnAudioFocusChangeListener
			internalListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
			promisedAudioFocus.toExpiringFuture().get()!!
		} catch (e: ExecutionException) {
			val cause = e.cause
			if (cause is UnableToGrantAudioFocusException)
				unableToGrantException = cause
		}
	}

	@Test
	fun thenAudioFocusIsNotGranted() {
		assertThat(unableToGrantException).isNotNull
	}
}
