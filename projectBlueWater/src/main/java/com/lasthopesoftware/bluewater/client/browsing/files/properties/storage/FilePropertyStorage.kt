package com.lasthopesoftware.bluewater.client.browsing.files.properties.storage

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.namehillsoftware.handoff.promises.Promise

class FilePropertyStorage(
	private val libraryConnections: ProvideLibraryConnections,
	private val checkIfConnectionIsReadOnly: CheckIfConnectionIsReadOnly,
	private val checkRevisions: CheckRevisions,
	private val filePropertiesContainerRepository: IFilePropertiesContainerRepository,
) : UpdateFileProperties {
	override fun promiseFileUpdate(libraryId: LibraryId, serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit> {
		TODO("Not yet implemented")
	}
}