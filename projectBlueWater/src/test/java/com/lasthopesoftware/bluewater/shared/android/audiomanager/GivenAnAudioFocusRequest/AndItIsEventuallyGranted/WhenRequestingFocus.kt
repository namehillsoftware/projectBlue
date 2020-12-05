package com.lasthopesoftware.bluewater.shared.android.audiomanager.GivenAnAudioFocusRequest.AndItIsEventuallyGranted

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
		private lateinit var result: AudioFocusRequestCompat
		private val audioFocusEvents = ArrayList<Int>()
		private val request = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
			.setOnAudioFocusChangeListener(audioFocusEvents::add)
			.build()
	}

	override fun before() {
		val audioManager = mock(AudioManager::class.java)
		`when`(audioManager.requestAudioFocus(any()))
			.thenReturn(AudioManager.AUDIOFOCUS_REQUEST_DELAYED)

		val promisedAudioFocus = audioManager.promiseAudioFocus(request)
		val internalListener = promisedAudioFocus as AudioManager.OnAudioFocusChangeListener
		internalListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_REQUEST_DELAYED)
		internalListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
		internalListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
		result = promisedAudioFocus.toFuture().get()!!
		internalListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
	}

	@Test
	fun thenTheRequestIsGrantedUsingTheInnerListener() {
		assertThat(result.onAudioFocusChangeListener.toString()).isEqualTo(request.onAudioFocusChangeListener.toString())
	}

	@Test
	fun thenOtherEventsAreForwardedCorrectly() {
		assertThat(audioFocusEvents).containsOnly(AudioManager.AUDIOFOCUS_REQUEST_DELAYED, AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
	}
}
