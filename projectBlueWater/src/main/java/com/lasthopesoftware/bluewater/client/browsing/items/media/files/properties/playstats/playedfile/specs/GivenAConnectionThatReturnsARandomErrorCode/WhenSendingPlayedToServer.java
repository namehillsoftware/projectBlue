package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.playedfile.specs.GivenAConnectionThatReturnsARandomErrorCode;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionResponseTuple;
import com.lasthopesoftware.bluewater.shared.exceptions.HttpResponseException;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenSendingPlayedToServer {

	private static HttpResponseException httpResponseException;
	private static int expectedResponseCode;

	@BeforeClass
	public static void before() throws InterruptedException {
		final Random random = new Random();
		do {
			expectedResponseCode = random.nextInt();
		} while (expectedResponseCode < 0 || (expectedResponseCode >= 200 && expectedResponseCode < 300));

		final FakeConnectionProvider connectionProvider = new FakeConnectionProvider();
		connectionProvider.mapResponse(p -> new FakeConnectionResponseTuple(expectedResponseCode, new byte[0]), "File/Played", "File=15", "FileType=Key");

		final PlayedFilePlayStatsUpdater updater = new PlayedFilePlayStatsUpdater(connectionProvider);

		try {
			new FuturePromise<>(updater.promisePlaystatsUpdate(new ServiceFile(15))).get();
		} catch (ExecutionException e) {
			if (e.getCause() instanceof HttpResponseException)
				httpResponseException = (HttpResponseException)e.getCause();
		}
	}

	@Test
	public void thenAnHttpResponseExceptionIsThrownWithTheResponseCode() {
		assertThat(httpResponseException.getResponseCode()).isEqualTo(expectedResponseCode);
	}
}
