package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.rendering

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.audio.AudioCapabilities
import com.google.android.exoplayer2.audio.DefaultAudioSink
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.lasthopesoftware.compilation.DebugFlag
import com.namehillsoftware.handoff.promises.Promise

class AudioRenderersFactory(private val context: Context, private val handler: Handler) : GetAudioRenderers {

	override fun newRenderers(): Promise<Array<MediaCodecAudioRenderer>> {
		return try {
			Promise(arrayOf(
				MediaCodecAudioRenderer(
					context,
					MediaCodecSelector.DEFAULT,
					false,
					handler,
					if (DebugFlag.getInstance().isDebugCompilation) AudioRenderingEventListener() else EmptyRenderersListener,
					DefaultAudioSink(AudioCapabilities.getCapabilities(context), emptyArray()))))
		} catch (err: Throwable) {
			Promise(err)
		}
	}
}
