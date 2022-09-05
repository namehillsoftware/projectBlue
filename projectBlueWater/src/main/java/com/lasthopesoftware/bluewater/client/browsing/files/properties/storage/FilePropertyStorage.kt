package com.lasthopesoftware.bluewater.client.browsing.files.properties.storage

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfScopedConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.namehillsoftware.handoff.promises.Promise

class FilePropertyStorage(
	private val libraryConnections: ProvideLibraryConnections,
	private val checkIfScopedConnectionIsReadOnly: CheckIfScopedConnectionIsReadOnly
) : UpdateFileProperties {
	override fun promiseFileUpdate(libraryId: LibraryId, serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit> {
		TODO("Not yet implemented")
	}
}
