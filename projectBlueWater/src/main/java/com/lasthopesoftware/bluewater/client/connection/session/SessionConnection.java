package com.lasthopesoftware.bluewater.client.connection.session;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.BuildUrlProviders;
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner;
import com.lasthopesoftware.bluewater.client.connection.builder.live.LiveUrlProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfoXmlRequest;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup;
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester;
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.access.views.LibraryViewsProvider;
import com.lasthopesoftware.bluewater.client.library.access.views.ProvideLibraryViews;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.resources.network.ActiveNetworkFinder;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SessionConnection {

	public static final String buildSessionBroadcast = MagicPropertyBuilder.buildMagicPropertyName(SessionConnection.class, "buildSessionBroadcast");
	public static final String buildSessionBroadcastStatus = MagicPropertyBuilder.buildMagicPropertyName(SessionConnection.class, "buildSessionBroadcastStatus");
	public static final String refreshSessionBroadcast = MagicPropertyBuilder.buildMagicPropertyName(SessionConnection.class, "refreshSessionBroadcast");
	public static final String isRefreshSuccessfulStatus = MagicPropertyBuilder.buildMagicPropertyName(SessionConnection.class, "isRefreshSuccessfulStatus");

	private static final Logger logger = LoggerFactory.getLogger(SessionConnection.class);
	private static final Object buildingConnectionPromiseSync = new Object();

	private static final int buildConnectionTimeoutTime = 10000;

	private static final CreateAndHold<BuildUrlProviders> lazyUrlScanner = new AbstractSynchronousLazy<BuildUrlProviders>() {
		@Override
		protected BuildUrlProviders create() {
			final ServerLookup serverLookup = new ServerLookup(new ServerInfoXmlRequest(Duration.millis(buildConnectionTimeoutTime)));
			final ConnectionTester connectionTester = new ConnectionTester(Duration.millis(buildConnectionTimeoutTime));

			return new UrlScanner(connectionTester, serverLookup);
		}
	};

	private static volatile SessionConnection sessionConnectionInstance;

	private final LocalBroadcastManager localBroadcastManager;
	private final ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider;
	private final ILibraryProvider libraryProvider;
	private final ProvideLibraryViews libraryViewsProvider;
	private final ILibraryStorage libraryStorage;
	private final ProvideLiveUrl liveUrlProvider;
	private final TestConnections connectionTester;

	private volatile Promise<IConnectionProvider> buildingSessionConnectionPromise = Promise.empty();
	private volatile int selectedLibraryId = -1;

	public static synchronized SessionConnection getInstance(Context context) {
		if (sessionConnectionInstance != null) return sessionConnectionInstance;

		final Context applicationContext = context.getApplicationContext();

		return sessionConnectionInstance = new SessionConnection(
			LocalBroadcastManager.getInstance(applicationContext),
			new SelectedBrowserLibraryIdentifierProvider(applicationContext),
			new LibraryRepository(applicationContext),
			new LibraryViewsProvider(),
			new LibraryRepository(applicationContext),
			new LiveUrlProvider(
				new ActiveNetworkFinder(applicationContext),
				lazyUrlScanner.getObject()),
			new ConnectionTester(Duration.standardSeconds(30)));
	}

	public SessionConnection(
		LocalBroadcastManager localBroadcastManager,
		ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider,
		ILibraryProvider libraryProvider,
		ProvideLibraryViews libraryViewsProvider,
		ILibraryStorage libraryStorage,
		ProvideLiveUrl liveUrlProvider,
		TestConnections connectionTester) {
		this.localBroadcastManager = localBroadcastManager;
		this.selectedLibraryIdentifierProvider = selectedLibraryIdentifierProvider;
		this.libraryProvider = libraryProvider;
		this.libraryViewsProvider = libraryViewsProvider;
		this.libraryStorage = libraryStorage;
		this.liveUrlProvider = liveUrlProvider;
		this.connectionTester = connectionTester;
	}

	public Promise<IConnectionProvider> promiseTestedSessionConnection() {
		synchronized (buildingConnectionPromiseSync) {
			return buildingSessionConnectionPromise = buildingSessionConnectionPromise
				.eventually(c -> c != null
					? connectionTester.promiseIsConnectionPossible(c)
						.eventually(result -> result
							? new Promise<>(c)
							: promiseBuiltSessionConnection(selectedLibraryId))
					: promiseBuiltSessionConnection(selectedLibraryId),
				e -> promiseBuiltSessionConnection(selectedLibraryId));
		}
	}

	public Promise<IConnectionProvider> promiseSessionConnection() {
		final int newSelectedLibraryId = selectedLibraryIdentifierProvider.getSelectedLibraryId();
		synchronized (buildingConnectionPromiseSync) {
			if (selectedLibraryId == newSelectedLibraryId) {
				return buildingSessionConnectionPromise = buildingSessionConnectionPromise.eventually(
					c -> c != null ? new Promise<>(c) : promiseBuiltSessionConnection(newSelectedLibraryId),
					e -> promiseBuiltSessionConnection(newSelectedLibraryId));
			}

			selectedLibraryId = newSelectedLibraryId;

			return buildingSessionConnectionPromise = buildingSessionConnectionPromise
				.eventually(
					$ -> promiseBuiltSessionConnection(newSelectedLibraryId),
					$ -> promiseBuiltSessionConnection(newSelectedLibraryId));
		}
	}

	private Promise<IConnectionProvider> promiseBuiltSessionConnection(final int selectedLibraryId) {
		doStateChange(BuildingSessionConnectionStatus.GettingLibrary);
		return libraryProvider
			.getLibrary(selectedLibraryId)
			.eventually(library -> {
				if (library == null || library.getAccessCode() == null || library.getAccessCode().isEmpty()) {
					doStateChange(BuildingSessionConnectionStatus.GettingLibraryFailed);
					return Promise.empty();
				}

				doStateChange(BuildingSessionConnectionStatus.BuildingConnection);

				return liveUrlProvider
					.promiseLiveUrl(library)
					.eventually(urlProvider -> {
						if (urlProvider == null) {
							doStateChange(BuildingSessionConnectionStatus.BuildingConnectionFailed);
							return Promise.empty();
						}

						final IConnectionProvider localConnectionProvider = new ConnectionProvider(urlProvider);

						if (library.getSelectedView() >= 0) {
							doStateChange(BuildingSessionConnectionStatus.BuildingSessionComplete);
							return new Promise<>(localConnectionProvider);
						}

						doStateChange(BuildingSessionConnectionStatus.GettingView);

						return libraryViewsProvider
							.promiseLibraryViewsFromConnection(localConnectionProvider)
							.eventually(libraryViews -> {
								if (libraryViews == null || libraryViews.size() == 0) {
									doStateChange(BuildingSessionConnectionStatus.GettingViewFailed);
									return Promise.empty();
								}

								final int selectedView = libraryViews.get(0).getKey();
								library.setSelectedView(selectedView);
								library.setSelectedViewType(Library.ViewType.StandardServerView);

								return libraryStorage
									.saveLibrary(library)
									.then(savedLibrary -> {
										doStateChange(BuildingSessionConnectionStatus.BuildingSessionComplete);
										return localConnectionProvider;
									});
							}, e -> {
								doStateChange(BuildingSessionConnectionStatus.GettingViewFailed);
								return new Promise<>(e);
							});
					}, e -> {
						doStateChange(BuildingSessionConnectionStatus.BuildingConnectionFailed);
						return new Promise<>(e);
					});
			}, e -> {
				doStateChange(BuildingSessionConnectionStatus.GettingLibraryFailed);
				return new Promise<>(e);
			});
	}

	private void doStateChange(int status) {
		doStateChange(localBroadcastManager, status);
	}

	private static void doStateChange(final Context context, final int status) {
		doStateChange(LocalBroadcastManager.getInstance(context), status);
	}

	private static void doStateChange(LocalBroadcastManager localBroadcastManager, final int status) {

		final Intent broadcastIntent = new Intent(buildSessionBroadcast);
		broadcastIntent.putExtra(buildSessionBroadcastStatus, status);
		localBroadcastManager.sendBroadcast(broadcastIntent);

		if (status == BuildingSessionConnectionStatus.BuildingSessionComplete)
			logger.info("Session started.");
	}

	public static class BuildingSessionConnectionStatus {
		public static final int GettingLibrary = 1;
		public static final int GettingLibraryFailed = 2;
		public static final int BuildingConnection = 3;
		public static final int BuildingConnectionFailed = 4;
		public static final int GettingView = 5;
		public static final int GettingViewFailed = 6;
		public static final int BuildingSessionComplete = 7;

		private static final CreateAndHold<Set<Integer>> runningConditionsLazy =
				new AbstractSynchronousLazy<Set<Integer>>() {
					@Override
					protected Set<Integer> create() {
						return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(BuildingSessionConnectionStatus.GettingLibrary, BuildingSessionConnectionStatus.BuildingConnection, BuildingSessionConnectionStatus.GettingView)));
					}
				};

		private static final CreateAndHold<Set<Integer>> completeConditionsLazy =
				new AbstractSynchronousLazy<Set<Integer>>() {
					@Override
					protected Set<Integer> create() {
						return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(BuildingSessionConnectionStatus.GettingLibraryFailed, BuildingSessionConnectionStatus.BuildingConnectionFailed, BuildingSessionConnectionStatus.GettingViewFailed, BuildingSessionConnectionStatus.BuildingSessionComplete)));
					}
				};

		static final Set<Integer> runningConditions = runningConditionsLazy.getObject();
		public static final Set<Integer> completeConditions = completeConditionsLazy.getObject();
	}
}
