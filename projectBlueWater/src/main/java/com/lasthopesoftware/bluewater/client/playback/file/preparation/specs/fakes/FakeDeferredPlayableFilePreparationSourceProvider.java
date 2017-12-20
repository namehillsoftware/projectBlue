package com.lasthopesoftware.bluewater.client.playback.file.preparation.specs.fakes;

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakePreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.ResolveablePlaybackHandler;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.MessengerOperator;
import com.namehillsoftware.handoff.promises.Promise;

public class FakeDeferredPlayableFilePreparationSourceProvider implements IPlayableFilePreparationSourceProvider {

	public final DeferredResolution deferredResolution = new DeferredResolution();

	@Override
	public PlayableFilePreparationSource providePlayableFilePreparationSource() {
		return (file, preparedAt) -> new Promise<>(deferredResolution);
	}

	@Override
	public int getMaxQueueSize() {
		return 1;
	}

	public static class DeferredResolution implements MessengerOperator<PreparedPlayableFile> {

		private Messenger<PreparedPlayableFile> resolve;

		public ResolveablePlaybackHandler resolve() {
			final ResolveablePlaybackHandler playbackHandler = new ResolveablePlaybackHandler();
			if (resolve != null)
				resolve.sendResolution(new FakePreparedPlayableFile<>(playbackHandler));
			return playbackHandler;
		}

		@Override
		public void send(Messenger<PreparedPlayableFile> resolve) {
			this.resolve = resolve;
		}
	}
}
