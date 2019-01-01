package com.lasthopesoftware.bluewater.client.playback.engine.specs.GivenAHaltedPlaylistStateManager;

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
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenChangingTracks {

	private static PositionedFile nextSwitchedFile;
	private static Library library;

	@BeforeClass
	public static void before() throws InterruptedException, ExecutionException {
		final FakeDeferredPlayableFilePreparationSourceProvider fakePlaybackPreparerProvider = new FakeDeferredPlayableFilePreparationSourceProvider();

		library = new Library();
		library.setId(1);
		library.setSavedTracksString(new FuturePromise<>(FileStringListUtilities.promiseSerializedFileStringList(Arrays.asList(
			new ServiceFile(1),
			new ServiceFile(2),
			new ServiceFile(3),
			new ServiceFile(4),
			new ServiceFile(5)))).get());

		final ISpecificLibraryProvider libraryProvider = mock(ISpecificLibraryProvider.class);
		when(libraryProvider.getLibrary()).thenReturn(new Promise<>(library));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.saveLibrary(any())).then(Promise::new);

		final IFilePropertiesContainerRepository filePropertiesContainerRepository = mock(IFilePropertiesContainerRepository.class);
		when(filePropertiesContainerRepository.getFilePropertiesContainer(new UrlKeyHolder<>("", new ServiceFile(4))))
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
