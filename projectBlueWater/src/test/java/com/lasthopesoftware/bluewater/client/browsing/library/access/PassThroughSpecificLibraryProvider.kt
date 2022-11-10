package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class PassThroughSpecificLibraryProvider(private val _library: Library) : ISpecificLibraryProvider {
    override fun promiseLibrary(): Promise<Library?> = _library.toPromise()
}
