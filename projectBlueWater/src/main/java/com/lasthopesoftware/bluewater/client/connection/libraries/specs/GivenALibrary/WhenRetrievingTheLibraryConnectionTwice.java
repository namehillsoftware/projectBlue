package com.lasthopesoftware.bluewater.client.connection.libraries.specs.GivenALibrary;

import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl;
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.DeferredPromise;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenRetrievingTheLibraryConnectionTwice {

	private static final IUrlProvider firstUrlProvider = mock(IUrlProvider.class);
	private static final List<BuildingConnectionStatus> statuses = new ArrayList<>();
	private static IConnectionProvider connectionProvider;
	private static IConnectionProvider secondConnectionProvider;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final Library library = new Library()
			.setId(2)
			.setAccessCode("aB5nf");

		final DeferredPromise<Library> libraryDeferredPromise = new DeferredPromise<>(library);

		final ILibraryProvider libraryProvider = mock(ILibraryProvider.class);
		when(libraryProvider.getLibrary(2)).thenReturn(libraryDeferredPromise);

		final ProvideLiveUrl liveUrlProvider = mock(ProvideLiveUrl.class);
		when(liveUrlProvider.promiseLiveUrl(library)).thenReturn(new Promise<>(firstUrlProvider));

		final LibraryConnectionProvider libraryConnectionProvider = new LibraryConnectionProvider(
			libraryProvider,
			Promise::new,
			liveUrlProvider,
			(provider) -> new Promise<>(Collections.singletonList(new Item(5))),
			OkHttpFactory.getInstance());

		final FuturePromise<IConnectionProvider> futureConnectionProvider = new FuturePromise<>(libraryConnectionProvider
			.promiseLibraryConnection(new LibraryId(2))
			.updates(statuses::add));

		final FuturePromise<IConnectionProvider> secondFutureConnectionProvider = new FuturePromise<>(libraryConnectionProvider
			.promiseLibraryConnection(new LibraryId(2))
			.updates(statuses::add));

		libraryDeferredPromise.resolve();

		connectionProvider = futureConnectionProvider.get();
		secondConnectionProvider = secondFutureConnectionProvider.get();
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
				BuildingConnectionStatus.GettingView,
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
