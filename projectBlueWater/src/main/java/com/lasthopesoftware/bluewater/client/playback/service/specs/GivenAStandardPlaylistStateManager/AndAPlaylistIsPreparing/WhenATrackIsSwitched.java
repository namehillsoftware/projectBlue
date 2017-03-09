package com.lasthopesoftware.bluewater.client.playback.service.specs.GivenAStandardPlaylistStateManager.AndAPlaylistIsPreparing;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPositionedFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.StatefulPlaybackHandler;
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

import io.reactivex.Observable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenATrackIsSwitched {

	private static PositionedPlaybackFile nextSwitchedFile;

	@BeforeClass
	public static void before() {
		FakePlaybackPreparerProvider fakePlaybackPreparerProvider = new FakePlaybackPreparerProvider();

		final Library library = new Library();
		library.setId(1);

		final ISpecificLibraryProvider libraryProvider = mock(ISpecificLibraryProvider.class);
		when(libraryProvider.getLibrary()).thenReturn(new Promise<>(library));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.saveLibrary(any())).thenReturn(new Promise<>(library));

		final PlaybackPlaylistStateManager playbackPlaylistStateManager = new PlaybackPlaylistStateManager(
			mock(IConnectionProvider.class),
			fakePlaybackPreparerProvider,
			mock(IPositionedFileQueueProvider.class),
			new NowPlayingRepository(libraryProvider, libraryStorage),
			1.0f);

		final Observable<PositionedPlaybackFile> trackChanges = Observable.create(playbackPlaylistStateManager);


		playbackPlaylistStateManager
			.startPlaylist(
				Arrays.asList(
					new File(1),
					new File(2),
					new File(3),
					new File(4),
					new File(5)), 0, 0);

		fakePlaybackPreparerProvider.deferredResolution.resolve();

		playbackPlaylistStateManager.changePosition(3, 0);

		nextSwitchedFile = trackChanges.blockingFirst();
	}

	@Test
	public void thenTheNextFileChangeIsTheSwitchedToTheCorrectTrackPosition() {
		assertThat(nextSwitchedFile.getPosition()).isEqualTo(3);
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
