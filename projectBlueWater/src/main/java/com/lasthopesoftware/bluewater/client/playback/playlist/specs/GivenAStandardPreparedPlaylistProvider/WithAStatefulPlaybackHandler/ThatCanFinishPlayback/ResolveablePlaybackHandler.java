package com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback;

import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.lasthopesoftware.promises.Promise;

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
