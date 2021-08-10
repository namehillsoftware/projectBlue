package com.lasthopesoftware.bluewater.client.connection.authentication

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.StandardRequest
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class ScopedConnectionAuthenticationChecker(private val connectionProvider: IConnectionProvider) : CheckIfScopedConnectionIsReadOnly {
	override fun promiseIsReadOnly(): Promise<Boolean> = connectionProvider.promiseResponse("Authenticate")
		.then { r ->
			r.body
				?.use { b -> b.byteStream().use(StandardRequest::fromInputStream) }
				?.let { sr -> sr.items["ReadOnly"]?.toInt() }
				?.let { ro -> ro != 0 }
				?: false
		}
		?: false.toPromise()
}
