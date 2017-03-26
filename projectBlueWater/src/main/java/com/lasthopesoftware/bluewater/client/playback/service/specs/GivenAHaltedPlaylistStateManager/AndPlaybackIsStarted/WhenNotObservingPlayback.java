package com.lasthopesoftware.bluewater.client.playback.service.specs.GivenAHaltedPlaylistStateManager.AndPlaybackIsStarted;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PositionedFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.specs.fakes.FakeDeferredPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.ResolveablePlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackPlaylistStateManager;
import com.lasthopesoftware.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 3/11/17.
 */

public class WhenNotObservingPlayback {
	private static Library library;
	private static PlaybackPlaylistStateManager playbackPlaylistStateManager;

	@BeforeClass
	public static void context() {
		final FakeDeferredPlaybackPreparerProvider fakePlaybackPreparerProvider = new FakeDeferredPlaybackPreparerProvider();

		library = new Library();
		library.setId(1);
		library.setNowPlayingId(5);

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
			new PositionedFileQueueProvider(),
			new NowPlayingRepository(libraryProvider, libraryStorage),
			cachedFilePropertiesProvider,
			1.0f);

		playbackPlaylistStateManager
			.startPlaylist(
				Arrays.asList(
					new File(1),
					new File(2),
					new File(3),
					new File(4),
					new File(5)), 0, 0);

		final ResolveablePlaybackHandler resolveablePlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve();
		resolveablePlaybackHandler.resolve();

		fakePlaybackPreparerProvider.deferredResolution.resolve();
	}

	@Test
	public void thenTheSavedTrackPositionIsZero() {
		assertThat(library.getNowPlayingId()).isEqualTo(1);
	}

	@Test
	public void thenTheManagerIsPlaying() {
		assertThat(playbackPlaylistStateManager.isPlaying()).isTrue();
	}
}
