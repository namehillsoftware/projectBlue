package com.lasthopesoftware.bluewater.client.playback.file.volume.GivenVolumeLevellingIsNotEnabled;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ProvideScopedFileProperties;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedCachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.playback.file.volume.MaxFileVolumeProvider;
import com.lasthopesoftware.bluewater.settings.volumeleveling.IVolumeLevelSettings;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class WhenGettingTheMaxVolume {

	private static float returnedVolume;

	@BeforeClass
	public static void before() throws InterruptedException {
		final ScopedCachedFilePropertiesProvider scopedCachedFilePropertiesProvider =
			new ScopedCachedFilePropertiesProvider(
				mock(IConnectionProvider.class),
				mock(IFilePropertiesContainerRepository.class),
				mock(ProvideScopedFileProperties.class));

		final IVolumeLevelSettings volumeLevelSettings = mock(IVolumeLevelSettings.class);

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
