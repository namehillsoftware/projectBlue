package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAServiceFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusableTrackHeadlineViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
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

class WhenShowingTheMenu {

	private val recordingMessageBus = RecordingTypedMessageBus<ItemListMenuMessage>()

	private val viewModel by lazy {
		val filePropertiesProvider = mockk<ProvideScopedFileProperties>().apply {
			every { promiseFileProperties(ServiceFile(99)) } returns mapOf(
				Pair("Artist", "fool"),
				Pair("Name", "coin"),
			).toPromise()
		}

		val stringResource = mockk<GetStringResources>().apply {
			every { loading } returns "past"
			every { unknownArtist } returns "next"
			every { unknownTrack } returns "shout"
		}

		ReusableTrackHeadlineViewModel(
			filePropertiesProvider,
			mockk {
				every { promiseUrlKey(any<ServiceFile>()) } answers {
					Promise(
						UrlKeyHolder(URL("http://test"), firstArg())
					)
				}
			},
			stringResource,
			mockk(),
			mockk(),
			recordingMessageBus,
			RecordingApplicationMessageBus(),
		)
	}

	@BeforeAll
	fun act() {
		viewModel.promiseUpdate(
			listOf(
				ServiceFile(13),
				ServiceFile(546),
				ServiceFile(695),
				ServiceFile(801),
				ServiceFile(76),
				ServiceFile(551),
				ServiceFile(5),
				ServiceFile(99),
				ServiceFile(285),
				ServiceFile(357),
				ServiceFile(920),
			),
			7,
		).toExpiringFuture().get()
		viewModel.showMenu()
	}

	@Test
	fun `then a menu shown message is sent`() {
		assertThat(
			recordingMessageBus.recordedMessages.filterIsInstance<ItemListMenuMessage.MenuShown>()
				.map { it.menuItem }).containsOnlyOnce(viewModel)
	}

	@Test
	fun thenTheArtistIsCorrect() {
		assertThat(viewModel.artist.value)
			.isEqualTo("fool")
	}

	@Test
	fun thenTheTrackNameIsCorrect() {
		assertThat(viewModel.title.value)
			.isEqualTo("coin")
	}

	@Test
	fun `then the menu is shown`() {
		assertThat(viewModel.isMenuShown.value).isTrue
	}
}
