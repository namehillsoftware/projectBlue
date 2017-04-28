package com.lasthopesoftware.bluewater.client.playback.state.specs.GivenAPlayingPlaylistStateManager;

import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.specs.fakes.FakeDeferredPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.state.PlaylistManager;
import com.lasthopesoftware.bluewater.client.playback.state.volume.PlaylistVolumeManager;
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

	private static PlaylistManager playlistManager;

	@BeforeClass
	public static void before() {
		final FakeDeferredPlaybackPreparerProvider fakePlaybackPreparerProvider = new FakeDeferredPlaybackPreparerProvider();

		final Library library = new Library();
		library.setId(1);

		final ISpecificLibraryProvider libraryProvider = mock(ISpecificLibraryProvider.class);
		when(libraryProvider.getLibrary()).thenReturn(new Promise<>(library));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.saveLibrary(any())).thenReturn(new Promise<>(library));

		playlistManager = new PlaylistManager(
			fakePlaybackPreparerProvider,
			Collections.singletonList(new CompletingFileQueueProvider()),
			new NowPlayingRepository(libraryProvider, libraryStorage),
			new PlaylistVolumeManager(1.0f));

		playlistManager
			.startPlaylist(
				Arrays.asList(
					new ServiceFile(1),
					new ServiceFile(2),
					new ServiceFile(3),
					new ServiceFile(4),
					new ServiceFile(5)), 0, 0);

		fakePlaybackPreparerProvider.deferredResolution.resolve();

		playlistManager.pause();
	}

	@Test
	public void thenThePlaybackStateIsNotPlaying() {
		assertThat(playlistManager.isPlaying()).isFalse();
	}
}
