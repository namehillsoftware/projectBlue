package com.lasthopesoftware.bluewater.client.playback.engine.specs.GivenAPlayingPlaylistStateManager;

import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine;
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.specs.fakes.FakeDeferredPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.volume.IPlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.ResolveablePlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.state.volume.PlaylistVolumeManager;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenChangingTracks {

	private static PositionedFile nextSwitchedFile;
	private static PositionedPlayableFile latestFile;

	private static List<PositionedPlayableFile> startedFiles = new ArrayList<>();

	@BeforeClass
	public static void before() throws IOException, InterruptedException {
		final FakeDeferredPlayableFilePreparationSourceProvider fakePlaybackPreparerProvider = new FakeDeferredPlayableFilePreparationSourceProvider();

		final Library library = new Library();
		library.setId(1);

		final ISpecificLibraryProvider libraryProvider = mock(ISpecificLibraryProvider.class);
		when(libraryProvider.getLibrary()).thenReturn(new Promise<>(library));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.saveLibrary(any())).thenReturn(new Promise<>(library));

		final PlaybackEngine playbackEngine = new PlaybackEngine(
			fakePlaybackPreparerProvider,
			() -> 1,
			Collections.singletonList(new CompletingFileQueueProvider()),
			new NowPlayingRepository(libraryProvider, libraryStorage),
			new PlaylistPlaybackBootstrapper(new PlaylistVolumeManager(1.0f), mock(IPlaybackHandlerVolumeControllerFactory.class)));

		final CountDownLatch countDownLatch = new CountDownLatch(2);

		playbackEngine
			.setOnPlaybackStarted(p -> startedFiles.add(p))
			.setOnPlayingFileChanged(p -> {
				latestFile = p;
				countDownLatch.countDown();
			})
			.startPlaylist(
				Arrays.asList(
					new ServiceFile(1),
					new ServiceFile(2),
					new ServiceFile(3),
					new ServiceFile(4),
					new ServiceFile(5)), 0, 0);

		final ResolveablePlaybackHandler playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve();

		playbackEngine.changePosition(3, 0).then(p -> {
			nextSwitchedFile = p;
			countDownLatch.countDown();
			return null;
		});

		fakePlaybackPreparerProvider.deferredResolution.resolve();
		playingPlaybackHandler.resolve();

		countDownLatch.await();
	}

	@Test
	public void thenTheNextFileChangeIsTheSwitchedToTheCorrectTrackPosition() {
		assertThat(nextSwitchedFile.getPlaylistPosition()).isEqualTo(3);
	}

	@Test
	public void thenTheLatestObservedFileIsAtTheCorrectTrackPosition() {
		assertThat(latestFile.getPlaylistPosition()).isEqualTo(3);
	}

	@Test
	public void thenTheFirstStartedFileIsCorrect() {
		assertThat(startedFiles.get(0).asPositionedFile()).isEqualTo(new PositionedFile(0, new ServiceFile(1)));
	}

	@Test
	public void thenTheChangedStartedFileIsCorrect() {
		assertThat(startedFiles.get(1).asPositionedFile()).isEqualTo(new PositionedFile(3, new ServiceFile(4)));
	}

	@Test
	public void thenThePlaylistIsStartedTwice() {
		assertThat(startedFiles).hasSize(2);
	}
}
