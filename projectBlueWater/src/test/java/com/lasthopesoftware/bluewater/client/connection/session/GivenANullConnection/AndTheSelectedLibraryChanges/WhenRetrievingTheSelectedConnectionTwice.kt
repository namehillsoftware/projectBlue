package com.lasthopesoftware.bluewater.client.connection.session.GivenANullConnection.AndTheSelectedLibraryChanges;

import androidx.test.core.app.ApplicationProvider;

import com.lasthopesoftware.AndroidContext;
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions;
import com.lasthopesoftware.bluewater.client.connection.session.SelectedConnection;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnectionReservation;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise;
import com.lasthopesoftware.resources.FakeMessageSender;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenRetrievingTheSelectedConnectionTwice extends AndroidContext {

	private static final IUrlProvider firstUrlProvider = mock(IUrlProvider.class);
	private static IConnectionProvider connectionProvider;

	@Override
	public void before() throws ExecutionException, InterruptedException, IllegalAccessException, InstantiationException, InvocationTargetException {

		final ManageConnectionSessions libraryConnections = mock(ManageConnectionSessions.class);
		when(libraryConnections.promiseLibraryConnection(any())).thenReturn(new ProgressingPromise<>((IConnectionProvider)null));
		when(libraryConnections.promiseLibraryConnection(new LibraryId(2))).thenReturn(new ProgressingPromise<>(new ConnectionProvider(firstUrlProvider, OkHttpFactory.getInstance())));

		final FakeSelectedLibraryProvider fakeSelectedLibraryProvider = new FakeSelectedLibraryProvider();

		try (SessionConnectionReservation ignored = new SessionConnectionReservation()) {
			fakeSelectedLibraryProvider.selectedLibraryId = -1;
			final SelectedConnection selectedConnection = new SelectedConnection(
				new FakeMessageSender(ApplicationProvider.getApplicationContext()),
				fakeSelectedLibraryProvider,
				libraryConnections);

			connectionProvider = new FuturePromise<>(selectedConnection.promiseSessionConnection()).get();

			fakeSelectedLibraryProvider.selectedLibraryId = 2;

			connectionProvider = new FuturePromise<>(selectedConnection.promiseSessionConnection()).get();
		}
	}

	@Test
	public void thenTheConnectionIsCorrect() {
		assertThat(connectionProvider.getUrlProvider()).isEqualTo(firstUrlProvider);
	}

	private static class FakeSelectedLibraryProvider implements ISelectedLibraryIdentifierProvider {

		int selectedLibraryId;

		@Override
		public LibraryId getSelectedLibraryId() {
			return new LibraryId(selectedLibraryId);
		}
	}
}
