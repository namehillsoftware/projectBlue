package com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults;

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType;
import com.namehillsoftware.handoff.promises.Promise;

public interface LookupDefaultPlaybackEngine {
	Promise<PlaybackEngineType> promiseDefaultEngineType();
}
