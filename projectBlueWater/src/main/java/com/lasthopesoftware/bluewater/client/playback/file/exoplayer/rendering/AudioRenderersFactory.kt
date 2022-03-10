package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.rendering

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.audio.AudioCapabilities
import com.google.android.exoplayer2.audio.DefaultAudioSink
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.lasthopesoftware.compilation.DebugFlag
import com.namehillsoftware.handoff.promises.Promise

class AudioRenderersFactory(private val context: Context, private val handler: Handler) : GetAudioRenderers {

	override fun newRenderers(): Promise<Array<Renderer>> =
		try {
			Promise(arrayOf(
				MediaCodecAudioRenderer(
					context,
					MediaCodecSelector.DEFAULT,
					false,
					if (DebugFlag.isDebugCompilation) handler else null,
					if (DebugFlag.isDebugCompilation) AudioRenderingEventListener() else null,
					DefaultAudioSink.Builder()
						.setAudioCapabilities(AudioCapabilities.getCapabilities(context))
						.build())))
		} catch (err: Throwable) {
			Promise(err)
		}
}
