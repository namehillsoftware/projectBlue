package com.lasthopesoftware.bluewater.shared.android.audiofocus.GivenAnAudioFocusRequest.AndItIsEventuallyGranted

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
			every { requestAudioFocus(any()) } returns  AudioManager.AUDIOFOCUS_REQUEST_DELAYED
		}

		val audioFocusManagement = AudioFocusManagement(audioManager)
		val promisedAudioFocus = audioFocusManagement.promiseAudioFocus(request)
		val internalListener = promisedAudioFocus as AudioManager.OnAudioFocusChangeListener
		internalListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_REQUEST_DELAYED)
		internalListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
		internalListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
		result = promisedAudioFocus.toExpiringFuture().get()!!
		internalListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
	}

	@Test
	fun `then the request is granted using the inner listener`() {
		assertThat(result.onAudioFocusChangeListener.toString()).isEqualTo(request.onAudioFocusChangeListener.toString())
	}

	@Test
	fun `then other events are forwarded correctly`() {
		assertThat(audioFocusEvents).containsOnly(
			AudioManager.AUDIOFOCUS_REQUEST_DELAYED,
			AudioManager.AUDIOFOCUS_REQUEST_GRANTED,
			AudioManager.AUDIOFOCUS_LOSS,
			AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
	}
}
