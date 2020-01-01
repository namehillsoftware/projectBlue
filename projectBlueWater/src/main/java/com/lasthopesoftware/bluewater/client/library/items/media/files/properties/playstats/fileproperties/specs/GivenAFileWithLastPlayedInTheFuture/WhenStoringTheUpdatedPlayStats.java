package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.fileproperties.specs.GivenAFileWithLastPlayedInTheFuture;

import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionResponseTuple;
import com.lasthopesoftware.bluewater.client.library.access.specs.FakeRevisionConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesStorage;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.SessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.fileproperties.FilePropertiesPlayStatsUpdater;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.specs.FakeFilePropertiesContainer;
import com.lasthopesoftware.resources.scheduling.ParsingScheduler;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenStoringTheUpdatedPlayStats {

	private static Map<String, String> fileProperties;
	private static long lastPlayed = Duration.millis(DateTime.now().plus(Duration.standardDays(10)).getMillis()).getStandardSeconds();

	@BeforeClass
	public static void before() throws InterruptedException {
		final FakeRevisionConnectionProvider connectionProvider = new FakeRevisionConnectionProvider();

		connectionProvider.setSyncRevision(1);

		final long duration = Duration.standardMinutes(5).getMillis();

		connectionProvider.mapResponse((params) ->
			new FakeConnectionResponseTuple(200, ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n" +
			"<MPL Version=\"2.0\" Title=\"MCWS - Files - 10936\" PathSeparator=\"\\\">\n" +
				"<Item>\n" +
					"<Field Name=\"Key\">23</Field>\n" +
					"<Field Name=\"Media Type\">Audio</Field>\n" +
					"<Field Name=\"" + SessionFilePropertiesProvider.LAST_PLAYED + "\">" + String.valueOf(lastPlayed) + "</Field>\n" +
					"<Field Name=\"Rating\">4</Field>\n" +
					"<Field Name=\"File Size\">2345088</Field>\n" +
					"<Field Name=\"" + SessionFilePropertiesProvider.DURATION + "\">" + String.valueOf(duration) + "</Field>\n" +
					"<Field Name=\"" + SessionFilePropertiesProvider.NUMBER_PLAYS + "\">52</Field>\n" +
				"</Item>\n" +
			"</MPL>\n").getBytes()),
			"File/GetInfo", "File=23");

		final FakeFilePropertiesContainer filePropertiesContainer = new FakeFilePropertiesContainer();
		final SessionFilePropertiesProvider sessionFilePropertiesProvider = new SessionFilePropertiesProvider(connectionProvider, filePropertiesContainer, ParsingScheduler.instance());

		final FilePropertiesPlayStatsUpdater filePropertiesPlayStatsUpdater = new FilePropertiesPlayStatsUpdater(sessionFilePropertiesProvider, new FilePropertiesStorage(connectionProvider, filePropertiesContainer));

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		filePropertiesPlayStatsUpdater
			.promisePlaystatsUpdate(new ServiceFile(23))
			.eventually(o -> sessionFilePropertiesProvider.promiseFileProperties(new ServiceFile(23)))
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
	public void thenTheLastPlayedIsNotUpdated() {
		assertThat(fileProperties.get(SessionFilePropertiesProvider.LAST_PLAYED)).isEqualTo(String.valueOf(lastPlayed));
	}

	@Test
	public void thenTheNumberPlaysIsTheSame() {
		assertThat(fileProperties.get(SessionFilePropertiesProvider.NUMBER_PLAYS)).isEqualTo("52");
	}
}
