package com.lasthopesoftware.bluewater.client.connection.authentication

import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.shared.StandardResponse
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

private val nullConnectionProviderPromise by lazy { false.toPromise() }

internal fun ProvideConnections?.promiseIsReadOnly(): Promise<Boolean> =
	this?.promiseResponse("Authenticate")
			?.then { r ->
				r.body
					.use { b -> b.byteStream().use(StandardResponse::fromInputStream) }
					?.let { sr -> sr.items["ReadOnly"]?.toInt() }
					?.let { ro -> ro != 0 }
					?: false
			}
			?: nullConnectionProviderPromise
