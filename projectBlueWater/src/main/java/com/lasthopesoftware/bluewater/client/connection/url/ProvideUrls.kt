package com.lasthopesoftware.bluewater.client.connection.url

import java.net.URL

interface ProvideUrls {
	fun getUrl(vararg params: String): String
	val baseUrl: URL
	val authCode: String?
	val certificateFingerprint: ByteArray?
}
