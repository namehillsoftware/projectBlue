package com.lasthopesoftware.bluewater.client.playback.engine;

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPreparedPlaybackQueueConfiguration;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparerProvider;

public interface PlaybackEngine extends IPlaybackPreparerProvider, IPreparedPlaybackQueueConfiguration {
}
