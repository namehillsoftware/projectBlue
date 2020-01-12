package com.lasthopesoftware.bluewater.client.connection.builder.lookup;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

public interface LookupServers {
	Promise<ServerInfo> promiseServerInformation(Library library);
}
