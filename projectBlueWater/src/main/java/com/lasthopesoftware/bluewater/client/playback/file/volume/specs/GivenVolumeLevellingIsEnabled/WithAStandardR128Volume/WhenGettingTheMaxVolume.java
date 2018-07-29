package com.lasthopesoftware.bluewater.client.playback.file.volume.specs.GivenVolumeLevellingIsEnabled.WithAStandardR128Volume;


import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.playback.file.volume.MaxFileVolumeProvider;
import com.lasthopesoftware.bluewater.settings.volumeleveling.IVolumeLevelSettings;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenGettingTheMaxVolume {

	private static float returnedVolume;

	@BeforeClass
	public static void before() throws InterruptedException {
		final IUrlProvider urlProvider = mock(IUrlProvider.class);
		when(urlProvider.getBaseUrl()).thenReturn("");

		final IConnectionProvider connectionProvider = mock(IConnectionProvider.class);
		when(connectionProvider.getUrlProvider()).thenReturn(urlProvider);

		final IFilePropertiesContainerRepository repository = mock(IFilePropertiesContainerRepository.class);
		when(repository.getFilePropertiesContainer(new UrlKeyHolder<>("", new ServiceFile(1))))
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

		final MaxFileVolumeProvider maxFileVolumeProvider =
			new MaxFileVolumeProvider(volumeLevelSettings, cachedFilePropertiesProvider);

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		maxFileVolumeProvider
			.getMaxFileVolume(new ServiceFile(1))
			.then(volume -> {
				returnedVolume = volume;
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenTheReturnedVolumeIsCorrect() {
		assertThat(returnedVolume).isCloseTo(.674107143f, offset(.00001f));
	}
}
