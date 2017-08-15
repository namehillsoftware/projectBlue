package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.factory.specs.GivenAVersionOfMediaCenterIsNotReturned;

import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesStorage;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.IPlaystatsUpdate;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.factory.PlaystatsUpdateSelector;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.fileproperties.FilePropertiesPlayStatsUpdater;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.specs.FakeFilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.servers.version.IProgramVersionProvider;
import com.lasthopesoftware.messenger.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenGettingThePlaystatsUpdater {

	private static IPlaystatsUpdate updater;

	@BeforeClass
	public static void before() throws InterruptedException {
		final FakeConnectionProvider fakeConnectionProvider = new FakeConnectionProvider();

		final IProgramVersionProvider programVersionProvider = mock(IProgramVersionProvider.class);
		when(programVersionProvider.promiseServerVersion())
			.thenReturn(Promise.empty());

		final FakeFilePropertiesContainer fakeFilePropertiesContainer = new FakeFilePropertiesContainer();
		final PlaystatsUpdateSelector playstatsUpdateSelector = new PlaystatsUpdateSelector(
			fakeConnectionProvider,
			new FilePropertiesProvider(fakeConnectionProvider, fakeFilePropertiesContainer),
			new FilePropertiesStorage(fakeConnectionProvider, fakeFilePropertiesContainer),
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
