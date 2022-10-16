package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.net.URL

class PassThroughScopedUrlKeyProvider(private val url: URL) : ProvideScopedUrlKey {
	override fun <Key> promiseUrlKey(key: Key): Promise<UrlKeyHolder<Key>?> = UrlKeyHolder(url, key).toPromise()
}
