package com.lasthopesoftware.bluewater.client.playback.file.volume.GivenVolumeLevellingIsEnabled.WithAVeryHighAdjustment;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedCachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.playback.file.volume.MaxFileVolumeProvider;
import com.lasthopesoftware.bluewater.settings.volumeleveling.IVolumeLevelSettings;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

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
				put(KnownFileProperties.VolumeLevelReplayGain, "25");
			}}));

		final ScopedFilePropertiesProvider scopedFilePropertiesProvider = new ScopedFilePropertiesProvider(connectionProvider, repository);

		final ScopedCachedFilePropertiesProvider scopedCachedFilePropertiesProvider =
			new ScopedCachedFilePropertiesProvider(
				connectionProvider,
				repository,
				scopedFilePropertiesProvider);

		final IVolumeLevelSettings volumeLevelSettings = mock(IVolumeLevelSettings.class);
		when(volumeLevelSettings.isVolumeLevellingEnabled()).thenReturn(true);

		final MaxFileVolumeProvider maxFileVolumeProvider =
			new MaxFileVolumeProvider(volumeLevelSettings, scopedCachedFilePropertiesProvider);

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		maxFileVolumeProvider
			.promiseMaxFileVolume(new ServiceFile(1))
			.then(volume -> {
				returnedVolume = volume;
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenTheReturnedVolumeIsOne() {
		assertThat(returnedVolume).isEqualTo(1);
	}
}
