package com.lasthopesoftware.bluewater.client.playback.service.specs.GivenAStandardPlaylistStateManager.AndPlaybackIsStarted;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PositionedFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackPlaylistStateManager;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 3/11/17.
 */

public class WhenNotObservingPlayback {
	private static Throwable caughtException;

	@BeforeClass
	public static void context() {
		final FakePlaybackPreparerProvider fakePlaybackPreparerProvider = new FakePlaybackPreparerProvider();

		final Library library = new Library();
		library.setId(1);

		final ISpecificLibraryProvider libraryProvider = mock(ISpecificLibraryProvider.class);
		when(libraryProvider.getLibrary()).thenReturn(new Promise<>(library));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.saveLibrary(any())).thenReturn(new Promise<>(library));

		final PlaybackPlaylistStateManager playbackPlaylistStateManager = new PlaybackPlaylistStateManager(
			mock(IConnectionProvider.class),
			fakePlaybackPreparerProvider,
			new PositionedFileQueueProvider(),
			new NowPlayingRepository(libraryProvider, libraryStorage),
			1.0f);

		try {
			playbackPlaylistStateManager
				.startPlaylist(
					Arrays.asList(
						new File(1),
						new File(2),
						new File(3),
						new File(4),
						new File(5)), 0, 0);

			fakePlaybackPreparerProvider.deferredResolution.resolve();
		} catch (NullPointerException e) {
			caughtException = e;
		}
	}

	@Test
	public void thenAnExceptionIsNotThrown() {
		assertThat(caughtException).isNull();
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
			final IBufferingPlaybackHandler playbackHandler = new FakeBufferingPlaybackHandler();
			if (resolve != null)
				resolve.withResult(playbackHandler);
			return playbackHandler;
		}

		@Override
		public void runWith(IResolvedPromise<IBufferingPlaybackHandler> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
			this.resolve = resolve;
		}
	}
}
