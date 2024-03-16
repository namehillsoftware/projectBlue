package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAServiceFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusableFileViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusablePlaylistFileViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
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

private const val libraryId = 235

class WhenShowingTheMenu {

	private val recordingMessageBus = RecordingTypedMessageBus<ItemListMenuMessage>()

	private val viewModel by lazy {
		val filePropertiesProvider = mockk<ProvideLibraryFileProperties>().apply {
			every { promiseFileProperties(LibraryId(libraryId), ServiceFile(99)) } returns mapOf(
				Pair("Artist", "fool"),
				Pair("Name", "coin"),
			).toPromise()
		}

		val stringResource = mockk<GetStringResources>().apply {
			every { loading } returns "past"
			every { unknownArtist } returns "next"
			every { unknownTrack } returns "shout"
		}

		ReusablePlaylistFileViewModel(
			recordingMessageBus,
			ReusableFileViewModel(
				filePropertiesProvider,
				stringResource,
				mockk {
					every { promiseUrlKey(LibraryId(libraryId), any<ServiceFile>()) } answers {
						Promise(
							UrlKeyHolder(URL("http://test"), arg(1))
						)
					}
				},
				RecordingApplicationMessageBus(),
			),
		)
	}

	@BeforeAll
	fun act() {
		viewModel.promiseUpdate(LibraryId(libraryId), ServiceFile(99)).toExpiringFuture().get()
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
