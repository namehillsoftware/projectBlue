package com.lasthopesoftware.bluewater.client.playback.service.state.specs.GivenAPlayingPlaylistStateManager;

import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.specs.fakes.FakeDeferredPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.service.state.ActivePlaylist;
import com.lasthopesoftware.bluewater.client.playback.service.state.IPausedPlaylist;
import com.lasthopesoftware.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.vedsoft.futures.callables.VoidFunc.runCarelessly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 3/12/17.
 */

public class WhenPlaybackIsPaused {

	private static IPausedPlaylist pausedPlaylist;

	@BeforeClass
	public static void before() throws InterruptedException {
		final FakeDeferredPlaybackPreparerProvider fakePlaybackPreparerProvider = new FakeDeferredPlaybackPreparerProvider();

		final Library library = new Library();
		library.setId(1);

		final ISpecificLibraryProvider libraryProvider = mock(ISpecificLibraryProvider.class);
		when(libraryProvider.getLibrary()).thenReturn(new Promise<>(library));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.saveLibrary(any())).thenReturn(new Promise<>(library));

		final ActivePlaylist activePlaylist = new ActivePlaylist(
			fakePlaybackPreparerProvider,
			new NowPlayingRepository(libraryProvider, libraryStorage),
			new CompletingFileQueueProvider(),
			Arrays.asList(
				new ServiceFile(1),
				new ServiceFile(2),
				new ServiceFile(3),
				new ServiceFile(4),
				new ServiceFile(5)));

		fakePlaybackPreparerProvider.deferredResolution.resolve();

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		activePlaylist
			.pause()
			.then(runCarelessly(paused -> {
				pausedPlaylist = paused;
				countDownLatch.countDown();
			}));

		countDownLatch.await(1, TimeUnit.SECONDS);
	}

	@Test
	public void thenThePausedPlaylistIsReturned() {
		assertThat(pausedPlaylist).isNotNull();
	}
}
