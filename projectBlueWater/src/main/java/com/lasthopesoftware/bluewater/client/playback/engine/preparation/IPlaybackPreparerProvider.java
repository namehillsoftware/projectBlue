package com.lasthopesoftware.bluewater.client.playback.engine.preparation;

import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparer;

public interface IPlaybackPreparerProvider extends IPreparedPlaybackQueueConfiguration {
	IPlaybackPreparer providePlaybackPreparer();
}
