package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndItsVersionIsLessThan21.AndAFileWithLastPlayedBeforeNowAndSongsDuration.AndNumberPlaysIsPresent

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.buildFilePropertiesXml
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
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
import org.joda.time.DateTime
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL
import java.net.URLDecoder

class WhenStoringTheUpdatedPlayStats {

	companion object {
		private const val serviceFileId = "23"
	}

	private val services by lazy {
		val duration = Duration.standardMinutes(5).millis
		originalLastPlayed = Duration.millis(DateTime.now().minus(Duration.standardDays(10)).millis).standardSeconds
		val fileProperties = mutableMapOf(
			Pair(KnownFileProperties.LastPlayed, originalLastPlayed.toString()),
			Pair(KnownFileProperties.NumberPlays, 52.toString()),
			Pair(KnownFileProperties.Duration, duration.toString()),
		)

		val connection = LiveMediaCenterConnection(
			MediaCenterConnectionDetails(TestUrl),
			FakeHttpConnectionProvider(
				mockk {
					every { promiseResponse(TestUrl.withMcApi().addPath("File/GetInfo").addParams("File=${serviceFileId}")) } returns PassThroughHttpResponse(
						200,
						"test",
						buildFilePropertiesXml(ServiceFile(serviceFileId), fileProperties).toByteArray().inputStream()
					).toPromise()

					every { promiseResponse(TestUrl.withMcApi().addPath("Alive")) } returns PassThroughHttpResponse(
						200,
						"OK",
						("""<Response Status="OK">
							|<Item Name="RuntimeGUID">{7FF5918E-9FDE-4D4D-9AE7-62DFFDD64397}</Item>
							|<Item Name="LibraryVersion">24</Item>
							|<Item Name="ProgramName">JRiver Media Center</Item>
							|<Item Name="ProgramVersion">20</Item><Item Name="FriendlyName">Media-Pc</Item>
							|<Item Name="AccessKey">FWsPXC9GJkh</Item></Response>""".trimMargin()
							).encodeToByteArray().inputStream()
					).toPromise()

					every { promiseResponse(match { arg -> arg.path.endsWith("File/SetInfo") && arg.query.contains("File=${serviceFileId}") }) } answers {
						val url = firstArg<URL>()

						val queryParams = url.query.split("&")

						val field = queryParams.first { it.startsWith("Field=") }.substringAfter("Field=")
						val value = queryParams.first { it.startsWith("Value=") }.substringAfter("Value=")

						fileProperties[URLDecoder.decode(field, "UTF-8")] = value

						PassThroughHttpResponse(
							200,
							"Ok",
							emptyByteArray.inputStream()
						).toPromise()
					}
				}
			),
			mockk(),
		)

		Pair(
			connection,
			fileProperties,
		)
	}

	private var fileProperties: Map<String, String>? = null
	private var originalLastPlayed: Long = 0

	@BeforeAll
	fun before() {
		val (filePropertiesPlayStatsUpdater, sessionFileProperties) = services
		fileProperties = sessionFileProperties
		filePropertiesPlayStatsUpdater
			.promisePlaystatsUpdate(ServiceFile(serviceFileId))
			.toExpiringFuture()
			.get()
	}

	@Test
	fun thenTheLastPlayedIsRecent() {
		assertThat(fileProperties!![KnownFileProperties.LastPlayed]?.toLong()).isGreaterThan(originalLastPlayed)
	}

	@Test
	fun thenTheNumberPlaysIsIncremented() {
		assertThat(fileProperties!![KnownFileProperties.NumberPlays]).isEqualTo("53")
	}
}
