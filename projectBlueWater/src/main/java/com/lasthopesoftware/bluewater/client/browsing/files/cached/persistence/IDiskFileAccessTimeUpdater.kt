package com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence

import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.namehillsoftware.handoff.promises.Promise

interface IDiskFileAccessTimeUpdater {
    fun promiseFileAccessedUpdate(cachedFile: CachedFile): Promise<CachedFile>
}
