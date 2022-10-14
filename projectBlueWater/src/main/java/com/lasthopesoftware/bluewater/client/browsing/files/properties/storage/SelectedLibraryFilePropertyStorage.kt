package com.lasthopesoftware.bluewater.client.browsing.files.properties.storage

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class SelectedLibraryFilePropertyStorage(
	private val selectedLibraryIdProvider: ProvideSelectedLibraryId,
	private val updateFileProperties: UpdateFileProperties
) : UpdateScopedFileProperties {
	override fun promiseFileUpdate(serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit> =
		selectedLibraryIdProvider
			.promiseSelectedLibraryId()
			.eventually { it?.let { l -> updateFileProperties.promiseFileUpdate(l, serviceFile, property, value, isFormatted) }.keepPromise(Unit) }
}
