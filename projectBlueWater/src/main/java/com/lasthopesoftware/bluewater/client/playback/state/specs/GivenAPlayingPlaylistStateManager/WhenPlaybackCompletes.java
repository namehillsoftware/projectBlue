package com.lasthopesoftware.bluewater.client.playback.state.specs.GivenAPlayingPlaylistStateManager;


import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlaying;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.specs.fakes.FakeDeferredPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.playback.file.volume.IPlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.state.PlaylistManager;
import com.lasthopesoftware.bluewater.client.playback.state.bootstrap.PlaylistPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.state.volume.PlaylistVolumeManager;
import com.lasthopesoftware.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenPlaybackCompletes {

	private static PlaylistManager playlistManager;
	private static NowPlaying nowPlaying;

	@BeforeClass
	public static void before() throws InterruptedException {
		final FakeDeferredPlaybackPreparerProvider fakePlaybackPreparerProvider = new FakeDeferredPlaybackPreparerProvider();

		final Library library = new Library();
		library.setId(1);

		final ISpecificLibraryProvider libraryProvider = mock(ISpecificLibraryProvider.class);
		when(libraryProvider.getLibrary()).thenReturn(new Promise<>(library));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.saveLibrary(any())).thenReturn(new Promise<>(library));

		final NowPlayingRepository nowPlayingRepository = new NowPlayingRepository(libraryProvider, libraryStorage);

		playlistManager = new PlaylistManager(
			fakePlaybackPreparerProvider,
			Collections.singletonList(new CompletingFileQueueProvider()),
			nowPlayingRepository,
			new PlaylistPlaybackBootstrapper(new PlaylistVolumeManager(1.0f), mock(IPlaybackHandlerVolumeControllerFactory.class)));

		playlistManager
			.startPlaylist(
				Arrays.asList(
					new ServiceFile(1),
					new ServiceFile(2),
					new ServiceFile(3),
					new ServiceFile(4),
					new ServiceFile(5)), 0, 0);

		fakePlaybackPreparerProvider.deferredResolution.resolve().resolve();
		fakePlaybackPreparerProvider.deferredResolution.resolve().resolve();
		fakePlaybackPreparerProvider.deferredResolution.resolve().resolve();
		fakePlaybackPreparerProvider.deferredResolution.resolve().resolve();
		fakePlaybackPreparerProvider.deferredResolution.resolve().resolve();

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		nowPlayingRepository
			.getNowPlaying()
			.next(np -> {
				nowPlaying = np;
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenThePlaybackStateIsNotPlaying() {
		assertThat(playlistManager.isPlaying()).isFalse();
	}

	@Test
	public void thenTheSavedFilePositionIsCorrect() {
		assertThat(nowPlaying.filePosition).isEqualTo(0);
	}

	@Test
	public void thenTheSavedPlaylistPositionIsCorrect() {
		assertThat(nowPlaying.playlistPosition).isEqualTo(0);
	}

	@Test
	public void thenTheSavedPlaylistIsCorrect() {
		assertThat(nowPlaying.playlist)
			.containsExactly(new ServiceFile(1),
				new ServiceFile(2),
				new ServiceFile(3),
				new ServiceFile(4),
				new ServiceFile(5));
	}
}
