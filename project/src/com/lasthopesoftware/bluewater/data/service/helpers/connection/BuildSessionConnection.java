package com.lasthopesoftware.bluewater.data.service.helpers.connection;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.LoggerFactory;

import android.content.Context;

import com.lasthopesoftware.bluewater.data.service.access.IDataTask;
import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.bluewater.data.service.objects.FileSystem;
import com.lasthopesoftware.bluewater.data.service.objects.FileSystem.OnGetFileSystemCompleteListener;
import com.lasthopesoftware.bluewater.data.service.objects.IItem;
import com.lasthopesoftware.bluewater.data.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

public class BuildSessionConnection {

	private static final AtomicBoolean isRunning = new AtomicBoolean();
	private static volatile BuildingSessionConnectionStatus mBuildingStatus = BuildingSessionConnectionStatus.GETTING_LIBRARY;
	private static final CopyOnWriteArraySet<OnBuildSessionStateChangeListener> mBuildSessionListeners = new CopyOnWriteArraySet<OnBuildSessionStateChangeListener>();
	private static final EnumSet<BuildingSessionConnectionStatus> mRunningConditions = EnumSet.of(BuildingSessionConnectionStatus.GETTING_LIBRARY, BuildingSessionConnectionStatus.BUILDING_CONNECTION, BuildingSessionConnectionStatus.GETTING_VIEW);
	private static final EnumSet<BuildingSessionConnectionStatus> mCompleteConditions = EnumSet.of(BuildingSessionConnectionStatus.GETTING_LIBRARY_FAILED, BuildingSessionConnectionStatus.BUILDING_CONNECTION_FAILED, BuildingSessionConnectionStatus.GETTING_VIEW_FAILED, BuildingSessionConnectionStatus.BUILDING_SESSION_COMPLETE);
	
	public static synchronized BuildingSessionConnectionStatus build(final Context context, OnBuildSessionStateChangeListener buildSessionStateChangeListener) {
		mBuildSessionListeners.add(buildSessionStateChangeListener);
		
		if (isRunning.get()) return mBuildingStatus;
		
		doStateChange(BuildingSessionConnectionStatus.GETTING_LIBRARY);
		LibrarySession.GetLibrary(context, new OnCompleteListener<Integer, Void, Library>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {				
				if (result == null || result.getAccessCode() == null || result.getAccessCode().isEmpty()) {
					doStateChange(BuildingSessionConnectionStatus.GETTING_LIBRARY_FAILED);
					isRunning.set(false);
					return;
				}
				
				doStateChange(BuildingSessionConnectionStatus.BUILDING_CONNECTION);
				final Library library = result;
				
				ConnectionManager.buildConfiguration(context, library.getAccessCode(), library.getAuthKey(), library.isLocalOnly(), new OnCompleteListener<Integer, Void, Boolean>() {

					@Override
					public void onComplete(ISimpleTask<Integer, Void, Boolean> owner, Boolean result) {
						if (result == Boolean.FALSE) {
							doStateChange(BuildingSessionConnectionStatus.BUILDING_CONNECTION_FAILED);
							return;
						}
						
						if (library.getSelectedView() >= 0) {
							doStateChange(BuildingSessionConnectionStatus.BUILDING_SESSION_COMPLETE);
							return;
						}
			        	
						doStateChange(BuildingSessionConnectionStatus.GETTING_VIEW);

						FileSystem.Instance.get(context, new OnGetFileSystemCompleteListener() {
							
							@Override
							public void onGetFileSystemComplete(FileSystem fileSystem) {
								fileSystem.addOnItemsCompleteListener(new IDataTask.OnCompleteListener<List<IItem<?>>>() {
									
									@Override
									public void onComplete(ISimpleTask<String, Void, List<IItem<?>>> owner, List<IItem<?>> result) {
										
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
								});
								
								fileSystem.getSubItemsAsync();
							}
						});
					}
				});
			}
			
		});
		
		return mBuildingStatus;
	}
	
	private static void doStateChange(final BuildingSessionConnectionStatus status) {
		mBuildingStatus = status;
		
		if (mRunningConditions.contains(status))
			isRunning.set(true);
		
		for (OnBuildSessionStateChangeListener listener : mBuildSessionListeners)
			listener.onBuildSessionStatusChange(status);
		
		if (status == BuildingSessionConnectionStatus.BUILDING_SESSION_COMPLETE)
			LoggerFactory.getLogger(LibrarySession.class).info("Session started.");
		
		if (mCompleteConditions.contains(status)) {
			isRunning.set(false);
			mBuildSessionListeners.clear();
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
