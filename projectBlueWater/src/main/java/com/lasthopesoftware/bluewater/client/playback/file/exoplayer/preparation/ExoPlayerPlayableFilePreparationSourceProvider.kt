package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.BestMatchUriProvider
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.AudioRenderersFactory
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.SpawnMediaSources
import org.joda.time.Minutes

class ExoPlayerPlayableFilePreparationSourceProvider(private val context: Context, private val handler: Handler, private val mediaSourceProvider: SpawnMediaSources, private val bestMatchUriProvider: BestMatchUriProvider) : IPlayableFilePreparationSourceProvider {

	companion object {
		private val maxBufferMs = lazy { Minutes.minutes(5).toStandardDuration().millis.toInt() }
		private val loadControl = lazy {
			val builder = DefaultLoadControl.Builder()
			builder.setBufferDurationsMs(
				DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
				maxBufferMs.value,
				DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
				DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
			builder.createDefaultLoadControl()
		}
	}

	private val trackSelector = lazy { DefaultTrackSelector(context) }

	private val renderersFactory = AudioRenderersFactory(context, handler)

	override fun getMaxQueueSize() = 1

	override fun providePlayableFilePreparationSource() = ExoPlayerPlaybackPreparer(
		context,
		mediaSourceProvider,
		trackSelector.value,
		loadControl.value,
		renderersFactory,
		handler,
		bestMatchUriProvider)
}
