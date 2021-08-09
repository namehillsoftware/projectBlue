package com.lasthopesoftware.bluewater.client.servers.version;

import com.namehillsoftware.handoff.promises.Promise;

public interface IProgramVersionProvider {
	Promise<SemanticVersion> promiseServerVersion();
}
