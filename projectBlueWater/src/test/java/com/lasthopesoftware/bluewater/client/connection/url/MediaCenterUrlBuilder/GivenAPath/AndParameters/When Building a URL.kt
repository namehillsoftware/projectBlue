package com.lasthopesoftware.bluewater.client.connection.url.MediaCenterUrlBuilder.GivenAPath.AndParameters

import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withMcApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URL

class `When Building a URL` {
	private val url by lazy {
		URL("https://WjgZaOVUF1:422")
			.withMcApi()
			.addPath("zazHp0e")
			.addParams(
				"fddcLlX=O6a4YPvil",
				"4RN0bCo7y",
				"8r9LezlUEl=&+30eZwqjVPu0",
			)
	}

	@Test
	fun `then the URL is correct`() {
		assertThat(url.toString()).isEqualTo("https://WjgZaOVUF1:422/MCWS/v1/zazHp0e?fddcLlX=O6a4YPvil&4RN0bCo7y&8r9LezlUEl=%26%2B30eZwqjVPu0")
	}
}
