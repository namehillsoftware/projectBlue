package com.lasthopesoftware.bluewater.shared.android.audiofocus

import android.media.AudioManager
import androidx.media3.common.audio.AudioFocusRequestCompat
import androidx.media3.common.audio.AudioManagerCompat
import androidx.media3.common.util.UnstableApi
import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.promises.Promise

@UnstableApi
class AudioFocusManagement(private val audioManager: AudioManager) : ControlAudioFocus {
	override fun promiseAudioFocus(audioFocusRequest: AudioFocusRequestCompat): Promise<AudioFocusRequestCompat> =
		AudioFocusPromise(audioFocusRequest, audioManager)

	override fun abandonAudioFocus(audioFocusRequest: AudioFocusRequestCompat) {
		AudioManagerCompat.abandonAudioFocusRequest(audioManager, audioFocusRequest)
	}

	@UnstableApi
	private class AudioFocusPromise(audioFocusRequest: AudioFocusRequestCompat, private val audioManager: AudioManager) : Promise<AudioFocusRequestCompat>(), AudioManager.OnAudioFocusChangeListener, CancellationResponse {
		private val innerAudioFocusChangeListener = audioFocusRequest.onAudioFocusChangeListener
		private val delegatingAudioFocusRequest = audioFocusRequest.buildUpon()
			.setOnAudioFocusChangeListener(this)
			.build()

		init {
			awaitCancellation(this)
			try {
				when (AudioManagerCompat.requestAudioFocus(audioManager, delegatingAudioFocusRequest)) {
					AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> resolve(delegatingAudioFocusRequest)
					AudioManager.AUDIOFOCUS_REQUEST_FAILED -> reject(UnableToGrantAudioFocusException())
				}
			} catch (t: Throwable) {
				reject(t)
			}
		}

		override fun onAudioFocusChange(focusChange: Int) {
			when (focusChange) {
				AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> resolve(delegatingAudioFocusRequest)
				AudioManager.AUDIOFOCUS_REQUEST_FAILED -> reject(UnableToGrantAudioFocusException())
			}

			innerAudioFocusChangeListener.onAudioFocusChange(focusChange)
		}

		override fun cancellationRequested() {
			AudioManagerCompat.abandonAudioFocusRequest(audioManager, delegatingAudioFocusRequest)
			resolve(delegatingAudioFocusRequest)
		}

		override fun toString(): String = innerAudioFocusChangeListener.toString()
	}
}

