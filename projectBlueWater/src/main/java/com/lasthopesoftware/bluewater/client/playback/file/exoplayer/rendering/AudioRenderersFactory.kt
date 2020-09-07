package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.rendering

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.audio.AudioCapabilities
import com.google.android.exoplayer2.audio.DefaultAudioSink
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.audio.SilenceSkippingAudioProcessor
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.lasthopesoftware.bluewater.client.playback.file.rendering.LookupSilenceSkippingSettings
import com.lasthopesoftware.compilation.DebugFlag
import com.namehillsoftware.handoff.promises.Promise

class AudioRenderersFactory(private val context: Context, private val handler: Handler, private val lookupSilenceSkippingSettings: LookupSilenceSkippingSettings) : GetAudioRenderers {

	override fun newRenderers(): Promise<Array<MediaCodecAudioRenderer>> =
		lookupSilenceSkippingSettings.promiseSkipSilenceIsEnabled().then { isEnabled ->
			val processors = if (isEnabled) arrayOf(newSilenceSkippingAudioProcessor()) else emptyArray()

			arrayOf(
				MediaCodecAudioRenderer(
					context,
					MediaCodecSelector.DEFAULT,
					false,
					handler,
					if (DebugFlag.getInstance().isDebugCompilation) AudioRenderingEventListener() else EmptyRenderersListener,
					DefaultAudioSink(AudioCapabilities.getCapabilities(context), processors)))
		}

	private fun newSilenceSkippingAudioProcessor(): SilenceSkippingAudioProcessor {
		val silenceSkippingAudioProcessor = SilenceSkippingAudioProcessor()
		silenceSkippingAudioProcessor.setEnabled(true)
		return silenceSkippingAudioProcessor
	}
}
