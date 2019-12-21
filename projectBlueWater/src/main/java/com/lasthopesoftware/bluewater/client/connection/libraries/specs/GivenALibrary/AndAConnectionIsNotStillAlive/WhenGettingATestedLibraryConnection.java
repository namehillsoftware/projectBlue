package com.lasthopesoftware.bluewater.client.connection.libraries.specs.GivenALibrary.AndAConnectionIsNotStillAlive;

import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl;
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
	public static void before() throws InterruptedException, ExecutionException {

		final Library library = new Library()
			.setId(2)
			.setAccessCode("aB5nf");

		final ILibraryProvider libraryProvider = mock(ILibraryProvider.class);
		when(libraryProvider.getLibrary(2)).thenReturn(new Promise<>(library));

		final ProvideLiveUrl liveUrlProvider = mock(ProvideLiveUrl.class);
		when(liveUrlProvider.promiseLiveUrl(library)).thenReturn(new Promise<>(firstUrlProvider));

		final FakeSelectedLibraryProvider fakeSelectedLibraryProvider = new FakeSelectedLibraryProvider();

		final TestConnections testConnections = mock(TestConnections.class);
		when(testConnections.promiseIsConnectionPossible(any()))
				.thenReturn(new Promise<>(false));

//		try (SessionConnectionReservation ignored = new SessionConnectionReservation()) {
//			fakeSelectedLibraryProvider.selectedLibraryId = 2;
//			final SessionConnection sessionConnection = new SessionConnection(
//				localBroadcastManager,
//				fakeSelectedLibraryProvider,
//				libraryProvider,
//				(provider) -> new Promise<>(Collections.singletonList(new Item(5))),
//				Promise::new,
//				liveUrlProvider,
//				testConnections,
//				OkHttpFactory.getInstance());
//
//			connectionProvider = new FuturePromise<>(sessionConnection.promiseSessionConnection()).get();
//			secondConnectionProvider = new FuturePromise<>(sessionConnection.promiseTestedSessionConnection()).get();
//		}

		final LibraryConnectionProvider libraryConnectionProvider = new LibraryConnectionProvider(null, null);

		final LibraryId libraryId = new LibraryId(2);
		connectionProvider = new FuturePromise<>(libraryConnectionProvider
			.promiseLibraryConnection(libraryId)
			.updates(statuses::add))
			.get();

		secondConnectionProvider = new FuturePromise<>(libraryConnectionProvider
			.promiseTestedLibraryConnection(libraryId)
			.updates(statuses::add))
			.get();
	}

	@Test
	public void thenTheConnectionIsCorrect() {
		assertThat(secondConnectionProvider.getUrlProvider()).isEqualTo(connectionProvider.getUrlProvider());
	}

	@Test
	public void thenGettingLibraryIsBroadcast() {
		Assertions.assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.GettingView,
				BuildingConnectionStatus.BuildingSessionComplete,
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingSessionComplete);
	}

	private static class FakeSelectedLibraryProvider implements ISelectedLibraryIdentifierProvider {

		int selectedLibraryId;

		@Override
		public int getSelectedLibraryId() {
			return selectedLibraryId;
		}
	}
}
