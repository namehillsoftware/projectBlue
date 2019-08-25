package com.lasthopesoftware.bluewater.client.library.permissions.storage.request.read;

import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;

/**
 * Created by david on 7/3/16.
 */
public class StorageReadPermissionsRequestedBroadcaster implements IStorageReadPermissionsRequestedBroadcast {
	public final static String ReadPermissionsNeeded = MagicPropertyBuilder.buildMagicPropertyName(StorageReadPermissionsRequestedBroadcaster.class, "ReadPermissionsNeeded");
	public final static String ReadPermissionsLibraryId = MagicPropertyBuilder.buildMagicPropertyName(StorageReadPermissionsRequestedBroadcaster.class, "ReadPermissionsLibraryId");

	private final LocalBroadcastManager localBroadcastManager;

	public StorageReadPermissionsRequestedBroadcaster(LocalBroadcastManager localBroadcastManager) {
		this.localBroadcastManager = localBroadcastManager;
	}

	@Override
	public void sendReadPermissionsRequestedBroadcast(int libraryId) {
		final Intent readPermissionsNeededIntent = new Intent(ReadPermissionsNeeded);
		readPermissionsNeededIntent.putExtra(ReadPermissionsLibraryId, libraryId);
		localBroadcastManager.sendBroadcast(readPermissionsNeededIntent);
	}
}
