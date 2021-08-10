package com.lasthopesoftware.bluewater.client.connection.authentication

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.StandardRequest
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

internal fun IConnectionProvider?.promiseIsReadOnly(): Promise<Boolean> =
	this?.promiseResponse("Authenticate")
			?.then { r ->
				r.body
					?.use { b -> b.byteStream().use(StandardRequest::fromInputStream) }
					?.let { sr -> sr.items["ReadOnly"]?.toInt() }
					?.let { ro -> ro != 0 }
					?: false
			}
			?: false.toPromise()
