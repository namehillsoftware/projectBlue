package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveMediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withMcApi
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.lasthopesoftware.resources.emptyByteArray
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenSendingPlayedToServer {

	private var isFilePlayedCalled = false
	private val updater by lazy {
        LiveMediaCenterConnection(
			MediaCenterConnectionDetails(TestUrl),
			FakeHttpConnectionProvider(
				mockk {
					every { promiseResponse(TestUrl.withMcApi().addPath("Alive")) } returns PassThroughHttpResponse(
						200,
						"OK",
						("""<Response Status="OK">
							|<Item Name="RuntimeGUID">{7FF5918E-9FDE-4D4D-9AE7-62DFFDD64397}</Item>
							|<Item Name="LibraryVersion">24</Item>
							|<Item Name="ProgramName">JRiver Media Center</Item>
							|<Item Name="ProgramVersion">24</Item><Item Name="FriendlyName">Media-Pc</Item>
							|<Item Name="AccessKey">FWsPXC9GJkh</Item></Response>""".trimMargin()
							).encodeToByteArray().inputStream()
					).toPromise()

					every { promiseResponse(TestUrl.withMcApi().addPath("File/Played").addParams("File=15", "FileType=Key")) } answers {
						isFilePlayedCalled = true

						PassThroughHttpResponse(
							200,
							"OK",
							emptyByteArray.inputStream()
						).toPromise()
					}
				}
			),
			mockk(),
		)
	}

	@BeforeAll
	fun act() {
		updater.promisePlaystatsUpdate(ServiceFile("15")).toExpiringFuture().get()
	}

	@Test
	fun `then the file is updated`() {
		assertThat(isFilePlayedCalled).isTrue
	}
}
