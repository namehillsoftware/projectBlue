package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.rendering

import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.namehillsoftware.handoff.promises.Promise

interface GetAudioRenderers {
	fun newRenderers(): Promise<Array<MediaCodecAudioRenderer>>
}
