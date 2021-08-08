package com.lasthopesoftware.bluewater.client.playback.engine.GivenAHaltedPlaylistEngine;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine;
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CyclicalFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlaying;
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenSettingEngineToComplete {

	private static final Library library = new Library();
	private static NowPlaying nowPlaying;

	@BeforeClass
	public static void before() throws InterruptedException, ExecutionException {
		final FakeDeferredPlayableFilePreparationSourceProvider fakePlaybackPreparerProvider = new FakeDeferredPlayableFilePreparationSourceProvider();

		library.setId(1);
		library.setSavedTracksString(new FuturePromise<>(FileStringListUtilities.promiseSerializedFileStringList(Arrays.asList(
			new ServiceFile(1),
			new ServiceFile(2),
			new ServiceFile(3),
			new ServiceFile(4),
			new ServiceFile(5)))).get());
		library.setNowPlayingId(0);
		library.setRepeating(true);

		final ISpecificLibraryProvider libraryProvider = () -> new Promise<>(library);

		final ILibraryStorage libraryStorage = new PassThroughLibraryStorage();

		final IFilePropertiesContainerRepository filePropertiesContainerRepository = mock(IFilePropertiesContainerRepository.class);
		when(filePropertiesContainerRepository.getFilePropertiesContainer(new UrlKeyHolder<>("", new ServiceFile(4))))
			.thenReturn(new FilePropertiesContainer(1, new HashMap<String, String>() {{
					put(KnownFileProperties.DURATION, "100");
			}}));

		final NowPlayingRepository repository = new NowPlayingRepository(libraryProvider, libraryStorage);

		final PlaybackEngine playbackEngine = new FuturePromise<>(PlaybackEngine.createEngine(
			new PreparedPlaybackQueueResourceManagement(
				fakePlaybackPreparerProvider,
				() -> 1),
			Arrays.asList(new CompletingFileQueueProvider(), new CyclicalFileQueueProvider()),
			repository,
			new PlaylistPlaybackBootstrapper(new PlaylistVolumeManager(1.0f)))).get();

		playbackEngine.playToCompletion();

		nowPlaying = new FuturePromise<>(repository.getNowPlaying()).get();
	}

	@Test
	public void thenNowPlayingIsSetToNotRepeating() {
		assertThat(nowPlaying.isRepeating).isFalse();
	}
}
