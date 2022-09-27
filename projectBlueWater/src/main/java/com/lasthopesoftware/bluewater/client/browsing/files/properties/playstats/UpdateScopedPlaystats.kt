package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise

interface UpdateScopedPlaystats {
    fun promisePlaystatsUpdate(serviceFile: ServiceFile): Promise<*>
}
