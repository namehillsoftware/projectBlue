package com.lasthopesoftware.bluewater.client.connection.url

import java.net.URL

/**
 * Created by david on 1/14/16.
 */
interface IUrlProvider {
	fun getUrl(vararg params: String): String?
	val baseUrl: URL?
	val authCode: String?
	val certificateFingerprint: ByteArray?
}
