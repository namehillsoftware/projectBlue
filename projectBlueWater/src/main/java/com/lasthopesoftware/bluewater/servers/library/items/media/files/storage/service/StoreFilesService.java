package com.lasthopesoftware.bluewater.servers.library.items.media.files.storage.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class StoreFilesService extends Service {

	private static final String setDownloadCompleteActivity = StoreFilesService.class.getCanonicalName() + "setDownloadComplete";

	public static void setDownloadComplete(long downloadId) {

	}

	public StoreFilesService() {
		
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);


	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class StoreMusicServiceBinder extends Binder {
		StoreFilesService getService() {
            return StoreFilesService.this;
        }
    }
	
	private final IBinder mBinder = new StoreMusicServiceBinder();
}
