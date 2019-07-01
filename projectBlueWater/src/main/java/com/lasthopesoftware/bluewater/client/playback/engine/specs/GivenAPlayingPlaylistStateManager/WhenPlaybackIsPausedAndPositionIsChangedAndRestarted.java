package com.lasthopesoftware.bluewater.client.playback.engine.specs.GivenAPlayingPlaylistStateManager;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlaying;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine;
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.specs.fakes.FakeDeferredPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.ResolveablePlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenPlaybackIsPausedAndPositionIsChangedAndRestarted {

	private static PlaybackEngine playbackEngine;
	private static NowPlaying nowPlaying;
	private static List<PositionedPlayingFile> positionedFiles = new ArrayList<>();

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
			.setOnPlayingFileChanged(f -> positionedFiles.add(f))
			.startPlaylist(
				Arrays.asList(
					new ServiceFile(1),
					new ServiceFile(2),
					new ServiceFile(3),
					new ServiceFile(4),
					new ServiceFile(5)), 0, 0);

		final ResolveablePlaybackHandler playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve();
		fakePlaybackPreparerProvider.deferredResolution.resolve();
		playingPlaybackHandler.resolve();

		playbackEngine.pause();

		final CountDownLatch countDownLatch = new CountDownLatch(1);

		playbackEngine
			.skipToNext()
			.eventually(p -> playbackEngine.skipToNext())
			.then(new VoidResponse<>(p -> playbackEngine.resume()))
			.then(obs -> fakePlaybackPreparerProvider.deferredResolution.resolve())
			.eventually(res -> nowPlayingRepository.getNowPlaying())
			.then(np -> {
				nowPlaying = np;
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenThePlaybackStateIsPlaying() {
		assertThat(playbackEngine.isPlaying()).isTrue();
	}

	@Test
	public void thenTheSavedPlaylistPositionIsCorrect() {
		assertThat(nowPlaying.playlistPosition).isEqualTo(3);
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

	@Test
	public void thenTheObservedFileIsCorrect() {
		assertThat(positionedFiles.get(positionedFiles.size() - 1).getPlaylistPosition()).isEqualTo(3);
	}

	@Test
	public void thenTheFirstSkippedFileIsOnlyObservedOnce() {
		assertThat(
			Stream.of(positionedFiles)
				.map(PositionedPlayingFile::asPositionedFile)
				.collect(Collectors.toList()))
			.containsOnlyOnce(new PositionedFile(1, new ServiceFile(2)));
	}

	@Test
	public void thenTheSecondSkippedFileIsNotObserved() {
		assertThat(
			Stream.of(positionedFiles)
				.map(PositionedPlayingFile::asPositionedFile)
				.collect(Collectors.toList()))
			.doesNotContain(new PositionedFile(2, new ServiceFile(3)));
	}
}
