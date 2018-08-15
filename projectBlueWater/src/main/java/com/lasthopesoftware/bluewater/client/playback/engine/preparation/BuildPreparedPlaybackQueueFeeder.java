package com.lasthopesoftware.bluewater.client.playback.engine.preparation;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

public interface BuildPreparedPlaybackQueueFeeder {
	Promise<IPlayableFilePreparationSourceProvider> build(Library library);
}
