package com.lasthopesoftware.bluewater.client.browsing.items.list.GivenALibrary.AndAnItem.AndItHasChildItems.AndAChildMenuIsShown

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeStoredItemAccess
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 703
private const val rootItemId = 707
private const val childItemId = 571

class WhenHidingTheMenu {

	private val recordingMessageBus = RecordingTypedMessageBus<ItemListMenuMessage>()

	private val viewModel by lazy {
		val itemProvider = mockk<ProvideItems>().apply {
			every { promiseItems(LibraryId(libraryId), ItemId(rootItemId)) } returns listOf(
				Item(482),
				Item(449),
				Item(160),
				Item(214),
				Item(childItemId),
			).toPromise()
		}

		val storedItemAccess = FakeStoredItemAccess()

		val viewModel = ItemListViewModel(
			itemProvider,
			mockk(relaxed = true, relaxUnitFun = true),
			mockk(),
			storedItemAccess,
			recordingMessageBus
		)

		viewModel.loadItem(LibraryId(libraryId), Item(rootItemId, "leaf")).toExpiringFuture().get()
		viewModel.items.value[4]
	}

	private var didMenuShow = false
	private var isMenuHidden = false

	@BeforeAll
	fun act() {
		didMenuShow = viewModel.showMenu()
		isMenuHidden = viewModel.hideMenu()
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
	fun `then the menu did show`() {
		assertThat(didMenuShow).isTrue
	}

	@Test
	fun `then it is indicated that the menu was hidden`() {
		assertThat(isMenuHidden).isTrue
	}

	@Test
	fun `then the menu is not shown`() {
		assertThat(viewModel.isMenuShown.value).isFalse
	}
}
