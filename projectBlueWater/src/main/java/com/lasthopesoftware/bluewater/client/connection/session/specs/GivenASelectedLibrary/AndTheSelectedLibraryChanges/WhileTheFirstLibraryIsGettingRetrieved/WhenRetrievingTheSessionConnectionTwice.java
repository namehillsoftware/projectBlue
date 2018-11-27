package com.lasthopesoftware.bluewater.client.connection.session.specs.GivenASelectedLibrary.AndTheSelectedLibraryChanges.WhileTheFirstLibraryIsGettingRetrieved;

import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.connection.session.specs.SessionConnectionReservation;
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.resources.specs.BroadcastRecorder;
import com.lasthopesoftware.resources.specs.ScopedLocalBroadcastManagerBuilder;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.*;
import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.buildSessionBroadcastStatus;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenRetrievingTheSessionConnectionTwice extends AndroidContext {

	private static final BroadcastRecorder broadcastRecorder = new BroadcastRecorder();
	private static final IUrlProvider firstUrlProvider = mock(IUrlProvider.class);
	private static final IUrlProvider secondUrlProvider = mock(IUrlProvider.class);
	private static IConnectionProvider connectionProvider;

	@Override
	public void before() throws ExecutionException, InterruptedException, IllegalAccessException, InstantiationException, InvocationTargetException {

		final Library library = new Library()
			.setId(2)
			.setAccessCode("aB5nf");

		final Library secondLibrary = new Library().setId(1).setAccessCode("b");

		final ILibraryProvider libraryProvider = mock(ILibraryProvider.class);
		when(libraryProvider.getLibrary(2)).thenReturn(new Promise<>(library));

		final DeferredPromise<Library> deferredSelectedLibraryPromise = new DeferredPromise<>();
		when(libraryProvider.getLibrary(1)).thenReturn(deferredSelectedLibraryPromise);

		final ProvideLiveUrl liveUrlProvider = mock(ProvideLiveUrl.class);
		when(liveUrlProvider.promiseLiveUrl(library)).thenReturn(new Promise<>(firstUrlProvider));
		when(liveUrlProvider.promiseLiveUrl(secondLibrary)).thenReturn(new Promise<>(secondUrlProvider));

		final FakeSelectedLibraryProvider fakeSelectedLibraryProvider = new FakeSelectedLibraryProvider();

		final LocalBroadcastManager localBroadcastManager = ScopedLocalBroadcastManagerBuilder.newScopedBroadcastManager(RuntimeEnvironment.application);
		localBroadcastManager.registerReceiver(
			broadcastRecorder,
			new IntentFilter(SessionConnection.buildSessionBroadcast));

		try (SessionConnectionReservation ignored = new SessionConnectionReservation()) {
			fakeSelectedLibraryProvider.selectedLibraryId = 2;
			final SessionConnection sessionConnection = new SessionConnection(
				localBroadcastManager,
				fakeSelectedLibraryProvider,
				libraryProvider,
				(provider) -> new Promise<>(Collections.singletonList(new Item(5))),
				Promise::new,
				liveUrlProvider,
				mock(TestConnections.class));

			final Promise<IConnectionProvider> promisedConnectionProvider = sessionConnection.promiseSessionConnection();

			fakeSelectedLibraryProvider.selectedLibraryId = 1;

			final Promise<IConnectionProvider> secondPromisedConnectionProvider = promisedConnectionProvider
					.eventually(p -> sessionConnection.promiseSessionConnection());

			deferredSelectedLibraryPromise.sendResolution(secondLibrary);

			connectionProvider = new FuturePromise<>(secondPromisedConnectionProvider).get();
		}
	}

	@Test
	public void thenTheConnectionIsCorrect() {
		assertThat(connectionProvider.getUrlProvider()).isEqualTo(secondUrlProvider);
	}

	@Test
	public void thenGettingLibraryIsBroadcast() {
		Assertions.assertThat(Stream.of(broadcastRecorder.recordedIntents).map(i -> i.getIntExtra(buildSessionBroadcastStatus, -1)).toList())
			.containsExactly(
				GettingLibrary,
				BuildingConnection,
				GettingView,
				BuildingSessionComplete,
				GettingLibrary,
				BuildingConnection,
				GettingView,
				BuildingSessionComplete);
	}

	private static class FakeSelectedLibraryProvider implements ISelectedLibraryIdentifierProvider {

		int selectedLibraryId;

		@Override
		public int getSelectedLibraryId() {
			return selectedLibraryId;
		}
	}

	private static class DeferredPromise<T> extends Promise<T> {
		public void sendResolution(T resolution) {
			resolve(resolution);
		}
	}
}
