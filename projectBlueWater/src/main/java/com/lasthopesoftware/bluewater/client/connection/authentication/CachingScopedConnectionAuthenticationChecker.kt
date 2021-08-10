package com.lasthopesoftware.bluewater.client.connection.authentication

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

class CachingScopedConnectionAuthenticationChecker(private val connectionProvider: IConnectionProvider, private val inner: CheckIfScopedConnectionIsReadOnly) :
	CheckIfScopedConnectionIsReadOnly {

	companion object {
		private val cachedConnectionResults = ConcurrentHashMap<UrlKeyHolder<String?>, Boolean>()
	}

	override fun promiseIsReadOnly(): Promise<Boolean> =
		connectionProvider.urlProvider.baseUrl
			?.let { url -> UrlKeyHolder(url, connectionProvider.urlProvider.authCode) }
			?.let { key ->
				cachedConnectionResults[key]?.toPromise()
					?: inner.promiseIsReadOnly().then { isReadOnly ->
						cachedConnectionResults[key] = isReadOnly
						isReadOnly
					}
			} ?: false.toPromise()
}
