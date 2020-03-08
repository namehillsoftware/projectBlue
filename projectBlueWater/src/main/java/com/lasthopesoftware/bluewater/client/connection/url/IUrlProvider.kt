package com.lasthopesoftware.bluewater.client.connection.url

/**
 * Created by david on 1/14/16.
 */
interface IUrlProvider {
	fun getUrl(vararg params: String): String?
	val baseUrl: String?
	val authCode: String?
	val certificateFingerprint: ByteArray?
}
