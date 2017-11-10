package com.lasthopesoftware.bluewater.client.playback.file.preparation.specs.fakes;

import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPreparedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakePreparedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.ResolveablePlaybackHandler;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.MessengerOperator;
import com.namehillsoftware.handoff.promises.Promise;

public class FakeDeferredPlaybackPreparerProvider implements IPlaybackPreparerProvider {

	public final DeferredResolution deferredResolution = new DeferredResolution();

	@Override
	public IPlaybackPreparer providePlaybackPreparer() {
		return (file, preparedAt) -> new Promise<>(deferredResolution);
	}

	public static class DeferredResolution implements MessengerOperator<IPreparedPlaybackFile> {

		private Messenger<IPreparedPlaybackFile> resolve;

		public ResolveablePlaybackHandler resolve() {
			final ResolveablePlaybackHandler playbackHandler = new ResolveablePlaybackHandler();
			if (resolve != null)
				resolve.sendResolution(new FakePreparedPlaybackFile<>(playbackHandler));
			return playbackHandler;
		}

		@Override
		public void send(Messenger<IPreparedPlaybackFile> resolve) {
			this.resolve = resolve;
		}
	}
}
