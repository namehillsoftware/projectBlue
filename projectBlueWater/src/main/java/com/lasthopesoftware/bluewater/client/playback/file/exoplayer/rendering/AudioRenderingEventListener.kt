package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.rendering;

import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AudioRenderingEventListener
implements
	AudioRendererEventListener {

	private static final Logger logger = LoggerFactory.getLogger(AudioRenderingEventListener.class);

	@Override
	public void onAudioEnabled(DecoderCounters counters) {
		if (!logger.isDebugEnabled()) return;

		logger.debug("Audio decoder counters updated");
	}

	@Override
	public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs,
										  long initializationDurationMs) {
		if (!logger.isDebugEnabled()) return;

		logger.debug("Audio decoder initialized. " +
			"decoderName=" + decoderName + ", " +
			"initializedTimestampMs=" + initializedTimestampMs + ", " +
			"initializationDurationMs=" + initializationDurationMs);
	}

	@Override
	public void onAudioDisabled(DecoderCounters counters) {
		if (!logger.isDebugEnabled()) return;

		logger.debug("Audio disabled.");
	}
}
