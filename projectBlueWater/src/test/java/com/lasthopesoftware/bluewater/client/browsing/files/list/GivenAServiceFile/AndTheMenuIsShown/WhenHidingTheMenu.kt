package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAServiceFile.AndTheMenuIsShown

import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusableTrackHeadlineViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import com.lasthopesoftware.resources.strings.GetStringResources
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenHidingTheMenu {

	private val recordingMessageBus = RecordingTypedMessageBus<ItemListMenuMessage>()

	private val viewModel by lazy {
		val stringResource = mockk<GetStringResources>().apply {
			every { loading } returns "past"
			every { unknownArtist } returns "next"
			every { unknownTrack } returns "shout"
		}

		ReusableTrackHeadlineViewModel(
			mockk(),
			stringResource,
			mockk(),
			mockk(),
			recordingMessageBus,
		)
	}

	private var wasMenuShown = false
	private var wasMenuHidden = false

	@BeforeAll
	fun act() {
		wasMenuShown = viewModel.showMenu()
		wasMenuHidden = viewModel.hideMenu()
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
	fun `then the menu was hidden`() {
		assertThat(wasMenuHidden).isTrue
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
}