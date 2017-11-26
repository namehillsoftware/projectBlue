package com.lasthopesoftware.bluewater.client.playback.engine;

import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.playback.queues.IPreparedPlaybackQueueConfiguration;

public interface BuildPlaybackEngines {
	<Engine extends IPlaybackPreparerProvider & IPreparedPlaybackQueueConfiguration> Engine build();
}
