package com.lasthopesoftware.bluewater.client.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakePreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.fakes.ResolvablePlaybackHandler;
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

		public ResolvablePlaybackHandler resolve() {
			final ResolvablePlaybackHandler playbackHandler = new ResolvablePlaybackHandler();
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
