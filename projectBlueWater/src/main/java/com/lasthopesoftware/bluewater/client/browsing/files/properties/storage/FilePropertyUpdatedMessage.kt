package com.lasthopesoftware.bluewater.client.browsing.files.properties.storage

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

data class FilePropertyUpdatedMessage(
	val libraryId: LibraryId,
	val serviceFile: ServiceFile,
) : ApplicationMessage
