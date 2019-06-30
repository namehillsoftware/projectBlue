package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation;

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;

public class MaxFileVolumePreparationProvider implements IPlayableFilePreparationSourceProvider {

	private final IPlayableFilePreparationSourceProvider preparationSourceProvider;

	public MaxFileVolumePreparationProvider(IPlayableFilePreparationSourceProvider preparationSourceProvider) {
		this.preparationSourceProvider = preparationSourceProvider;
	}

	@Override
	public PlayableFilePreparationSource providePlayableFilePreparationSource() {
		return new MaxFileVolumePreparer(preparationSourceProvider.providePlayableFilePreparationSource());
	}

	@Override
	public int getMaxQueueSize() {
		return preparationSourceProvider.getMaxQueueSize();
	}
}
