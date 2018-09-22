package com.lasthopesoftware.bluewater.client.connection.session.specs.GivenASelectedLibrary.AndALiveUrlIsNotFound;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.connection.session.specs.SessionConnectionReservation;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenRetrievingTheSessionConnectionTwice extends AndroidContext {

	private static final IUrlProvider firstUrlProvider = mock(IUrlProvider.class);
	private static IConnectionProvider connectionProvider;

	@Override
	public void before() throws ExecutionException, InterruptedException {

		final Library library = new Library()
			.setId(2)
			.setAccessCode("aB5nf");

		final ILibraryProvider libraryProvider = mock(ILibraryProvider.class);
		when(libraryProvider.getLibrary(2)).thenReturn(new Promise<>(library));

		final ProvideLiveUrl liveUrlProvider = mock(ProvideLiveUrl.class);
		when(liveUrlProvider.promiseLiveUrl(library))
			.thenReturn(new Promise<>(new IOException("An error!")))
			.thenReturn(new Promise<>(firstUrlProvider));

		final FakeSelectedLibraryProvider fakeSelectedLibraryProvider = new FakeSelectedLibraryProvider();

		try (SessionConnectionReservation ignored = new SessionConnectionReservation()) {
			final SessionConnection sessionConnection = new SessionConnection(
				RuntimeEnvironment.application,
				fakeSelectedLibraryProvider,
				libraryProvider,
				(provider) -> new Promise<>(Collections.singletonList(new Item(5))),
				Promise::new,
				liveUrlProvider);

			connectionProvider = new FuturePromise<>(
				sessionConnection.promiseSessionConnection()
					.eventually(
						c -> sessionConnection.promiseSessionConnection(),
						e -> sessionConnection.promiseSessionConnection())).get();
		}
	}

	@Test
	public void thenTheConnectionIsCorrect() {
		assertThat(connectionProvider.getUrlProvider()).isEqualTo(firstUrlProvider);
	}

	private static class FakeSelectedLibraryProvider implements ISelectedLibraryIdentifierProvider {

		final int selectedLibraryId = 2;

		@Override
		public int getSelectedLibraryId() {
			return selectedLibraryId;
		}
	}
}
