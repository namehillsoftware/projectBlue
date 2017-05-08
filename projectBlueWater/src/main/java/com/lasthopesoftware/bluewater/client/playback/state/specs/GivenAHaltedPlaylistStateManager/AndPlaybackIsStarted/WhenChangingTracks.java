package com.lasthopesoftware.bluewater.client.playback.state.specs.GivenAHaltedPlaylistStateManager.AndPlaybackIsStarted;

import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.specs.fakes.FakeDeferredPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.playback.file.volume.IPlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.state.PlaylistManager;
import com.lasthopesoftware.bluewater.client.playback.state.bootstrap.PlaylistPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.state.volume.PlaylistVolumeManager;
import com.lasthopesoftware.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 3/13/17.
 */

public class WhenChangingTracks {

	private static PositionedFile nextSwitchedFile;
	private static PositionedPlaybackFile latestFile;

	@BeforeClass
	public static void before() throws IOException, InterruptedException {
		final FakeDeferredPlaybackPreparerProvider fakePlaybackPreparerProvider = new FakeDeferredPlaybackPreparerProvider();

		final Library library = new Library();
		library.setId(1);

		final ISpecificLibraryProvider libraryProvider = mock(ISpecificLibraryProvider.class);
		when(libraryProvider.getLibrary()).thenReturn(new Promise<>(library));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.saveLibrary(any())).thenReturn(new Promise<>(library));

		final PlaylistManager playlistManager = new PlaylistManager(
			fakePlaybackPreparerProvider,
			Collections.singletonList(new CompletingFileQueueProvider()),
			new NowPlayingRepository(libraryProvider, libraryStorage),
			new PlaylistPlaybackBootstrapper(new PlaylistVolumeManager(1.0f), mock(IPlaybackHandlerVolumeControllerFactory.class)));

		final CountDownLatch countDownLatch = new CountDownLatch(2);

		playlistManager
			.startPlaylist(
				Arrays.asList(
					new ServiceFile(1),
					new ServiceFile(2),
					new ServiceFile(3),
					new ServiceFile(4),
					new ServiceFile(5)), 0, 0)
			.then(obs -> obs.subscribe(p -> {
				latestFile = p;
				countDownLatch.countDown();
			}));

		fakePlaybackPreparerProvider.deferredResolution.resolve().resolve();

		playlistManager.changePosition(3, 0).then(p -> {
			nextSwitchedFile = p;
			countDownLatch.countDown();
			return null;
		});

		fakePlaybackPreparerProvider.deferredResolution.resolve();

		countDownLatch.await(1, TimeUnit.SECONDS);
	}

	@Test
	public void thenTheNextFileChangeIsTheSwitchedToTheCorrectTrackPosition() {
		assertThat(nextSwitchedFile.getPlaylistPosition()).isEqualTo(3);
	}

	@Test
	public void thenTheLatestObservedFileIsAtTheCorrectTrackPosition() {
		assertThat(latestFile.getPlaylistPosition()).isEqualTo(3);
	}
}
