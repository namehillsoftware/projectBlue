package com.lasthopesoftware.bluewater.client.connection.builder

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.namehillsoftware.handoff.promises.Promise

interface BuildUrlProviders {
	fun promiseBuiltUrlProvider(library: Library): Promise<IUrlProvider?>
}
