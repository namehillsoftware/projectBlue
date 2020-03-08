package com.lasthopesoftware.bluewater.client.connection.builder.lookup

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise
import xmlwise.XmlElement

interface RequestServerInfoXml {
	fun promiseServerInfoXml(libraryId: LibraryId): Promise<XmlElement?>
}
