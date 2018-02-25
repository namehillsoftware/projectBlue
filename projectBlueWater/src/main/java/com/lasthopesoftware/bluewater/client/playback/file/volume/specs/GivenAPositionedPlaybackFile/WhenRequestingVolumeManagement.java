package com.lasthopesoftware.bluewater.client.playback.file.volume.specs.GivenAPositionedPlaybackFile;


import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.playback.file.EmptyFileVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.MaxFileVolumeProvider;
import com.lasthopesoftware.bluewater.client.playback.file.volume.PlaybackHandlerMaxVolumeModifier;
import com.lasthopesoftware.bluewater.client.playback.file.volume.PlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.state.volume.IVolumeManagement;
import com.lasthopesoftware.bluewater.settings.volumeleveling.IVolumeLevelSettings;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenRequestingVolumeManagement {

	private static IVolumeManagement playbackFileVolumeController;

	@BeforeClass
	public static void before() throws InterruptedException {
		final IUrlProvider urlProvider = mock(IUrlProvider.class);
		when(urlProvider.getBaseUrl()).thenReturn("");

		final IConnectionProvider connectionProvider = mock(IConnectionProvider.class);
		when(connectionProvider.getUrlProvider()).thenReturn(urlProvider);

		final IFilePropertiesContainerRepository repository = mock(IFilePropertiesContainerRepository.class);
		when(repository.getFilePropertiesContainer(new UrlKeyHolder<>("", 1)))
			.thenReturn(new FilePropertiesContainer(0, new HashMap<String, String>() {{
				put(FilePropertiesProvider.VolumeLevelR128, "-13.5");
			}}));

		final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(connectionProvider, repository);

		final CachedFilePropertiesProvider cachedFilePropertiesProvider =
			new CachedFilePropertiesProvider(
				connectionProvider,
				repository,
				filePropertiesProvider);

		final IVolumeLevelSettings volumeLevelSettings = mock(IVolumeLevelSettings.class);
		when(volumeLevelSettings.isVolumeLevellingEnabled()).thenReturn(true);

		final PlaybackHandlerVolumeControllerFactory playbackHandlerVolumeControllerFactory =
			new PlaybackHandlerVolumeControllerFactory(new MaxFileVolumeProvider(volumeLevelSettings, cachedFilePropertiesProvider));

		playbackFileVolumeController =
			playbackHandlerVolumeControllerFactory
				.manageVolume(new PositionedPlayableFile(
					1,
					mock(PlayableFile.class),
					new EmptyFileVolumeManager(),
					new ServiceFile(1)), 1);
	}

	@Test
	public void thenAPlaybackFileVolumeHandlerIsReturned() {
		assertThat(playbackFileVolumeController).isExactlyInstanceOf(PlaybackHandlerMaxVolumeModifier.class);
	}
}
