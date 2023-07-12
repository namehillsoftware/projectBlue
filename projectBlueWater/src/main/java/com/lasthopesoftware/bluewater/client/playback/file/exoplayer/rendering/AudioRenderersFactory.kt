package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.rendering

import android.content.Context
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.metadata.MetadataOutput
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.video.VideoRendererEventListener
import com.lasthopesoftware.compilation.DebugFlag

@UnstableApi class AudioRenderersFactory(private val context: Context) : RenderersFactory {

	@OptIn(UnstableApi::class)
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
				DefaultAudioSink.Builder(context).build())
		)
}
