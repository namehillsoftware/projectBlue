package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.rendering

import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.decoder.DecoderCounters
import org.slf4j.LoggerFactory

class AudioRenderingEventListener : AudioRendererEventListener {
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

    companion object {
        private val logger = LoggerFactory.getLogger(
            AudioRenderingEventListener::class.java
        )
    }
}
