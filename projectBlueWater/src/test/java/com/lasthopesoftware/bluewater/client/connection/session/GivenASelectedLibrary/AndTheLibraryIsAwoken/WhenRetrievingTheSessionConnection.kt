package com.lasthopesoftware.bluewater.client.connection.session.GivenASelectedLibrary.AndTheLibraryIsAwoken;

import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.test.core.app.ApplicationProvider;

import com.annimon.stream.Stream;
import com.lasthopesoftware.AndroidContext;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus;
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnectionReservation;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.lasthopesoftware.resources.BroadcastRecorder;
import com.lasthopesoftware.resources.ScopedLocalBroadcastManager;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.BuildingConnection;
import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.BuildingSessionComplete;
import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.GettingLibrary;
import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.SendingWakeSignal;
import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.buildSessionBroadcastStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenRetrievingTheSessionConnection extends AndroidContext {

	private static final BroadcastRecorder broadcastRecorder = new BroadcastRecorder();
	private static final IUrlProvider urlProvider = mock(IUrlProvider.class);
	private static IConnectionProvider connectionProvider;

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
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.SendingWakeSignal);
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.BuildingConnection);
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.BuildingConnectionComplete);
			deferredConnectionProvider.sendResolution(new ConnectionProvider(urlProvider, OkHttpFactory.getInstance()));

			connectionProvider = futureConnectionProvider.get();
		}
	}

	@Test
	public void thenTheConnectionIsCorrect() {
		assertThat(connectionProvider.getUrlProvider()).isEqualTo(urlProvider);
	}

	@Test
	public void thenGettingLibraryIsBroadcast() {
		assertThat(Stream.of(broadcastRecorder.recordedIntents).map(i -> i.getIntExtra(buildSessionBroadcastStatus, -1)).toList())
			.containsExactly(
				GettingLibrary,
				SendingWakeSignal,
				BuildingConnection,
				BuildingSessionComplete);
	}
}
