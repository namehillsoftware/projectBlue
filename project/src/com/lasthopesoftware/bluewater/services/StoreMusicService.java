package com.lasthopesoftware.bluewater.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class StoreMusicService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class StoreMusicServiceBinder extends Binder {
		StoreMusicService getService() {
            return StoreMusicService.this;
        }
    }
	
	private final IBinder mBinder = new StoreMusicServiceBinder();
}
