package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.playedfile.specs.GivenAConnectionThatReturnsARandomErrorCode;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater;
import com.lasthopesoftware.bluewater.shared.exceptions.HttpResponseException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenSendingPlayedToServer {

	private static Object functionEnded;
	private static HttpResponseException httpResponseException;
	private static int expectedResponseCode;

	@BeforeClass
	public static void before() throws InterruptedException, IOException {
		final Random random = new Random();
		do {
			expectedResponseCode = random.nextInt();
		} while (expectedResponseCode >= 200 && expectedResponseCode < 300);

		final IConnectionProvider connectionProvider = mock(IConnectionProvider.class);

		final HttpURLConnection urlConnection = mock(HttpURLConnection.class);
		when(urlConnection.getResponseCode()).thenReturn(expectedResponseCode);

		when(connectionProvider.getConnection("File/Played", "File=15", "FileType=Key"))
			.thenReturn(urlConnection);

		final PlayedFilePlayStatsUpdater updater = new PlayedFilePlayStatsUpdater(connectionProvider);

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		updater
			.promisePlaystatsUpdate(new ServiceFile(15))
			.then(v -> {
				functionEnded = v;

				countDownLatch.countDown();
				return null;
			})
			.excuse(e -> {
				if (e instanceof HttpResponseException) {
					httpResponseException = (HttpResponseException)e;
				}

				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenAnHttpResponseExceptionIsThrownWithTheResponseCode() {
		assertThat(httpResponseException.getResponseCode()).isEqualTo(expectedResponseCode);
	}
}
