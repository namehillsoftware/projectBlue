package com.lasthopesoftware.bluewater.shared.android.audiomanager

import android.media.AudioManager
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.namehillsoftware.handoff.promises.Promise

fun AudioManager.promiseAudioFocus(audioFocusRequest: AudioFocusRequestCompat): Promise<Boolean> = AudioFocusPromise(audioFocusRequest, this)

private class AudioFocusPromise(audioFocusRequest: AudioFocusRequestCompat, audioManager: AudioManager) : Promise<Boolean>(), AudioManager.OnAudioFocusChangeListener {
	private val innerAudioFocusChangeListener = audioFocusRequest.onAudioFocusChangeListener

	init {
		val delegatingAudioFocusRequest = AudioFocusRequestCompat.Builder(audioFocusRequest)
			.setOnAudioFocusChangeListener(this)
			.build()

		when (AudioManagerCompat.requestAudioFocus(audioManager, delegatingAudioFocusRequest)) {
			AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> resolve(true)
			AudioManager.AUDIOFOCUS_REQUEST_FAILED -> resolve(true)
		}
	}

	override fun onAudioFocusChange(focusChange: Int) {
		when (focusChange) {
			AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> resolve(true)
			AudioManager.AUDIOFOCUS_REQUEST_FAILED -> reject(UnableToGetAudioFocusException())
			else -> innerAudioFocusChangeListener.onAudioFocusChange(focusChange)
		}
	}

	override fun toString(): String = innerAudioFocusChangeListener.toString()
}

class UnableToGetAudioFocusException : Throwable()
