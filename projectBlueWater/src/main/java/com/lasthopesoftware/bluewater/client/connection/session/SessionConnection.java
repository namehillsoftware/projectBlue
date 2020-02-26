package com.lasthopesoftware.bluewater.client.connection.session;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.IntDef;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.namehillsoftware.handoff.promises.Promise;
import com.vedsoft.futures.runnables.OneParameterAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class SessionConnection implements OneParameterAction<BuildingConnectionStatus> {

	public static final String buildSessionBroadcast = MagicPropertyBuilder.buildMagicPropertyName(SessionConnection.class, "buildSessionBroadcast");
	public static final String buildSessionBroadcastStatus = MagicPropertyBuilder.buildMagicPropertyName(SessionConnection.class, "buildSessionBroadcastStatus");

	private static final Logger logger = LoggerFactory.getLogger(SessionConnection.class);

	private static volatile SessionConnection sessionConnectionInstance;

	private final LocalBroadcastManager localBroadcastManager;
	private final ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider;
	private final ProvideLibraryConnections libraryConnections;

	public static synchronized SessionConnection getInstance(Context context) {
		if (sessionConnectionInstance != null) return sessionConnectionInstance;

		final Context applicationContext = context.getApplicationContext();

		return sessionConnectionInstance = new SessionConnection(
			LocalBroadcastManager.getInstance(applicationContext),
			new SelectedBrowserLibraryIdentifierProvider(applicationContext),
			LibraryConnectionProvider.Instance.get(applicationContext));
	}

	public SessionConnection(
		LocalBroadcastManager localBroadcastManager,
		ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider,
		ProvideLibraryConnections libraryConnections) {
		this.localBroadcastManager = localBroadcastManager;
		this.selectedLibraryIdentifierProvider = selectedLibraryIdentifierProvider;
		this.libraryConnections = libraryConnections;
	}

	public Promise<IConnectionProvider> promiseTestedSessionConnection() {
		final LibraryId newSelectedLibraryId = selectedLibraryIdentifierProvider.getSelectedLibraryId();

		return libraryConnections
			.promiseTestedLibraryConnection(newSelectedLibraryId)
			.updates(this);
	}

	public Promise<IConnectionProvider> promiseSessionConnection() {
		final LibraryId newSelectedLibraryId = selectedLibraryIdentifierProvider.getSelectedLibraryId();

		if (newSelectedLibraryId == null) return Promise.empty();

		return libraryConnections
			.promiseLibraryConnection(newSelectedLibraryId)
			.updates(this);
	}

	@Override
	public void runWith(BuildingConnectionStatus connectionStatus) {
		doStateChange(connectionStatus);
	}

	private void doStateChange(BuildingConnectionStatus status) {
		final Intent broadcastIntent = new Intent(buildSessionBroadcast);
		broadcastIntent.putExtra(buildSessionBroadcastStatus, BuildingSessionConnectionStatus.GetSessionConnectionStatus(status));
		localBroadcastManager.sendBroadcast(broadcastIntent);

		if (status == BuildingConnectionStatus.BuildingConnectionComplete)
			logger.info("Session started.");
	}

	public static class BuildingSessionConnectionStatus {

		@Retention(RetentionPolicy.SOURCE)
		@IntDef({GettingLibrary, GettingLibraryFailed, BuildingConnection, BuildingConnectionFailed, BuildingSessionComplete})
		@interface SessionConnectionStatus {}
		public static final int GettingLibrary = 1;
		public static final int GettingLibraryFailed = 2;
		public static final int BuildingConnection = 3;
		public static final int BuildingConnectionFailed = 4;
		public static final int BuildingSessionComplete = 7;

		@SessionConnectionStatus
		static int GetSessionConnectionStatus(BuildingConnectionStatus connectionStatus) {
			switch (connectionStatus) {
				case GettingLibrary: return GettingLibrary;
				case GettingLibraryFailed: return GettingLibraryFailed;
				case BuildingConnection: return BuildingConnection;
				case BuildingConnectionFailed: return BuildingConnectionFailed;
				case BuildingConnectionComplete: return BuildingSessionComplete;
			}

			throw new IndexOutOfBoundsException();
		}
	}
}
