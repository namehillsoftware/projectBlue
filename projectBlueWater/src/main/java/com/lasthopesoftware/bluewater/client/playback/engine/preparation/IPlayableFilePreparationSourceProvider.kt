package com.lasthopesoftware.bluewater.client.playback.engine.preparation;

import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;

public interface IPlayableFilePreparationSourceProvider extends IPreparedPlaybackQueueConfiguration {
	PlayableFilePreparationSource providePlayableFilePreparationSource();
}
