package com.lasthopesoftware.permissions.storage.write;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.permissions.storage.read.request.StorageReadPermissionsRequestedBroadcaster;

/**
 * Created by david on 7/3/16.
 */
public class StorageWritePermissionsNeededBroadcast implements IStorageWritePermissionsNeededBroadcast {
	public final static String WritePermissionsNeeded = MagicPropertyBuilder.buildMagicPropertyName(StorageReadPermissionsRequestedBroadcaster.class, "WritePermissionsNeeded");

	private final LocalBroadcastManager localBroadcastManager;

	public StorageWritePermissionsNeededBroadcast(LocalBroadcastManager localBroadcastManager) {
		this.localBroadcastManager = localBroadcastManager;
	}

	@Override
	public void sendWritePermissionsNeededBroadcast() {
		localBroadcastManager.sendBroadcast(new Intent(WritePermissionsNeeded));
	}
}
