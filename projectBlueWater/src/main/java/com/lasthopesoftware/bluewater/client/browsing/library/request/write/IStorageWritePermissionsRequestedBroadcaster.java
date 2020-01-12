package com.lasthopesoftware.bluewater.client.browsing.library.request.write;

/**
 * Created by david on 7/3/16.
 */
public interface IStorageWritePermissionsRequestedBroadcaster {
	void sendWritePermissionsNeededBroadcast(int libraryId);
}
