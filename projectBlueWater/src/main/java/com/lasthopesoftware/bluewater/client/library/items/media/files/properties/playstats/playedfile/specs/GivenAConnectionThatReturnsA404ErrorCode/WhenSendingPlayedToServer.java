package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.playedfile.specs.GivenAConnectionThatReturnsA404ErrorCode;

import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater;
import com.lasthopesoftware.bluewater.shared.exceptions.HttpResponseException;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenSendingPlayedToServer {

	private static HttpResponseException httpResponseException;

	@BeforeClass
	public static void before() throws InterruptedException {
		final FakeConnectionProvider connectionProvider = new FakeConnectionProvider();

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
		assertThat(httpResponseException.getResponseCode()).isEqualTo(404);
	}
}
