package com.lasthopesoftware.bluewater.client.servers.version

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideLibraryServerVersion {
	fun promiseServerVersion(libraryId: LibraryId): Promise<SemanticVersion?>
}
