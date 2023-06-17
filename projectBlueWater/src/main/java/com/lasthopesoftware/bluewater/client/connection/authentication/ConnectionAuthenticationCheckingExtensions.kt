package com.lasthopesoftware.bluewater.client.connection.authentication

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.StandardResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

private val nullConnectionProviderPromise by lazy { false.toPromise() }

internal fun IConnectionProvider?.promiseIsReadOnly(): Promise<Boolean> =
	this?.promiseResponse("Authenticate")
			?.then { r ->
				r.body
					?.use { b -> b.byteStream().use(StandardResponse::fromInputStream) }
					?.let { sr -> sr.items["ReadOnly"]?.toInt() }
					?.let { ro -> ro != 0 }
					?: false
			}
			?: nullConnectionProviderPromise
