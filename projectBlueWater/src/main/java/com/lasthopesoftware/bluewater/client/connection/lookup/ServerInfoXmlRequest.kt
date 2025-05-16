package com.lasthopesoftware.bluewater.client.connection.lookup

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.requests.ProvideHttpPromiseClients
import com.lasthopesoftware.bluewater.client.connection.settings.LookupValidConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.resources.io.promiseStringBody
import com.lasthopesoftware.resources.io.promiseXmlDocument
import com.namehillsoftware.handoff.promises.Promise
import org.jsoup.nodes.Document
import java.net.URL

class ServerInfoXmlRequest(
	private val connectionSettings: LookupValidConnectionSettings,
	private val clientFactory: ProvideHttpPromiseClients
) : RequestMediaCenterServerInfoXml {
	override fun promiseServerInfoXml(libraryId: LibraryId): Promise<Document?> = Promise.Proxy { cp ->
		connectionSettings
			.promiseConnectionSettings(libraryId)
			.also(cp::doCancel)
			.eventually { library ->
				library
					?.let { it as? MediaCenterConnectionSettings }
					?.run { clientFactory.getClient().promiseResponse(URL("https://webplay.jriver.com/libraryserver/lookup?id=$accessCode")) }
					?.also(cp::doCancel)
					?.promiseStringBody()
					?.also(cp::doCancel)
					?.promiseXmlDocument()
					.keepPromise()
			}
	}
}
