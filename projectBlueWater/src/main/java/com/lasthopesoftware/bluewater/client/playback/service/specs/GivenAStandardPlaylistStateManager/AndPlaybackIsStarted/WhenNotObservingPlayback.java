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
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.ResolveablePlaybackHandler;
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
	private static Library library;
	private static PlaybackPlaylistStateManager playbackPlaylistStateManager;

	@BeforeClass
	public static void context() {
		final FakePlaybackPreparerProvider fakePlaybackPreparerProvider = new FakePlaybackPreparerProvider();

		library = new Library();
		library.setId(1);
		library.setNowPlayingId(5);

		final ISpecificLibraryProvider libraryProvider = mock(ISpecificLibraryProvider.class);
		when(libraryProvider.getLibrary()).thenReturn(new Promise<>(library));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.saveLibrary(any())).thenReturn(new Promise<>(library));

		playbackPlaylistStateManager = new PlaybackPlaylistStateManager(
			mock(IConnectionProvider.class),
			fakePlaybackPreparerProvider,
			new PositionedFileQueueProvider(),
			new NowPlayingRepository(libraryProvider, libraryStorage),
			1.0f);

		playbackPlaylistStateManager
			.startPlaylist(
				Arrays.asList(
					new File(1),
					new File(2),
					new File(3),
					new File(4),
					new File(5)), 0, 0);

		final ResolveablePlaybackHandler resolveablePlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve();
		resolveablePlaybackHandler.resolve();

		fakePlaybackPreparerProvider.deferredResolution.resolve();
	}

	@Test
	public void thenTheSavedTrackPositionIsZero() {
		assertThat(library.getNowPlayingId()).isEqualTo(1);
	}

	@Test
	public void thenTheManagerIsPlaying() {
		assertThat(playbackPlaylistStateManager.isPlaying()).isTrue();
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

		public ResolveablePlaybackHandler resolve() {
			final ResolveablePlaybackHandler playbackHandler = new ResolveablePlaybackHandler();
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
