package com.lasthopesoftware.bluewater.client.connection.builder.lookup;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;

public class ServerDiscoveryException extends Exception {

	ServerDiscoveryException(LibraryId library, String serverMessage) {
		super("Unable to find server for library " + library.getId() + ", the server responded with: \"" + serverMessage + "\"");
	}

	ServerDiscoveryException(LibraryId library) {
		super("Unable to find server for library " + library.getId());
	}
}
