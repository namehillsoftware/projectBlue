package com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback;

import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.Promise;

public class ResolveablePlaybackHandler extends FakeBufferingPlaybackHandler {

	private final Promise<PlayedFile> promise;
	private Messenger<PlayedFile> resolve;

	public ResolveablePlaybackHandler() {
		promise = new Promise<>((messenger) -> this.resolve = messenger);
	}

	public void resolve() {
		if (resolve != null)
			resolve.sendResolution(this);

		resolve = null;
	}

	public Promise<PlayedFile> getPromise() {
		return promise;
	}
}
