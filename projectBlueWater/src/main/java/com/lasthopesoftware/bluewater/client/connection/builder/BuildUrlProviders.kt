package com.lasthopesoftware.bluewater.client.connection.builder

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.namehillsoftware.handoff.promises.Promise

interface BuildUrlProviders {
	fun promiseBuiltUrlProvider(libraryId: LibraryId): Promise<IUrlProvider?>
}
