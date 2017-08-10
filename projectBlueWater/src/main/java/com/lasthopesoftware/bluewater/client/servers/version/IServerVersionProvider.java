package com.lasthopesoftware.bluewater.client.servers.version;

import com.lasthopesoftware.messenger.promises.Promise;

public interface IServerVersionProvider {
	Promise<ProgramVersion> promiseServerVersion();
}
