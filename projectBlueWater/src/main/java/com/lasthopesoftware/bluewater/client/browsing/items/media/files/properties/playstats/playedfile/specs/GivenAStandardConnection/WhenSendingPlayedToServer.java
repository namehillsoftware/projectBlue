package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.playedfile.specs.GivenAStandardConnection;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionResponseTuple;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenSendingPlayedToServer {

	private static Object functionEnded;

	@BeforeClass
	public static void before() throws InterruptedException, ExecutionException {
		final FakeConnectionProvider connectionProvider = new FakeConnectionProvider();
		connectionProvider.mapResponse(p  -> new FakeConnectionResponseTuple(200, new byte[0]), "File/Played", "File=15", "FileType=Key");

		final PlayedFilePlayStatsUpdater updater = new PlayedFilePlayStatsUpdater(connectionProvider);

		functionEnded = new FuturePromise<>(updater
			.promisePlaystatsUpdate(new ServiceFile(15))).get();
	}

	@Test
	public void thenTheFileIsUpdated() {
		assertThat(functionEnded).isNull();
	}
}
