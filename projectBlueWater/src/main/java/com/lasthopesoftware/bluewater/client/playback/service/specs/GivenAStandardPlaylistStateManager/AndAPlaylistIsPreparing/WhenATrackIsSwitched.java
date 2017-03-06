package com.lasthopesoftware.bluewater.client.playback.service.specs.GivenAStandardPlaylistStateManager.AndAPlaylistIsPreparing;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.INowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPositionedFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.StatefulPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackPlaylistStateManager;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

import static org.mockito.Mockito.mock;

public class WhenATrackIsSwitched {

	@BeforeClass
	public static void before() {
		FakePlaybackPreparerProvider fakePlaybackPreparerProvider = new FakePlaybackPreparerProvider();

		final PlaybackPlaylistStateManager playbackPlaylistStateManager =
			new PlaybackPlaylistStateManager(
				mock(IConnectionProvider.class),
				fakePlaybackPreparerProvider,
				mock(IPositionedFileQueueProvider.class),
				mock(INowPlayingRepository.class),
				1.0f);

		playbackPlaylistStateManager.startPlaylist(
			Arrays.asList(
				new File(1),
				new File(2),
				new File(3),
				new File(4),
				new File(5)), 0, 0);

		fakePlaybackPreparerProvider.deferredResolution.resolve();
	}

	@Test
	public void thenThePreviousPlaylistPreparationIsCancelled() {

	}

	@Test
	public void thenTheNextPlaylistIsPrepared() {

	}

	private static class FakePlaybackPreparerProvider implements IPlaybackPreparerProvider {

		final DeferredResolution deferredResolution = new DeferredResolution();

		@Override
		public IPlaybackPreparer providePlaybackPreparer() {
			return (file, preparedAt) -> new Promise<>(deferredResolution);
		}
	}

	private static class DeferredResolution implements ThreeParameterAction<IResolvedPromise<IBufferingPlaybackHandler>, IRejectedPromise, OneParameterAction<Runnable>> {

		private IResolvedPromise<IBufferingPlaybackHandler> resolve;

		public IBufferingPlaybackHandler resolve() {
			final IBufferingPlaybackHandler playbackHandler = new StatefulPlaybackHandler();
			this.resolve.withResult(playbackHandler);
			return playbackHandler;
		}

		@Override
		public void runWith(IResolvedPromise<IBufferingPlaybackHandler> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
			this.resolve = resolve;
		}
	}
}
