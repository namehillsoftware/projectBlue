package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.ProvidePlayableFilePreparationSources
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.volume.ProvideMaxFileVolume

class MaxFileVolumePreparationProvider(
    private val preparationSourceProvider: ProvidePlayableFilePreparationSources,
    private val maxFileVolume: ProvideMaxFileVolume
) : ProvidePlayableFilePreparationSources by preparationSourceProvider {
    override fun providePlayableFilePreparationSource(): PlayableFilePreparationSource = MaxFileVolumePreparer(
		preparationSourceProvider.providePlayableFilePreparationSource(),
		maxFileVolume
	)
}
