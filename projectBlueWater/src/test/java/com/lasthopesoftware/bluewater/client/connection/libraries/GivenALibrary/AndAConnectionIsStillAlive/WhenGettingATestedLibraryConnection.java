package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndAConnectionIsStillAlive;

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl;
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.connection.waking.NoopServerAlarm;
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenGettingATestedLibraryConnection {

	private static final List<BuildingConnectionStatus> statuses = new ArrayList<>();
	private static final IUrlProvider firstUrlProvider = mock(IUrlProvider.class);
	private static IConnectionProvider connectionProvider;
	private static IConnectionProvider secondConnectionProvider;

	@BeforeClass
	public static void before() throws InterruptedException, ExecutionException, TimeoutException {

		final Library library = new Library()
			.setId(2)
			.setAccessCode("aB5nf");

		final ILibraryProvider libraryProvider = mock(ILibraryProvider.class);
		final DeferredPromise<Library> libraryDeferredPromise = new DeferredPromise<>(library);
		when(libraryProvider.getLibrary(new LibraryId(2))).thenReturn(libraryDeferredPromise);

		final ProvideLiveUrl liveUrlProvider = mock(ProvideLiveUrl.class);
		when(liveUrlProvider.promiseLiveUrl(library)).thenReturn(new Promise<>(firstUrlProvider));

		final TestConnections connectionsTester = mock(TestConnections.class);
		when(connectionsTester.promiseIsConnectionPossible(any()))
				.thenReturn(new Promise<>(true));

		final LibraryConnectionProvider libraryConnectionProvider = new LibraryConnectionProvider(
			libraryProvider,
			new NoopServerAlarm(),
			liveUrlProvider,
			connectionsTester,
			OkHttpFactory.getInstance());

		final LibraryId libraryId = new LibraryId(2);
		final FuturePromise<IConnectionProvider> futureConnectionProvider = new FuturePromise<>(libraryConnectionProvider
			.promiseLibraryConnection(libraryId)
			.updates(statuses::add));

		libraryDeferredPromise.resolve();
		connectionProvider = futureConnectionProvider.get(30, TimeUnit.SECONDS);

		secondConnectionProvider = new FuturePromise<>(libraryConnectionProvider
			.promiseTestedLibraryConnection(libraryId)
			.updates(statuses::add)).get();
	}

	@Test
	public void thenTheConnectionIsCorrect() {
		assertThat(secondConnectionProvider).isEqualTo(connectionProvider);
	}

	@Test
	public void thenGettingLibraryIsBroadcast() {
		Assertions.assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete);
	}
}
