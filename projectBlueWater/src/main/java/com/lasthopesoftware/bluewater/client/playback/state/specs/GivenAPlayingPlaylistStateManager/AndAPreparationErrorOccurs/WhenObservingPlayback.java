package com.lasthopesoftware.bluewater.client.playback.state.specs.GivenAPlayingPlaylistStateManager.AndAPreparationErrorOccurs;

import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlaying;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPreparedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakePreparedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.IPlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.ResolveablePlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.queues.PreparationException;
import com.lasthopesoftware.bluewater.client.playback.state.PlaylistManager;
import com.lasthopesoftware.bluewater.client.playback.state.bootstrap.PlaylistPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.state.volume.PlaylistVolumeManager;
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenObservingPlayback {

	private static PreparationException error;
	private static NowPlaying nowPlaying;

	@BeforeClass
	public static void context() throws InterruptedException {
		final DeferredErrorPlaybackPreparer deferredErrorPlaybackPreparer = new DeferredErrorPlaybackPreparer();

		final IPlaybackPreparerProvider fakePlaybackPreparerProvider = () -> deferredErrorPlaybackPreparer;

		final Library library = new Library();
		library.setId(1);

		final ISpecificLibraryProvider libraryProvider = mock(ISpecificLibraryProvider.class);
		when(libraryProvider.getLibrary()).thenReturn(new Promise<>(library));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.saveLibrary(any())).thenReturn(new Promise<>(library));

		final NowPlayingRepository nowPlayingRepository = new NowPlayingRepository(libraryProvider, libraryStorage);
		final PlaylistManager playlistManager = new PlaylistManager(
			fakePlaybackPreparerProvider,
			() -> 1,
			Collections.singletonList(new CompletingFileQueueProvider()),
			nowPlayingRepository,
			new PlaylistPlaybackBootstrapper(new PlaylistVolumeManager(1.0f), mock(IPlaybackHandlerVolumeControllerFactory.class)));

		playlistManager
			.setOnPlaylistError(e -> {
				if (e instanceof PreparationException)
					error = (PreparationException)e;
			})
			.startPlaylist(
				Arrays.asList(
					new ServiceFile(1),
					new ServiceFile(2),
					new ServiceFile(3),
					new ServiceFile(4),
					new ServiceFile(5)), 0, 0);

		deferredErrorPlaybackPreparer.resolve().resolve();
		deferredErrorPlaybackPreparer.reject();

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		nowPlayingRepository.getNowPlaying()
			.then(np -> {
				nowPlaying = np;
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenTheErrorIsBroadcast() {
		assertThat(error).isNotNull();
	}

	@Test
	public void thenTheFilePositionIsSaved() {
		assertThat(nowPlaying.playlistPosition).isEqualTo(1);
	}

	private static class DeferredErrorPlaybackPreparer implements IPlaybackPreparer {

		private Messenger<IPreparedPlaybackFile> messenger;

		ResolveablePlaybackHandler resolve() {
			final ResolveablePlaybackHandler playbackHandler = new ResolveablePlaybackHandler();
			if (messenger != null)
				messenger.sendResolution(new FakePreparedPlaybackFile<>(playbackHandler));

			return playbackHandler;
		}

		void reject() {
			if (messenger != null)
				messenger.sendRejection(new Exception());
		}

		@Override
		public Promise<IPreparedPlaybackFile> promisePreparedPlaybackHandler(ServiceFile serviceFile, int preparedAt) {
			return new Promise<>(messenger -> this.messenger = messenger);
		}
	}
}
