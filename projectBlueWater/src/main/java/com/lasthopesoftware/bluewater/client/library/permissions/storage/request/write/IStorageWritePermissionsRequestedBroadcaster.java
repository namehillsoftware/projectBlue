package com.lasthopesoftware.bluewater.client.library.permissions.storage.request.write;

/**
 * Created by david on 7/3/16.
 */
public interface IStorageWritePermissionsRequestedBroadcaster {
	void sendWritePermissionsNeededBroadcast(int libraryId);
}
