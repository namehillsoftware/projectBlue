package com.lasthopesoftware.bluewater.shared.android.audiofocus.GivenAnAudioFocusRequest.AndItIsImmediatelyGranted

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

class WhenRequestingFocus : AndroidContext() {

	companion object {
		private lateinit var result: AudioFocusRequestCompat
		private val audioFocusEvents = ArrayList<Int>()
		private val request = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
			.setOnAudioFocusChangeListener(audioFocusEvents::add)
			.build()
	}

	override fun before() {
		val audioManager = mockk<AudioManager> {
			every { requestAudioFocus(any()) } returns AudioManager.AUDIOFOCUS_REQUEST_GRANTED
		}

		val audioFocusManagement = AudioFocusManagement(audioManager)
		val promisedAudioFocus = audioFocusManagement.promiseAudioFocus(request)
		val internalListener = promisedAudioFocus as AudioManager.OnAudioFocusChangeListener
		result = promisedAudioFocus.toExpiringFuture().get()!!
		internalListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
	}

	@Test
	fun `then the request is granted using the inner listener`() {
		assertThat(result.onAudioFocusChangeListener.toString()).isEqualTo(request.onAudioFocusChangeListener.toString())
	}

	@Test
	fun `then other events are forwarded correctly`() {
		assertThat(audioFocusEvents).containsOnly(AudioManager.AUDIOFOCUS_LOSS)
	}
}
