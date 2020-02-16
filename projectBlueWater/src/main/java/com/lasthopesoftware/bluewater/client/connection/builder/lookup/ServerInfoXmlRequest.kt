package com.lasthopesoftware.bluewater.client.connection.builder.lookup

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.connection.HttpPromisedResponse
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.OkHttpClient
import okhttp3.Request
import xmlwise.XmlElement
import xmlwise.Xmlwise

class ServerInfoXmlRequest(private val client: OkHttpClient) : RequestServerInfoXml {
	override fun promiseServerInfoXml(library: Library): Promise<XmlElement?> {
		val request = Request.Builder()
			.url("https://webplay.jriver.com/libraryserver/lookup?id=" + library.accessCode)
			.build()

		return HttpPromisedResponse(client.newCall(request))
			.then {	r ->
				r.body?.use { b ->
					Xmlwise.createXml(b.string())
				}
			}
	}
}
