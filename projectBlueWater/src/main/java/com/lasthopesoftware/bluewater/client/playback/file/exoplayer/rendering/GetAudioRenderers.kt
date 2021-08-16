package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.rendering

import com.google.android.exoplayer2.Renderer
import com.namehillsoftware.handoff.promises.Promise

fun interface GetAudioRenderers {
	fun newRenderers(): Promise<Array<Renderer>>
}
