package com.lasthopesoftware.bluewater.client.playback.engine.selection;

import com.namehillsoftware.handoff.promises.Promise;

public interface LookupSelectedPlaybackEngineType {
	Promise<PlaybackEngineType> promiseSelectedPlaybackEngineType();
}
