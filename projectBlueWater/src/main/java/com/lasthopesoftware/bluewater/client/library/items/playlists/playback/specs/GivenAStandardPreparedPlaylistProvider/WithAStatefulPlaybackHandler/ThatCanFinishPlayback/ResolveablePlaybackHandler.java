package com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback;

import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.lasthopesoftware.promises.Promise;

/**
 * Created by david on 12/7/16.
 */

public class ResolveablePlaybackHandler extends FakeBufferingPlaybackHandler {

	private IResolvedPromise<IPlaybackHandler> resolve;

	public void resolve() {
		if (this.resolve != null)
			this.resolve.sendResolution(this);

		this.resolve = null;
	}

	@Override
	public Promise<IPlaybackHandler> promisePlayback() {
		super.promisePlayback();
		return new Promise<>((resolve, reject) -> this.resolve = resolve);
	}
}
