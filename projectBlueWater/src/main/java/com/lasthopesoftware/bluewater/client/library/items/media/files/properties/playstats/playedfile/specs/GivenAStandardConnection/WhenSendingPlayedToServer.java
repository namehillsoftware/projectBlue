package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.playedfile.specs.GivenAStandardConnection;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenSendingPlayedToServer {

	private static Boolean isUpdated;

	@BeforeClass
	public static void before() throws InterruptedException, IOException {
		final IConnectionProvider connectionProvider = mock(IConnectionProvider.class);;

		final HttpURLConnection urlConnection = mock(HttpURLConnection.class);
		when(urlConnection.getResponseCode()).thenReturn(200);

		when(connectionProvider.getConnection("File/Played", "File=15", "FileType=Key"))
			.thenReturn(urlConnection);

		final PlayedFilePlayStatsUpdater updater = new PlayedFilePlayStatsUpdater(connectionProvider);

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		updater
			.promisePlaystatsUpdate(15)
			.then(u -> {
				isUpdated = u;
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenTheFileIsUpdated() throws IOException {
		assertThat(isUpdated).isTrue();
	}
}
