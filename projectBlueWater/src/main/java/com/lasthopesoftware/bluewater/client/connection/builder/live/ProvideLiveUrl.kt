package com.lasthopesoftware.bluewater.client.connection.builder.live

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.namehillsoftware.handoff.promises.Promise

interface ProvideLiveUrl {
	fun promiseLiveUrl(libraryId: LibraryId): Promise<IUrlProvider?>
}
