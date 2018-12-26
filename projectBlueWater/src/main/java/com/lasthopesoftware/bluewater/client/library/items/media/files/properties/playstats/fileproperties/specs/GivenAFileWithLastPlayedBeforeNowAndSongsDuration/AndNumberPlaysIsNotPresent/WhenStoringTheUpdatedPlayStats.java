package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.fileproperties.specs.GivenAFileWithLastPlayedBeforeNowAndSongsDuration.AndNumberPlaysIsNotPresent;

import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.specs.FakeRevisionConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesStorage;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.fileproperties.FilePropertiesPlayStatsUpdater;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.specs.FakeFilePropertiesContainer;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

public class WhenStoringTheUpdatedPlayStats {

	private static Map<String, String> fileProperties;

	@BeforeClass
	public static void before() throws InterruptedException {
		final FakeRevisionConnectionProvider connectionProvider = new FakeRevisionConnectionProvider();

		connectionProvider.setSyncRevision(1);

		final long duration = Duration.standardMinutes(5).getMillis();
		final long lastPlayed = Duration.millis(DateTime.now().minus(Duration.standardDays(10)).getMillis()).getStandardSeconds();

		connectionProvider.mapResponse((params) ->
			new FakeConnectionProvider.ResponseTuple(200, ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n" +
			"<MPL Version=\"2.0\" Title=\"MCWS - Files - 10936\" PathSeparator=\"\\\">\n" +
				"<Item>\n" +
					"<Field Name=\"Key\">23</Field>\n" +
					"<Field Name=\"Media Type\">Audio</Field>\n" +
					"<Field Name=\"" + FilePropertiesProvider.LAST_PLAYED + "\">" + String.valueOf(lastPlayed) + "</Field>\n" +
					"<Field Name=\"Rating\">4</Field>\n" +
					"<Field Name=\"File Size\">2345088</Field>\n" +
					"<Field Name=\"" + FilePropertiesProvider.DURATION + "\">" + String.valueOf(duration) + "</Field>\n" +
				"</Item>\n" +
			"</MPL>\n").getBytes()),
			"File/GetInfo", "File=23");

		final FakeFilePropertiesContainer filePropertiesContainer = new FakeFilePropertiesContainer();
		final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(connectionProvider, filePropertiesContainer);

		final FilePropertiesPlayStatsUpdater filePropertiesPlayStatsUpdater = new FilePropertiesPlayStatsUpdater(filePropertiesProvider, new FilePropertiesStorage(connectionProvider, filePropertiesContainer));

		final ServiceFile serviceFile = new ServiceFile(23);
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		filePropertiesPlayStatsUpdater
			.promisePlaystatsUpdate(serviceFile)
			.eventually(o -> filePropertiesProvider.promiseFileProperties(serviceFile))
			.then(o -> {
				fileProperties = o;
				countDownLatch.countDown();
				return null;
			})
			.excuse(e -> {
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenTheLastPlayedIsRecent() {
		assertThat(Long.parseLong(fileProperties.get(FilePropertiesProvider.LAST_PLAYED))).isCloseTo(Duration.millis(System.currentTimeMillis()).getStandardSeconds(), offset(10L));
	}

	@Test
	public void thenTheNumberPlaysIsIncremented() {
		assertThat(fileProperties.get(FilePropertiesProvider.NUMBER_PLAYS)).isEqualTo("1");
	}
}
