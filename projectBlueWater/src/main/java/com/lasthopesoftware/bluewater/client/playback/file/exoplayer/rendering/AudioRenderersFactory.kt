package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.rendering

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.audio.*
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.lasthopesoftware.bluewater.client.playback.file.rendering.LookupSilenceSkippingSettings
import com.lasthopesoftware.compilation.DebugFlag
import com.namehillsoftware.handoff.promises.Promise

class AudioRenderersFactory(private val context: Context, private val handler: Handler, private val lookupSilenceSkippingSettings: LookupSilenceSkippingSettings) : GetAudioRenderers {

	override fun newRenderers(): Promise<Array<MediaCodecAudioRenderer>> =
		newProcessors().then { processors ->
			arrayOf(
				MediaCodecAudioRenderer(
					context,
					MediaCodecSelector.DEFAULT,
					false,
					handler,
					if (DebugFlag.getInstance().isDebugCompilation) AudioRenderingEventListener() else EmptyRenderersListener,
					DefaultAudioSink(AudioCapabilities.getCapabilities(context), processors)))
		}

	private fun newProcessors(): Promise<Array<AudioProcessor>> =
		lookupSilenceSkippingSettings.promiseSkipSilenceIsEnabled().eventually { isEnabled ->
			if (!isEnabled) Promise(emptyArray())
			else newSilenceSkippingAudioProcessor().then { arrayOf(it) }
		}

	private fun newSilenceSkippingAudioProcessor(): Promise<SilenceSkippingAudioProcessor> =
		lookupSilenceSkippingSettings.promiseMinimumSilenceDuration().then { minimumSilenceDuration ->
			val silenceSkippingAudioProcessor = SilenceSkippingAudioProcessor(
				minimumSilenceDuration.millis * 1000,
				SilenceSkippingAudioProcessor.DEFAULT_PADDING_SILENCE_US,
				SilenceSkippingAudioProcessor.DEFAULT_SILENCE_THRESHOLD_LEVEL)
			silenceSkippingAudioProcessor.setEnabled(true)
			silenceSkippingAudioProcessor
		}
}
