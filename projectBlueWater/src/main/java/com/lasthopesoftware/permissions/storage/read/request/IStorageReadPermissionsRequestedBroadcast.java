package com.lasthopesoftware.permissions.storage.read.request;

/**
 * Created by david on 7/3/16.
 */
public interface IStorageReadPermissionsRequestedBroadcast {
	void sendReadPermissionsRequestedBroadcast(int libraryId);
}
