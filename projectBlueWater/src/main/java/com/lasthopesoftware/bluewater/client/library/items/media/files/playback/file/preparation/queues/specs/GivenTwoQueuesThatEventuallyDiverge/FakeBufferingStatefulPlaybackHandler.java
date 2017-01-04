package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.specs.GivenTwoQueuesThatEventuallyDiverge;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.StatefulPlaybackHandler;
import com.lasthopesoftware.promises.ExpectedPromise;
import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 1/4/17.
 */
class FakeBufferingStatefulPlaybackHandler extends StatefulPlaybackHandler {
	@Override
	public IPromise<IBufferingPlaybackHandler> bufferPlaybackFile() {
		return new ExpectedPromise<>(() -> FakeBufferingStatefulPlaybackHandler.this);
	}
}
