package com.lasthopesoftware.bluewater.client.playback.engine.exoplayer

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.audio.*
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.video.VideoRendererEventListener
import com.lasthopesoftware.compilation.DebugFlag
import com.namehillsoftware.lazyj.Lazy
import org.slf4j.LoggerFactory

class AudioRenderersFactory(private val context: Context, private val handler: Handler) : RenderersFactory, GetAudioRenderers {

	companion object {
		private val lazyTextOutputLogger = Lazy { TextOutputLogger() }
		private val lazyMetadataOutputLogger = Lazy { MetadataOutputLogger() }
		private val logger = LoggerFactory.getLogger(AudioRenderersFactory::class.java)
	}

	override fun createRenderers(
		eventHandler: Handler,
		videoRendererEventListener: VideoRendererEventListener,
		audioRendererEventListener: AudioRendererEventListener,
		textRendererOutput: TextOutput,
		metadataRendererOutput: MetadataOutput,
		drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?): Array<Renderer> {
		return arrayOf(
			MediaCodecAudioRenderer(
				context,
				MediaCodecSelector.DEFAULT,
				false,
				eventHandler,
				audioRendererEventListener,
				DefaultAudioSink(AudioCapabilities.getCapabilities(context), arrayOf(SilenceSkippingAudioProcessor())))
		)
	}

	override fun newRenderers(): Array<MediaCodecAudioRenderer> {
		return arrayOf(
			MediaCodecAudioRenderer(
				context,
				MediaCodecSelector.DEFAULT,
				false,
				handler,
				if (DebugFlag.getInstance().isDebugCompilation) AudioRenderingEventListener() else EmptyRenderersListener,
				DefaultAudioSink(AudioCapabilities.getCapabilities(context), arrayOf(SilenceSkippingAudioProcessor())))
		)
	}
}
