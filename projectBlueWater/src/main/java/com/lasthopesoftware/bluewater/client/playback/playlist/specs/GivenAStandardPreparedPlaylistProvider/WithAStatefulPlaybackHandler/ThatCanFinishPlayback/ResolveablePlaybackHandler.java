package com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback;

import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.Promise;

public class ResolveablePlaybackHandler extends FakeBufferingPlaybackHandler {

	private final Promise<PlayableFile> promise;
	private Messenger<PlayableFile> resolve;

	public ResolveablePlaybackHandler() {
		promise = new Promise<>((messenger) -> this.resolve = messenger);
	}

	public void resolve() {
		if (this.resolve != null)
			this.resolve.sendResolution(this);

		this.resolve = null;
	}

	@Override
	public Promise<PlayableFile> promisePlayback() {
		super.promisePlayback();
		return promise;
	}
}
