package com.lasthopesoftware.bluewater.shared.android.audiomanager

import android.media.AudioManager
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.namehillsoftware.handoff.promises.Promise

fun AudioManager.promiseAudioFocus(audioFocusRequest: AudioFocusRequestCompat): Promise<AudioFocusRequestCompat> = AudioFocusPromise(audioFocusRequest, this)

private class AudioFocusPromise(audioFocusRequest: AudioFocusRequestCompat, audioManager: AudioManager) : Promise<AudioFocusRequestCompat>(), AudioManager.OnAudioFocusChangeListener {
	private val innerAudioFocusChangeListener = audioFocusRequest.onAudioFocusChangeListener
	private val delegatingAudioFocusRequest = AudioFocusRequestCompat.Builder(audioFocusRequest)
		.setOnAudioFocusChangeListener(this)
		.build()

	init {
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
			else -> innerAudioFocusChangeListener.onAudioFocusChange(focusChange)
		}
	}

	override fun toString(): String = innerAudioFocusChangeListener.toString()
}

class UnableToGrantAudioFocusException : Throwable()
