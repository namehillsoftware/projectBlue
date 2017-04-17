package com.lasthopesoftware.bluewater.client.playback.service.specs.GivenAHaltedPlaylistStateManager.AndAPlaylistIsPreparing;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.specs.fakes.FakeDeferredPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackPlaylistStateManager;
import com.lasthopesoftware.promises.Promise;

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
		final FakeDeferredPlaybackPreparerProvider fakePlaybackPreparerProvider = new FakeDeferredPlaybackPreparerProvider();

		final Library library = new Library();
		library.setId(1);

		final ISpecificLibraryProvider libraryProvider = mock(ISpecificLibraryProvider.class);
		when(libraryProvider.getLibrary()).thenReturn(new Promise<>(library));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.saveLibrary(any())).thenReturn(new Promise<>(library));

		final IConnectionProvider connectionProvider = mock(IConnectionProvider.class);

		final IFilePropertiesContainerRepository filePropertiesContainerRepository = FilePropertyCache.getInstance();
		final CachedFilePropertiesProvider cachedFilePropertiesProvider =
			new CachedFilePropertiesProvider(
				connectionProvider,
				filePropertiesContainerRepository,
				new FilePropertiesProvider(
					connectionProvider,
					filePropertiesContainerRepository));

		final PlaybackPlaylistStateManager playbackPlaylistStateManager = new PlaybackPlaylistStateManager(
			fakePlaybackPreparerProvider,
			new CompletingFileQueueProvider(),
			new NowPlayingRepository(libraryProvider, libraryStorage),
			cachedFilePropertiesProvider,
			1.0f);

		Observable.create(playbackPlaylistStateManager).subscribe(p -> nextSwitchedFile = p);

		playbackPlaylistStateManager
			.startPlaylist(
				Arrays.asList(
					new ServiceFile(1),
					new ServiceFile(2),
					new ServiceFile(3),
					new ServiceFile(4),
					new ServiceFile(5)), 0, 0);

		playbackPlaylistStateManager.changePosition(3, 0);

		fakePlaybackPreparerProvider.deferredResolution.resolve();
	}

	@Test
	public void thenTheNextFileChangeIsTheSwitchedToTheCorrectTrackPosition() {
		assertThat(nextSwitchedFile.getPlaylistPosition()).isEqualTo(3);
	}
}
