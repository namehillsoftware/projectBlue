package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.namehillsoftware.handoff.promises.Promise

class SelectedConnectionFilePropertiesProvider(private val selectedConnection: ProvideSelectedConnection, private val innerFactory: (IConnectionProvider) -> ProvideScopedFileProperties) : ProvideScopedFileProperties {
	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>> =
		selectedConnection.promiseSessionConnection()
			.eventually { c -> c?.let(innerFactory)?.promiseFileProperties(serviceFile) }
}
