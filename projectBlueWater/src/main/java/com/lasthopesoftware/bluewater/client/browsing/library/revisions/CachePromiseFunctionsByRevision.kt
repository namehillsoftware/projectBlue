package com.lasthopesoftware.bluewater.client.browsing.library.revisions

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface CachePromiseFunctionsByRevision<Input: Any, Output> {
	fun getOrAdd(libraryId: LibraryId, input: Input, factory: (Input) -> Promise<Output>): Promise<Output>
}
