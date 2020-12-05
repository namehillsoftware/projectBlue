package com.lasthopesoftware.bluewater.client.playback.service.audiomanager.GivenAnAudioFocusRequest.AndItIsImmediatelyGranted

import android.media.AudioManager
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.playback.service.audiomanager.promiseAudioFocus
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.*

class WhenRequestingFocus : AndroidContext() {

	companion object {
		private lateinit var result: AudioFocusRequestCompat
		private val audioFocusEvents = ArrayList<Int>()
		private val request = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
			.setOnAudioFocusChangeListener(audioFocusEvents::add)
			.build()
	}

	override fun before() {
		val audioManager = mock(AudioManager::class.java)
		`when`(audioManager.requestAudioFocus(any()))
			.thenReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)

		val promisedAudioFocus = audioManager.promiseAudioFocus(request)
		val internalListener = promisedAudioFocus as AudioManager.OnAudioFocusChangeListener
		result = promisedAudioFocus.toFuture().get()!!
		internalListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
	}

	@Test
	fun thenTheRequestIsGrantedUsingTheInnerListener() {
		assertThat(result.onAudioFocusChangeListener.toString()).isEqualTo(request.onAudioFocusChangeListener.toString())
	}

	@Test
	fun thenOtherEventsAreForwardedCorrectly() {
		assertThat(audioFocusEvents).containsOnly(AudioManager.AUDIOFOCUS_LOSS)
	}
}
