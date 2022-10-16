package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.namehillsoftware.handoff.promises.Promise

interface ProvideScopedUrlKey {
	fun <Key> promiseUrlKey(key: Key): Promise<UrlKeyHolder<Key>?>
}
