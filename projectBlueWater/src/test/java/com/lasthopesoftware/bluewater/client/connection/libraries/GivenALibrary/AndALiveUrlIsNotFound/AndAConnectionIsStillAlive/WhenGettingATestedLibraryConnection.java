package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndALiveUrlIsNotFound.AndAConnectionIsStillAlive;

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

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenGettingATestedLibraryConnection {

	private static final List<BuildingConnectionStatus> statuses = new ArrayList<>();
	private static final IUrlProvider firstUrlProvider = mock(IUrlProvider.class);
	private static IConnectionProvider connectionProvider;

	@BeforeClass
	public static void before() throws InterruptedException, ExecutionException {

		final Library library = new Library()
			.setId(2)
			.setAccessCode("aB5nf");

		final ILibraryProvider libraryProvider = mock(ILibraryProvider.class);
		final DeferredPromise<Library> libraryDeferredPromise = new DeferredPromise<>(library);
		final DeferredPromise<Library> secondLibraryDeferredPromise = new DeferredPromise<>(library);
		when(libraryProvider.getLibrary(new LibraryId(2)))
			.thenReturn(libraryDeferredPromise)
			.thenReturn(secondLibraryDeferredPromise);

		final ProvideLiveUrl liveUrlProvider = mock(ProvideLiveUrl.class);
		when(liveUrlProvider.promiseLiveUrl(library))
			.thenReturn(Promise.empty())
			.thenReturn(new Promise<>(firstUrlProvider));

		final TestConnections testConnections = mock(TestConnections.class);
		when(testConnections.promiseIsConnectionPossible(any()))
				.thenReturn(new Promise<>(false));

		final LibraryConnectionProvider libraryConnectionProvider = new LibraryConnectionProvider(
			libraryProvider,
			new NoopServerAlarm(),
			liveUrlProvider,
			testConnections,
			OkHttpFactory.getInstance());

		final LibraryId libraryId = new LibraryId(2);
		final FuturePromise<IConnectionProvider> futureConnectionProvider = new FuturePromise<>(libraryConnectionProvider
			.promiseLibraryConnection(libraryId)
			.updates(statuses::add)
			.eventually(
				c -> libraryConnectionProvider.promiseTestedLibraryConnection(libraryId).updates(statuses::add),
				c -> libraryConnectionProvider.promiseTestedLibraryConnection(libraryId).updates(statuses::add)));

		libraryDeferredPromise.resolve();
		secondLibraryDeferredPromise.resolve();

		connectionProvider = futureConnectionProvider.get();
	}

	@Test
	public void thenTheConnectionIsCorrect() {
		assertThat(connectionProvider.getUrlProvider()).isEqualTo(firstUrlProvider);
	}

	@Test
	public void thenGettingLibraryIsBroadcast() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionFailed,
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete);
	}
}
