package com.lasthopesoftware.bluewater.servers.connection;

import android.content.Context;

import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.servers.library.access.LibraryViewsProvider;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.store.Library;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

public class SessionConnection {

	private static final AtomicBoolean isRunning = new AtomicBoolean();
	private static volatile BuildingSessionConnectionStatus buildingStatus = BuildingSessionConnectionStatus.GETTING_LIBRARY;
	private static final CopyOnWriteArraySet<OnBuildSessionStateChangeListener> buildSessionListeners = new CopyOnWriteArraySet<>();
	private static final EnumSet<BuildingSessionConnectionStatus> runningConditions = EnumSet.of(BuildingSessionConnectionStatus.GETTING_LIBRARY, BuildingSessionConnectionStatus.BUILDING_CONNECTION, BuildingSessionConnectionStatus.GETTING_VIEW);
	private static final EnumSet<BuildingSessionConnectionStatus> completeConditions = EnumSet.of(BuildingSessionConnectionStatus.GETTING_LIBRARY_FAILED, BuildingSessionConnectionStatus.BUILDING_CONNECTION_FAILED, BuildingSessionConnectionStatus.GETTING_VIEW_FAILED, BuildingSessionConnectionStatus.BUILDING_SESSION_COMPLETE);

	private static ConnectionProvider sessionConnectionProvider;

	public static HttpURLConnection getSessionConnection(String... params) throws IOException {
		return sessionConnectionProvider.getConnection(params);
	}
	
	public static synchronized BuildingSessionConnectionStatus build(final Context context, OnBuildSessionStateChangeListener buildSessionStateChangeListener) {
		buildSessionListeners.add(buildSessionStateChangeListener);
		
		if (isRunning.get()) return buildingStatus;
		
		doStateChange(BuildingSessionConnectionStatus.GETTING_LIBRARY);
		LibrarySession.GetActiveLibrary(context, new OnCompleteListener<Integer, Void, Library>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, final Library library) {
				if (library == null || library.getAccessCode() == null || library.getAccessCode().isEmpty()) {
					doStateChange(BuildingSessionConnectionStatus.GETTING_LIBRARY_FAILED);
					isRunning.set(false);
					return;
				}
				
				doStateChange(BuildingSessionConnectionStatus.BUILDING_CONNECTION);
				
				AccessConfigurationBuilder.buildConfiguration(context, library.getAccessCode(), library.getAuthKey(), library.isLocalOnly(), new OnCompleteListener<Void, Void, AccessConfiguration>() {

					@Override
					public void onComplete(ISimpleTask<Void, Void, AccessConfiguration> owner, AccessConfiguration result) {
						if (result == null) {
							doStateChange(BuildingSessionConnectionStatus.BUILDING_CONNECTION_FAILED);
							return;
						}

						sessionConnectionProvider = new ConnectionProvider(result);
						
						if (library.getSelectedView() >= 0) {
							doStateChange(BuildingSessionConnectionStatus.BUILDING_SESSION_COMPLETE);
							return;
						}

						doStateChange(BuildingSessionConnectionStatus.GETTING_VIEW);

						LibraryViewsProvider.provide()
								.onComplete(new OnCompleteListener<Void, Void, List<Item>>() {

									@Override
									public void onComplete(ISimpleTask<Void, Void, List<Item>> owner, List<Item> result) {

										if (result == null || result.size() == 0) {
											doStateChange(BuildingSessionConnectionStatus.GETTING_VIEW_FAILED);
											return;
										}

										doStateChange(BuildingSessionConnectionStatus.GETTING_VIEW);
										final int selectedView = result.get(0).getKey();
										library.setSelectedView(selectedView);

										LibrarySession.SaveLibrary(context, library, new OnCompleteListener<Void, Void, Library>() {

											@Override
											public void onComplete(ISimpleTask<Void, Void, Library> owner, Library result) {
												doStateChange(BuildingSessionConnectionStatus.BUILDING_SESSION_COMPLETE);
											}
										});
									}
								})
								.execute();
					}
				});
			}
			
		});
		
		return buildingStatus;
	}

	public static void refresh() {

	}
	
	private static void doStateChange(final BuildingSessionConnectionStatus status) {
		buildingStatus = status;
		
		if (runningConditions.contains(status))
			isRunning.set(true);
		
		for (OnBuildSessionStateChangeListener listener : buildSessionListeners)
			listener.onBuildSessionStatusChange(status);
		
		if (status == BuildingSessionConnectionStatus.BUILDING_SESSION_COMPLETE)
			LoggerFactory.getLogger(LibrarySession.class).info("Session started.");
		
		if (completeConditions.contains(status)) {
			isRunning.set(false);
			buildSessionListeners.clear();
		}
	}
	
	public interface OnBuildSessionStateChangeListener {
		void onBuildSessionStatusChange(BuildingSessionConnectionStatus status);
	}
	
	public enum BuildingSessionConnectionStatus {
		GETTING_LIBRARY,
		GETTING_LIBRARY_FAILED,
		BUILDING_CONNECTION,
		BUILDING_CONNECTION_FAILED,
		GETTING_VIEW,
		GETTING_VIEW_FAILED,
		BUILDING_SESSION_COMPLETE
	}
}
