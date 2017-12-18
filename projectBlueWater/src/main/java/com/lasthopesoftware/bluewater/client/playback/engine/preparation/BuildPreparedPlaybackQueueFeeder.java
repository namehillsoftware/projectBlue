package com.lasthopesoftware.bluewater.client.playback.engine.preparation;

import com.lasthopesoftware.bluewater.client.library.repository.Library;

public interface BuildPreparedPlaybackQueueFeeder {
	IPlaybackPreparerProvider build(Library library);
}
