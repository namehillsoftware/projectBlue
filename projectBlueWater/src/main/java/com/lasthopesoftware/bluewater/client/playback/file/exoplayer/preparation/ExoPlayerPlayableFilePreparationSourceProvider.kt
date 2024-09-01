package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation

import android.content.Context
import android.os.Handler
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.LoadControl
import com.lasthopesoftware.bluewater.client.browsing.files.uri.BestMatchUriProvider
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.exoplayer.ExoPlayerProvider
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.SpawnMediaSources
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.rendering.AudioRenderersFactory

@UnstableApi class ExoPlayerPlayableFilePreparationSourceProvider(
	private val context: Context,
	private val loadControl: LoadControl,
	private val playbackHandler: Handler,
	private val eventHandler: Handler,
	private val mediaSourceProvider: SpawnMediaSources,
	private val bestMatchUriProvider: BestMatchUriProvider
) : IPlayableFilePreparationSourceProvider {
	private val renderersFactory = AudioRenderersFactory(context)

	private val exoPlayerProvider by lazy {
		ExoPlayerProvider(
			context,
			renderersFactory,
			loadControl,
			playbackHandler
		)
	}

    override val maxQueueSize get() = 1

	override fun providePlayableFilePreparationSource() = ExoPlayerPlaybackPreparer(
		mediaSourceProvider,
		exoPlayerProvider,
		playbackHandler,
		eventHandler,
		bestMatchUriProvider
	)
}
