package com.lasthopesoftware.bluewater.client.browsing.library.request.read;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;

public interface BroadcastReadPermissionsRequest {
	void sendReadPermissionsRequestedBroadcast(LibraryId libraryId);
}
