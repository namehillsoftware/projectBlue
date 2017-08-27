package com.lasthopesoftware.bluewater.client.playback.file.preparation.specs.fakes;

import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.ResolveablePlaybackHandler;
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerOperator;
import com.lasthopesoftware.messenger.promises.Promise;

public class FakeDeferredPlaybackPreparerProvider implements IPlaybackPreparerProvider {

	public final DeferredResolution deferredResolution = new DeferredResolution();

	@Override
	public IPlaybackPreparer providePlaybackPreparer() {
		return (file, preparedAt) -> new Promise<>(deferredResolution);
	}

	public static class DeferredResolution implements MessengerOperator<IBufferingPlaybackHandler> {

		private Messenger<IBufferingPlaybackHandler> resolve;

		public ResolveablePlaybackHandler resolve() {
			final ResolveablePlaybackHandler playbackHandler = new ResolveablePlaybackHandler();
			if (resolve != null)
				resolve.sendResolution(playbackHandler);
			return playbackHandler;
		}

		@Override
		public void send(Messenger<IBufferingPlaybackHandler> resolve) {
			this.resolve = resolve;
		}
	}
}
