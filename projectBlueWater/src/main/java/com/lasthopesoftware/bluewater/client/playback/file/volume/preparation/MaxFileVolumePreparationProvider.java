package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation;

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ProvideMaxFileVolume;

public class MaxFileVolumePreparationProvider implements IPlayableFilePreparationSourceProvider {

	private final IPlayableFilePreparationSourceProvider preparationSourceProvider;
	private final ProvideMaxFileVolume maxFileVolume;

	public MaxFileVolumePreparationProvider(IPlayableFilePreparationSourceProvider preparationSourceProvider, ProvideMaxFileVolume maxFileVolume) {
		this.preparationSourceProvider = preparationSourceProvider;
		this.maxFileVolume = maxFileVolume;
	}

	@Override
	public PlayableFilePreparationSource providePlayableFilePreparationSource() {
		return new MaxFileVolumePreparer(preparationSourceProvider.providePlayableFilePreparationSource(), maxFileVolume);
	}

	@Override
	public int getMaxQueueSize() {
		return preparationSourceProvider.getMaxQueueSize();
	}
}
