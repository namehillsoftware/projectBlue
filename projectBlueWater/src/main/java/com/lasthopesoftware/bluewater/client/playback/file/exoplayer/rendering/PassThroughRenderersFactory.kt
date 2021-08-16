package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.rendering

import android.os.Handler
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.video.VideoRendererEventListener

class PassThroughRenderersFactory(private val renderers: Array<Renderer>) : RenderersFactory {
	override fun createRenderers(eventHandler: Handler, videoRendererEventListener: VideoRendererEventListener, audioRendererEventListener: AudioRendererEventListener, textRendererOutput: TextOutput, metadataRendererOutput: MetadataOutput): Array<Renderer> = renderers
}
