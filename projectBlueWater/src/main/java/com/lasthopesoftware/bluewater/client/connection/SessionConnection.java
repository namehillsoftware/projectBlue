package com.lasthopesoftware.bluewater.client.connection;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.connection.helpers.ConnectionTester;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.access.LibraryViewsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.messenger.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.ILazy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.lasthopesoftware.bluewater.client.connection.SessionConnection.BuildingSessionConnectionStatus.completeConditions;
import static com.lasthopesoftware.bluewater.client.connection.SessionConnection.BuildingSessionConnectionStatus.runningConditions;
import static com.vedsoft.futures.callables.VoidFunc.runCarelessly;

public class SessionConnection {

	public static final String buildSessionBroadcast = MagicPropertyBuilder.buildMagicPropertyName(SessionConnection.class, "buildSessionBroadcast");
	public static final String buildSessionBroadcastStatus = MagicPropertyBuilder.buildMagicPropertyName(SessionConnection.class, "buildSessionBroadcastStatus");
	public static final String refreshSessionBroadcast = MagicPropertyBuilder.buildMagicPropertyName(SessionConnection.class, "refreshSessionBroadcast");
	public static final String isRefreshSuccessfulStatus = MagicPropertyBuilder.buildMagicPropertyName(SessionConnection.class, "isRefreshSuccessfulStatus");

	private static volatile boolean isRunning;
	private static volatile int buildingStatus = BuildingSessionConnectionStatus.GettingLibrary;

	private static final Logger logger = LoggerFactory.getLogger(SessionConnection.class);

	private static ConnectionProvider sessionConnectionProvider;

	public static HttpURLConnection getSessionConnection(String... params) throws IOException {
		return sessionConnectionProvider.getConnection(params);
	}

	public static ConnectionProvider getSessionConnectionProvider() {
		return sessionConnectionProvider;
	}

	public static boolean isBuilt() {
		return sessionConnectionProvider != null;
	}
	
	public static synchronized int build(final Context context) {
		if (isRunning) return buildingStatus;
		
		doStateChange(context, BuildingSessionConnectionStatus.GettingLibrary);
		final ISelectedLibraryIdentifierProvider libraryIdentifierProvider = new SelectedBrowserLibraryIdentifierProvider(context);

		final LibraryRepository libraryRepository = new LibraryRepository(context);
		libraryRepository
			.getLibrary(libraryIdentifierProvider.getSelectedLibraryId())
			.eventually(library -> {
				if (library == null || library.getAccessCode() == null || library.getAccessCode().isEmpty()) {
					doStateChange(context, BuildingSessionConnectionStatus.GettingLibraryFailed);
					isRunning = false;
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
											.then(runCarelessly(savedLibrary -> doStateChange(context, BuildingSessionConnectionStatus.BuildingSessionComplete)));
								});
							});
				})
				.excuse(e -> {
					logger.error("There was an error building the session connection", e);
					doStateChange(context, BuildingSessionConnectionStatus.GettingViewFailed);

					return null;
				});
		
		return buildingStatus;
	}

	public static void refresh(final Context context) {
		refresh(context, -1);
	}

	private static void refresh(final Context context, final int timeout) {
		if (sessionConnectionProvider == null)
			throw new NullPointerException("The session connection needs to be built first.");

		final Promise<Boolean> promisedConnectionTest =
			timeout > 0
				? ConnectionTester.doTest(sessionConnectionProvider, timeout)
				: ConnectionTester.doTest(sessionConnectionProvider);

		promisedConnectionTest
			.then(result -> {
				if (!result) build(context);

				final Intent refreshBroadcastIntent = new Intent(refreshSessionBroadcast);
				refreshBroadcastIntent.putExtra(isRefreshSuccessfulStatus, result);
				LocalBroadcastManager.getInstance(context).sendBroadcast(refreshBroadcastIntent);

				return null;
			});
	}
	
	private static void doStateChange(final Context context, final int status) {
		buildingStatus = status;
		
		if (runningConditions.contains(status)) isRunning = true;

		final Intent broadcastIntent = new Intent(buildSessionBroadcast);
		broadcastIntent.putExtra(buildSessionBroadcastStatus, status);
		LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
		
		if (status == BuildingSessionConnectionStatus.BuildingSessionComplete)
			LoggerFactory.getLogger(LibrarySession.class).info("Session started.");
		
		if (completeConditions.contains(status)) isRunning = false;
	}

	public static class BuildingSessionConnectionStatus {
		public static final int GettingLibrary = 1;
		public static final int GettingLibraryFailed = 2;
		public static final int BuildingConnection = 3;
		public static final int BuildingConnectionFailed = 4;
		public static final int GettingView = 5;
		public static final int GettingViewFailed = 6;
		public static final int BuildingSessionComplete = 7;

		private static final ILazy<Set<Integer>> runningConditionsLazy =
				new AbstractSynchronousLazy<Set<Integer>>() {
					@Override
					protected Set<Integer> initialize() throws Exception {
						return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(BuildingSessionConnectionStatus.GettingLibrary, BuildingSessionConnectionStatus.BuildingConnection, BuildingSessionConnectionStatus.GettingView)));
					}
				};

		private static final ILazy<Set<Integer>> completeConditionsLazy =
				new AbstractSynchronousLazy<Set<Integer>>() {
					@Override
					protected Set<Integer> initialize() throws Exception {
						return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(BuildingSessionConnectionStatus.GettingLibraryFailed, BuildingSessionConnectionStatus.BuildingConnectionFailed, BuildingSessionConnectionStatus.GettingViewFailed, BuildingSessionConnectionStatus.BuildingSessionComplete)));
					}
				};

		static final Set<Integer> runningConditions = runningConditionsLazy.getObject();
		public static final Set<Integer> completeConditions = completeConditionsLazy.getObject();
	}
}
