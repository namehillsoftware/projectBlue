package com.lasthopesoftware.bluewater.client.library.permissions.storage.request.read;

/**
 * Created by david on 7/3/16.
 */
public interface IStorageReadPermissionsRequestedBroadcast {
	void sendReadPermissionsRequestedBroadcast(int libraryId);
}
