package com.lasthopesoftware.bluewater.shared.android.audiofocus

import androidx.media.AudioFocusRequestCompat
import com.namehillsoftware.handoff.promises.Promise

interface ControlAudioFocus {
	fun promiseAudioFocus(audioFocusRequest: AudioFocusRequestCompat): Promise<AudioFocusRequestCompat>

	fun abandonAudioFocus(audioFocusRequest: AudioFocusRequestCompat)
}
