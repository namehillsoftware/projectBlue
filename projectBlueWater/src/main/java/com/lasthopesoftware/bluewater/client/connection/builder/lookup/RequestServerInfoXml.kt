package com.lasthopesoftware.bluewater.client.connection.builder.lookup

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise
import org.jsoup.nodes.Document

interface RequestServerInfoXml {
	fun promiseServerInfoXml(libraryId: LibraryId): Promise<Document?>
}
