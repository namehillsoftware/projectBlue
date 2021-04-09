package com.lasthopesoftware.bluewater.shared.android.audiofocus

import android.media.AudioManager
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.namehillsoftware.handoff.promises.Promise

class AudioFocusManagement(private val audioManager: AudioManager) : ControlAudioFocus {
	override fun promiseAudioFocus(audioFocusRequest: AudioFocusRequestCompat): Promise<AudioFocusRequestCompat> =
		AudioFocusPromise(audioFocusRequest, audioManager)

	override fun abandonAudioFocus(audioFocusRequest: AudioFocusRequestCompat) {
		AudioManagerCompat.abandonAudioFocusRequest(audioManager, audioFocusRequest)
	}

	private class AudioFocusPromise(audioFocusRequest: AudioFocusRequestCompat, private val audioManager: AudioManager) : Promise<AudioFocusRequestCompat>(), AudioManager.OnAudioFocusChangeListener, Runnable {
		private val innerAudioFocusChangeListener = audioFocusRequest.onAudioFocusChangeListener
		private val delegatingAudioFocusRequest = AudioFocusRequestCompat.Builder(audioFocusRequest)
			.setOnAudioFocusChangeListener(this)
			.build()

		init {
			respondToCancellation(this)
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

		override fun run() {
			AudioManagerCompat.abandonAudioFocusRequest(audioManager, delegatingAudioFocusRequest)
			resolve(delegatingAudioFocusRequest)
		}

		override fun toString(): String = innerAudioFocusChangeListener.toString()
	}
}

