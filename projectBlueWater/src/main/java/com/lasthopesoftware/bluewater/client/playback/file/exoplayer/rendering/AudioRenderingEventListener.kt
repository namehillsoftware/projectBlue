package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.rendering

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import com.lasthopesoftware.bluewater.shared.lazyLogger

@UnstableApi class AudioRenderingEventListener : AudioRendererEventListener {

	companion object {
		private val logger by lazyLogger<AudioRenderingEventListener>()
	}

    override fun onAudioEnabled(counters: DecoderCounters) {
        if (!logger.isDebugEnabled) return
        logger.debug("Audio decoder counters updated")
    }

    override fun onAudioDecoderInitialized(
        decoderName: String, initializedTimestampMs: Long,
        initializationDurationMs: Long
    ) {
        if (!logger.isDebugEnabled) return
        logger.debug(
            "Audio decoder initialized. " +
                    "decoderName=" + decoderName + ", " +
                    "initializedTimestampMs=" + initializedTimestampMs + ", " +
                    "initializationDurationMs=" + initializationDurationMs
        )
    }

    override fun onAudioDisabled(counters: DecoderCounters) {
        if (!logger.isDebugEnabled) return
        logger.debug("Audio disabled.")
    }
}
