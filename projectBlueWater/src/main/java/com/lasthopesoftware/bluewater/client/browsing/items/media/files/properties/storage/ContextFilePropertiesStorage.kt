package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.Companion.promiseSelectedConnection
import com.namehillsoftware.handoff.promises.Promise

class ContextFilePropertiesStorage(private val context: Context, private val innerConstructor: (IConnectionProvider) -> ScopedFilePropertiesStorage) : UpdateFileProperties {
	private val lazyInner = lazy { context.promiseSelectedConnection().then { it?.let(innerConstructor) } }

	override fun promiseFileUpdate(serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit> =
		lazyInner.value.eventually { it?.promiseFileUpdate(serviceFile, property, value, isFormatted) }
}
