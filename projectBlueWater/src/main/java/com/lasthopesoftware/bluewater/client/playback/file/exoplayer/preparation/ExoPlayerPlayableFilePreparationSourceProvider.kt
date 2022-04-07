package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.DefaultLoadControl
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.BestMatchUriProvider
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.exoplayer.ExoPlayerProvider
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.SpawnMediaSources
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.rendering.AudioRenderersFactory
import org.joda.time.Minutes

class ExoPlayerPlayableFilePreparationSourceProvider(
	private val context: Context,
	private val playbackHandler: Handler,
	private val eventHandler: Handler,
	private val mediaSourceProvider: SpawnMediaSources,
	private val bestMatchUriProvider: BestMatchUriProvider
) : IPlayableFilePreparationSourceProvider {

	companion object {
		private val maxBufferMs by lazy { Minutes.minutes(5).toStandardDuration().millis.toInt() }
		private val loadControl by lazy {
			val builder = DefaultLoadControl.Builder()
			builder
				.setBufferDurationsMs(
					DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
					maxBufferMs,
					DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
					DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
				.setPrioritizeTimeOverSizeThresholds(true)
			builder.build()
		}
	}

	private val renderersFactory = AudioRenderersFactory(context, eventHandler)

	private val exoPlayerProvider by lazy {
		ExoPlayerProvider(
			context,
			renderersFactory,
			loadControl,
			playbackHandler
		)
	}

	override fun getMaxQueueSize() = 1

	override fun providePlayableFilePreparationSource() = ExoPlayerPlaybackPreparer(
		mediaSourceProvider,
		exoPlayerProvider,
		eventHandler,
		bestMatchUriProvider
	)
}
