package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.fileproperties.specs.GivenAFileWithLastPlayedBeforeNowAndSongsDuration.AndNumberPlaysIsPresent;

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.access.specs.FakeRevisionConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.fileproperties.FilePropertiesPlayStatsUpdater;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenStoringTheUpdatedPlayStats {

	private static Map<String, String> fileProperties;

	@BeforeClass
	public static void before() throws InterruptedException {
		final IUrlProvider urlProvider = mock(IUrlProvider.class);
		when(urlProvider.getBaseUrl()).thenReturn("");

		final FakeRevisionConnectionProvider connectionProvider = new FakeRevisionConnectionProvider();

		connectionProvider.setSyncRevision(2);

		final long duration = 5 * 1000 * 60;

		final IFilePropertiesContainerRepository repository = mock(IFilePropertiesContainerRepository.class);
		when(repository.getFilePropertiesContainer(new UrlKeyHolder<>("", 23)))
			.thenReturn(new FilePropertiesContainer(0, new HashMap<String, String>() {{
				put(FilePropertiesProvider.NUMBER_PLAYS, "52");
				put(FilePropertiesProvider.LAST_PLAYED, String.valueOf(System.currentTimeMillis() - duration - 1000000));
				put(FilePropertiesProvider.DURATION, String.valueOf(duration));
			}}));

		final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(connectionProvider, repository);

		final FilePropertiesPlayStatsUpdater filePropertiesPlayStatsUpdater = new FilePropertiesPlayStatsUpdater(connectionProvider, filePropertiesProvider);

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		filePropertiesPlayStatsUpdater
			.promisePlaystatsUpdate(new ServiceFile(23))
			.eventually(o -> filePropertiesProvider.promiseFileProperties(23))
			.then(o -> {
				fileProperties = o;
				countDownLatch.countDown();
				return null;
			});;

		countDownLatch.await();
	}

	@Test
	public void thenTheLastPlayedIsRecent() {
		assertThat(Long.parseLong(fileProperties.get(FilePropertiesProvider.LAST_PLAYED))).isCloseTo(System.currentTimeMillis(), offset(10000L));
	}

	@Test
	public void thenTheNumberPlaysIsIncremented() {
		assertThat(fileProperties.get(FilePropertiesProvider.NUMBER_PLAYS)).isEqualTo(53);
	}
}
