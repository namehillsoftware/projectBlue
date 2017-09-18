package com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback;

import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.Promise;

public class ResolveablePlaybackHandler extends FakeBufferingPlaybackHandler {

	private final Promise<IPlaybackHandler> promise;
	private Messenger<IPlaybackHandler> resolve;

	public ResolveablePlaybackHandler() {
		promise = new Promise<>((messenger) -> this.resolve = messenger);
	}

	public void resolve() {
		if (this.resolve != null)
			this.resolve.sendResolution(this);

		this.resolve = null;
	}

	@Override
	public Promise<IPlaybackHandler> promisePlayback() {
		super.promisePlayback();
		return promise;
	}
}
