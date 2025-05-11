package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideEditableFilePropertyDefinitions {
	fun promiseEditableFilePropertyDefinitions(libraryId: LibraryId): Promise<Set<EditableFilePropertyDefinition>>
}
