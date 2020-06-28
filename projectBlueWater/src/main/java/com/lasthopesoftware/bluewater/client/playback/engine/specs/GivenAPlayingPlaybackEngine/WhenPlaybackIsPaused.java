package com.lasthopesoftware.bluewater.client.playback.engine.specs.GivenAPlayingPlaybackEngine;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.storage.NowPlaying;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine;
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.specs.fakes.FakeDeferredPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.ResolvablePlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenPlaybackIsPaused {

	private static PlaybackEngine playbackEngine;
	private static NowPlaying nowPlaying;
	private static ResolvablePlaybackHandler resolveablePlaybackHandler;

	@BeforeClass
	public static void before() throws InterruptedException {
		final FakeDeferredPlayableFilePreparationSourceProvider fakePlaybackPreparerProvider = new FakeDeferredPlayableFilePreparationSourceProvider();

		final Library library = new Library();
		library.setId(1);

		final ISpecificLibraryProvider libraryProvider = mock(ISpecificLibraryProvider.class);
		when(libraryProvider.getLibrary()).thenReturn(new Promise<>(library));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.saveLibrary(any())).thenReturn(new Promise<>(library));

		final NowPlayingRepository nowPlayingRepository = new NowPlayingRepository(libraryProvider, libraryStorage);

		playbackEngine = new PlaybackEngine(
			new PreparedPlaybackQueueResourceManagement(
				fakePlaybackPreparerProvider,
				() -> 1),
			Collections.singletonList(new CompletingFileQueueProvider()),
			nowPlayingRepository,
			new PlaylistPlaybackBootstrapper(new PlaylistVolumeManager(1.0f)));

		playbackEngine
			.startPlaylist(
				Arrays.asList(
					new ServiceFile(1),
					new ServiceFile(2),
					new ServiceFile(3),
					new ServiceFile(4),
					new ServiceFile(5)), 0, 0);

		final ResolvablePlaybackHandler playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve();
		resolveablePlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve();
		playingPlaybackHandler.resolve();

		resolveablePlaybackHandler.setCurrentPosition(30);

		playbackEngine.pause();

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		nowPlayingRepository
			.getNowPlaying()
			.then(np -> {
				nowPlaying = np;
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenThePlayerIsNotPlaying() {
		assertThat(resolveablePlaybackHandler.isPlaying()).isFalse();
	}

	@Test
	public void thenThePlaybackStateIsNotPlaying() {
		assertThat(playbackEngine.isPlaying()).isFalse();
	}

	@Test
	public void thenTheSavedFilePositionIsCorrect() {
		assertThat(nowPlaying.filePosition).isEqualTo(30);
	}

	@Test
	public void thenTheSavedPlaylistPositionIsCorrect() {
		assertThat(nowPlaying.playlistPosition).isEqualTo(1);
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
