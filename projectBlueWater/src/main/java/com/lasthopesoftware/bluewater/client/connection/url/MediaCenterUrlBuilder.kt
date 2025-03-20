package com.lasthopesoftware.bluewater.client.connection.url

import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import java.net.URL

object MediaCenterUrlBuilder {
	fun buildUrl(baseUrl: URL, path: String, vararg params: String): URL =
		baseUrl.addPath("/MCWS/v1/").addPath(path).addParams(*params)
}
