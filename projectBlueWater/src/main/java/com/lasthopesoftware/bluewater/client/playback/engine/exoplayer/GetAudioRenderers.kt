package com.lasthopesoftware.bluewater.client.playback.engine.exoplayer

import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer

interface GetAudioRenderers {
	fun newRenderers(): Array<MediaCodecAudioRenderer>
}
