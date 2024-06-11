package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.net.URL

class PassThroughUrlKeyProvider(private val url: URL): ProvideUrlKey {
	override fun <Key> promiseUrlKey(libraryId: LibraryId, key: Key): Promise<UrlKeyHolder<Key>?> =
		UrlKeyHolder(url, key).toPromise()

	override fun <Key> promiseGuaranteedUrlKey(libraryId: LibraryId, key: Key): Promise<UrlKeyHolder<Key>> =
		UrlKeyHolder(url, key).toPromise()
}
