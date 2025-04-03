package com.lasthopesoftware.bluewater.client.connection.url.MediaCenterUrlBuilder.GivenAPath

import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withMcApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URL

class `When Building a URL` {
	private val url by lazy {
		TestUrl.withMcApi().addPath("CpKPr73LwAn/RLbudgaL")
	}

	@Test
	fun `then the URL is correct`() {
		assertThat(url).isEqualTo(URL(TestMcwsUrl, "CpKPr73LwAn/RLbudgaL"))
	}
}
