package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.rendering

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.audio.AudioCapabilities
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.audio.DefaultAudioSink
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.video.VideoRendererEventListener
import com.lasthopesoftware.compilation.DebugFlag

class AudioRenderersFactory(private val context: Context) : RenderersFactory {

	override fun createRenderers(
		eventHandler: Handler,
		videoRendererEventListener: VideoRendererEventListener,
		audioRendererEventListener: AudioRendererEventListener,
		textRendererOutput: TextOutput,
		metadataRendererOutput: MetadataOutput
	): Array<Renderer> =
		arrayOf(
			MediaCodecAudioRenderer(
				context,
				MediaCodecSelector.DEFAULT,
				false,
				if (DebugFlag.isDebugCompilation) eventHandler else null,
				if (DebugFlag.isDebugCompilation) AudioRenderingEventListener() else null,
				DefaultAudioSink.Builder()
					.setAudioCapabilities(AudioCapabilities.getCapabilities(context))
					.build()))
}
