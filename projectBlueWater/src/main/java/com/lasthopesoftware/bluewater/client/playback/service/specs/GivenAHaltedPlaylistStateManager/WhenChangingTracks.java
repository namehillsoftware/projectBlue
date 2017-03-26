package com.lasthopesoftware.bluewater.client.playback.service.specs.GivenAHaltedPlaylistStateManager;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PositionedFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.specs.fakes.FakeDeferredPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackPlaylistStateManager;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 3/13/17.
 */

public class WhenChangingTracks {

	private static PositionedPlaybackFile nextSwitchedFile;
	private static Library library;

	@BeforeClass
	public static void before() throws IOException, InterruptedException {
		final FakeDeferredPlaybackPreparerProvider fakePlaybackPreparerProvider = new FakeDeferredPlaybackPreparerProvider();

		library = new Library();
		library.setId(1);
		library.setSavedTracksString(FileStringListUtilities.serializeFileStringList(Arrays.asList(
			new File(1),
			new File(2),
			new File(3),
			new File(4),
			new File(5))));

		final ISpecificLibraryProvider libraryProvider = mock(ISpecificLibraryProvider.class);
		when(libraryProvider.getLibrary()).thenReturn(new Promise<>(library));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.saveLibrary(any())).then(Promise::new);

		final IUrlProvider urlProvider = mock(IUrlProvider.class);
		when(urlProvider.getBaseUrl()).thenReturn("");

		final IConnectionProvider connectionProvider = mock(IConnectionProvider.class);
		when(connectionProvider.getUrlProvider()).thenReturn(urlProvider);

		final HttpURLConnection urlConnection = mock(HttpURLConnection.class);
		when(urlConnection.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
		when(connectionProvider.getConnection(any())).thenReturn(urlConnection);

		final IFilePropertiesContainerRepository filePropertiesContainerRepository = mock(IFilePropertiesContainerRepository.class);
		when(filePropertiesContainerRepository.getFilePropertiesContainer(new UrlKeyHolder<>("", 4)))
			.thenReturn(new FilePropertiesContainer(1, new HashMap<String, String>() {{
					put(FilePropertiesProvider.DURATION, "100");
			}}));

		final CachedFilePropertiesProvider cachedFilePropertiesProvider =
			new CachedFilePropertiesProvider(
				connectionProvider,
				filePropertiesContainerRepository,
				new FilePropertiesProvider(
					connectionProvider,
					filePropertiesContainerRepository));

		final PlaybackPlaylistStateManager playbackPlaylistStateManager = new PlaybackPlaylistStateManager(
			fakePlaybackPreparerProvider,
			new PositionedFileQueueProvider(),
			new NowPlayingRepository(libraryProvider, libraryStorage),
			cachedFilePropertiesProvider,
			1.0f);

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		Observable.create(playbackPlaylistStateManager).subscribe(p -> {
			nextSwitchedFile = p;
			countDownLatch.countDown();
		});

		playbackPlaylistStateManager.changePosition(3, 0);

		countDownLatch.await(1, TimeUnit.SECONDS);
	}

	@Test
	public void thenTheNextFileChangeIsTheSwitchedToTheCorrectTrackPosition() {
		assertThat(nextSwitchedFile.getPosition()).isEqualTo(3);
	}

	@Test
	public void thenTheSavedLibraryIsAtTheCorrectTrackPosition() {
		assertThat(library.getNowPlayingId()).isEqualTo(3);
	}
}
