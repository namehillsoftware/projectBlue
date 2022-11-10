package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.namehillsoftware.handoff.promises.Promise

interface ISpecificLibraryProvider {
	fun promiseLibrary(): Promise<Library?>
}
