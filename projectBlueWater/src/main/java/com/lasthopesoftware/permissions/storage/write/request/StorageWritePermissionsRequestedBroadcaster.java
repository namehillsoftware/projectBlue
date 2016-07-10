package com.lasthopesoftware.permissions.storage.write.request;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.permissions.storage.read.request.StorageReadPermissionsRequestedBroadcaster;

/**
 * Created by david on 7/3/16.
 */
public class StorageWritePermissionsRequestedBroadcaster implements IStorageWritePermissionsRequestedBroadcaster {
	public final static String WritePermissionsNeeded = MagicPropertyBuilder.buildMagicPropertyName(StorageReadPermissionsRequestedBroadcaster.class, "WritePermissionsNeeded");
	public final static String WritePermissionsLibraryId = MagicPropertyBuilder.buildMagicPropertyName(StorageWritePermissionsRequestedBroadcaster.class, "WritePermissionsLibraryId");

	private final LocalBroadcastManager localBroadcastManager;

	public StorageWritePermissionsRequestedBroadcaster(LocalBroadcastManager localBroadcastManager) {
		this.localBroadcastManager = localBroadcastManager;
	}

	@Override
	public void sendWritePermissionsNeededBroadcast(int libraryId) {
		final Intent writePermissionsNeededIntent = new Intent(WritePermissionsNeeded);
		writePermissionsNeededIntent.putExtra(WritePermissionsLibraryId, libraryId);
		localBroadcastManager.sendBroadcast(writePermissionsNeededIntent);
	}
}
