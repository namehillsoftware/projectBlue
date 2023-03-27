package com.lasthopesoftware.bluewater.client.connection.builder.lookup

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.HttpPromisedResponse
import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.Request
import xmlwise.XmlElement
import xmlwise.Xmlwise

class ServerInfoXmlRequest(private val libraryProvider: ILibraryProvider, private val clientFactory: ProvideOkHttpClients) : RequestServerInfoXml {
	override fun promiseServerInfoXml(libraryId: LibraryId): Promise<XmlElement?> = CancellableProxyPromise { cp ->
		libraryProvider.promiseLibrary(libraryId).eventually { library ->
			library
				?.run {
					Request.Builder()
						.url("https://webplay.jriver.com/libraryserver/lookup?id=$accessCode")
						.build()
				}
				?.let { request -> HttpPromisedResponse(clientFactory.getJriverCentralClient().newCall(request)).also(cp::doCancel) }
				?.then { r -> r.body?.use { b -> Xmlwise.createXml(b.string()) } }
				.keepPromise()
		}
	}
}
