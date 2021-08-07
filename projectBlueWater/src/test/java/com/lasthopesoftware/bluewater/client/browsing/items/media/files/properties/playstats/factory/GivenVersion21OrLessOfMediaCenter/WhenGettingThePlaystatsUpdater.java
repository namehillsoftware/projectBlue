package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.factory.GivenVersion21OrLessOfMediaCenter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FakeFilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesStorage;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.IPlaystatsUpdate;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.factory.PlaystatsUpdateSelector;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.fileproperties.FilePropertiesPlayStatsUpdater;
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.servers.version.IProgramVersionProvider;
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class WhenGettingThePlaystatsUpdater {

	private static IPlaystatsUpdate updater;

	@BeforeClass
	public static void before() throws InterruptedException {
		final FakeConnectionProvider fakeConnectionProvider = new FakeConnectionProvider();

		final IProgramVersionProvider programVersionProvider = mock(IProgramVersionProvider.class);
		when(programVersionProvider.promiseServerVersion())
			.thenReturn(new Promise<>(new SemanticVersion(21, 0, 0)));

		final FakeFilePropertiesContainer fakeFilePropertiesContainer = new FakeFilePropertiesContainer();
		final PlaystatsUpdateSelector playstatsUpdateSelector = new PlaystatsUpdateSelector(
			fakeConnectionProvider,
			new ScopedFilePropertiesProvider(fakeConnectionProvider, fakeFilePropertiesContainer),
			new ScopedFilePropertiesStorage(fakeConnectionProvider, fakeFilePropertiesContainer),
			programVersionProvider);

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		playstatsUpdateSelector.promisePlaystatsUpdater()
			.then(u -> {
				updater = u;
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenTheFilePropertiesPlaystatsUpdaterIsGiven() {
		assertThat(updater).isInstanceOf(FilePropertiesPlayStatsUpdater.class);
	}
}
