package com.lasthopesoftware.bluewater.client.playback.engine.exoplayer;

import android.content.Context;
import android.os.Handler;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

public class AudioRenderersFactory implements RenderersFactory {
	private final Context context;

	public AudioRenderersFactory(Context context) {
		this.context = context;
	}

	@Override
	public Renderer[] createRenderers(
		Handler eventHandler,
		VideoRendererEventListener videoRendererEventListener,
		AudioRendererEventListener audioRendererEventListener,
		TextOutput textRendererOutput,
		MetadataOutput metadataRendererOutput,
		DrmSessionManager<FrameworkMediaCrypto> drmSessionManager) {
		return new Renderer[] {
			new MediaCodecAudioRenderer(
				context,
				MediaCodecSelector.DEFAULT,
				drmSessionManager,
				false,
				eventHandler,
				audioRendererEventListener,
				AudioCapabilities.getCapabilities(context))
		};
	}
}
