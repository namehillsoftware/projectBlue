package com.lasthopesoftware.bluewater.client.connection.session.GivenASelectedLibrary.AndGettingTheLibraryFaults;

import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.test.core.app.ApplicationProvider;

import com.annimon.stream.Stream;
import com.lasthopesoftware.AndroidContext;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnectionReservation;
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.lasthopesoftware.resources.BroadcastRecorder;
import com.lasthopesoftware.resources.ScopedLocalBroadcastManager;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.GettingLibrary;
import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.GettingLibraryFailed;
import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.buildSessionBroadcastStatus;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenRetrievingTheSessionConnection extends AndroidContext {

	private static final BroadcastRecorder broadcastRecorder = new BroadcastRecorder();
	private static IConnectionProvider connectionProvider;
	private static IOException exception;

	@Override
	public void before() throws ExecutionException, InterruptedException, IllegalAccessException, InstantiationException, InvocationTargetException {

		final LocalBroadcastManager localBroadcastManager = ScopedLocalBroadcastManager.newScopedBroadcastManager(ApplicationProvider.getApplicationContext());
		localBroadcastManager.registerReceiver(
			broadcastRecorder,
			new IntentFilter(SessionConnection.buildSessionBroadcast));

		final ProvideLibraryConnections libraryConnections = mock(ProvideLibraryConnections.class);
		final DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider> deferredConnectionProvider = new DeferredProgressingPromise<>();
		when(libraryConnections.promiseLibraryConnection(new LibraryId(2)))
			.thenReturn(deferredConnectionProvider);

		try (SessionConnectionReservation ignored = new SessionConnectionReservation()) {
			final SessionConnection sessionConnection = new SessionConnection(
				localBroadcastManager,
				() -> new LibraryId(2),
				libraryConnections);

			final FuturePromise<IConnectionProvider> futureConnectionProvider = new FuturePromise<>(sessionConnection.promiseSessionConnection());

			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.GettingLibrary);
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.GettingLibraryFailed);
			deferredConnectionProvider.sendRejection(new IOException("OMG"));

			try {
				connectionProvider = futureConnectionProvider.get();
			} catch (ExecutionException e) {
				if (e.getCause() instanceof IOException)
					exception = (IOException) e.getCause();
			}
		}
	}

	@Test
	public void thenAConnectionProviderIsNotReturned() {
		assertThat(connectionProvider).isNull();
	}

	@Test
	public void thenAnIOExceptionIsReturned() {
		assertThat(exception).isNotNull();
	}

	@Test
	public void thenGettingLibraryIsBroadcast() {
		Assertions.assertThat(Stream.of(broadcastRecorder.recordedIntents).map(i -> i.getIntExtra(buildSessionBroadcastStatus, -1)).toList())
			.containsExactly(
				GettingLibrary,
				GettingLibraryFailed);
	}
}
