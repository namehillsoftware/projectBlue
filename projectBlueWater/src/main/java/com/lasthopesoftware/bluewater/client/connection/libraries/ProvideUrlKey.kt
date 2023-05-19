package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.namehillsoftware.handoff.promises.Promise

interface ProvideUrlKey {
	fun <Key> promiseUrlKey(libraryId: LibraryId, key: Key): Promise<UrlKeyHolder<Key>?>

	fun <Key> promiseGuaranteedUrlKey(libraryId: LibraryId, key: Key): Promise<UrlKeyHolder<Key>>
}
