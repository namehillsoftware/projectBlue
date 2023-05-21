package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.volume.ProvideMaxFileVolume

class MaxFileVolumePreparationProvider(
    private val preparationSourceProvider: IPlayableFilePreparationSourceProvider,
    private val maxFileVolume: ProvideMaxFileVolume
) : IPlayableFilePreparationSourceProvider by preparationSourceProvider {
    override fun providePlayableFilePreparationSource(): PlayableFilePreparationSource = MaxFileVolumePreparer(
		preparationSourceProvider.providePlayableFilePreparationSource(),
		maxFileVolume
	)
}
