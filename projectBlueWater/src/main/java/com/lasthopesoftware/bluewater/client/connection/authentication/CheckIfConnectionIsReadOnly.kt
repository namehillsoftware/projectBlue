package com.lasthopesoftware.bluewater.client.connection.authentication

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface CheckIfConnectionIsReadOnly {
	fun promiseIsReadOnly(libraryId: LibraryId): Promise<Boolean>
}
