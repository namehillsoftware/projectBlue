package com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults;

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType;
import com.namehillsoftware.handoff.promises.Promise;

public class DefaultPlaybackEngineLookup implements LookupDefaultPlaybackEngine {
	@Override
	public Promise<PlaybackEngineType> promiseDefaultEngineType() {
		return new Promise<>(PlaybackEngineType.ExoPlayer);
	}
}
