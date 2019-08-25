package com.lasthopesoftware.bluewater.client.library.permissions.storage.request.write;

import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;

/**
 * Created by david on 7/3/16.
 */
public class StorageWritePermissionsRequestedBroadcaster implements IStorageWritePermissionsRequestedBroadcaster {
	public final static String WritePermissionsNeeded = MagicPropertyBuilder.buildMagicPropertyName(StorageWritePermissionsRequestedBroadcaster.class, "WritePermissionsNeeded");
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
