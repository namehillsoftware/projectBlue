package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAServiceFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusableTrackHeadlineViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

private const val serviceFileId = 889

class WhenItsPropertiesChange {

	private val recordingMessageBus = RecordingTypedMessageBus<ItemListMenuMessage>()

	private val recordingApplicationMessageBus = RecordingApplicationMessageBus()

	private val viewModel by lazy {
		val filePropertiesProvider = mockk<ProvideScopedFileProperties>().apply {
			every { promiseFileProperties(ServiceFile(serviceFileId)) } returns mapOf(
				Pair("Artist", "give"),
				Pair("Name", "although"),
			).toPromise() andThen mapOf(
				Pair("Artist", "pupil"),
				Pair("Name", "classify"),
			).toPromise()
		}

		val stringResource = mockk<GetStringResources>().apply {
			every { loading } returns "adoption"
			every { unknownArtist } returns "sudden"
			every { unknownTrack } returns "dead"
		}

		ReusableTrackHeadlineViewModel(
			filePropertiesProvider,
			mockk {
				every { promiseUrlKey(any<ServiceFile>()) } answers {
					Promise(
						UrlKeyHolder(URL("http://maybe"), firstArg())
					)
				}
			},
			stringResource,
			mockk(),
			mockk(),
			recordingMessageBus,
			recordingApplicationMessageBus,
		)
	}

	@BeforeAll
	fun act() {
		viewModel.promiseUpdate(
			listOf(
				ServiceFile(13),
				ServiceFile(546),
				ServiceFile(serviceFileId),
				ServiceFile(801),
				ServiceFile(76),
				ServiceFile(551),
				ServiceFile(5),
				ServiceFile(99),
				ServiceFile(285),
				ServiceFile(357),
				ServiceFile(920),
			),
			2,
		).toExpiringFuture().get()
		recordingApplicationMessageBus.sendMessage(
			FilePropertiesUpdatedMessage(
				UrlKeyHolder(
					URL("http://maybe"),
					ServiceFile(serviceFileId)
				)
			)
		)
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(viewModel.artist.value).isEqualTo("pupil")
	}

	@Test
	fun `then the track name is correct`() {
		assertThat(viewModel.title.value).isEqualTo("classify")
	}

	@Test
	fun `then the menu is not shown`() {
		assertThat(viewModel.isMenuShown.value).isFalse
	}
}