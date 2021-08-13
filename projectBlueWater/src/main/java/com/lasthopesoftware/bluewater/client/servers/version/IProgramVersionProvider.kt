package com.lasthopesoftware.bluewater.client.servers.version

import com.namehillsoftware.handoff.promises.Promise

interface IProgramVersionProvider {
    fun promiseServerVersion(): Promise<SemanticVersion?>
}
