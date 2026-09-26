package com.lasthopesoftware.bluewater.shared.android.audiofocus

import androidx.media3.common.audio.AudioFocusRequestCompat
import androidx.media3.common.util.UnstableApi
import com.namehillsoftware.handoff.promises.Promise

interface ControlAudioFocus {
	@UnstableApi
	fun promiseAudioFocus(audioFocusRequest: AudioFocusRequestCompat): Promise<AudioFocusRequestCompat>

	@UnstableApi
	fun abandonAudioFocus(audioFocusRequest: AudioFocusRequestCompat)
}
