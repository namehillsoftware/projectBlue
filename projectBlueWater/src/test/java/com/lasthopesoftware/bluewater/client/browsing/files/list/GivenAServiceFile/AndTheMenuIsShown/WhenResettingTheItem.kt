package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAServiceFile.AndTheMenuIsShown

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusableFileViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusablePlaylistFileViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
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

class WhenResettingTheItem {

	private val recordingMessageBus = RecordingTypedMessageBus<ItemListMenuMessage>()

	private val viewModel by lazy {
		val stringResource = mockk<GetStringResources>().apply {
			every { loading } returns "past"
			every { unknownArtist } returns "next"
			every { unknownTrack } returns "shout"
		}

		ReusablePlaylistFileViewModel(
			recordingMessageBus,
			ReusableFileViewModel(
				mockk(),
				stringResource,
				mockk {
					every { promiseUrlKey(any(), any<ServiceFile>()) } answers {
						Promise(
							UrlKeyHolder(URL("http://test"), firstArg())
						)
					}
				},
				RecordingApplicationMessageBus(),
			)
		)
	}

	private var wasMenuShown = false

	@BeforeAll
	fun act() {
		wasMenuShown = viewModel.showMenu()
		viewModel.reset()
	}

	@Test
	fun `then the menu is hidden`() {
		assertThat(viewModel.isMenuShown.value).isFalse
	}

	@Test
	fun `then the menu was shown`() {
		assertThat(wasMenuShown).isTrue
	}

	@Test
	fun `then a menu shown message is sent`() {
		assertThat(
			recordingMessageBus.recordedMessages.filterIsInstance<ItemListMenuMessage.MenuShown>()
				.map { it.menuItem }).containsOnlyOnce(viewModel)
	}

	@Test
	fun `then a menu hidden message is sent`() {
		assertThat(
			recordingMessageBus.recordedMessages.filterIsInstance<ItemListMenuMessage.MenuHidden>()
				.map { it.menuItem }).containsOnlyOnce(viewModel)
	}

	@Test
	fun `then title is correct`() {
		assertThat(viewModel.title.value).isEqualTo("past")
	}

	@Test
	fun `then artist is correct`() {
		assertThat(viewModel.artist.value).isEqualTo("")
	}
}
