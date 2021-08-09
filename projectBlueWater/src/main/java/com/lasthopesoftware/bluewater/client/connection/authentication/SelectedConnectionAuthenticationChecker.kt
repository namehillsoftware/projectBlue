package com.lasthopesoftware.bluewater.client.connection.authentication

import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.lasthopesoftware.bluewater.shared.StandardRequest
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class SelectedConnectionAuthenticationChecker(private val selectedConnection: ProvideSelectedConnection) : CheckIfScopedConnectionIsReadOnly {
	override fun promiseIsReadOnly(): Promise<Boolean> =
		selectedConnection.promiseSessionConnection().eventually { c ->
			c?.promiseResponse("Authenticate")
				?.then { r ->
					r.body
						?.use { b -> b.byteStream().use(StandardRequest::fromInputStream) }
						?.let { sr -> sr.items["ReadOnly"]?.toInt() }
						?.let { ro -> ro != 0 }
						?: false
				}
				?: false.toPromise()
		}
}
