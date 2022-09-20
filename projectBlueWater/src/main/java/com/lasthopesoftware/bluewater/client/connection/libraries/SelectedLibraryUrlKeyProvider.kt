package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class SelectedLibraryUrlKeyProvider(
	private val selectedLibraryId: ProvideSelectedLibraryId,
	private val urlKey: ProvideUrlKey,
)
	: ProvideScopedUrlKeyProvider {
	override fun <Key> promiseUrlKey(key: Key): Promise<UrlKeyHolder<Key>?> =
		selectedLibraryId
			.promiseSelectedLibraryId()
			.eventually { it?.let { urlKey.promiseUrlKey(it, key) }.keepPromise() }
}
