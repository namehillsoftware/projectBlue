package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.namehillsoftware.handoff.promises.Promise

class SelectedConnectionFilePropertiesStorage(private val selectedConnectionProvider: SelectedConnectionProvider, private val innerConstructor: (IConnectionProvider) -> ScopedFilePropertiesStorage) : UpdateFileProperties {
	private val lazyInner = lazy { selectedConnectionProvider.promiseSessionConnection().then { it?.let(innerConstructor) } }

	override fun promiseFileUpdate(serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit> =
		lazyInner.value.eventually { it?.promiseFileUpdate(serviceFile, property, value, isFormatted) }
}
