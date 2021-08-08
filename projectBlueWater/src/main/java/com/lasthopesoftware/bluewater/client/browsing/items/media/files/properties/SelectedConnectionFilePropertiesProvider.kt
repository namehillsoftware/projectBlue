package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.namehillsoftware.handoff.promises.Promise

class SelectedConnectionFilePropertiesProvider(private val selectedConnection: ProvideSelectedConnection, private val innerFactory: (IConnectionProvider) -> ProvideScopedFileProperties) : ProvideScopedFileProperties {
	private val lazyInner = lazy { selectedConnection.promiseSessionConnection().then { c -> c?.let(innerFactory) } }

	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>> =
		lazyInner.value.eventually { it?.promiseFileProperties(serviceFile) }
}
