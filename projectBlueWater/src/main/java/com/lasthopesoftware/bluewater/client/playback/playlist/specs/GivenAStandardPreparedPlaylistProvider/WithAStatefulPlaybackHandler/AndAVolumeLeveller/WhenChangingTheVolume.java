package com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.AndAVolumeLeveller;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.EmptyFileVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.volume.MaxFileVolumeProvider;
import com.lasthopesoftware.bluewater.client.playback.file.volume.PlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.playlist.IPlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer;
import com.lasthopesoftware.bluewater.settings.volumeleveling.IVolumeLevelSettings;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.resources.scheduling.ParsingScheduler;
import com.namehillsoftware.handoff.promises.Promise;
import io.reactivex.Observable;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenChangingTheVolume {

	private static final EmptyFileVolumeManager volumeManager = new EmptyFileVolumeManager();

	@BeforeClass
	public static void before() {
		final FakeBufferingPlaybackHandler playbackHandler = new FakeBufferingPlaybackHandler();
		playbackHandler.promisePlayback();

		final Promise<PositionedPlayableFile> positionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlayableFile(0, playbackHandler, volumeManager, new ServiceFile(1)));

		final PreparedPlayableFileQueue preparedPlaybackFileQueue = mock(PreparedPlayableFileQueue.class);
		when(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(0))
			.thenReturn(positionedPlaybackHandlerContainer);

		final IUrlProvider urlProvider = mock(IUrlProvider.class);
		when(urlProvider.getBaseUrl()).thenReturn("");

		final IConnectionProvider connectionProvider = mock(IConnectionProvider.class);
		when(connectionProvider.getUrlProvider()).thenReturn(urlProvider);

		final IFilePropertiesContainerRepository repository = mock(IFilePropertiesContainerRepository.class);
		when(repository.getFilePropertiesContainer(new UrlKeyHolder<>("", new ServiceFile(1))))
			.thenReturn(new FilePropertiesContainer(0, new HashMap<String, String>() {{
				put(FilePropertiesProvider.VolumeLevelR128, "-13.5");
			}}));

		final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(connectionProvider, repository, ParsingScheduler.instance());

		final CachedFilePropertiesProvider cachedFilePropertiesProvider =
			new CachedFilePropertiesProvider(
				connectionProvider,
				repository,
				filePropertiesProvider);

		final IVolumeLevelSettings volumeLevelSettings = mock(IVolumeLevelSettings.class);
		when(volumeLevelSettings.isVolumeLevellingEnabled()).thenReturn(true);

		final IPlaylistPlayer playlistPlayback =
			new PlaylistPlayer(
				preparedPlaybackFileQueue,
				new PlaybackHandlerVolumeControllerFactory(
					new MaxFileVolumeProvider(volumeLevelSettings, cachedFilePropertiesProvider)),
				0);

		Observable
			.create(playlistPlayback)
			.blockingFirst();

		playlistPlayback.setVolume(0.8f);
	}

	@Test
	public void thenTheVolumeIsChanged() {
		assertThat(volumeManager.getVolume()).isCloseTo(0.539285714f, offset(.001f));
	}
}
