package com.lasthopesoftware.bluewater.shared.android.audiomanager.GivenAnAudioFocusRequest.AndItCannotBeGranted

import android.media.AudioManager
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.shared.android.audiomanager.promiseAudioFocus
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.*

class WhenRequestingFocus : AndroidContext() {

	companion object {
		private var result = false
	}

	override fun before() {
		val audioManager = mock(AudioManager::class.java)
		`when`(audioManager.requestAudioFocus(any()))
			.thenReturn(AudioManager.AUDIOFOCUS_REQUEST_FAILED)

		val request = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
			.setOnAudioFocusChangeListener {  }
			.build()

		result = audioManager.promiseAudioFocus(request).toFuture().get()!!
	}

	@Test
	fun thenItIsNotReturnedAsGranted() {
		assertThat(result).isFalse
	}
}
