package com.lasthopesoftware.bluewater.client.connection.session;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.connection.AccessConfigurationBuilder;
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl;
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.access.views.LibraryViewsProvider;
import com.lasthopesoftware.bluewater.client.library.access.views.ProvideLibraryViews;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

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

	private static volatile int buildingStatus = BuildingSessionConnectionStatus.GettingLibrary;
	private static volatile ConnectionProvider sessionConnectionProvider;
	private static volatile Promise<IConnectionProvider> staticBuildingSessionConnectionPromise;
	private static volatile int staticSelectedLibraryId;

	private final Context context;
	private final ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider;
	private final ILibraryProvider libraryProvider;
	private final ProvideLibraryViews libraryViewsProvider;
	private final ILibraryStorage libraryStorage;
	private final ProvideLiveUrl liveUrlProvider;

	private volatile Promise<IConnectionProvider> buildingSessionConnectionPromise = Promise.empty();
	private volatile int selectedLibraryId = -1;

	public static ConnectionProvider getSessionConnectionProvider() {
		return sessionConnectionProvider;
	}

	public static boolean isBuilt() {
		return sessionConnectionProvider != null;
	}

	public static void refresh(final Context context) {
		if (sessionConnectionProvider == null)
			throw new NullPointerException("The session connection needs to be built first.");

		ConnectionTester.doTest(sessionConnectionProvider)
			.then(result -> {
				if (!result) build(context);

				final Intent refreshBroadcastIntent = new Intent(refreshSessionBroadcast);
				refreshBroadcastIntent.putExtra(isRefreshSuccessfulStatus, result);
				LocalBroadcastManager.getInstance(context).sendBroadcast(refreshBroadcastIntent);

				return null;
			});
	}

	public SessionConnection(
		Context context,
		ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider,
		ILibraryProvider libraryProvider,
		ProvideLibraryViews libraryViewsProvider,
		ILibraryStorage libraryStorage,
		ProvideLiveUrl liveUrlProvider) {
		this.context = context;
		this.selectedLibraryIdentifierProvider = selectedLibraryIdentifierProvider;
		this.libraryProvider = libraryProvider;
		this.libraryViewsProvider = libraryViewsProvider;
		this.libraryStorage = libraryStorage;
		this.liveUrlProvider = liveUrlProvider;
	}

	public Promise<IConnectionProvider> promiseSessionConnection() {
		final int newSelectedLibraryId = selectedLibraryIdentifierProvider.getSelectedLibraryId();
		synchronized (buildingConnectionPromiseSync) {
			if (selectedLibraryId == newSelectedLibraryId) {
				return buildingSessionConnectionPromise = buildingSessionConnectionPromise.eventually(
					c -> c != null ? new Promise<>(c) : promiseBuiltSessionConnection(selectedLibraryId),
					e -> promiseBuiltSessionConnection(selectedLibraryId));
			}

			selectedLibraryId = newSelectedLibraryId;

			return buildingSessionConnectionPromise = buildingSessionConnectionPromise
				.eventually($ -> promiseBuiltSessionConnection(newSelectedLibraryId));
		}
	}

	private Promise<IConnectionProvider> promiseBuiltSessionConnection(final int selectedLibraryId) {
		doStateChange(context, BuildingSessionConnectionStatus.GettingLibrary);
		return libraryProvider
			.getLibrary(selectedLibraryId)
			.eventually(library -> {
				if (library == null || library.getAccessCode() == null || library.getAccessCode().isEmpty()) {
					doStateChange(context, BuildingSessionConnectionStatus.GettingLibraryFailed);
					return Promise.empty();
				}

				doStateChange(context, BuildingSessionConnectionStatus.BuildingConnection);

				return liveUrlProvider
					.promiseLiveUrl(library)
					.eventually(urlProvider -> {
						if (urlProvider == null) {
							doStateChange(context, BuildingSessionConnectionStatus.BuildingConnectionFailed);
							return Promise.empty();
						}

						final IConnectionProvider localConnectionProvider = new ConnectionProvider(urlProvider);

						if (library.getSelectedView() >= 0) {
							doStateChange(context, BuildingSessionConnectionStatus.BuildingSessionComplete);
							return new Promise<>(localConnectionProvider);
						}

						doStateChange(context, BuildingSessionConnectionStatus.GettingView);

						return libraryViewsProvider
							.promiseLibraryViewsFromConnection(localConnectionProvider)
							.eventually(libraryViews -> {
								if (libraryViews == null || libraryViews.size() == 0) {
									doStateChange(context, BuildingSessionConnectionStatus.GettingViewFailed);
									return new Promise<>(localConnectionProvider);
								}

								final int selectedView = libraryViews.get(0).getKey();
								library.setSelectedView(selectedView);
								library.setSelectedViewType(Library.ViewType.StandardServerView);

								return libraryStorage
									.saveLibrary(library)
									.then(savedLibrary -> {
										doStateChange(context, BuildingSessionConnectionStatus.BuildingSessionComplete);
										return localConnectionProvider;
									});
							});
					}, e -> {
						doStateChange(context, BuildingSessionConnectionStatus.BuildingConnectionFailed);
						return new Promise<>(e);
					});
			});
	}

	public static int build(final Context context) {
		final ISelectedLibraryIdentifierProvider libraryIdentifierProvider = new SelectedBrowserLibraryIdentifierProvider(context);
		final int newSelectedLibraryId = libraryIdentifierProvider.getSelectedLibraryId();
		synchronized (buildingConnectionPromiseSync) {
			if (staticBuildingSessionConnectionPromise != null) {
				if (staticSelectedLibraryId == newSelectedLibraryId) return buildingStatus;

				staticSelectedLibraryId = newSelectedLibraryId;
			}

			staticBuildingSessionConnectionPromise = (staticBuildingSessionConnectionPromise != null
				? staticBuildingSessionConnectionPromise.eventually(v -> promiseBuiltSessionConnection(context, newSelectedLibraryId))
				: promiseBuiltSessionConnection(context, newSelectedLibraryId)).then(v -> {
					synchronized (buildingConnectionPromiseSync) {
						if (staticSelectedLibraryId == newSelectedLibraryId)
							staticBuildingSessionConnectionPromise = null;
						return null;
					}
				}, e -> {
					logger.error("There was an error building the session connection", e);
					doStateChange(context, BuildingSessionConnectionStatus.GettingViewFailed);

					synchronized (buildingConnectionPromiseSync) {
						if (staticSelectedLibraryId == newSelectedLibraryId)
							staticBuildingSessionConnectionPromise = null;
						return null;
					}
				});

			return buildingStatus;
		}
	}

	private static Promise<Void> promiseBuiltSessionConnection(final Context context, final int selectedLibraryId) {
		final LibraryRepository libraryRepository = new LibraryRepository(context);
		doStateChange(context, BuildingSessionConnectionStatus.GettingLibrary);
		return libraryRepository
			.getLibrary(selectedLibraryId)
			.eventually(library -> {
				if (library == null || library.getAccessCode() == null || library.getAccessCode().isEmpty()) {
					doStateChange(context, BuildingSessionConnectionStatus.GettingLibraryFailed);
					return Promise.empty();
				}

				doStateChange(context, BuildingSessionConnectionStatus.BuildingConnection);

				return
					AccessConfigurationBuilder
						.buildConfiguration(context, library)
						.eventually(urlProvider -> {
							if (urlProvider == null) {
								doStateChange(context, BuildingSessionConnectionStatus.BuildingConnectionFailed);
								return Promise.empty();
							}

							sessionConnectionProvider = new ConnectionProvider(urlProvider);

							if (library.getSelectedView() >= 0) {
								doStateChange(context, BuildingSessionConnectionStatus.BuildingSessionComplete);
								return Promise.empty();
							}

							doStateChange(context, BuildingSessionConnectionStatus.GettingView);

							return LibraryViewsProvider
								.provide(sessionConnectionProvider)
								.eventually(libraryViews -> {
									if (libraryViews == null || libraryViews.size() == 0) {
										doStateChange(context, BuildingSessionConnectionStatus.GettingViewFailed);
										return Promise.empty();
									}

									doStateChange(context, BuildingSessionConnectionStatus.GettingView);
									final int selectedView = libraryViews.get(0).getKey();
									library.setSelectedView(selectedView);
									library.setSelectedViewType(Library.ViewType.StandardServerView);

									return
										libraryRepository
											.saveLibrary(library)
											.then(savedLibrary -> {
												doStateChange(context, BuildingSessionConnectionStatus.BuildingSessionComplete);
												return null;
											});
								});
						});
			});
	}

	private static void doStateChange(final Context context, final int status) {
		buildingStatus = status;

		final Intent broadcastIntent = new Intent(buildSessionBroadcast);
		broadcastIntent.putExtra(buildSessionBroadcastStatus, status);
		LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);

		if (status == BuildingSessionConnectionStatus.BuildingSessionComplete)
			LoggerFactory.getLogger(LibrarySession.class).info("Session started.");
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
