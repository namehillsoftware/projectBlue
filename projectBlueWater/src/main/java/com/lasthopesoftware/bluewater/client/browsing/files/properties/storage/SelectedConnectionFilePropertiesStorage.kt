package com.lasthopesoftware.bluewater.client.browsing.files.properties.storage

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.namehillsoftware.handoff.promises.Promise

class SelectedConnectionFilePropertiesStorage(private val selectedConnectionProvider: ProvideSelectedConnection, private val innerConstructor: (IConnectionProvider) -> ScopedFilePropertiesStorage) : UpdateFileProperties {
	override fun promiseFileUpdate(serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit> =
		selectedConnectionProvider
			.promiseSessionConnection()
			.eventually { it?.let(innerConstructor)?.promiseFileUpdate(serviceFile, property, value, isFormatted) ?: Promise.empty() }
}
