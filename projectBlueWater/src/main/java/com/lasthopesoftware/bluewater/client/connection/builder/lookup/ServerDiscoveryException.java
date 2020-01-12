package com.lasthopesoftware.bluewater.client.connection.builder.lookup;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;

public class ServerDiscoveryException extends Exception {

	ServerDiscoveryException(Library library, String serverMessage) {
		super("Unable to find server for key " + library.getAccessCode() + ", the server responded with: \"" + serverMessage + "\"");
	}

	ServerDiscoveryException(Library library) {
		super("Unable to find server for key " + library.getAccessCode());
	}
}
