package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.namehillsoftware.handoff.promises.Promise

class SelectedConnectionFilePropertiesStorage(private val selectedConnectionProvider: SelectedConnectionProvider, private val innerConstructor: (IConnectionProvider) -> ScopedFilePropertiesStorage) : UpdateFileProperties {
	override fun promiseFileUpdate(serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit> =
		selectedConnectionProvider
			.promiseSessionConnection()
			.eventually { it?.let(innerConstructor)?.promiseFileUpdate(serviceFile, property, value, isFormatted) ?: Promise.empty() }
}
