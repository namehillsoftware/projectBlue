package com.lasthopesoftware.bluewater.client.playback.state.specs.GivenAPlayingPlaylistStateManager;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.specs.fakes.FakeDeferredPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.state.PlaybackPlaylistStateManager;
import com.lasthopesoftware.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 3/12/17.
 */

public class WhenPlaybackIsPaused {

	private static PlaybackPlaylistStateManager playbackPlaylistStateManager;

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

		playbackPlaylistStateManager = new PlaybackPlaylistStateManager(
			fakePlaybackPreparerProvider,
			Collections.singletonList(new CompletingFileQueueProvider()),
			new NowPlayingRepository(libraryProvider, libraryStorage),
			cachedFilePropertiesProvider,
			1.0f);

		playbackPlaylistStateManager
			.startPlaylist(
				Arrays.asList(
					new ServiceFile(1),
					new ServiceFile(2),
					new ServiceFile(3),
					new ServiceFile(4),
					new ServiceFile(5)), 0, 0);

		fakePlaybackPreparerProvider.deferredResolution.resolve();

		playbackPlaylistStateManager.pause();
	}

	@Test
	public void thenThePlaybackStateIsNotPlaying() {
		assertThat(playbackPlaylistStateManager.isPlaying()).isFalse();
	}
}
