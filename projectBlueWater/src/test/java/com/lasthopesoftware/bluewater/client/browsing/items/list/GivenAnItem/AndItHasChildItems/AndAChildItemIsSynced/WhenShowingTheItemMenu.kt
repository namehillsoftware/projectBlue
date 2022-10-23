package com.lasthopesoftware.bluewater.client.browsing.items.list.AndItHasChildItems.AndAChildItemIsSynced

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 391
private const val rootItemId = 217
private const val childItemId = 637

class WhenShowingTheItemMenu {

	private val recordingMessageBus = RecordingTypedMessageBus<ItemListMenuMessage>()

	private val viewModel by lazy {
		val selectedLibraryIdProvider = mockk<ProvideSelectedLibraryId>().apply {
			every { promiseSelectedLibraryId() } returns LibraryId(libraryId).toPromise()
		}

		val itemProvider = mockk<ProvideItems>().apply {
			every { promiseItems(LibraryId(libraryId), ItemId(rootItemId)) } returns listOf(
				Item(55),
				Item(137),
				Item(766),
				Item(childItemId),
				Item(812),
			).toPromise()
		}

		val storedItemAccess = FakeStoredItemAccess(StoredItem(libraryId, childItemId, StoredItem.ItemType.ITEM))

		ItemListViewModel(
			selectedLibraryIdProvider,
			itemProvider,
			mockk(relaxed = true, relaxUnitFun = true),
			storedItemAccess,
			mockk(),
			mockk(),
			recordingMessageBus,
		)
	}

	@BeforeAll
	fun act() {
		viewModel.loadItem(Item(rootItemId, "leaf")).toExpiringFuture().get()
		viewModel.items.value[3].showMenu()
	}

	@Test
	fun `then a menu shown message is sent`() {
		assertThat(recordingMessageBus.recordedMessages.filterIsInstance<ItemListMenuMessage.MenuShown>()
				.map { it.menuItem }).containsOnlyOnce(viewModel.items.value[3])
	}

	@Test
	fun `then the child item is marked for sync`() {
		assertThat(viewModel.items.value[3].isSynced.value).isTrue
	}

	@Test
	fun `then the menu is shown`() {
		assertThat(viewModel.items.value[3].isMenuShown.value).isTrue
	}

	@Test
	fun `then the root item is NOT marked for sync`() {
		assertThat(viewModel.isSynced.value).isFalse
	}

	@Test
	fun `then the item value is correct`() {
		assertThat(viewModel.itemValue.value).isEqualTo("leaf")
	}

	@Test
	fun `then the view model is finished loading`() {
		assertThat(viewModel.isLoading.value).isFalse
	}

	@Test
	fun `then the loaded files are correct`() {
		assertThat(viewModel.items.value.map { it.item })
			.hasSameElementsAs(
				listOf(
					Item(55),
					Item(137),
					Item(766),
					Item(childItemId),
					Item(812),
				)
			)
	}
}
