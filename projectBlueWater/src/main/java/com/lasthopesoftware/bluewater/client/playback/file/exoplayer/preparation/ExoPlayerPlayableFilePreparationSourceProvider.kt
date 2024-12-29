package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation

import android.content.Context
import android.os.Handler
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import com.lasthopesoftware.bluewater.client.browsing.files.uri.BestMatchUriProvider
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.exoplayer.ExoPlayerProvider
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.BufferingExoPlayerProvider
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.SpawnMediaSources
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.rendering.AudioRenderersFactory
import com.lasthopesoftware.resources.executors.HandlerExecutor
import org.joda.time.Minutes

@UnstableApi class ExoPlayerPlayableFilePreparationSourceProvider(
	private val context: Context,
	private val playbackHandler: Handler,
	private val interactionsHandler: HandlerExecutor,
	private val mediaSourceProvider: SpawnMediaSources,
	private val bestMatchUriProvider: BestMatchUriProvider
) : IPlayableFilePreparationSourceProvider {
	companion object {
		private val maxBufferMs by lazy { Minutes.minutes(5).toStandardDuration().millis.toInt() }
	}

	private val renderersFactory = AudioRenderersFactory(context)

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

	private val exoPlayerProvider by lazy {
		ExoPlayerProvider(
			context,
			renderersFactory,
			loadControl,
			interactionsHandler,
			playbackHandler
		)
	}

	private val bufferingExoPlayerProvider by lazy {
		BufferingExoPlayerProvider(interactionsHandler)
	}

    override val maxQueueSize get() = 1

	override fun providePlayableFilePreparationSource() = ExoPlayerPlaybackPreparer(
		mediaSourceProvider,
		exoPlayerProvider,
		bufferingExoPlayerProvider,
		bestMatchUriProvider
	)
}
