package com.lasthopesoftware.bluewater.client.connection.url.MediaCenterUrlBuilder.GivenAPath

import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.connection.url.MediaCenterUrlBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URL

class `When Building a URL` {
	private val url by lazy {
		MediaCenterUrlBuilder.buildUrl(
			TestUrl,
			"CpKPr73LwAn/RLbudgaL"
		)
	}

	@Test
	fun `then the URL is correct`() {
		assertThat(url).isEqualTo(URL(TestMcwsUrl, "CpKPr73LwAn/RLbudgaL"))
	}
}
