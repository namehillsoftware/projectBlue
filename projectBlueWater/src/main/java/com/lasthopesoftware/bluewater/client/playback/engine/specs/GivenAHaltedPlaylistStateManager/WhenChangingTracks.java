package com.lasthopesoftware.bluewater.client.playback.engine.specs.GivenAHaltedPlaylistStateManager;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine;
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.specs.fakes.FakeDeferredPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.volume.IPlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenChangingTracks {

	private static PositionedFile nextSwitchedFile;
	private static Library library;

	@BeforeClass
	public static void before() throws IOException, InterruptedException {
		final FakeDeferredPlayableFilePreparationSourceProvider fakePlaybackPreparerProvider = new FakeDeferredPlayableFilePreparationSourceProvider();

		library = new Library();
		library.setId(1);
		library.setSavedTracksString(FileStringListUtilities.serializeFileStringList(Arrays.asList(
			new ServiceFile(1),
			new ServiceFile(2),
			new ServiceFile(3),
			new ServiceFile(4),
			new ServiceFile(5))));

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

		final PlaybackEngine playbackEngine = new PlaybackEngine(
			fakePlaybackPreparerProvider,
			() -> 1,
			Collections.singletonList(new CompletingFileQueueProvider()),
			new NowPlayingRepository(libraryProvider, libraryStorage),
			new PlaylistPlaybackBootstrapper(new PlaylistVolumeManager(1.0f), mock(IPlaybackHandlerVolumeControllerFactory.class)));

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		playbackEngine.changePosition(3, 0).then(p -> {
			nextSwitchedFile = p;
			countDownLatch.countDown();
			return null;
		});

		countDownLatch.await(1, TimeUnit.SECONDS);
	}

	@Test
	public void thenTheNextFileChangeIsTheSwitchedToTheCorrectTrackPosition() {
		assertThat(nextSwitchedFile.getPlaylistPosition()).isEqualTo(3);
	}

	@Test
	public void thenTheSavedLibraryIsAtTheCorrectTrackPosition() {
		assertThat(library.getNowPlayingId()).isEqualTo(3);
	}
}
