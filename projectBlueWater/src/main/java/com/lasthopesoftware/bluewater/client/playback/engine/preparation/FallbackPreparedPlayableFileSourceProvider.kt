package com.lasthopesoftware.bluewater.client.playback.engine.preparation

import com.lasthopesoftware.bluewater.client.playback.file.preparation.FallbackPlaybackPreparer
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource

class FallbackPreparedPlayableFileSourceProvider(
	private val primaryProvider: ProvidePlayableFilePreparationSources,
	private val fallbackProvider: ProvidePlayableFilePreparationSources,
) : ProvidePlayableFilePreparationSources {
	override fun providePlayableFilePreparationSource(): PlayableFilePreparationSource = FallbackPlaybackPreparer(
		primaryProvider.providePlayableFilePreparationSource(),
		fallbackProvider.providePlayableFilePreparationSource()
	)
}
