package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class ScopedUrlKeyProvider(private val connectionProvider: IConnectionProvider) : ProvideScopedUrlKey {
	override fun <Key> promiseUrlKey(key: Key): Promise<UrlKeyHolder<Key>?> =
		connectionProvider.urlProvider.baseUrl?.let { UrlKeyHolder(it, key) }?.toPromise().keepPromise()
}
