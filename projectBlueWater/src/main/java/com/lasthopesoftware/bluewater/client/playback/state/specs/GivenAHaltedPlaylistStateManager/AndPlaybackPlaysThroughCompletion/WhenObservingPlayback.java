package com.lasthopesoftware.bluewater.client.playback.state.specs.GivenAHaltedPlaylistStateManager.AndPlaybackPlaysThroughCompletion;

import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.specs.fakes.FakeDeferredPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.playback.file.volume.IPlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.ResolveablePlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.state.PlaylistManager;
import com.lasthopesoftware.bluewater.client.playback.state.bootstrap.PlaylistPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.state.volume.PlaylistVolumeManager;
import com.namehillsoftware.handoff.promises.Promise;

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

public class WhenObservingPlayback {

	private static boolean isPlaying;
	private static PositionedPlaybackFile firstPlayingFile;
	private static boolean isCompleted;

	@BeforeClass
	public static void context() throws IOException, InterruptedException {
		final FakeDeferredPlaybackPreparerProvider fakePlaybackPreparerProvider = new FakeDeferredPlaybackPreparerProvider();

		final Library library = new Library();
		library.setId(1);

		final ISpecificLibraryProvider libraryProvider = mock(ISpecificLibraryProvider.class);
		when(libraryProvider.getLibrary()).thenReturn(new Promise<>(library));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.saveLibrary(any())).thenReturn(new Promise<>(library));

		final PlaylistManager playlistManager = new PlaylistManager(
			fakePlaybackPreparerProvider,
			() -> 1,
			Collections.singletonList(new CompletingFileQueueProvider()),
			new NowPlayingRepository(libraryProvider, libraryStorage),
			new PlaylistPlaybackBootstrapper(new PlaylistVolumeManager(1.0f), mock(IPlaybackHandlerVolumeControllerFactory.class)));

		final CountDownLatch countDownLatch = new CountDownLatch(5);

		playlistManager
			.setOnPlaybackStarted(p -> firstPlayingFile = p)
			.setOnPlayingFileChanged(p -> countDownLatch.countDown())
			.setOnPlaybackCompleted(() -> isCompleted = true)
			.startPlaylist(
				Arrays.asList(
					new ServiceFile(1),
					new ServiceFile(2),
					new ServiceFile(3),
					new ServiceFile(4),
					new ServiceFile(5)), 0, 0);

		ResolveablePlaybackHandler playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve();
		for (int i = 0; i < 4; i ++) {
			final ResolveablePlaybackHandler newPlayingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve();
			playingPlaybackHandler.resolve();
			playingPlaybackHandler = newPlayingPlaybackHandler;
		}
		playingPlaybackHandler.resolve();

		countDownLatch.await(1, TimeUnit.SECONDS);

		isPlaying = playlistManager.isPlaying();
	}

	@Test
	public void thenTheFirstPlayingFileIsTheFirstServiceFile() {
		assertThat(firstPlayingFile.getServiceFile()).isEqualTo(new ServiceFile(1));
	}

	@Test
	public void thenThePlaylistIsNotPlaying() {
		assertThat(isPlaying).isFalse();
	}

	@Test
	public void thenThePlaybackIsCompleted() {
		assertThat(isCompleted).isTrue();
	}
}
