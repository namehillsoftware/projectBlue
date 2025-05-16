package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.live.eventuallyFromDataAccess
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class EditableFilePropertyDefinitionProvider(
	private val libraryConnections: ProvideLibraryConnections,
) : ProvideEditableFilePropertyDefinitions {
	override fun promiseEditableFilePropertyDefinitions(libraryId: LibraryId): Promise<Set<EditableFilePropertyDefinition>> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventuallyFromDataAccess { it?.promiseEditableFilePropertyDefinitions().keepPromise(emptySet()) }
}
