package com.lasthopesoftware.bluewater.client.connection.builder.lookup

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.HttpPromisedResponse
import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser

class ServerInfoXmlRequest(private val libraryProvider: ILibraryProvider, private val clientFactory: ProvideOkHttpClients) : RequestServerInfoXml {
	override fun promiseServerInfoXml(libraryId: LibraryId): Promise<Document?> = Promise.Proxy { cp ->
		libraryProvider.promiseLibrary(libraryId).also(cp::doCancel).eventually { library ->
			library
				?.run {
					Request.Builder()
						.url("https://webplay.jriver.com/libraryserver/lookup?id=$accessCode")
						.build()
				}
				?.let { request -> HttpPromisedResponse(clientFactory.getJriverCentralClient().newCall(request)).also(cp::doCancel) }
				?.then { r -> r.body.use { b -> Jsoup.parse(b.string(), Parser.xmlParser()) } }
				.keepPromise()
		}
	}
}
