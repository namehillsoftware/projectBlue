package com.lasthopesoftware.bluewater.client.connection.builder.lookup

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.namehillsoftware.handoff.promises.Promise
import xmlwise.XmlElement

interface RequestServerInfoXml {
	fun promiseServerInfoXml(library: Library): Promise<XmlElement?>
}
