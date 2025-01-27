package com.lasthopesoftware.bluewater.client.connection.authentication

import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.io.promiseStandardResponse
import com.namehillsoftware.handoff.promises.Promise

private val nullConnectionProviderPromise by lazy { false.toPromise() }

internal fun ProvideConnections?.promiseIsReadOnly(): Promise<Boolean> =
	this?.promiseResponse("Authenticate")
		?.promiseStandardResponse()
		?.then { sr ->
			sr.items["ReadOnly"]?.toInt()?.let { ro -> ro != 0 } ?: false
		}
		?: nullConnectionProviderPromise
